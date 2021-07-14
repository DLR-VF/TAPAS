package de.dlr.ivf.tapas.persistence.db;

import com.lmax.disruptor.*;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.execution.sequential.io.TPS_WritableTrip;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import org.postgresql.core.BaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This writer should run in a separate thread. It uses a modified version of "copy from stdin"
 * of the {@link org.postgresql.copy.CopyManager} by reading byte arrays directly from a {@link RingBuffer}.
 * It is set up for a multiple producer scenario. (eg. the workers that are handling state machine transitions and need to write trips)
 *
 * A simulation progress update task is also implemented which updates the written trip count inside the database.*
 *
 */

public class TPS_PipedDbWriter implements Runnable, TPS_TripWriter {

    private long total_trip_count;

    private AtomicInteger registered_trips = new AtomicInteger(0);
    private AtomicInteger written_trips = new AtomicInteger(0);
    private int buffer_size;

    private Connection connection;
    private TPS_CopyManager copy_manager;
    private TPS_DB_IOManager pm;

    private RingBuffer<TPS_WritableTripEvent> ring_buffer;

    private ScheduledExecutorService update_task;

    private String copy_string;

    /**
     *
     * @param pm the persistence manager
     * @param total_trip_count expected trip to be written
     * @param buffer_size original size of the {@link RingBuffer}. Must be a power of 2!
     */

    public TPS_PipedDbWriter(TPS_PersistenceManager pm, long total_trip_count, int buffer_size){

        this.pm = (TPS_DB_IOManager) pm;
        this.buffer_size = buffer_size;
        try {
            this.connection = this.pm.getDbConnector().getConnection(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        char csv_delimiter = ';';
        this.copy_string = String.format("COPY %s FROM STDIN (FORMAT TEXT, ENCODING 'UTF-8', DELIMITER '"+ csv_delimiter +"', HEADER false)",pm.getParameters().getString(ParamString.DB_TABLE_TRIPS));

        this.total_trip_count = total_trip_count;
        init();
    }

    @Override
    public void run() {
        try {

            //start the pipe and block
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Opening database pipeline");
            copy_manager.copyIn(copy_string,this.ring_buffer,written_trips);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Closing database pipeline with "+ registered_trips.get()+" written trips.");

            String query = "UPDATE "+this.pm.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS)+" SET sim_finished = true, sim_progress = "+total_trip_count+", sim_total = "+ total_trip_count +", timestamp_finished = now() WHERE sim_key = '"+this.pm.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
            pm.execute(query);

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Closing the writer...");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the {@link TPS_CopyManager} and sets up the {@link RingBuffer}
     */
    private void init(){
        try {
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO,"Setting up the copy manager...");
            this.copy_manager = new TPS_CopyManager((BaseConnection) this.connection);

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO,"Setting up the persistence disruptor...");

            EventFactory<TPS_WritableTripEvent> ef = TPS_WritableTripEvent::new;
            this.ring_buffer = RingBuffer.createMultiProducer(ef, buffer_size, new BlockingWaitStrategy());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Puts a {@link TPS_WritableTrip} onto the {@link RingBuffer}. This method will block when the {@link RingBuffer} is at full capacity
     * @param trip the trip to write
     */
    public void writeTrip(TPS_WritableTrip trip) {

        long sequenceId = ring_buffer.next();
        TPS_WritableTripEvent event = ring_buffer.get(sequenceId);
        event.setTripByteArray(trip);
        ring_buffer.publish(sequenceId);
        registered_trips.getAndIncrement();
    }

    /**
     * Shuts down the simulation update task and puts an empty byte array onto the {@link RingBuffer} which will when consumed close the database pipeline
     */
    public void finish(){
        this.update_task.shutdownNow();

        //put an empty byte array onto the ring to signal the copy manager that the transaction is over
        long sequenceId = ring_buffer.next();
        TPS_WritableTripEvent event = ring_buffer.get(sequenceId);
        event.setEmptyTripArray();
        ring_buffer.publish(sequenceId);
    }

    /**
     *
     * @return remaining capacity of the {@link RingBuffer}
     */
    public int getRegisteredTripCount(){
        return (int) (this.buffer_size - this.ring_buffer.remainingCapacity());
    }

    /**
     *
     * @return the total amount of written trips
     */
    public int getWrittenTripCount(){ return this.written_trips.get();}


    /**
     * Starts the simulation progress update task. The first call of this method will start the task.
     * Consecutive calls will be ignored.
     */
    public void startSimulationProgressUpdateTask(){
        if(this.update_task == null) {
            Runnable task = () -> {
                String query = "UPDATE " + this.pm.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) + " SET sim_started= true, sim_progress = " + written_trips.get() + ", sim_total = " + total_trip_count + " WHERE sim_key = '" + this.pm.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
                pm.execute(query);
            };
            this.update_task = Executors.newSingleThreadScheduledExecutor();
            this.update_task.scheduleAtFixedRate(task, 10, 10, TimeUnit.SECONDS);
        }
    }
}
