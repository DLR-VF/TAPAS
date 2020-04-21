package de.dlr.ivf.tapas.tools.locationallocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

public class LocationProcessor {

	/**
	 * local variable to store the header information
	 */
	private HashMap<Integer, String> headerMap = new HashMap<>();
	private int posOfIdColumn = -1;

	/**
	 * Method to initialize the header tokens.
	 * 
	 * @param headerString
	 *            the header string of the csv-file
	 */
	public void loadHeaders(String headerString) {
		headerString = headerString.trim();
		StringTokenizer tok = new StringTokenizer(headerString, ";");
		int i = 0;
		String header;
		while (tok.hasMoreTokens()) {
			header = tok.nextToken();
			if (header.equals("Loc_ID"))
				posOfIdColumn = i;
			this.headerMap.put(i++, header);
		}

	}

	/**
	 * This method ads a location if it is corect initialized to the address
	 * filter list
	 * 
	 * @param entry
	 *            the location, which holds the data
	 */
	public void addLocationToAddressProcessing(Location entry) {
		if (entry.isCorrectInitialized()) {
			this.locations.add(entry);
		}
	}

	List<Location> locations = new ArrayList<>();

	/**
	 * Returns the list of locations, where the housenumbers need to be
	 * processed
	 * 
	 * @return
	 */
	public List<Location> getLocationsForAddressProcessing() {
		return locations;
	}

	List<Location> locationsToRelocalize = new ArrayList<>();

	/**
	 * This method ads a location if it is corect initialized to the relocation
	 * list
	 * 
	 * @param entry
	 *            the location, which holds the data
	 */
	public void addLocationToRelocate(Location entry) {
		if (entry.isCorrectInitialized()) {
			this.locationsToRelocalize.add(entry);
		}
	}

	/**
	 * Returns the list of locations, where the location should be relocated
	 * 
	 * @return
	 */
	public List<Location> getLocationsForRelocation() {
		return locationsToRelocalize;
	}

	/**
	 * This class represents one location.
	 * 
	 * @author hein_mh
	 * 
	 */
	public class Location {

		public static final int STATUS_NOT_DISTINCT_IN_DATABASE = 1;
		public static final int STATUS_DISTINCT_IN_DATABASE = 0;

		private boolean correctInitialized = false;

		private int myHash = -1;
		private boolean isUpdatedByDB = false;
		private int sqlQueryIndex =-1;
		/**
		 * @return the sqlQueryIndex
		 */
		public int getSqlQueryIndex() {
			return sqlQueryIndex;
		}
		/**
		 * @param sqlQueryIndex the sqlQueryIndex to set
		 */
		public void setSqlQueryIndex(int sqlQueryIndex) {
			this.sqlQueryIndex = sqlQueryIndex;
		}
		/**
		 * @return the correctInitialized
		 */
		public boolean isCorrectInitialized() {
			return correctInitialized;
		}
		/**
		 * 
		 * @param isUpdatedByDB
		 *            true if the values set are inserted by a row from the
		 *            database
		 */
		public void setUpdatedByDB(boolean isUpdatedByDB) {
			this.isUpdatedByDB = isUpdatedByDB;
		}
		public boolean isUpdatedByDB() {
			return isUpdatedByDB;
		}
		/**
		 * The HashMap for the values
		 */
		private HashMap<String, String> valueMap = new HashMap<>();

		private int status =STATUS_DISTINCT_IN_DATABASE;

		/**
		 * Add one value at position valID.
		 * 
		 * @param valID
		 *            The column in the csv-file.
		 * @param val
		 *            The value to set.
		 * @return Returns true if succesfull. False if valID is unknown or
		 *         valID is already set.
		 */
		public boolean addValue(int valID, String val) {
			boolean returnVal = false;
			String valName = LocationProcessor.this.headerMap.get(valID);
			if (valName != null && this.valueMap.get(valName) == null) {
				this.valueMap.put(valName, val);
				returnVal = true;
			}
			return returnVal;
		}

