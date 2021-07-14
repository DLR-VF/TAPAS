/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.dlr.ivf.tapas.loc.TPS_Coordinate;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.Matrix;
import de.dlr.ivf.tapas.util.parameters.ParamString;

public class TPS_MainDiagonalLengthEstimator extends TPS_BasicConnectionClass {

	class Location{
		int taz=-1,cappa=-1;
		TPS_Coordinate point;
	}
	
	Map<Integer, List<Location>> locations = new HashMap<>();
	Map<Integer, Location> tazes = new HashMap<>();
	Map<Integer, Double> intraTazDist = new HashMap<>();
	Map<Integer, Double> tazBeeLineFactor = new HashMap<>();
	Matrix dist;
	Matrix beeline;
	
	public void readLocations() {
		
		String query = "with cappa as ("+
			"	select sum(hh_persons ) as cappa, hh_coordinate as geom, hh_taz_id as taz_id"+
			"	from "+this.parameterClass.getString(ParamString.DB_TABLE_HOUSEHOLD)+" bh "+
			"	where hh_key ='"+this.parameterClass.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY)+"'"+
			"	group by hh_coordinate, hh_taz_id union "+
			"select sum(bl.loc_capacity) as cappa, loc_coordinate  as geom, loc_taz_id as taz_id"+
			"	from "+this.parameterClass.getString(ParamString.DB_TABLE_LOCATION)+" bl "+
			" where key = '" + this.parameterClass.getString(ParamString.DB_LOCATION_KEY) + "' " +
			"	group by loc_coordinate, loc_taz_id) "+
		"select sum(cappa)::integer as cappa, st_x(geom) as lon, st_y(geom) as lat, taz_id "+
			"from cappa "+
			"group by geom, taz_id	";
		try{
			
					
			ResultSet rs = dbCon.executeQuery(query, this);
			while(rs.next()) {
				Location loc = new Location();
				loc.taz = rs.getInt("taz_id");
				loc.cappa = rs.getInt("cappa");
				loc.point = new TPS_Coordinate(rs.getDouble("lon"), rs.getDouble("lat"));
				List<Location> tazList = this.locations.get(loc.taz);
				//check if we need a new list
				if(tazList == null) {
					tazList = new ArrayList<>();
					this.locations.put(loc.taz, tazList);
				}
				tazList.add(loc);
			}
			rs.close();
			
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" readLocations: SQL-Error during statement: "+query);
			e.printStackTrace();
		}	
	}
	
	public void loadDistMatrix(String name) {
		
		String query = "SELECT matrix_values FROM " +
				this.parameterClass.getString(ParamString.DB_TABLE_MATRICES) + " WHERE matrix_name='" +
				name + "'";
        ResultSet rs = dbCon.executeQuery(query, this);
		try{
			if (rs.next()) {

				int[] iArray = TPS_DB_IO.extractIntArray(rs, "matrix_values");
				int len = (int) Math.sqrt(iArray.length);
				this.dist = new Matrix(len, len, 0);
	            for (int index = 0; index < iArray.length; index++) {
	            	this.dist.setRawValue(index, iArray[index]);
	            }
	            
			}
			rs.close();
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" readLocations: SQL-Error during statement: "+query);
			e.printStackTrace();
		}		
	}
	
	public void createIntraTAZTable(String schema, String name) {
		
		
		String query= "DROP TABLE IF EXISTS "+schema+"."+name+ ";"
				+ "CREATE TABLE "+schema+"."+name+" ("+
			"	id int4 NOT NULL,"+
			"	taz int4 NOT NULL,"+
			"	weight float8 NULL"+
			");";
        
		dbCon.execute(query, this);
		//add the geometry
		query = "select AddGeometryColumn('"+schema+"','"+name+"','geom',4326,'POINT',2)";
		dbCon.execute(query, this);
	}

	public void fillIntraTAZTable(String schema, String name, int tazID) {
		
		String query;
	
		//fill the table
		int id=1;
		for(Location l: this.locations.get(tazID)){
			query = "INSERT INTO "+schema+"."+name+" VALUES ("+id+","+tazID+","+l.cappa+",ST_SETSRID(ST_MAKEPOINT("+l.point.getValue(0)+","+l.point.getValue(1)+"),4326));";
			dbCon.execute(query, this);
			id++;
		}
	}
	
	public void calcBeelineTAZDist() {
		for(Entry<Integer, List<Location>> e : this.locations.entrySet()) {
			int start, stop;
			double beelineAccumulated=0, beeline;
			int sumWeight=0,weight;
			Location l1, l2;
			List<Location> locs= e.getValue();
			for(start= 0; start< locs.size(); start++) {
				l1= locs.get(start);
				for(stop= start+1; stop <locs.size(); stop++) {
					l2= locs.get(stop);
					weight = Math.min(l1.cappa, l2.cappa);
					beeline = l1.point.getEuclidianDistance(l2.point);
					sumWeight+= weight;
					beelineAccumulated += beeline*weight;
				}
			}
			if(sumWeight>0)
				this.intraTazDist.put(e.getKey(), (beelineAccumulated/sumWeight));
			else
				this.intraTazDist.put(e.getKey(), -1d);
		}
	}
	
	public void printStats() {
		System.out.println("Tazes found:"+this.locations.size());
		for(Entry<Integer, List<Location>> e : this.locations.entrySet()) {
			int sumCappa=0;
			for (Location l: e.getValue()) {
				sumCappa+=l.cappa;
			}
			double distBL =this.intraTazDist.get(e.getKey());
			double distMatrix = this.dist.getValue((e.getKey()-1), (e.getKey()-1));
			System.out.println("Taz: "+e.getKey()+": locs: "+e.getValue().size()+" cappa: "+sumCappa+" intraBL: "+distBL+" bl actor: "+this.tazBeeLineFactor.get(e.getKey())+" Distdiff: "+((distBL*this.tazBeeLineFactor.get(e.getKey()))-distMatrix));
		}
		
	}
	
	public void readTAZ() {
		String query = "SELECT taz_id, st_x(taz_coordinate) as lon, st_y(taz_coordinate) as lat FROM " +
				this.parameterClass.getString(ParamString.DB_TABLE_TAZ);
        ResultSet rs = dbCon.executeQuery(query, this);
		try{
			//read tazes
			while (rs.next()) {
				Location taz = new Location();
				taz.taz = rs.getInt("taz_id");
				taz.cappa = 0;
				taz.point = new TPS_Coordinate(rs.getDouble("lon"), rs.getDouble("lat"));
				this.tazes.put(taz.taz, taz);
			}
			rs.close();
			
			// now calc the beelines
			this.beeline= new Matrix(this.tazes.size());
			for(Entry<Integer, Location> e: this.tazes.entrySet()) {
				for(Entry<Integer, Location> f: this.tazes.entrySet()) {
					double value =Double.MAX_VALUE;;// very high value
					if(e.getValue().taz!=f.getValue().taz) {
						this.beeline.setValue((e.getValue().taz)-1, (f.getValue().taz)-1, e.getValue().point.getEuclidianDistance(f.getValue().point));
					}
					this.beeline.setValue((e.getValue().taz)-1, (e.getValue().taz)-1, value);
				}				
			}
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" readLocations: SQL-Error during statement: "+query);
			e.printStackTrace();
		}
	}
	
	public void calcBeeLineFactors() {
		Location target;
		double dist,tmp;
		for(Location ref: this.tazes.values()) {
			dist = Double.MAX_VALUE;
			target = null;
			for(Location t: this.tazes.values()) {
				if(t.taz!=ref.taz){
					tmp= this.dist.getValue(ref.taz-1, t.taz-1);
					if(tmp<dist) {
						target = t;
						dist= tmp;
					}
				}
			}
			if(target!=null) {
				double bl = ref.point.getEuclidianDistance(target.point);
				this.tazBeeLineFactor.put(ref.taz, dist/bl);
			}
			else {
				this.tazBeeLineFactor.put(ref.taz, 1.4);				
			}
		}
	}
	

	public void saveIntraTAZ(String schema, String name) {
		createIntraTAZTable(schema,name);
		for(Integer e : this.tazes.keySet()) {
			fillIntraTAZTable(schema,name,e );
		}
	}
	
	
	public void saveNewMatrix(String name) {
		for(Entry<Integer, List<Location>> e : this.locations.entrySet()) {
			double distBL =this.intraTazDist.get(e.getKey());
			this.dist.setValue((e.getKey()-1), (e.getKey()-1),distBL*this.tazBeeLineFactor.get(e.getKey()));
		}
		this.storeInDB(name, this.dist.vals, 0);
	}
	
	public static void main(String[] args) {
		TPS_MainDiagonalLengthEstimator worker = new TPS_MainDiagonalLengthEstimator();
		worker.parameterClass.paramStringClass.setString(ParamString.DB_TABLE_HOUSEHOLD, "core.berlin_households_1223");
		worker.parameterClass.paramStringClass.setString(ParamString.DB_TABLE_LOCATION, "core.berlin_locations_1223");
		worker.parameterClass.paramStringClass.setString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY, "VEU2_MID2008_Y2010_REF");
		worker.parameterClass.paramStringClass.setString(ParamString.DB_TABLE_MATRICES, "core.berlin_matrices");
		worker.parameterClass.paramStringClass.setString(ParamString.DB_TABLE_TAZ, "core.berlin_taz_1223");
		
		worker.readLocations();
		worker.calcBeelineTAZDist();
		worker.loadDistMatrix("WALK_IHK_DIST");
		worker.readTAZ();
		worker.calcBeeLineFactors();
		worker.printStats();
		worker.saveIntraTAZ("intra_taz", "berlin_taz");
		//worker.saveNewMatrix("WALK_IHK_DIST_NEW_HD");
		
	}

}
