package de.dlr.ivf.tapas.tools;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

/**
 * This tool adjusts the capacity of given locations to a new population.
 *
 */

public class TPS_CapacityScale extends TPS_BasicConnectionClass {

	class DistrictInfo{
		int id = -1;
		int numPersons =0;
		int numPersonsNew =0;
		Map<Integer,Integer> locMap = new HashMap<>();
		public void increasePersons(int num){
			this.numPersons+=num;
		}
		public void increasePersonsNew(int num){
			this.numPersonsNew+=num;
		}
		public void addLocInfo(int code, int cappa){
			
			if(this.locMap.containsKey(code)){
				cappa+=this.locMap.get(code); 
			}
			this.locMap.put(code, cappa);
		}
	}
	
	Map<Integer,DistrictInfo> districts = new HashMap<>();
	
	Map<Integer,Integer> taz2District = new HashMap<>();
	/**
	 * Load the mapping form the tazes to the districts. 
	 * This information is hidden in the "no" column of the table
	 * @param table
	 */
	public void loadTaz2DistrictMapping(String table){
		String query = "";
		try{
			query = "select gid, ((no/100000)::integer%100)::integer as bez from "+table;
			ResultSet rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				int taz = rs.getInt("gid");
				int district = rs.getInt("bez");
				this.taz2District.put(taz,district);
			}
			rs.close();
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+": SQL-Error during statement: "+query);
			e.printStackTrace();
		}	
	}
	
	public void createDistricts(){
		for(Entry<Integer, Integer> e: this.taz2District.entrySet()){
			if(this.districts.containsKey(e.getValue()))
				continue;
			DistrictInfo tmp = new DistrictInfo();
			tmp.id = e.getValue();
			this.districts.put(tmp.id, tmp);
		}
	}
	
	public void getLocationInfo(String locTable, String tazTable){
		String query = "";
		try{
			query = "with l as (select sum(loc_capacity)as cappa, count(*) as num , loc_code, loc_taz_id from "+locTable+" group by loc_code, loc_taz_id)"+
						"select sum(cappa) as bez_cappa , sum(num) as bez_num,loc_code, ((no/100000)::integer%100)::integer as bez "
						+ "from l join "+tazTable+" as m "
						+ "	on l.loc_taz_id= m.gid "
						+ "group by bez, loc_code";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				int district = rs.getInt("bez");
				int num = rs.getInt("bez_cappa");
				int code = rs.getInt("loc_code");
				this.districts.get(district).addLocInfo(code, num);
			}
			rs.close();
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+": SQL-Error during statement: "+query);
			e.printStackTrace();
		}			
	}
	
	public void fillNewLocations(String schema, String oldLoc, String newLoc){
		String newtable = schema+"."+newLoc;
		//drop old and create new table
		String query = "DROP TABLE IF EXISTS "+newtable+";"+

		"CREATE TABLE "+newtable+
		"("+
		"  loc_id integer NOT NULL,"+
		"  loc_blk_id integer,"+
		"  loc_taz_id integer NOT NULL,"+
		"  loc_code integer NOT NULL,"+
		"  loc_enterprise character varying(25),"+
		"  loc_capacity integer,"+
		"  loc_has_fix_capacity boolean NOT NULL DEFAULT false,"+
		"  loc_group_id integer NOT NULL DEFAULT (-1),"+
		"  loc_type text,"+
		"  loc_unit text,"+
		"  CONSTRAINT "+newLoc+"_pkey PRIMARY KEY (loc_id)"+
		"  USING INDEX TABLESPACE index"+
		")"+
		"WITH ("+
		"  OIDS=FALSE"+
		");";
		this.dbCon.execute(query, this);
		query = "Select addgeometrycolumn('"+schema+"','"+newLoc+"','loc_coordinate', 4326,'POINT',2);";
		this.dbCon.execute(query, this);

		//insert old values
		query = "INSERT INTO "+newtable+" (SELECT * FROM "+schema+"."+oldLoc+")";
		this.dbCon.execute(query, this);

		//do the update
		for(Entry<Integer, Integer> e: this.taz2District.entrySet()){
			DistrictInfo d= this.districts.get(e.getValue());
			double factor = (double)d.numPersonsNew/(double)d.numPersons;
			query="UPDATE "+newtable+" set loc_capacity =(loc_capacity*"+factor+")::integer"+
					" where loc_taz_id="+e.getKey();

			this.dbCon.execute(query, this);
		}
	}
	
	public void loadPersons(String table, String keyRef, String keyScen){
		String query = "";
		try{
			query = "select sum(hh_persons) as persons, hh_taz_id from "+table+" where hh_key='"+keyRef+"' group by hh_taz_id";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				int taz = rs.getInt("hh_taz_id");
				int num = rs.getInt("persons");
				this.districts.get(this.taz2District.get(taz)).increasePersons(num);
			}
			rs.close();
			query = "select sum(hh_persons) as persons, hh_taz_id from "+table+" where hh_key='"+keyScen+"' group by hh_taz_id";
			rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				int taz = rs.getInt("hh_taz_id");
				int num = rs.getInt("persons");
				this.districts.get(this.taz2District.get(taz)).increasePersonsNew(num);
			}
			rs.close();
			
			
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+": SQL-Error during statement: "+query);
			e.printStackTrace();
		}	
	}
	
	
	
	public static void main(String[] args) {
		TPS_CapacityScale worker = new TPS_CapacityScale();
		worker.loadTaz2DistrictMapping("core.berlin_taz_multiline");
		worker.createDistricts();
		worker.loadPersons("core.berlin_households", "VEU2_MID2008_Y2010_REF", "IHK_MID2008_Y2030_REF");
		worker.getLocationInfo("core.berlin_locations3", "core.berlin_taz_multiline");
		worker.fillNewLocations("core", "berlin_locations3", "berlin_locations_ihk");

	}

}
