package de.dlr.ivf.tapas.persistence.db;

import com.lmax.disruptor.*;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.plan.state.TPS_WritableTrip;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import org.postgresql.core.BaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TPS_PipedDbWriter implements Runnable, TPS_TripWriter {

    private int total_trip_count;

    private AtomicInteger registered_trips = new AtomicInteger(0);
    private AtomicInteger written_trips = new AtomicInteger(0);
    private int buffer_size;

    private Connection connection;
    private TPS_CopyManager copy_manager;
    private TPS_DB_IOManager pm;

    private RingBuffer<TPS_WritableTripEvent> ring_buffer;

    private ScheduledExecutorService update_task;

    private String copy_string;

    public TPS_PipedDbWriter(TPS_PersistenceManager pm, int total_trip_count, int buffer_size){

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

            //drop the primary key if it exists //todo don'T put it on the tabke in the first place
            PreparedStatement remove_key_statement = connection.prepareStatement("ALTER TABLE "+this.pm.getParameters().getString(ParamString.DB_TABLE_TRIPS)+" DROP CONSTRAINT IF EXISTS "+this.pm.getParameters().getString(ParamString.DB_TABLE_TRIPS)+"_pkey;");
            remove_key_statement.execute();
            remove_key_statement.close();

            //start the pipe and block
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Opening database pipeline");
            copy_manager.copyIn(copy_string,this.ring_buffer,written_trips);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Closing database pipeline with "+ registered_trips.get()+" written trips.");

            String query = "UPDATE "+this.pm.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS)+" SET sim_finished = true, sim_progress = "+total_trip_count+", sim_total = "+ total_trip_count +", timestamp_finished = now() WHERE sim_key = '"+this.pm.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
            pm.execute(query);

            //now add the primary key
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Adding Primary Key to "+ this.pm.getParameters().getString(ParamString.DB_TABLE_TRIPS)+" to check data consistency...");
            PreparedStatement add_key_statement = connection.prepareStatement("ALTER TABLE "+this.pm.getParameters().getString(ParamString.DB_TABLE_TRIPS)+" ADD PRIMARY KEY (p_id, hh_id, start_time_min);");
            add_key_statement.execute();
            add_key_statement.close();
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Closing the writer...");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void init(){
        try {
            this.copy_manager = new TPS_CopyManager((BaseConnection) this.connection);

            EventFactory<TPS_WritableTripEvent> ef = TPS_WritableTripEvent::new;

            this.ring_buffer = RingBuffer.createMultiProducer(ef,buffer_size,new BusySpinWaitStrategy());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void writeTrip(TPS_WritableTrip trip) {

        long sequenceId = ring_buffer.next();
        TPS_WritableTripEvent event = ring_buffer.get(sequenceId);
        event.setTripByteArray(trip);
        ring_buffer.publish(sequenceId);
        registered_trips.getAndIncrement();

    }

    public void finish(){
        this.update_task.shutdownNow();
        //put an empty byte array onto the ring to signal the copy manager that the transaction is over
        long sequenceId = ring_buffer.next();
        TPS_WritableTripEvent event = ring_buffer.get(sequenceId);
        event.setEmptyTripArray();
        ring_buffer.publish(sequenceId);
    }
    public int getRegisteredTripCount(){
        return (int) (this.buffer_size - this.ring_buffer.remainingCapacity());
    }

    public int getWrittenTripCount(){ return this.written_trips.get();}

    public void startSimulationProgressUpdateTask(){
        Runnable task = () -> {
            String query = "UPDATE "+this.pm.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS)+" SET sim_started= true, sim_progress = "+written_trips.get()+", sim_total = "+ total_trip_count +" WHERE sim_key = '"+this.pm.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
            pm.execute(query);
        };
        this.update_task = Executors.newSingleThreadScheduledExecutor();
        this.update_task.scheduleAtFixedRate(task,10,10, TimeUnit.SECONDS);
    }
}
