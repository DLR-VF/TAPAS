package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.TPS_WritableTrip;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.concurrent.LinkedBlockingQueue;


//TODO make this a singleton
public class TPS_TripToDbWriter implements Runnable {
    private final Object POISON_PILL = new Object();
    private LinkedBlockingQueue<Object> trips_to_store = new LinkedBlockingQueue<>();
    private boolean debugging;

    private TPS_PersistenceManager pm;
    private PreparedStatement ps;
    private PreparedStatement debug_ps;
    private int batchcount = 1;
    private Connection con = null;

    public TPS_TripToDbWriter(TPS_PersistenceManager pm){
        this.pm = pm;
        String statement = "INSERT INTO " + pm.getParameters().getString(ParamString.DB_TABLE_TRIPS) + " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,? ,?,?,?,?, ?)";
        String debug_statement = "INSERT INTO debug."+pm.getParameters().getString(ParamString.DB_TABLE_TRIPS) + " VALUES (?,?,?,?,?,?)";
        TPS_DB_Connector connector = ((TPS_DB_IOManager) pm).getDbConnector();
        String debugString = System.getProperty("debug");
        debugging = debugString != null && debugString.equalsIgnoreCase("true");
        //init();



        try {
            this.ps = connector.getConnection(this).prepareStatement(statement);


            if (debugging){
                connector.getConnection(this)
                        .prepareStatement("DROP TABLE IF EXISTS debug."+pm.getParameters().getString(ParamString.DB_TABLE_TRIPS))
                        .execute();
                connector.getConnection(this)
                                     .prepareStatement("CREATE TABLE debug."+pm.getParameters().getString(ParamString.DB_TABLE_TRIPS)+" (p_id int, hh_id int, start_time_min int, travel_time_sec int, activity_start_min int, activity_duration_min int)")
                                     .execute();
                this.debug_ps = connector.getConnection(this).prepareStatement(debug_statement);
            }
        } catch (SQLException e) {
            debugging = false;
            e.printStackTrace();
        }
    }



    @Override
    public void run() {
        TPS_WritableTrip trip;
        Object next_queue_item;
        int max_elements = 10000;
        int trip_count = 0;
        try {
            while ((next_queue_item = trips_to_store.take()) != POISON_PILL) {
                trip = (TPS_WritableTrip) next_queue_item;
                int index = 1;
                if(debugging){
                    debug_ps.setInt(index++, trip.getPersonId());
                    debug_ps.setInt(index++, trip.getHouseholdId());
                    debug_ps.setInt(index++, (int) (trip.getTrip().getOriginalStart()* 1.66666666e-2+ 0.5));
                    debug_ps.setInt(index++, trip.getTrip().getOriginalDuration());
                    debug_ps.setInt(index++,  (int) (trip.getStay().getOriginalStart()* 1.66666666e-2+ 0.5));
                    debug_ps.setInt(index,  (int) (trip.getStay().getOriginalDuration()* 1.66666666e-2+ 0.5));
                    debug_ps.execute();
                    debug_ps.clearParameters();
                }

                index = 1;

                ps.setInt(index++, trip.getPersonId());
                ps.setInt(index++, trip.getHouseholdId());
                ps.setInt(index++, trip.getSchemeId());
                ps.setDouble(index++, trip.getScoreCombined());
                ps.setDouble(index++, trip.getScoreFinance());
                ps.setDouble(index++, trip.getScoreTime());
                ps.setInt(index++, trip.getTazIdStart());
                ps.setBoolean(index++, trip.getTazHasTollStart());
                ps.setInt(index++, trip.getBlockIdStart());
                ps.setInt(index++, trip.getLocIdStart());
                ps.setDouble(index++, trip.getLonStart());
                ps.setDouble(index++, trip.getLatStart());
                ps.setInt(index++, trip.getTazIdEnd());
                ps.setBoolean(index++, trip.getTazHasTollEnd());
                ps.setInt(index++, trip.getBlockIdEnd());
                ps.setInt(index++, trip.getLocIdEnd());
                ps.setDouble(index++, trip.getLonEnd());
                ps.setDouble(index++, trip.getLatEnd());
                ps.setInt(index++, trip.getStartTimeMin());
                ps.setDouble(index++, trip.getTravelTimeSec());
                ps.setInt(index++, trip.getMode());
                ps.setInt(index++, trip.getCarType());
                ps.setDouble(index++, trip.getDistanceBlMeter());
                ps.setDouble(index++, trip.getDistanceRealMeter());
                ps.setInt(index++, trip.getActivity());
                ps.setBoolean(index++, trip.getIsAtHome());
                ps.setInt(index++, trip.getActivityStartMin());
                ps.setInt(index++, trip.getActivityDurationMin());
                ps.setInt(index++, trip.getCarIndex());
                ps.setBoolean(index++, trip.getIsRestrictedCar());
                ps.setInt(index++, trip.getPersonGroup());
                ps.setInt(index++, trip.getTazBbrTypeStart());
                ps.setInt(index++, trip.getBbrTypeHome());
                ps.setInt(index++, trip.getLocSelectionMotive());
                ps.setInt(index, trip.getLocSelectionMotiveSupply());

                ps.addBatch();
                ps.clearParameters();
                trip_count++;

                if(trip_count > max_elements){
                    executeBatch();
                    trip_count = 0;
                }
            }
        }catch (InterruptedException | SQLException e){
            e.printStackTrace();
        }
        System.out.println("OH NO! Someone just poisoned the trip writer, writing the rest to the database");
        try {
            executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void writeTrip(TPS_Plan plan, TPS_TourPart tour_part, TPS_Trip trip) throws InterruptedException {

            this.trips_to_store.put(new TPS_WritableTrip(plan,tour_part,trip));
    }

    public void finish(){
        try {
            this.trips_to_store.put(POISON_PILL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void executeBatch() throws SQLException {

        int[] batch_result = ps.executeBatch();
        System.out.println("executing batch: "+batchcount+++" with "+batch_result.length+" elements");
        OptionalInt optional_execution_error = Arrays.stream(batch_result)
                .filter(i -> i == PreparedStatement.EXECUTE_FAILED)
                .findFirst();

        if (optional_execution_error.isPresent()) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.ERROR, "Storing of trips failed!");
        }

        ps.clearBatch();
    }

    private void init(){
        try {
            this.con = ((TPS_DB_IOManager) pm).getDbConnector().getConnection(this);
            CopyManager cm = new CopyManager((BaseConnection) con);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