		/**
		 * Add one value for the given name.
		 * 
		 * @param valName
		 *            The value key in the csv-file.
		 * @param val
		 *            The value to set.
		 * @return Returns true if succesfull. False if valName is unknown or
		 *         valName is already set.
		 */
		public boolean addValue(String valName, String val) {
			boolean returnVal = false;
			if (LocationProcessor.this.headerMap.containsValue(valName) && this.valueMap.get(valName) == null) {
				this.valueMap.put(valName, val);
				returnVal = true;
			}
			return returnVal;
		}

		/**
		 * Updates one value at position valID.
		 * 
		 * @param valID
		 *            The column in the csv-file.
		 * @param val
		 *            The value to set.
		 * @return Returns true if succesfull. False if valID is unknown or
		 *         valID is not set.
		 */
		public boolean updateValue(int valID, String val) {
			boolean returnVal = false;
			String valName = LocationProcessor.this.headerMap.get(valID);
			if (valName != null && this.valueMap.get(valName) != null) {
				this.valueMap.put(valName, val);
				returnVal = true;
			}
			return returnVal;
		}

		/**
		 * Updates one value for the given name.
		 * 
		 * @param valName
		 *            The value key in the csv-file.
		 * @param val
		 *            The value to set.
		 * @return Returns true if succesfull. False if valName is unknown or
		 *         valName is not set.
		 */
		public boolean updateValue(String valName, String val) {
			boolean returnVal = false;
			if (LocationProcessor.this.headerMap.containsValue(valName) && this.valueMap.get(valName) != null) {
				this.valueMap.put(valName, val);
				returnVal = true;
			}
			return returnVal;
		}

		/**
		 * Updates one value for the given name. If not existent the value is
		 * added
		 * 
		 * @param valName the value key in the csv-file.
		 * @param val the value to set.
		 */
		public void updateOrAddValue(String valName, String val) {
			if (LocationProcessor.this.headerMap.containsValue(valName) && this.valueMap.get(valName) != null) {
				this.valueMap.put(valName, val);
			} else {
				this.valueMap.put(valName, val);
			}
		}

		/**
		 * This method gets the value for a specified column-index.
		 * 
		 * @param valID
		 *            The column index
		 * @return Returns the stored value or null if the value is not set or
		 *         the column is unknown
		 */
		public String getValue(int valID) {
			String valName = LocationProcessor.this.headerMap.get(valID);
			if (valName != null) {
				return this.valueMap.get(valName);
			}
			return null;
		}

		/**
		 * This method gets the value for a specified value name
		 * 
		 * @param valName
		 *            The name of the value
		 * @return Returns the stored value or null if the value is not set or
		 *         the value name is unknown
		 */
		public String getValue(String valName) {
			if (LocationProcessor.this.headerMap.containsValue(valName)) {
				return this.valueMap.get(valName);
			}
			return null;
		}

		public String getID(){
			if(LocationProcessor.this.posOfIdColumn>=0){
				return this.getValue(LocationProcessor.this.posOfIdColumn);				
			}
			else{
				return null;
			}
		}

		public void setID(String id){
			if(LocationProcessor.this.posOfIdColumn>=0){
				this.updateValue(LocationProcessor.this.posOfIdColumn, id);				
			}
		}

