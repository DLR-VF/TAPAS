package de.dlr.ivf.tapas.persistence.db;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.plan.state.TPS_WritableTrip;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TPS_PipedDbWriter implements Runnable, TPS_TripWriter{

    private PipedOutputStream output_stream;
    private PipedInputStream input_stream;

    private int total_trip_count;

    private AtomicInteger registered_trips = new AtomicInteger(0);
    private AtomicInteger written_trips = new AtomicInteger(0);

    private Connection connection;
    private CopyManager copy_manager;
    private TPS_DB_IOManager pm;

    private RingBuffer<TPS_WritableTripEvent> ring_buffer;
    private Disruptor<TPS_WritableTripEvent> disruptor;

    private ScheduledExecutorService update_task;

    private String copy_string;

    public TPS_PipedDbWriter(TPS_PersistenceManager pm, int total_trip_count){

        this.pm = (TPS_DB_IOManager) pm;
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
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Opening database pipeline");
            copy_manager.copyIn(copy_string,input_stream);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Closing database pipeline with "+ registered_trips.get()+" written trips.");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
    private void init(){
        try {
            this.copy_manager = this.connection.unwrap(PGConnection.class).getCopyAPI();
            this.output_stream = new PipedOutputStream();
            this.input_stream = new PipedInputStream(output_stream);

            int buffer_size = 1 << 18;
            EventFactory<TPS_WritableTripEvent> ef = TPS_WritableTripEvent::new;
            this.disruptor = new Disruptor<>(ef, buffer_size, DaemonThreadFactory.INSTANCE, ProducerType.MULTI, new BusySpinWaitStrategy());

            this.ring_buffer = disruptor.getRingBuffer();

            EventHandler<TPS_WritableTripEvent> handler = (event, sequence, endOffBatch) -> {
                this.output_stream.write(event.getTripAsByteArray());
                this.written_trips.getAndIncrement();
            };

            disruptor.handleEventsWith(handler);
            disruptor.start();

        } catch (SQLException | IOException e) {
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
        try {
            this.update_task.shutdownNow();
            this.output_stream.flush();
            this.output_stream.close();
            String query = "UPDATE "+this.pm.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS)+" SET sim_finished = true, sim_progress = "+total_trip_count+", sim_total = "+ total_trip_count +", timestamp_finished = now() WHERE sim_key = '"+this.pm.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
            pm.execute(query);
            disruptor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public int getRegisteredTripCount(){
        return this.registered_trips.get();
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
