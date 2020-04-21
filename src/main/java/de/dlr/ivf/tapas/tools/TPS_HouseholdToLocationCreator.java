package de.dlr.ivf.tapas.tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

/**
 * This class creates a new location for every geographic units to perform social activities (aka. visit a friend)
 */

public class TPS_HouseholdToLocationCreator extends TPS_BasicConnectionClass {
	
	
	class DB_Location{
		  int loc_id;
		  int loc_blk_id;
		  int loc_taz_id;
		  int loc_code;
		  String loc_enterprise="";
		  int loc_capacity;
		  boolean loc_has_fix_capacity;
		  double x;
		  double y;
	}
	
	int actLocID=-1;
	
	List<DB_Location> socialLocations = new LinkedList<>();
	
	
	public void writeLocations(){
		String query;
		//fill the lookup table
		for(DB_Location loc : this.socialLocations){
			query = "INSERT INTO core.berlin_locations VALUES ("+
					loc.loc_id+", "+
					loc.loc_blk_id+","+
					loc.loc_taz_id+","+
					loc.loc_code+","+
					"'"+loc.loc_enterprise+"',"+
					loc.loc_capacity+","+
					(loc.loc_has_fix_capacity?"TRUE":"FALSE")+","+
					"st_setsrid(st_makepoint("+loc.x+","+loc.y+"),4326),"+
					"null,"+
					"-1,"+
					"null,"+
					"null"+
					")";
			//System.out.println(this.getClass().getCanonicalName()+" fillLocationWeights statement: "+query);
			dbCon.execute(query, this);
		}

		//clean up
		query = "VACUUM FULL ANALYZE core.berlin_locations";
		dbCon.execute(query, this);
		query = "REINDEX TABLE core.berlin_locations";
		dbCon.execute(query, this);
	}
	
	public void loadHouseholdsPerBlock(String key){
		String query = "";
		try{
			
			query = "SELECT foo2.blk_id AS blk_nr, (sum(bar.hh_persons))::integer AS persons, foo2.blk_taz_id AS taz, st_X(foo2.blk_coordinate) AS lon, st_Y(foo2.blk_coordinate) AS lat FROM core.berlin_households AS bar "+
					"JOIN core.berlin_blocks_multiline AS foo ON st_within(bar.hh_coordinate, foo.the_geom) "+
					"JOIN core.berlin_blocks As foo2 on foo2.blk_id=foo.blocknr "+
					"WHERE bar.hh_key='"+key+"' GROUP BY foo2.blk_id, foo2.blk_coordinate, foo2.blk_taz_id";				
			ResultSet rs = dbCon.executeQuery(query, this);
			
			while(rs.next()){
				DB_Location loc = new DB_Location();
				loc.loc_blk_id = rs.getInt("blk_nr");
				loc.loc_capacity = rs.getInt("persons");
				loc.loc_code=13;
				loc.loc_has_fix_capacity=false;
				loc.loc_taz_id= rs.getInt("taz");
				loc.x= rs.getDouble("lon");
				loc.y= rs.getDouble("lat");
				loc.loc_id = this.actLocID;
				this.socialLocations.add(loc);
				this.actLocID++;
			}
			
			rs.close();
									
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" createTable: SQL-Error during statement: "+query);
			e.printStackTrace();
		}		
		
	}
	
	public void readMaxLocID(){
		String query = "";
		try{
			
			query = "SELECT max(loc_id) as id FROM core.berlin_locations";				
			ResultSet rs = dbCon.executeQuery(query, this);
			
			if(rs.next()){
				this.actLocID = rs.getInt("id");
				this.actLocID++;
			}
			
			rs.close();
									
		}catch(SQLException e){
			System.err.println(this.getClass().getCanonicalName()+" createTable: SQL-Error during statement: "+query);
			e.printStackTrace();
		}		
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TPS_HouseholdToLocationCreator worker = new TPS_HouseholdToLocationCreator();
		worker.readMaxLocID();
		worker.loadHouseholdsPerBlock("MID2008_Y2008");
		worker.writeLocations();

	}
}
