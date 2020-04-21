package de.dlr.ivf.scripts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

/**
 * This script was used in the project of ecoMove to build a new car fleet.
 * Kept for historic reasons.
 */

public class CarFleetAdjuster extends TPS_BasicConnectionClass {
	String keyVal ="MID2008_Y2008";
	String newKeyVal ="ecoMove_2008_100";
	String tableName ="core.berlin_cars";
	
	class ValueDistribution{
		int newKBA;
		double probability;
	}
	class Car{
		int car_id=0, kba_no=0, engine_type=0, emission_type =0;
		boolean is_company_car=false,restriction=false;
		String car_key;
		double fix_costs=0;
	}
	
	Map<Integer,ValueDistribution> kbaValueMapping = new HashMap<>();
	Map<Integer,Car> cars = new HashMap<>();
	
	
	
	public void loadKBAValueMapping(String fileName){
		FileReader in; BufferedReader input; String line;
		try
		{
			int oldKBA, newKBA;
			double prob;
			// read the file for mapping-values
			in = new FileReader (fileName);
			input = new BufferedReader (in);
			while((line = input.readLine()) != null)
			{
				String[] tokens = line.split(";");
				if(tokens.length==3){
					oldKBA = Integer.parseInt(tokens[0]);
					newKBA = Integer.parseInt(tokens[1]);
					prob= Double.parseDouble(tokens[2]);
					ValueDistribution dist = new ValueDistribution();
					dist.newKBA=newKBA; //the new kba segment
					dist.probability = prob; // the mapping probability
					this.kbaValueMapping.put(oldKBA, dist);
				}
			}
			input.close();
			in.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		
	}
	
	
	private Car extractCarData(ResultSet rs){
		Car car = new Car();
		try{
			car.car_id = rs.getInt("car_id");
			car.car_key = rs.getString("car_key");
			if(rs.getObject("emission_type")!=null)
				car.emission_type = rs.getInt("emission_type");
			if(rs.getObject("kba_no")!=null)
				car.kba_no = rs.getInt("kba_no");
			if(rs.getObject("engine_type")!=null)
				car.engine_type = rs.getInt("engine_type");
			if(rs.getObject("is_company_car")!=null)
				car.is_company_car = rs.getBoolean("is_company_car");
			if(rs.getObject("restriction")!=null)
				car.restriction = rs.getBoolean("restriction");
			if(rs.getObject("fix_costs")!=null)
				car.fix_costs = rs.getDouble("fix_costs");
		}
		catch (SQLException e){
			e.printStackTrace();
		}
		
		return car;
	}
	
	public void makeModifiedFleet(){
		String query="";
		ResultSet rs;
		try{
			//load cars
			query = "SELECT car_id, kba_no, engine_type, is_company_car, car_key, emission_type, restriction, fix_costs FROM "+this.tableName+" WHERE car_key = '"+this.keyVal+"'";
			rs = dbCon.executeQuery(query, this);
			while(rs.next()){
				Car car = this.extractCarData(rs);
				this.cars.put(car.car_id, car);
			}
			rs.close();
			
			//modify cars
			for(Car car: this.cars.values()){
				double random = Math.random();
				ValueDistribution dist = this.kbaValueMapping.get(car.kba_no);
				if(dist==null){
					System.out.println(car.kba_no);
				}
				else{
					if(dist.probability>=random){
						car.kba_no = this.kbaValueMapping.get(car.kba_no).newKBA;
					}
				}
				car.car_key=this.newKeyVal;
			}

			//delete old entries
			query = "DELETE FROM "+this.tableName+" WHERE car_key = '"+this.newKeyVal+"'";
			dbCon.execute(query, this);

			//save back with a prepared statement
			
			PreparedStatement pS;
		
				
			query = "INSERT INTO "+this.tableName+" ( car_id, kba_no, engine_type, is_company_car, car_key, emission_type, restriction, fix_costs ) VALUES (?,?,?,?,?,?,?,?)";
			pS = this.dbCon.getConnection(this).prepareStatement(query);
			int count=0;
			//now parse the values in the prepared statement
			for(Car car: this.cars.values()){
			
				pS.setInt(1, car.car_id);
				pS.setInt(2, car.kba_no);
				pS.setInt(3, car.engine_type);
				pS.setBoolean(4, car.is_company_car);
				pS.setString(5, car.car_key);
				pS.setInt(6, car.emission_type);
				pS.setBoolean(7, car.restriction);
				pS.setDouble(8, car.fix_costs);
				pS.addBatch();
				count++;
				if(count>1000){
					count =0;
					pS.executeBatch();
					pS.clearBatch();
				}
			}
			pS.executeBatch();
			pS.clearBatch();
			pS.close();
			
			
			
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" loadDB: SQL-Error during statement: "+query);
			e.printStackTrace();
		}		
	}
	
	
	public static void main(String[] args) {
		CarFleetAdjuster worker = new CarFleetAdjuster();
		worker.keyVal = "MID2008_Y2008";
		worker.newKeyVal = args[0];
		worker.loadKBAValueMapping(args[1]);
		worker.makeModifiedFleet();
	}
}