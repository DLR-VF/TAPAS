package de.dlr.ivf.scripts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
/**
 * Class which realizes the DB-Import for a DISCO-Sample from STATA.
 * 
 * This class handles the table creation, DB import and car clustering
 * @author hein_mh
 *
 */
public class DiscoImporter extends TPS_BasicConnectionClass {

	String schema = "core";
	final String tablename_households = "_disco";
	final String tablename_cars = "_car";
	final String tablename_alternatives = "_alternatives";
	/**
	 * Method to create the tables for the given basename. Existing tables will be deleted!
	 * @param basename
	 */
	public void createTables(String basename){
		String tableBasename = schema+"."+basename; // extend the basename with the given schema
		//create the household table
		String query = "DROP TABLE IF EXISTS "+tableBasename+tablename_households;
		this.dbCon.execute(query, this);

		query = "CREATE TABLE "+tableBasename+tablename_households +
				"(" +
				"	hhid integer," +
				"   hh_gew double precision," +
				"   polgk integer," +
				"   male_fs integer," +
				"   alt integer," +
				"   size1 integer," +
				"   fuel1 integer," +
				"	vmt1 integer," +
				"   size2 integer," +
				"   fuel2 integer," +
				"   vmt2 integer," +
				"   value1 double precision," +
				"   cost1 double precision," +
				"   tax1 double precision," +
				"   value2 double precision," +
				"   cost2 double precision," +
				"   tax2 double precision," +
				"   ewqkm integer," +
				"   n_cars integer," +
				"   large integer," +
				"   erwerb double precision," +
				"   p double precision," +
				"   income double precision," +
				"   fs integer," +
				"   hhsize integer," +
				"   CONSTRAINT "+basename+tablename_households+"_pkey PRIMARY KEY (hhid, alt)" +
				") WITH (  OIDS = FALSE);";

		this.dbCon.execute(query, this);

		//create the car table
		query = "DROP TABLE IF EXISTS "+tableBasename+tablename_cars;
		this.dbCon.execute(query, this);

		query = "CREATE TABLE "+tableBasename+tablename_cars +
				"(" +
				"	car_id integer," +
				"   size integer," +
				"   fuel integer," +
				"	vmt integer," +
				"   value double precision," +
				"   cost double precision," +
				"   tax double precision," +
				"   CONSTRAINT "+basename+tablename_cars+"_pkey PRIMARY KEY (car_id)" +
				") WITH (  OIDS = FALSE);";

		this.dbCon.execute(query, this);

		//create the alternative table
		query = "DROP TABLE IF EXISTS "+tableBasename+tablename_alternatives;
		this.dbCon.execute(query, this);

		query = "CREATE TABLE "+tableBasename+tablename_alternatives +
				"(" +
				"	alt integer," +
				"   num_car integer," +
				"   car_ids integer[]," +
				"   CONSTRAINT "+basename+tablename_alternatives+"_pkey PRIMARY KEY (alt)" +
				") WITH (  OIDS = FALSE);";

		this.dbCon.execute(query, this);
	}
	
