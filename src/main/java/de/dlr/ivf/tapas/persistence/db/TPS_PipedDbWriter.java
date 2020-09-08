package de.dlr.ivf.tapas.persistence.db;

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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class TPS_PipedDbWriter implements Runnable, TPS_TripWriter{

    private char csv_delimiter = ';';

    private PipedOutputStream output_stream;
    private PipedInputStream input_stream;
    private PrintWriter pw;

    private AtomicInteger written_trips = new AtomicInteger(0);

    private Connection connection;
    private CopyManager copy_manager;

    private String copy_string;

    public TPS_PipedDbWriter(TPS_PersistenceManager pm){
        try {
            this.connection = ((TPS_DB_IOManager)pm).getDbConnector().getConnection(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.copy_string = String.format("COPY %s FROM STDIN (FORMAT TEXT, ENCODING 'UTF-8', DELIMITER '"+this.csv_delimiter+"', HEADER false)",pm.getParameters().getString(ParamString.DB_TABLE_TRIPS));
        init();
    }

    @Override
    public void run() {
        try {
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Starting database pipeline");
            copy_manager.copyIn(copy_string,input_stream);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Stopping database pipeline with "+written_trips.get()+" written trips.");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void init(){
        try {
            this.copy_manager = this.connection.unwrap(PGConnection.class).getCopyAPI();
            this.output_stream = new PipedOutputStream();
            this.input_stream = new PipedInputStream(output_stream);
            this.pw = new PrintWriter(output_stream,true);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
    public void writeTrip(TPS_WritableTrip trip) throws IOException {
        if(written_trips.incrementAndGet() % 10000 == 0)
            System.out.println("written a 10k batch of trips");
        String csv_string =  ""+trip.getPersonId()+csv_delimiter+trip.getHouseholdId()+csv_delimiter+trip.getSchemeId()+csv_delimiter+trip.getScoreCombined()+csv_delimiter+trip.getScoreFinance()+csv_delimiter+trip.getScoreTime()+csv_delimiter+trip.getTazIdStart()+
                csv_delimiter+trip.getTazHasTollStart()+csv_delimiter+trip.getBlockIdStart()+csv_delimiter+trip.getLocIdStart()+csv_delimiter+trip.getLonStart()+csv_delimiter+trip.getLatStart()+csv_delimiter+trip.getTazIdEnd()+csv_delimiter+trip.getTazHasTollEnd()+
                csv_delimiter+trip.getBlockIdEnd()+csv_delimiter+trip.getLocIdEnd()+csv_delimiter+trip.getLonEnd()+csv_delimiter+trip.getLatEnd()+csv_delimiter+trip.getStartTimeMin()+csv_delimiter+trip.getTravelTimeSec()+csv_delimiter+trip.getMode()+
                csv_delimiter+trip.getCarType()+csv_delimiter+trip.getDistanceBlMeter()+csv_delimiter+trip.getDistanceRealMeter()+csv_delimiter+trip.getActivity()+csv_delimiter+trip.getIsAtHome()+csv_delimiter+trip.getActivityStartMin()+
                csv_delimiter+trip.getActivityDurationMin()+csv_delimiter+trip.getCarIndex()+csv_delimiter+trip.getIsRestrictedCar()+csv_delimiter+trip.getPersonGroup()+csv_delimiter+trip.getTazBbrTypeStart()+csv_delimiter+trip.getBbrTypeHome()+
                csv_delimiter+trip.getLocSelectionMotive()+csv_delimiter+trip.getLocSelectionMotiveSupply();
        pw.println(csv_string);

    }

    public void finish(){
        try {
            this.output_stream.flush();
            this.output_stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
