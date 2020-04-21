package de.dlr.ivf.tapas.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager.Behaviour;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;



@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_TripDeleter {

	String key = null;
	ArrayList<Integer> hh_id = new ArrayList<>();
	int hhCounter = 0;
	TPS_ParameterClass parameterClass;

	public TPS_TripDeleter(TPS_ParameterClass parameterClass){
		this.parameterClass = parameterClass;
	}

	/**
	 * Clears the HHID-resetlist
	 */
	public void clearHHID(){
		hh_id.clear();
		hhCounter =0;		
	}
	
	/**
	 * Adds one household to the set list
	 * @param id the id to reset
	 */
	public void putHHID(int id) {
		if (!hh_id.contains(id)) {
			hh_id.add(id);
			hhCounter++;
		} else {
			if(TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
				TPS_Logger.log(SeverenceLogLevel.WARN, "Haushalt " + id + " existiert bereits und wird ignoriert");
			}
		}
	}

	/**
	 * This method creates a StringBuffer which contains a SQL-conform Array with all integers above 0 in the ResultSet column 1
	 * @param rs The ResultSet to scan
	 * @return A StringBuffer containing a SQL-Array-String
	 * @throws SQLException
	 */
	public StringBuffer createStringBuffer(ResultSet rs) throws SQLException {
		StringBuffer sb = new StringBuffer("ARRAY[");
		if (rs.next()) {
			sb.append(rs.getString(1) + ",");
			while (rs.next()) {
				if (Integer.parseInt(rs.getString(1)) > 0)
					sb.append(rs.getString(1) + ",");
			}
		} else {
			return null;
		}
		sb.setCharAt(sb.length() - 1, ']');
		return sb;
	}

	/**
	 * Method to roll back all transactions and reset the connection to its initial state
	 * @param connection The Connection to reset
	 * @param autoCommit should autocommit be enabled? false = transaction mode
	 * @param isolationLevel The isolation level for the connection
	 * @param statement The statement to close.
	 * @throws SQLException
	 */
	public void resetConnection(Connection connection, boolean autoCommit,
			int isolationLevel, Statement statement) throws SQLException {
		connection.rollback();
		connection.setAutoCommit(autoCommit);
		connection.setTransactionIsolation(isolationLevel);
		statement.close();
		connection.close();
	}

	/**
	 * Returns the key for the simulation
	 * @return
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the key for the simulation
	 * @param key
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the ArrayList of selected household ids
	 * @return
	 */
	public ArrayList<Integer> getHh_id() {
		return hh_id;
	}

	/**
	 * Set an ArrayList containing the selected household ids
	 * @param hhId
	 */
	public void setHh_id(ArrayList<Integer> hhId) {
		hh_id = hhId;
	}

	/**
	 * Method to delete all trips from the trip table which belong to the selected household ids:
	 * 
	 * diese Funktion soll folgendes tun: 
	 * 0: hole die sim parameter aus public.simulations (FALSE für den Aufruf-Behaviour...) check 
	 * 1: sammle alle locations der trips zu den Personen diesen hh_id 
	 * 2: dekrementiere die locations in temp.locations_<key> 
	 * 3: lösche diese hh_id aus der trip-table 
	 * 4: setze alle hh_id in temp.households_<key> auf started= false, finished = false, serverip=null 
	 * 5: dekrementiere sim_progress aus public.simulations um count(hh_id) 
	 * 6: setze flags "sim_finished" und "sim_started" in public.simulations auf false
	 * 
	 * 
	 * @param connection The connection to use.
	 * @return the number of households deleted
	 * @throws SQLException
	 */
	public int deleteHouseholdsFromTriptable(Connection connection){
		int isolationLevel;
		boolean autoCommit;
		String query ="";

		try {
			synchronized (connection) {
				autoCommit = connection.getAutoCommit();
				isolationLevel = connection.getTransactionIsolation();
				Statement statement = connection.createStatement();
				// AutoCommit to FALSE
				connection.setAutoCommit(false);
				connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

				// get all ids for all locations
				StringBuilder sb_hhId = new StringBuilder("ARRAY[");
				if (hh_id.size() == 0) {
					if(TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
						TPS_Logger.log(SeverenceLogLevel.WARN, "No households given. Program stops.");
					}
					resetConnection(connection, autoCommit, isolationLevel, statement);
					return 0;
				}
				for (Integer hh_id : this.hh_id) {
					sb_hhId.append(hh_id + ",");
				}
				sb_hhId.setCharAt(sb_hhId.length() - 1, ']');

				// get all Person_IDs from HH-id
				//query = "SELECT p_id from " + ParamString.DB_TABLE_PERSON.getString() + " WHERE p_hh_id = ANY("
				//		+ sb_hhId.toString() + ") AND p_key = '" + ParamString.DB_HOUSEHOLD_AND_PERSON_KEY.getString()+ "'";
				query =TPS_DB_IOManager.buildQuery(this.parameterClass, "getpersonidsfromhh_trip", this.parameterClass.getString(ParamString.DB_TABLE_PERSON), sb_hhId.toString(),
				 this.parameterClass.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY));
				ResultSet rs_persons = statement.executeQuery(query);

				// put all person_ids in an array
				StringBuffer sb_personIds = createStringBuffer(rs_persons);
				rs_persons.close();
				if (sb_personIds == null) {
					if(TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
						TPS_Logger.log(SeverenceLogLevel.WARN, "No matching person IDs found. Program stops.");
					}
					resetConnection(connection, autoCommit, isolationLevel, statement);
					return 0;
				}
				// check if all households in a trip have been processed
				for (Integer iterator_elem : hh_id) {
					query = "Select * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_HOUSEHOLD_TMP) +
							" WHERE hh_id =" + iterator_elem;
					ResultSet rs_hhTest = statement.executeQuery(query);
					if (!rs_hhTest.next()) {
						hh_id.remove(iterator_elem);
					}
					rs_hhTest.close();
					if (hh_id.isEmpty()) {
						resetConnection(connection, autoCommit, isolationLevel, statement);
						return 0;
					}
				}

				// Get location_ids from person_ids
				//query = "SELECT loc_id_end FROM " + ParamString.DB_TABLE_TRIPS.getString() + " WHERE p_id = ANY("
				//		+ sb_personIds.toString() + ")";
				query = TPS_DB_IOManager.buildQuery(this.parameterClass,
				 "getlocidsfromperson", this.parameterClass.getString(ParamString.DB_TABLE_TRIPS), sb_personIds.toString());
				ResultSet rs_locIds = statement.executeQuery(query);

				Map<String, Integer> sortedIds = new HashMap<>();
				int sum = 0;
				// Loc_IDs sortieren:
				if (rs_locIds.next()) {
					if (Integer.parseInt(rs_locIds.getString(1)) > 0) {
						sortedIds.put(rs_locIds.getString(1), 1);
					} else {
						if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
							TPS_Logger.log(SeverenceLogLevel.INFO, "1. Weg nach Hause, Programm fortsetzen");
						}
					}
					while (rs_locIds.next()) {
						if (!sortedIds.containsKey(rs_locIds.getString(1))
								&& Integer.parseInt(rs_locIds.getString(1)) > 0) {
							sortedIds.put(rs_locIds.getString(1), 1);
						} else if (sortedIds.containsKey(rs_locIds.getString(1))
								&& Integer.parseInt(rs_locIds.getString(1)) > 0) {
							int oldValue = sortedIds.get(rs_locIds.getString(1));
							sortedIds.remove(rs_locIds.getString(1));
							sortedIds.put(rs_locIds.getString(1), oldValue + 1);
						}
						sum++;
					}
				} else {
					if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
						TPS_Logger.log(SeverenceLogLevel.INFO, "No locations found; continuing.");
					}
				}
				rs_locIds.close();

				if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
					TPS_Logger.log(SeverenceLogLevel.INFO, "Found " + sortedIds.size() + " locations with " + sum + " occupancies to remove");
				}
				while (!sortedIds.isEmpty()) {
					Map<String, Integer> sortedIdsUse = new HashMap<>();
					Iterator<String> it = sortedIds.keySet().iterator();
					StringBuilder sb_LocIds = new StringBuilder("ARRAY[");

					if (!it.hasNext()) {
						break;
					}
					while (it.hasNext()) {
						Object nextKey = it.next();
						sb_LocIds.append(nextKey.toString() + ",");
						int val = sortedIds.get(nextKey.toString()) - 1;
						if (val > 0) {
							sortedIdsUse.put(nextKey.toString(), val);
						} else {
							it.remove();
						}
					}
					sb_LocIds.setCharAt(sb_LocIds.length() - 1, ']');

					// Decrement locations
					// System.out.println("sbLocID: " + sb_LocIds.toString());

					query = TPS_DB_IOManager.buildQuery(this.parameterClass, "incr_occupancy",
							this.parameterClass.getString(ParamString.DB_TABLE_LOCATION_TMP), sb_LocIds.toString(), -1,
							!TPS_DB_IOManager.BEHAVIOUR.equals(Behaviour.FAT));
					statement.execute(query);
					sortedIds = sortedIdsUse;
				}

				if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
					TPS_Logger.log(SeverenceLogLevel.INFO, "deleting hhs");
				}

				// delete hh_id from triptable
				query = TPS_DB_IOManager.buildQuery(this.parameterClass, "deletehhfromtrip",
						this.parameterClass.getString(ParamString.DB_TABLE_TRIPS), sb_hhId.toString());
				statement.execute(query);

				if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
					TPS_Logger.log(SeverenceLogLevel.INFO, "resetting hhs");
				}

				// set households on started = false/finished = false & server_ip =
				// null
				query = TPS_DB_IOManager.buildQuery(this.parameterClass, "sethhfalse", this.parameterClass.getString(ParamString.DB_TABLE_HOUSEHOLD_TMP),
						sb_hhId.toString());
				statement.execute(query);

				// decrement sim_progress and set flags "started" & "finished" =
				// FALSE
				if(TPS_Logger.isLogging(SeverenceLogLevel.INFO)) {
					TPS_Logger.log(SeverenceLogLevel.INFO, "decreasing progress bar");
				}
				query = TPS_DB_IOManager.buildQuery(this.parameterClass, "decrProgress", key, hh_id.size());
				statement.execute(query);
				connection.commit();
				connection.setAutoCommit(autoCommit);
				connection.setTransactionIsolation(isolationLevel);
				//vacuum trips tmp household and locations
				query = "VACUUM "+this.parameterClass.getString(ParamString.DB_TABLE_HOUSEHOLD_TMP);
				statement.execute(query);
				query = "VACUUM "+this.parameterClass.getString(ParamString.DB_TABLE_LOCATION_TMP);
				statement.execute(query);
				query = "VACUUM "+this.parameterClass.getString(ParamString.DB_TABLE_TRIPS);
				statement.execute(query);

				
				statement.close();
				// connection.close();
			}
		} catch (SQLException e) {
			TPS_Logger.log(SeverenceLogLevel.ERROR, "Exception during SQL! Query: " + query, e);
		}
		return hh_id.size();
	}

	/**
	 * Main to test this function
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException, SQLException {
		if (args.length < 2) {
			System.out
					.println("Usage: TripDeleter <simulation_key> <hh_id1> <hh_id2> ...");
			return;
		}
		String runKey = args[0];
		

		File configFile = new File("T:/Simulationen/Berlin/DB_Test/full_test_perseus/run_Run_full_new.csv");
		TPS_ParameterClass parameterClass = new TPS_ParameterClass();
		parameterClass.setString(ParamString.RUN_IDENTIFIER, runKey);
		parameterClass.loadRuntimeParameters(configFile);
		TPS_DB_Connector manager;

		manager = new TPS_DB_Connector(parameterClass);

		TPS_TripDeleter worker = new TPS_TripDeleter(parameterClass);
		worker.setKey(runKey);
		// HH-IDs einlesen
		for (int i = 1; i < args.length; ++i) {
			worker.putHHID(Integer.parseInt(args[i]));
		}

		
		Connection con = manager.getConnection(worker);



		int hhDone = worker.deleteHouseholdsFromTriptable(con);
		if (hhDone != 0) {
			System.out.println("Programm wurde mit " + hhDone + " von "
					+ (args.length - 1) + "Haushalten aufegrufen.");
		} else
			System.out
					.println("Programm wurde abgebrochen, es wurden keine Änderungen übernommen.");
	}

}

