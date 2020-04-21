package de.dlr.ivf.tapas.runtime.sumoDaemon;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.dlr.ivf.tapas.iteration.TPS_SumoConverter;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.runtime.util.DaemonControlProperties;
import de.dlr.ivf.tapas.util.Matrix;
import de.dlr.ivf.tapas.util.MatrixMap;
import de.dlr.ivf.tapas.util.TPS_Argument;
import de.dlr.ivf.tapas.util.TPS_Argument.TPS_ArgumentType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import de.dlr.ivf.tapas.util.parameters.CURRENCY;
import de.dlr.ivf.tapas.util.parameters.ParamString;

@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class SumoDaemon extends Thread  {


	boolean keepOnRunning = true;
	boolean keepPreviousIteratioData = true;
	
	
	static final int SumoPollingInterval = 10; //seconds
	private HashMap<String,Integer> simKeysTriggerIteration = new HashMap<>();
	
    private boolean checkAndReestablishCon() throws SQLException{
    	boolean returnVal = true;
    	if(this.manager.getConnection(this).isClosed()){
    		returnVal = false;
    		this.manager.getConnection(this);
    	}
    	return returnVal;
    }
    
	/**
	 * Parameter array with all parameters for this main method.<br>
	 * PARAMETERS[0] = tapas network directory path
	 */
	private static TPS_ArgumentType<?>[] PARAMETERS = {new TPS_ArgumentType<>("tapas network directory path", File.class) };

	/**
	 * The reference to the connection manager, needed to reestablish the connection, if necessary
	 */
	private TPS_DB_Connector manager =null;

	/**
	 * Path of the TAPAS network directory
	 */
	private File tapasNetworkDirectory;

	/**
	 * TPS_ParameterClass parameter container
	 */
	private TPS_ParameterClass parameterClass;
	
	
	public SumoDaemon(File tapasNetworkDirectory){
		this.tapasNetworkDirectory = tapasNetworkDirectory;
		File propFile = new File("daemon.properties");
		DaemonControlProperties prop = new DaemonControlProperties(propFile);
		
		String loginFile= prop.get(DaemonControlProperties.DaemonControlPropKey.LOGIN_CONFIG);

		this.parameterClass = new TPS_ParameterClass();
		File runtimeFile = new File(new File(this.tapasNetworkDirectory, this.parameterClass.SIM_DIR), loginFile);
		try {
			this.parameterClass.loadRuntimeParameters(runtimeFile);
			this.parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, this.tapasNetworkDirectory.getPath());
			this.manager = new TPS_DB_Connector(this.parameterClass);
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Starting Tapas-Sumo-Connector. Press Control-c to finish it.");
		Object[] parameters = TPS_Argument.checkArguments(args, PARAMETERS);
		File tapasNetworkDirectory = ((File) parameters[0]).getParentFile();
		SumoDaemon sumoServer = new SumoDaemon(tapasNetworkDirectory);
		//simServer.initRMIService();
		sumoServer.start();
		sumoServer.join();

	}

	private String actualKey = "";
	private int actualIter = 0;
	

	
	private void readParameter(){
		this.parameterClass.setString(ParamString.RUN_IDENTIFIER, this.actualKey);
		this.parameterClass.setString(ParamString.CURRENCY, CURRENCY.EUR.name());
		this.parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, this.tapasNetworkDirectory.getAbsolutePath());
		String query = "";
		//read parameters
		try {
			if(this.checkAndReestablishCon()){
				query = "SELECT * FROM " +this.parameterClass.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS) + " WHERE sim_key = '"	+ this.actualKey + "'";
				ResultSet rs = manager.executeQuery(query, this);
				TPS_SumoConverter.readParametersFromDBMaintainLoginInfo(rs, this.parameterClass);
				rs.close();				
			}
		} catch (SQLException e) {
			System.err.println("Error in SQL-String: "+query);			
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * This method adopts the string-value to point to the actual iteration
	 * @param val the old value
	 * @return the new string for the db
	 */
	private String updateStringIter(String val){
		String appendix = "_"+this.actualKey+"_IT_"+this.actualIter;
		String oldAppendix = "_"+this.actualKey+"_IT_"+(this.actualIter-1);
		if(!val.endsWith(appendix)){ //error catching
			if(this.actualIter==0){//first_iteration
					val+=appendix;
			}					
			else{
				val = val.replace(oldAppendix, appendix);
			}
		}
		return val;
	}
	
	private List<String> createNewMatrixMap(String oldName, String pVal){
		//clone matrixmap
		
		StringBuilder query = new StringBuilder();
		ResultSet rs;
		List<String> newMatrixNames = new ArrayList<>();

		try {
			boolean updateStatement = false;
			//check if the name exists and we need an update instead of an insert
			query = new StringBuilder(
					"SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_MATRIXMAPS) +
							" WHERE \"matrixMap_name\" = '" + pVal + "'");
			rs = manager.executeQuery(query.toString(), this);
			if (rs.next()) {
				//oho name exists!
				updateStatement = true;
			}
			rs.close();

			
			//get father
			query = new StringBuilder(
					"SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_MATRIXMAPS) +
							" WHERE \"matrixMap_name\" = '" + oldName + "'");
			rs = manager.executeQuery(query.toString(), this);
			if (rs.next()) {
				// build entry
				if(updateStatement){
					query = new StringBuilder(
							"UPDATE " + this.parameterClass.getString(ParamString.DB_TABLE_MATRIXMAPS) +
									" SET \"matrixMap_name\" = '" + pVal + "', \"matrixMap_num\" = " +
									rs.getInt("matrixMap_num") + ", \"matrixMap_matrixNames\" = " + "'{");
					// build array of matrixnames with new iteration appendix
					Object array = rs.getArray("matrixMap_matrixNames").getArray();
					if (array instanceof String[]) {
						String[] matrixNames = (String[]) array;
						for (int j = 0; j < matrixNames.length; ++j) {
							newMatrixNames.add(updateStringIter(matrixNames[j]));
							if (j < matrixNames.length - 1) {							
								query.append(updateStringIter(matrixNames[j])).append(",");
							} else {
								query.append(updateStringIter(matrixNames[j]));
							}
						}
	
					} else {
						throw new SQLException("Cannot cast to string array");
					}
					query.append("}', \"matrixMap_distribution\" = '{");
	
					// build distribution
					array = rs.getArray("matrixMap_distribution").getArray();
					if (array instanceof Double[]) {
						Double[] distribution = (Double[]) array;
						for (int j = 0; j < distribution.length; ++j) {
							if (j < distribution.length - 1)
								query.append(distribution[j].toString()).append(",");
							else
								query.append(distribution[j].toString());
						}
	
					} else {
						throw new SQLException("Cannot cast to double array");
					}
					query.append("}' WHERE \"matrixMap_name\" = '").append(pVal).append("'");
				}
				else{
					query = new StringBuilder(
							"INSERT INTO " + this.parameterClass.getString(ParamString.DB_TABLE_MATRIXMAPS) +
									" (\"matrixMap_name\",\"matrixMap_num\",\"matrixMap_matrixNames\",\"matrixMap_distribution\")" +
									" VALUES('" + pVal + "'," + rs.getInt("matrixMap_num") + ",'{");
					// build array of matrixnames with new iteration appendix
					Object array = rs.getArray("matrixMap_matrixNames").getArray();
					if (array instanceof String[]) {
						String[] matrixNames = (String[]) array;
						for (int j = 0; j < matrixNames.length; ++j) {
							newMatrixNames.add(updateStringIter(matrixNames[j]));
							if (j < matrixNames.length - 1) {							
								query.append(updateStringIter(matrixNames[j])).append(",");
							} else {
								query.append(updateStringIter(matrixNames[j]));
							}
						}
	
					} else {
						throw new SQLException("Cannot cast to string array");
					}
					query.append("}','{");
	
					// build distribution
					array = rs.getArray("matrixMap_distribution").getArray();
					if (array instanceof Double[]) {
						Double[] distribution = (Double[]) array;
						for (int j = 0; j < distribution.length; ++j) {
							if (j < distribution.length - 1)
								query.append(distribution[j].toString()).append(",");
							else
								query.append(distribution[j].toString());
						}
	
					} else {
						throw new SQLException("Cannot cast to double array");
					}
					query.append("}')");
				}
				manager.execute(query.toString(), this);

			}
		} catch (SQLException e) {
			System.err.println("Error in SQL-String: "+query);						
			e.printStackTrace();
		}
		return newMatrixNames;
	}

	/**
	 * This method starts a new iteration for the set simulation key
	 */
	private void changeSimulationStatus(boolean start){
		String query = "";
		try {
			if (this.checkAndReestablishCon()) {
				String status = start?"TRUE":"FALSE";
				query = "UPDATE simulations SET sim_started="+ status+" WHERE sim_key='" + this.actualKey + "'";
				this.manager.execute(query, this);

			}
		} catch (SQLException e) {
			System.err.println("Error in SQL-String: "+query);						
			e.printStackTrace();
		}
	}

	
	@SuppressWarnings("unused")
	private String getOutputPath(){
		return new File(this.parameterClass.getString(ParamString.FILE_WORKING_DIRECTORY)+System.getProperty("file.separator") + this.parameterClass.OUTPUT_DIR+this.parameterClass.getString(ParamString.DB_TABLE_TRIPS)+"_iter_"+this.actualIter).getAbsolutePath()+System.getProperty("file.separator");
	}
	
    //~ Inner Classes -----------------------------------------------------------------------------
    private class RunWhenShuttingDownSumo extends Thread {
        public void run() {
            System.out.println("Control-C caught. Shutting down...");
            //tell out loop to finish
            SumoDaemon.this.keepOnRunning = false;
        }
    }

    
    private int readSumoIteration(String sim_key){
    	int returnVal = -1;
		String query ="";
		try {
			if(this.checkAndReestablishCon()){
				String sumoStatusTable = this.manager.readSingleParameter(sim_key,"DB_SCHEMA_CORE")+this.manager.readSingleParameter(sim_key,"DB_TABLE_SUMO_STATUS");
				if(!sumoStatusTable.equals("")){
					query = "SELECT max(iteration) as iter, count(*)::integer as num FROM "+sumoStatusTable+" WHERE msg_type = 'finished' and sim_key ='"+sim_key+"'";
		    	    ResultSet rs = this.manager.executeQuery(query, this);
		    	    if(rs.next()){
		    	    	if(rs.getInt("num")>0)
		    	    		returnVal = rs.getInt("iter");
		    	    }
		    	    rs.close();
				}
			}
		}catch (SQLException e) {
			System.err.println("Error in sql-statement: "+query);
			e.printStackTrace();
		}
    	return returnVal;
    }
    
    private void searchSimsToProcess(){
		String query ="";
		List<String> possibleSims = new LinkedList<>();
		try {
			if(this.checkAndReestablishCon()){
				query= "SELECT t.sim_key from core.global_sumo_status as t join simulations as s on s.sim_key=t.sim_key where msg_type='finished' and sim_finished=TRUE";
	    	    ResultSet rs = this.manager.executeQuery(query, this);
	    	    while(rs.next()){
	    	    	possibleSims.add(rs.getString("sim_key"));
	    	    }
	    	    rs.close();
	    	    for(String sim : possibleSims){
	    	    	int maxIter = (int)(Double.parseDouble(this.manager.readSingleParameter(sim,"MAX_SUMO_ITERATION"))+0.5);
	    	    	if(maxIter>0){
		    	    	int actTapasIter = (int)(Double.parseDouble(this.manager.readSingleParameter(sim,"ITERATION"))+0.5);
		    	    	int actSumoIteration = this.readSumoIteration(sim);
		    	    	if(actTapasIter<=actSumoIteration && actSumoIteration< maxIter &&0<=actSumoIteration){
		    	    		simKeysTriggerIteration.put(sim, actTapasIter);
		    	    	}
	    	    	}
	    	    }
			}
		}catch (SQLException e) {
			System.err.println("Error in sql-statement: "+query);
			e.printStackTrace();
		}
    		    	
    	
    }
	
    private void resetSimulation(String sim_key){
    	String table;
		String query ="";
		int num;
		try {
			if(this.checkAndReestablishCon()){
				//stop simulation
				this.changeSimulationStatus(false);
				table = this.manager.readSingleParameter(sim_key, "DB_TABLE_TRIPS");
				table +="_"+sim_key;							

				if(this.keepPreviousIteratioData) {
					//copy simulation
					String lastIterTable =  table+"_it"+this.actualIter;
					query ="CREATE TABLE "+lastIterTable+" (LIKE "+table+")";
					this.manager.execute(query, this);
					query ="INSERT INTO "+lastIterTable+" (SELECT * FROM "+table+" )";
					num = this.manager.executeUpdate(query, this);
		    	    System.out.println("copied "+num+ " trips from table "+table+ " into table "+lastIterTable);
				}

				//delete trips
				query= "DELETE FROM "+table;
				num = this.manager.executeUpdate(query, this);
	    	    System.out.println("Deleted "+num+ " trips in table "+table);
	    	    //reset households
				table = this.manager.readSingleParameter(sim_key, "DB_SCHEMA_TEMP");
				table +="households_"+sim_key;
	    	    query= "UPDATE "+table+" SET hh_started =false,hh_finished=false, server_ip= NULL ";
	    	    num = this.manager.executeUpdate(query, this);
	    	    System.out.println("Reset "+num+ " households in table "+table);
	    	    	    	    
				//reset location occupancies
				table = this.manager.readSingleParameter(sim_key, "DB_SCHEMA_TEMP");
				table +="locations_"+sim_key;
	    	    query= "UPDATE "+table+" SET loc_occupancy =0";
	    	    num = this.manager.executeUpdate(query, this);
	    	    System.out.println("Reset "+num+ " locations in table "+table);
	    	    
				//set up new parameters
	    	    query= "UPDATE simulations SET sim_finished = FALSE, sim_progress = 0, timestamp_started =NULL, timestamp_finished =NULL WHERE sim_key ='"+sim_key+"'";
	    	    this.manager.executeUpdate(query, this);
	    	    //set up new iteration
				int nextIter = this.actualIter+1;
				this.manager.updateSingleParameter(this.actualKey, "ITERATION", Integer.toString(nextIter));
	    	    //start simulation
				this.changeSimulationStatus(true);
			
			}
		}catch (SQLException e) {
			System.err.println("Error in sql-statement: "+query);
			e.printStackTrace();
		}
    }

    private void saveMatrices(MatrixMap map, List<String> newNames){
    	String table = this.parameterClass.getString(ParamString.DB_TABLE_MATRICES);
    	String query;
		for(int i=0; i<map.matrices.length; ++i){
			String name = newNames.get(i);
			query = "INSERT INTO "+table +
					" (\"matrix_name\",\"matrix_values\")"
					+ " VALUES('" + name + "',";
			// build array of matrix-ints
			query +=TPS_DB_IO.matrixToSQLArray(map.matrices[i].matrix, 0);
			query += ")";
			this.manager.execute(query, this);
		}
	}
    
    private MatrixMap loadSumoValues(String matrixMap, double weight){
		MatrixMap oldM = this.manager.readMatrixMap(matrixMap, 0, this);
		MatrixMap newM = oldM.clone();

		Map<Integer,Integer> tazMap = new HashMap<>();
		ResultSet rs;
		String query ="";
		String table = this.parameterClass.getString(ParamString.DB_TABLE_TAZ);
		String entryTable;
		int taz_num_start, taz_num_end, tazStart, tazEnd;
		double tt;
		double timeslot;
		double weightedTT;
		double weightOld = 1.0-weight;
		try {
			query = "SELECT taz_id, taz_num_id FROM "+table;
			rs = this.manager.executeQuery(query, this);
			while(rs.next()){
				tazMap.put(rs.getInt("taz_num_id"), rs.getInt("taz_id")-1); //-1 for offset!
			}
			rs.close();
			//load the values from the DB:
			table = this.parameterClass.getString(ParamString.DB_SCHEMA_TEMP)+"sumo_od_"+this.actualKey;
			entryTable = this.parameterClass.getString(ParamString.DB_SCHEMA_TEMP)+"sumo_od_entry_"+this.actualKey;
			query = "SELECT t.taz_id_start, t.taz_id_end, t.interval_end, e.travel_time_sec[3] as tt FROM "+table+ " as t join "+entryTable+ " as e on t.entry_id = e.entry_id";
			rs = this.manager.executeQuery(query, this);
			while(rs.next()){
				taz_num_start = rs.getInt("taz_id_start");
				taz_num_end = rs.getInt("taz_id_end");
				tazStart = tazMap.get(taz_num_start);
				tazEnd = tazMap.get(taz_num_end);
				tt = rs.getDouble("tt");
				timeslot = rs.getDouble("interval_end");
				Matrix tmp = newM.getMatrix((int)timeslot);
				weightedTT = tmp.getValue(tazStart, tazEnd)*weightOld+tt*weight; //weight the new value
				tmp.setValue(tazStart, tazEnd, weightedTT);
			}
				
			rs.close();
			
		} catch (SQLException e) {
			System.err.println("Error in sql-statement: "+query);
			e.printStackTrace();
		}
		
		
		return newM;
    }
    
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */

	public void run() {
		Runtime.getRuntime().addShutdownHook(new RunWhenShuttingDownSumo());

		while (keepOnRunning) {
			this.searchSimsToProcess();
			for (Entry<String, Integer> entries : this.simKeysTriggerIteration.entrySet()) {
				this.actualKey = entries.getKey();
				this.actualIter = entries.getValue();
				if (this.actualKey.length() > 0) {
					System.out
							.println("Processing simulation: " + this.actualKey + " in iteration: " + this.actualIter);
					
					this.readParameter();					

					String mapName = this.parameterClass.getString(ParamString.DB_NAME_MATRIX_TT_MIT);
					String newName = this.updateStringIter(mapName);
					
					MatrixMap mivTT = this.loadSumoValues(mapName,0.33);
					List<String> matrixNames = this.createNewMatrixMap(mapName, newName);
					this.saveMatrices(mivTT, matrixNames);
					this.manager.updateSingleParameter(this.actualKey, "DB_NAME_MATRIX_TT_MIT", newName);
					this.manager.updateSingleParameter(this.actualKey, "ITERATION", Integer.toString((this.actualIter+1)));
					this.resetSimulation(this.actualKey);
					System.out.println(
							"Finished simulation: " + this.actualKey + " in iteration: " + this.actualIter);
				}
			}
			simKeysTriggerIteration.clear(); // everything done (for now)!
			//wait a bit for next polling
			try {
				Thread.sleep(SumoPollingInterval*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}			
}
