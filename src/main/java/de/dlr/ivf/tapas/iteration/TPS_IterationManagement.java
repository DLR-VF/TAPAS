package de.dlr.ivf.tapas.iteration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.MatrixMap;
import de.dlr.ivf.tapas.util.TPS_TripDeleter;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;



public abstract class TPS_IterationManagement {
			
	/**
	 * Reference to the db connector for establishing SQL connectivity.
	 */
	TPS_DB_Connector dbManager = null;

	/**
	 * parameter class reference
	 */
	private final TPS_ParameterClass parameterClass;


	TPS_IterationManagement(TPS_ParameterClass parameterClass){
		this.parameterClass = parameterClass;
	}


	/**
	 * @param dbManager the dbManager to set
	 */
	public void setDBConnector(TPS_DB_Connector dbManager) {
		this.dbManager = dbManager;
	}

    /**
     * An array of MatrixMap for the new travel times
     */
	private final MatrixMap[] newTT = new MatrixMap[TPS_Mode.MODE_TYPE_ARRAY.length];

	
	/**
	 * @return the newTT
	 */
	public MatrixMap[] getNewTravelTimeMatrix() {
		return newTT;
	}

	/**
	 * Method to map the household id and person id to a unique long-value
	 * @param hh_id the id of the household
	 * @param p_id the id of the person in this household
	 * @return a long value for the hashmap used in calculatePlanDerivation
	 */
	public long hhAndPersonIDToHash(int hh_id, int p_id){
		long value;
		
		value = (((long)hh_id)<<32)+(long)p_id;
		
		return value;
	}
	
	/**
	 * Method to extract the person id from a hash key.
	 * @param hash the key value form a entry returned by calculatePlanDerivation
	 * @return the person id
	 */
	public int hashToPersonID(long hash){
		return (int) hash; // mask the lower 32 bit
	}
	
	/**
	 * Method to extract the household id from a hash key.
	 * @param hash the key value form a entry returned by calculatePlanDerivation
	 * @return the household id
	 */
	public int hashToHHID(long hash){
		long hh_id = hash>>>32; // shift 32 zeros from left 
		return (int) hh_id;
	}
	
	
	/**
	 * Method to prepare the given plans for recalculation. This automatically triggers a recalculation.
	 * @param plans A map of plans to recalculate. 
	 */
	public void resetPlansForRecalculation(ArrayList<Long> plans){
		String query ="";
		
		try{
			TPS_TripDeleter worker = new TPS_TripDeleter(this.parameterClass);
			Connection con = this.dbManager.getConnection(worker);

			worker.setKey(this.parameterClass.getString(ParamString.RUN_IDENTIFIER));
			// read HH-IDs 
			//performance tweak: 10000 -chunks!
			int counter =0;
			for (Long id : plans) {				
				worker.putHHID(this.hashToHHID(id));
				counter++;
				if(counter==10000){
					if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
						TPS_Logger.log(SeverenceLogLevel.INFO, "Deleting "+counter+" households from triptable");
					}
					worker.deleteHouseholdsFromTriptable(con);
					worker.clearHHID();
					counter=0;
				}
			}
			//rest
			if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
				TPS_Logger.log(SeverenceLogLevel.INFO, "Deleting "+counter+" households from triptable");
			}
			worker.deleteHouseholdsFromTriptable(con);
			worker.clearHHID();
			
			if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
				TPS_Logger.log(SeverenceLogLevel.INFO, "Done!");
			}
		}catch (SQLException e){
			TPS_Logger.log(SeverenceLogLevel.ERROR, "Exception during SQL! Query: "+query, e);
		}
	}
	
	public class PlanDeviation{
		public double oldTime=0;
		public double newTime=0;
		final double[] oldDist = new double[TPS_Mode.MODE_TYPE_ARRAY.length];
		final double[] newDist = new double[TPS_Mode.MODE_TYPE_ARRAY.length];
	}
	/**
	 * Function to calculate the derivation of the day plans according to the new matrices
	 * @param iteration The iteration index to calculate
	 * @return The HashMap, which stores the travel time derivation for every persons plan.
	 */
	public HashMap<Long, PlanDeviation> calculatePlanDerivation(int iteration){
		HashMap<Long, PlanDeviation> map = new HashMap<>();
		
		String query ="";
		String key = this.parameterClass.getString(ParamString.RUN_IDENTIFIER);
		String region = this.parameterClass.getString(ParamString.DB_REGION);
		try{
			boolean recordsFound;
			int step=0;
			int chunk= 1000000;
			Connection con = this.dbManager.getConnection(this);
			Statement stat = con.createStatement();
			ResultSet rs;
			int hhID, pID, mode; 
			long keyVal;
			PlanDeviation timeDiff;
			double ttNew, distNew, ttOld, distOld;

			do{
				recordsFound=false;
				query = "SELECT trip.p_id, trip.hh_id, trip.start_time_min, " +
					"it.travel_time_sec as tt_new, trips.travel_time_sec as tt_old "+
					"it.travel_distance_m as dist_new, trips.travel_distance_m as dist_old, trips.mode "+
					"FROM "+region+"_trips_"+key+" AS trips " +
					"LEFT OUTER JOIN temp.trips_"+key+" AS it " +
					"ON (it.p_id = trips.p_id AND it.hh_id = trips.hh_id AND it.start_time_min == trips.start_time_min AND it.iteration="+iteration+") " +
					"ORDER BY trips.hh_id, trips.p_id, trips.start_time_min LIMIT "+chunk+" OFFSET "+(step*chunk);
				rs = stat.executeQuery(query);
				
				step++; // for the next chunk
				
				while (rs.next()){
					recordsFound=true;
					hhID = rs.getInt("hh_id");
					pID= rs.getInt("p_id");
					keyVal = hhAndPersonIDToHash(hhID, pID); 
					
					if(map.containsKey(keyVal)){ // is this person known?
						timeDiff = map.get(keyVal);
					}
					else{
						timeDiff = new PlanDeviation(); //new person
					}
					
					mode = rs.getInt("mode");
					ttOld = rs.getDouble("tt_old");
					distOld = rs.getDouble("dist_old");
					if(rs.getObject("tt_new")!=null){
						ttNew = rs.getDouble("tt_new");
					}
					else{
						ttNew=ttOld; //no update information
					}
					
					if(rs.getObject("dist_new")!=null){
						distNew = rs.getDouble("dist_new");
					}
					else{
						distNew=distOld; //no update information
					}				
					
					timeDiff.newTime+= ttNew;
					timeDiff.oldTime+= ttOld;
					timeDiff.newDist[mode]+= distNew; // mode dependent distance! : change of costs!
					timeDiff.oldDist[mode]+= distOld;
					map.put(keyVal, timeDiff);
					
				}
			}while(recordsFound);
			
			
		}catch (SQLException e){
			TPS_Logger.log(SeverenceLogLevel.ERROR, "Exception during SQL! Query: "+query, e);
		}
		
		
		return map;
	}
	
	/**
	 * Method to select persons and plans, which should be recalculated.
	 * @param derivationMap The derivation map of the plans
	 * @param actIter the actual iteration
	 * @param maxIter the maximum value actIter can reach
	 * @return A map of plans for recalculation
	 */
	public abstract ArrayList<Long> selectPlansForRecalculation(HashMap<Long, PlanDeviation> derivationMap, int actIter, int maxIter);
	
}
