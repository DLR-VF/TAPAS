package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.Matrix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TPS_ParkingSpaceAproximator extends TPS_BasicConnectionClass {

	Map<Integer, Integer> personsPerTaz = new HashMap<>();
	Map<Integer, Integer> carsPerTaz = new HashMap<>();
	Map<Integer, Integer> areaPerTaz = new HashMap<>();
	int maxTAZ=Integer.MIN_VALUE, minTAZ = Integer.MAX_VALUE, numberOfTaz =0;
	double[] access = new double[0];
	double[] egress = new double[0];
	double maxEgress=0;
	double minEgress=Integer.MAX_VALUE;
	double maxAccess=0;
	double minAccess=Integer.MAX_VALUE;
	
	public void loadPersonsAndCars(String db, String key){
		String query ="";
		try{
			query = "SELECT hh_taz_id, sum(hh_persons)::integer as persons, sum(hh_cars)::integer as cars FROM "+db+" WHERE hh_key= '"+key+"' group by hh_taz_id";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			int taz;
			int persons;
			int cars;
			while(rs.next()){				
				taz = rs.getInt("hh_taz_id");
				persons = rs.getInt("persons");
				cars = rs.getInt("cars");
				this.personsPerTaz.put(taz, persons);
				this.carsPerTaz.put(taz, cars);
			}		
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" readParameters: SQL-Error during statement: "+query);
			e.printStackTrace();
		}
	}
	
	public void loadTazArea(String db){
		String query ="";
		try{
			query = "SELECT gid, st_area(geography(the_geom))::integer as sqrmeter FROM "+db;
			ResultSet rs = this.dbCon.executeQuery(query, this);
			int taz;
			int area;
			while(rs.next()){
				taz = rs.getInt("gid");
				area = rs.getInt("sqrmeter");
				this.areaPerTaz.put(taz, area);
				maxTAZ = Math.max(maxTAZ,  taz);
				minTAZ = Math.min(minTAZ,  taz);
			}			
			numberOfTaz= 1+maxTAZ-minTAZ;
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" readParameters: SQL-Error during statement: "+query);
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Theory behind it:
	 * 
	 * 1. Searching for a parking lot:
	 * The probability to find a parking lot in a area with a fixed 
	 * availability of parking spaces increases linearly with the 
	 * street length covered.
	 * The availability of parking lots decreases linearly with the density 
	 * of cars is a specific region. 
	 * ->Extension: We found this true for the first 90%! So interpolate between 0 and the 80%-Median
	 * So we interpolate between a given minimum (e. g. 10 m) and 
	 * maximum search distance (e.g. 1500m). 
	 * Additionally a search speed is needed to calculate the search time.
	 * 
	 * 2. Going back to your original destination
	 * If you found a parking lot, you have to walk back to your 
	 * original destination. But the search-strategy is most probable a spiral
	 * around the original destination. We assume grid-blocks (Manhattan style).
	 * The search strategy is as follows: 
	 * First round you drive around the block, called center block (4 Streets)
	 * Second round you drive around the cross around the center block (12 streets -draw it!)
	 * Third round you drive around the diamond arount the cros (20 streets)
	 * Forth round 28 Streets...
	 * 
	 * When you draw it you see that the length of the search line increases 
	 * by eight segments every round (plus + street to the next circle, 
	 * but we ommit that ;) )
	 * 
	 * The length of going back to the origin is the number of rounds but the 
	 * length of the search distance is the sum of street segments for all rounds
	 * 
	 * So if you know the block distance, a walking speed and have a search 
	 * length for finding a lot, you can calculate the walking distance 
	 * between lot and destination.
	 * 
	 * @param minSearch the minimum search distance in meters eg 10m
	 * @param maxSearch the maximum search distance in meters e.g. 1500m
	 * @param searchSpeedCar the speed during the search in m/s (e.g. 14.4km/h = 4m/s)
	 * @param blockDistance the average block length in meters (e.g. 1000)
	 * @param walkSpeed the speed going back in m/s (e.g. 3.6km/h = 1m/s)
	 * @param saturationFactor is the factor between 0 and 1, how many regions are affected by less than the maximum search radius.
	 */

	public void calcParkingTimes(double minSearch, double maxSearch, double searchSpeedCar, double blockDistance, double walkSpeed, double saturationFactor){
		this.access = new double[this.numberOfTaz];
		this.egress = new double[this.numberOfTaz];
		double[] egressSort = new double[this.numberOfTaz];

		double highestDensity = 0, lowestDensity = 1e99, density;
		int carsPerTaz, areaPerTaz;
		//first calc the density and find the normalization factors
		for(Integer taz: this.carsPerTaz.keySet()){
			carsPerTaz = this.carsPerTaz.get(taz);
			areaPerTaz = this.areaPerTaz.get(taz);
			density = ((double)carsPerTaz)*1000000.0/(double)areaPerTaz;
			this.egress[taz-minTAZ]=density;
			egressSort[taz-minTAZ]=density;
			highestDensity = Math.max(highestDensity, density);
			lowestDensity = Math.min(lowestDensity, density);
		}
		
		saturationFactor = Math.max(0.,Math.min(1.0, saturationFactor));
		
		Arrays.sort(egressSort);
		
		lowestDensity = egressSort[0];
		int indexSaturation = (int)((egressSort.length-1)*saturationFactor);
		//chop to reasonabele values
		indexSaturation = Math.max(1, Math.min(egressSort.length-1, indexSaturation));
		highestDensity = egressSort[indexSaturation];
		
		//now normalize the density to the min/max searchradius
		for(int i=0; i< egress.length;++i){
			//normalize from 0 to 1
			this.egress[i]= (egress[i]-lowestDensity)/(highestDensity-lowestDensity);			
			//extract from min to max
			this.egress[i] = egress[i]*(maxSearch-minSearch)+minSearch;			
			//in egress is now the search distance to find a parking lot			
			//now calculate the walking time back and put it in access
			int round=1; 
			double distanceCovered= 4* blockDistance;
			while(distanceCovered<egress[i]){
				round++;
				distanceCovered += 8* blockDistance+distanceCovered;
			}
			//ok we have now the number of rounds to circle around the destination
			//now calculate the time to get there:
			this.access[i] = round* blockDistance/walkSpeed;
			this.maxAccess= Math.max(this.maxAccess, this.access[i]);
			this.minAccess= Math.min(this.minAccess, this.access[i]);
			
			//ok now we calculate the egress time: 
			//search distance/search speed + walking time back
			this.egress[i] = this.egress[i]/searchSpeedCar + this.access[i];
			this.maxEgress= Math.max(this.maxEgress, this.egress[i]);
			this.minEgress= Math.min(this.minEgress, this.egress[i]);
		}		
	}
	
	
	public void storeSQLMatrix(String db, String nameEgress, String nameAccess){
		//delete old values
		String query = "DELETE FROM "+db+" WHERE matrix_name= '"+nameEgress+"'";
		this.dbCon.execute(query, this);
		query = "DELETE FROM "+db+" WHERE matrix_name= '"+nameAccess+"'";
		this.dbCon.execute(query, this);

		//create the access/egress matrix:
		//Egress: all values TO this cell have the same egress
		//Access: all values FROM this cell have the same access
		Matrix egressM = new Matrix(this.egress.length);
		Matrix accessM = new Matrix(this.access.length);
		for(int i=0; i< this.egress.length; ++i){
			for(int j = 0; j<this.egress.length; ++j){
				egressM.setValue(i, j, this.egress[i]); //same destination (i)
				accessM.setValue(i, j, this.access[j]); //same origin (j)
			}
		}

		//save to db!
		query = "INSERT INTO "+db +	" (\"matrix_name\",\"matrix_values\")" + " VALUES('" + nameEgress + "',";
		// build array of matrix-ints
		query +=TPS_DB_IO.matrixToSQLArray(egressM, 0);
		query += ")";
		this.dbCon.execute(query, this);

		query = "INSERT INTO "+db +
				" (\"matrix_name\",\"matrix_values\")"
				+ " VALUES('" + nameAccess + "',";
		// build array of matrix-ints
		query +=TPS_DB_IO.matrixToSQLArray(accessM, 0);
		query += ")";
		this.dbCon.execute(query, this);
	}
	
	
	public static void main(String[] args) {
		String key= "VEU2_MID2008_Y2010_REF";
		String accessName= "MIT_ACCESS_1223_"+key;
		String egressName= "MIT_EGRESS_1223_"+key;
		String region= "berlin";
		//lets boogie!
		TPS_ParkingSpaceAproximator worker = new TPS_ParkingSpaceAproximator();
		System.out.println("Loading tazes and areas for region "+region);
		worker.loadTazArea("core."+region+"_taz_1223_multiline");
		System.out.println("Found "+worker.numberOfTaz+" tazes!");

		System.out.println("Loading households and cars for key "+key);
		worker.loadPersonsAndCars("core."+region+"_households_1223", key);
		System.out.println("Calc parking times");		
		worker.calcParkingTimes(10, 1000, 2.8, 100, 1,0.9);
		System.out.println("Access min/max: "+worker.minAccess+"/"+worker.maxAccess);
		System.out.println("Egress min/max: "+worker.minEgress+"/"+worker.maxEgress);
		System.out.println("Save access/egress "+accessName+"-"+egressName);		
		worker.storeSQLMatrix("core."+region+"_matrices", egressName, accessName);
		System.out.println("Finished");		
		
		

	}

}
