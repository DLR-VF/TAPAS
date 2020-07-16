package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.TPS_WritableTrip;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import de.dlr.ivf.tapas.util.parameters.ParamString;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.concurrent.LinkedBlockingQueue;


//TODO make this a singleton
public class TPS_TripToDbWriter implements Runnable {
    public static final Object POISON_PILL = new Object();
    private LinkedBlockingQueue<TPS_WritableTrip> trips_to_store = new LinkedBlockingQueue<>();

    private TPS_PersistenceManager pm;
    private PreparedStatement ps;

    public TPS_TripToDbWriter(TPS_PersistenceManager pm){
        this.pm = pm;
        String statement = "INSERT INTO " + pm.getParameters().getString(ParamString.DB_TABLE_TRIPS) + " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,? ,?,?,?,?, ?)";
        TPS_DB_Connector connector = ((TPS_DB_IOManager) pm).getDbConnector();
        try {
            this.ps = connector.getConnection(this).prepareStatement(statement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void run() {
        TPS_WritableTrip trip;
        int max_elements = 10000;
        int trip_count = 0;
        try {
            while ((trip = trips_to_store.take()) != POISON_PILL) {

                int index = 1;

                ps.setInt(index++, trip.getPersonId());
                ps.setInt(index++, trip.getHouseholdId());
                ps.setInt(index++, trip.getSchemeId());
                ps.setDouble(index++, trip.getScoreCombined());
                ps.setDouble(index++, trip.getScoreFinance());
                ps.setDouble(index++, trip.getScoreTime());
                ps.setInt(index++, trip.getTazIdStart());
                ps.setBoolean(index++,trip.getTazHasTollStart());
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
                ps.setInt(index++, trip.getLocSelectionMotiveSupply());

                ps.addBatch();
                ps.clearParameters();
                trip_count++;
                if(trip_count > max_elements){

                    OptionalInt optional_execution_error = Arrays.stream(ps.executeBatch())
                                                                 .filter(i -> i == PreparedStatement.EXECUTE_FAILED)
                                                                 .findFirst();

                    if (optional_execution_error.isPresent()) {
                        TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.ERROR, "Storing of trips failed!");
                            }
                    ps.clearBatch();
                    trip_count = 0;
                }
            }
        }catch (InterruptedException | SQLException e){
            e.printStackTrace();
        }
    }


    public void writeTrip(TPS_Plan plan, TPS_TourPart tour_part, TPS_Trip trip){

        try {
            this.trips_to_store.put(new TPS_WritableTrip(plan,tour_part,trip,this.pm));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