		/***
		 *
		 * @param entry
		 */
		public Location(String entry) {
			entry = entry.trim();
			// put double quotes into empty tags
			String tmp = entry.replaceAll(";;", ";\"\";");

			while (tmp.length() != entry.length()) {
				entry = tmp;
				tmp = entry.replaceAll(";;", ";\"\";");
			}
			if (entry.endsWith(";"))
				entry = entry + "\"\"";

			StringTokenizer tok = new StringTokenizer(entry, ";");
			int i = 0;
			String token;
			if (tok.countTokens() != LocationProcessor.this.headerMap.size()) {
				System.out.println("Error: Found " + tok.countTokens() + " tokens. Expected: " + LocationProcessor.this.headerMap.size() + " tokens.");
				while (tok.hasMoreTokens()) {
					System.out.println(tok.nextToken());
				}
				return;
			}
			StringBuilder allForHash = new StringBuilder();
			while (tok.hasMoreTokens()) {
				token = tok.nextToken();
				token = token.trim();
				if (!token.equals("\"\"") || (Constants.ENABLE_CHANGES_OF_MARCO && token.length() > 0)) {
					if (!this.addValue(i, token)) {
						System.out.println(" Unknown token at column " + (i + 1) + " value: " + token);
						return;
					} else {
						if (i != LocationProcessor.this.posOfIdColumn) {
							allForHash.append(token);
						}
					}
				}
				++i;
			}
			correctInitialized = true;
			myHash = allForHash.toString().hashCode();
		}

		/***
		 *
		 * @return
		 */
		public String locationToString() {
			StringBuilder csvString = new StringBuilder();
			String separator = ";";
			for (int i = 0; i < LocationProcessor.this.headerMap.size(); ++i) {
				// last entry should not finish with a separator
				if (i == LocationProcessor.this.headerMap.size() - 1)
					separator = "";

				if (this.getValue(i) != null) {
					// value
					if(LocationProcessor.this.headerMap.get(i).equals("Fläche/Besucher etc pro Tag")){
						csvString.append(this.getValue(i).replaceAll(" ", "")).append(separator);
					}
					else{
						csvString.append(this.getValue(i)).append(separator);
					}
				} else {
					// null
					csvString.append("\"\"").append(separator);
				}
			}
			return csvString + "\n";
		}

		/**
		 * Method which returns true if everything is the same except the "id"
		 * which indicates a duplicate import
		 * 
		 * @param loc
		 *            the test location
		 * @return true if they differ only in the location id-field
		 */

		public boolean isDuplicateImport(Location loc) {
			// boolean returnVal = true;
			// for(int i=0; i< LocationProcessor.this.headerMap.size() &&
			// returnVal;++i){
			// if(!LocationProcessor.this.headerMap.get(i).equals("Loc_ID")){
			// if((loc.getValue(i)!=null && this.getValue(i)!=null)){ //both
			// exist
			// returnVal = loc.getValue(i).equals(this.getValue(i)); //are they
			// the same?
			// }
			// else if(loc.getValue(i)==null && this.getValue(i)==null){//both
			// do not exist
			// returnVal = true;
			// }
			// else{
			// returnVal=false; //one exist one not
			// }
			// }
			// }
			// return returnVal;
			return this.myHash == loc.myHash;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		public int getStatus() {
			return status;
		}

	}

	public String locationToString(Location loc) {
		StringBuilder csvString = new StringBuilder();
		String separator = ";";
		for (int i = 0; i < this.headerMap.size(); ++i) {
			// last entry should not finish with a separator
			if (i == this.headerMap.size() - 1)
				separator = "";

			if (loc.getValue(i) != null) {
				// value
				csvString.append(loc.getValue(i)).append(separator);
			} else {
				// null
				csvString.append("\"\"").append(separator);
			}
		}
		return csvString + "\n";
	}

	@SuppressWarnings("unused")
	public boolean hasDuplicate(Location loc, int startSearchAt) {
		// we have a lot of doctors which share the same location
		if(!Constants.ENABLE_CHANGES_OF_MARCO){
			if (loc.getValue("Typ der Gelegenheit").equals("Gesundheit"))
				return false;
			else 
				if (loc.getValue("Quelle") != null && loc.getValue("Quelle").equals("Klicktel")) // a
																									// lot
																									// of
																									// services
																									// share
																									// the
																									// same
																									// location,
																									// e.g.
																									// shopping
																									// center
				return false;
		}
		for (int i = startSearchAt; i < this.getLocationsForRelocation().size(); ++i) {
			Location locRef = this.getLocationsForRelocation().get(i);
			if (locRef.isDuplicateImport(loc))
				return true;
		}
		return false;
	}