	/**
	 * Method to import the data from the given file to the household table
	 * @param basename the tablename to store the data to
	 * @param fileName the file to read the data from
	 */
	public void fillTables(String basename, String fileName){
		String query = "";
		String tableName = schema+"."+basename+tablename_households; // extend the basename with the given schema
		try {
			
			BufferedReader fr= new BufferedReader(new FileReader(fileName));
			String line;
			String[] tokens;
			int i;
            int counter;
            int chunkSize =1024;
            int[] check;
            int globalCounter=0;
            int fs;
            int hh_size;
            PreparedStatement st = dbCon.getConnection(this).prepareStatement("INSERT INTO "+tableName+" VALUES ("+
						"?,?,?,?,?,"+
						"?,?,?,?,?,"+
						"?,?,?,?,?,"+
						"?,?,?,?,?,"+
						"?,?,?,?,? )");

			fr.readLine(); globalCounter++;//header
			
			counter =0;
						
			while((line = fr.readLine())!=null){
				globalCounter++;
				tokens = line.split(",");
				//correct number of tokens
				if(tokens.length!=25)
					continue;
				
				//get driver license number
				fs = Integer.parseInt(tokens[23]);				
				//calculate the number of male peoples with driver license
				fs= (int)Math.round(fs*Double.parseDouble(tokens[3]));
				
				//TODO: this can become wrong, if a 4 person household is transformed below 3.5!  
				//calculate household size
				hh_size = (int)Math.round(Double.parseDouble(tokens[24]));
				
				i=0;
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++; 		//hhid
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++; 	//hh_gew
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++; 		//polgk
				st.setInt(i+1, fs); i++;								//male_fs
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//alt
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//size1
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//fuel1
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//vmt1
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//size2
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//fuel2
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//vmt2
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//value1
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//cost1
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//tax1
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//value2
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//cost2
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//tax2
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//ewqkm
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//n_cars
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//large
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//erwerb
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//p
				st.setDouble(i+1, Double.parseDouble(tokens[i])); i++;	//income
				st.setInt(i+1, Integer.parseInt(tokens[i])); i++;		//fs
				st.setInt(i+1, hh_size); i++;							//hhsize
				
				st.addBatch();
				counter++;
				if(counter%chunkSize==0){
					check = st.executeBatch();
					for(int j=0; j<check.length;++j){
						if(check[j] == java.sql.PreparedStatement.EXECUTE_FAILED){
							System.err.println("Error in statement for line number "+(globalCounter-check.length+j));
						}
					}
					counter = 0;
				}				
			}			
			check = st.executeBatch(); //flush remainers
			for(int j=0; j<check.length;++j){
				if(check[j] == java.sql.PreparedStatement.EXECUTE_FAILED){
					System.err.println("Error in statement for line number "+(globalCounter-check.length+j));
				}
			}
			fr.close();
			
			
		} catch (SQLException e) {
			System.err.println("Error during query: " +query);
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	/**
	 * This method finds all distinct car groups and stores them in the car-table
	 * @param basename the table name
	 */
	public void fillCars(String basename){
		String query = "";
		String tableName = schema+"."+basename; // extend the basename with the given schema
		try {
			ResultSet rs, rs2;
			int i=0;
			query="SELECT DISTINCT value1, tax1, cost1, size1, fuel1, vmt1 FROM "+tableName+tablename_households+" ORDER BY size1, fuel1, vmt1, cost1, value1, tax1";
			rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				//see if we have this car
				query = "SELECT car_id FROM " +tableName+this.tablename_cars+" WHERE " +
						"value	="+rs.getDouble("value1")+" AND " +
						"tax  	="+rs.getDouble("tax1"  )+" AND " +
						"cost	="+rs.getDouble("cost1" )+" AND " +
						"size	="+rs.getInt("size1" )+" AND " +
						"fuel	="+rs.getInt("fuel1" )+" AND " +
						"vmt	="+rs.getInt("vmt1"  );
				rs2 = this.dbCon.executeQuery(query, this);
				if(rs2.next()){ //we have it!
					rs2.close();
					continue; // next element
				}
				rs2.close();
				//new car: insert into db
				query = "INSERT INTO " +tableName+this.tablename_cars+" VALUES( " +
						i+", "
						+rs.getInt("size1" )+", " +
						+rs.getInt("fuel1" )+", " +
						+rs.getInt("vmt1"  )+", " +
						+rs.getDouble("value1")+", " +
						+rs.getDouble("cost1" )+", " +
						+rs.getDouble("tax1"  )+")";
				this.dbCon.execute(query, this);
				i++;
			}
			rs.close();
			//now the second car
			query="SELECT DISTINCT value2, tax2, cost2, size2, fuel2, vmt2 FROM "+tableName+tablename_households+" ORDER BY size2, fuel2, vmt2, cost2, value2, tax2";
			rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				//see if we have this car
				query = "SELECT car_id FROM " +tableName+this.tablename_cars+" WHERE " +
						"value	="+rs.getDouble("value2")+" AND " +
						"tax  	="+rs.getDouble("tax2"  )+" AND " +
						"cost	="+rs.getDouble("cost2" )+" AND " +
						"size	="+rs.getInt("size2" )+" AND " +
						"fuel	="+rs.getInt("fuel2" )+" AND " +
						"vmt	="+rs.getInt("vmt2"  );
				rs2 = this.dbCon.executeQuery(query, this);
				if(rs2.next()){ //we have it!
					rs2.close();
					continue; // next element
				}
				rs2.close();
				//new car: insert into db
				query = "INSERT INTO " +tableName+this.tablename_cars+" VALUES( " +
						i+", "+
						+rs.getInt("size2" )+", " +
						+rs.getInt("fuel2" )+", " +
						+rs.getInt("vmt2"  )+", " +
						+rs.getDouble("value2")+", " +
						+rs.getDouble("cost2" )+", " +
						+rs.getDouble("tax2"  )+")";
				this.dbCon.execute(query, this);
				i++;
			}
			rs.close();
			

			
			
		} catch (SQLException e) {
			System.err.println("Error during query: " +query);
			e.printStackTrace();
		}
	}
	/**
	 * This Method finds the corresponding car-IDs for car1 and car2 in the household table
	 * @param basename the basename of the two tables
	 */
	public void findCarIDs(String basename){
		String query = "";
		String tableName = schema+"."+basename; // extend the basename with the given schema
		try {
			
			//add two new columns and delete old ones if existing			
			query="ALTER TABLE "+tableName+tablename_households+" DROP COLUMN IF EXISTS car1_id";
			this.dbCon.execute(query, this);
			query="ALTER TABLE "+tableName+tablename_households+" ADD COLUMN car1_id integer";
			this.dbCon.execute(query, this);
			query="ALTER TABLE "+tableName+tablename_households+" DROP COLUMN IF EXISTS car2_id";
			this.dbCon.execute(query, this);
			query="ALTER TABLE "+tableName+tablename_households+" ADD COLUMN car2_id integer";
			this.dbCon.execute(query, this);
			
			//now update the column car1			
			query= "UPDATE "+tableName+tablename_households+" AS hh SET car1_id= " +
					"	(SELECT car_id FROM "+tableName+this.tablename_cars+" AS car WHERE " +
						"car.value	= hh.value1 AND "+
						"car.tax  	= hh.tax1 AND " +
						"car.cost	= hh.cost1 AND " +
						"car.size	= hh.size1 AND " +
						"car.fuel	= hh.fuel1 AND " +
						"car.vmt	= hh.vmt1)";			
			this.dbCon.execute(query, this);
			
			//now update the column car2			
			query= "UPDATE "+tableName+tablename_households+" AS hh SET car2_id= " +
					"	(SELECT car_id FROM "+tableName+this.tablename_cars+" AS car WHERE " +
						"car.value	= hh.value2 AND "+
						"car.tax  	= hh.tax2 AND " +
						"car.cost	= hh.cost2 AND " +
						"car.size	= hh.size2 AND " +
						"car.fuel	= hh.fuel2 AND " +
						"car.vmt	= hh.vmt2)";			
			this.dbCon.execute(query, this);

			//now fill the alternative-table
			query="SELECT DISTINCT alt, n_cars, car1_id, car2_id FROM  "+tableName+tablename_households+" ORDER BY alt, n_cars, car1_id, car2_id";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			int numCar;
			while(rs.next()){
				//get the data
				numCar = rs.getInt("n_cars");
				//prepare statement
				query = "INSERT INTO "+tableName+tablename_alternatives+" VALUES (" +rs.getInt("alt")+", "+numCar +", ";
				//construct car array
				switch(numCar){
				case 0:
					query +="NULL";
					break;
				case 1:
					query +="'{"+rs.getInt("car1_id")+"}'";
					break;
				case 2:
					query +="'{"+rs.getInt("car1_id")+","+rs.getInt("car2_id")+"}'";
					break;
				}				
				
				//finish query and fire!
				query +=")";
				this.dbCon.execute(query, this);
			
			}
			
		} catch (SQLException e) {
			System.err.println("Error during query: " +query);
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//String[] names = {"TAPAS_BERLIN_2010","TAPAS_POLGK1_2010","TAPAS_POLGK2_2010","TAPAS_POLGK3_2010","TAPAS_POLGK4_2010","TAPAS_POLGK5_2010","TAPAS_POLGK6_2010","TAPAS_POLGK45_2010"};
		String[] names = {"TAPAS_POLGK1_2030","TAPAS_POLGK2_2030","TAPAS_POLGK3_2030","TAPAS_POLGK4_2030","TAPAS_POLGK5_2030","TAPAS_POLGK6_2030","TAPAS_POLGK45_2030"};
			
		for(String name : names){
			DiscoImporter worker = new DiscoImporter();
			worker.createTables(name);
			worker.fillTables(name, "W:\\"+name+".csv");
			worker.fillCars(name);
			worker.findCarIDs(name);
		}

	}

}