	/**
	 * @param args
	 *            0: filename for input, 1: filename for output, 2: mode
	 *            (process name, generate address), 3: db-config
	 */
	public static void main(String[] args) {
		//String inputPath = "C:\\Users\\breh_ma\\Desktop\\relocating\\DFG.csv";
		// String inputPath =
		// "Z:\\Austausch_FN_RC_FingerWeg\\DFG_Kurze_Wege\\Output\\Für die Verortung\\DFG-Datensatz_MF.csv";
//		String outputPath = "C:\\Users\\breh_ma\\Desktop\\relocating\\MF";
//		String runtimeConfig = "T:\\Simulationen\\runtime.csv";
//		args = new String[]{inputPath, outputPath, runtimeConfig};
		LocationProcessor worker = new LocationProcessor();

		if (!(args.length == 2 || args.length == 3)) {
			System.out.println("Wrong number of arguments!\nUsage: LocationProcessor <input> <output> <if generate: db-config>");
		}

		FileReader in = null;
		BufferedReader input = null;
		String line, header;
		FileWriter outBad, outNoAction, outGood, outNoAdress, outRelocated, outRelocatedError, outNotInRegion, outDuplicate, outArea, outNotDistinctDBResultError;
		Locator locationGenerator;
		try {
			String streetValue, houseNumber;
			StreetnameProcessor processor = new StreetnameProcessor();
			in = new FileReader(args[0]);
			outGood = new FileWriter(args[1] + "_transformed.csv");
			outBad = new FileWriter(args[1] + "_bad.csv");
			outNoAction = new FileWriter(args[1] + "_noAction.csv");
			outNoAdress = new FileWriter(args[1] + "_noAdress.csv");
			input = new BufferedReader(in);
			// read header
			header = input.readLine();
			worker.loadHeaders(header);

			// write header in files
			outGood.write(header + "\n");
			outBad.write(header + "\n");
			outNoAction.write(header + "\n");
			outNoAdress.write(header + "\n");

			int maxID = 0;
			while ((line = input.readLine()) != null) {

				Location tmp = worker.new Location(line);
				if (tmp.isCorrectInitialized()) {
					streetValue = tmp.getValue("Straße");
					houseNumber = tmp.getValue("Hausnummer");
					if(tmp.getValue("Loc_ID")!=null)
						maxID = Math.max(maxID, Integer.parseInt(tmp.getValue("Loc_ID")));
					
					//worker.addLocationToRelocate(tmp);
					
					if (streetValue != null) {
						if (houseNumber == null) {
							worker.addLocationToAddressProcessing(tmp);
						} else {
							// adress ok
							outNoAction.write(line + "\n");
							worker.addLocationToRelocate(tmp);
						}
					} else {
						// no adress!
						outNoAdress.write(line + "\n");
						worker.addLocationToRelocate(tmp);
					}
				} else {
					System.out.println("Can not read: " + line);
				}
			}
			input.close();
			outNoAction.close();
			outNoAdress.close();
			System.out.println("Max id: " + maxID);

//			outRelocated = new FileWriter(args[1] + "_relocated.csv");
//			outRelocated.write(header + "\n");
//			for (int i = 0; i < worker.getLocationsForRelocation().size(); ++i) {
//				Location loc = worker.getLocationsForRelocation().get(i);
//				outRelocated.write(loc.locationToString());
//			}
//			outRelocated.close();
			
			for (Location loc : worker.getLocationsForAddressProcessing()) {
				// set the city in the processor
				String cityValue = loc.getValue("Stadt / Kreis Bezeichnung");
				streetValue = loc.getValue("Straße");
				processor.setCity(cityValue);

				if (processor.processStreet(streetValue)) {
					loc.updateValue("Straße", processor.getStreet());
					if (processor.getStreetNumber() != null && loc.getValue("Hausnummer") == null)
						loc.addValue("Hausnummer", processor.getStreetNumber());
					if (processor.getZipCode() != null && loc.getValue("PLZ") == null)
						loc.addValue("PLZ", processor.getZipCode());
					outGood.write(loc.locationToString());
				} else {
					System.out.println("Can not process: " + streetValue);
					outBad.write(loc.locationToString());
				}
				worker.addLocationToRelocate(loc);
			}
			outGood.close();
			outBad.close();
			if (args.length == 3) {

				// init and open files
				TPS_ParameterClass parameterClass = new TPS_ParameterClass();
				parameterClass.loadRuntimeParameters(new File(args[2]));
				locationGenerator = new Locator(new TPS_DB_Connector(parameterClass), Constants.ENABLE_CHANGES_OF_MARCO);
				outRelocated = new FileWriter(args[1] + "_relocated.csv");
				outRelocatedError = new FileWriter(args[1] + "_not_relocated.csv");
				outNotDistinctDBResultError = new FileWriter(args[1] + "_not_unique_db_result.csv");
				outNotInRegion = new FileWriter(args[1] + "_not_in_region.csv");
				outDuplicate = new FileWriter(args[1] + "_duplicate.csv");
				outArea = new FileWriter(args[1] + "_area.csv");
				outRelocated.write(header + "\n");
				outRelocatedError.write(header + "\n");
				outNotDistinctDBResultError.write(header + "\n");
				outNotInRegion.write(header + "\n");
				outDuplicate.write(header + "\n");
				outArea.write(header + "\n");
				// process locations
				int counter = 0;
				HashMap<String,String> strangeStreets = new HashMap<>();
				for (int i = 0; i < worker.getLocationsForRelocation().size(); ++i) {
					Location loc = worker.getLocationsForRelocation().get(i);
					counter++;
					if (counter % 100 == 0 || counter == worker.getLocationsForRelocation().size()) {
						System.out.println("Processed " + counter + " from " + worker.getLocationsForRelocation().size() + " locations");
					}
					//loc.setID(Integer.toString(counter)); // reindex to avoid stupid double indices!
					if (worker.hasDuplicate(loc, i + 1)) {
						outDuplicate.write(loc.locationToString());
					} else {
						if (loc.getValue("KoordTyp") != null && loc.getValue("KoordTyp").equals("Centroid")) {
							outArea.write(loc.locationToString());
						} else {
							if (loc.getValue("Straße") != null)
								loc.updateValue("Straße", processor.replaceStreetTag(loc.getValue("Straße")));
							if (locationGenerator.localizeLocation(loc)) {
								outRelocated.write(loc.locationToString());
							} else {
								if (loc.getStatus() == Location.STATUS_NOT_DISTINCT_IN_DATABASE) {
									outNotDistinctDBResultError.write(loc.locationToString());
									strangeStreets.put(loc.getValue("Straße")+" "+loc.getValue("Hausnummer"), loc.getID());
								} else if (locationGenerator.isInRegion(loc))
									outRelocatedError.write(loc.locationToString());
								else {
									outNotInRegion.write(loc.locationToString());
								}
							}
						}
					}
				}
				// close files
				outRelocated.close();
				outRelocatedError.close();
				outNotDistinctDBResultError.close();
				outNotInRegion.close();
				outDuplicate.close();
				outArea.close();
				FileWriter strangeStreetnames = new FileWriter(args[1] + "_strangers.csv");
				for(Entry<String, String> entry: strangeStreets.entrySet()){
					strangeStreetnames.append(entry.getKey()+";"+entry.getValue()+"\n");
				}
				strangeStreetnames.close();
				
			}
		} catch (Throwable ex) {
			System.out.println("\t '--> Error: " + ex.getMessage());
			ex.printStackTrace();
		}// catch
		finally {
			try {
				if (input != null)
					input.close();
				if (in != null)
					in.close();
			}// try
			catch (IOException ex) {
				System.out.println("\t '--> Could not close : " + args[0]);
			}// catch
		}// finally

	}

}
