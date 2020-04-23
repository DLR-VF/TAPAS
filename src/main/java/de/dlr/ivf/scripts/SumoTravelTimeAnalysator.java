package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SumoTravelTimeAnalysator extends TPS_BasicConnectionClass {
	
	static final String region ="berlin";
	static final String tt_name ="CAR_1193_2010_T0_TT_TOP3_2016y_09m_23d_12h_45m_01s_459ms_IT_3";
	static final String simkey ="2016y_09m_23d_12h_45m_01s_459ms";
	static final String target_table ="berlin_sumo_comp_2016_09_26";
	static final String comp_Name ="CAR_1193_2010_T0_TT_TOP3_2016y_09m_23d_12h_45m_01s_459ms_IT_4";

	//static final String tt_name ="INITIAL_SUMO_MIV_TIMES_TAZ_1193";
	//static final String sumo_table ="temp.sumo_all_pairs_berlin_tapas_2012q2_2013y_11m_08d_14h_70pc_42";
	//static final String target_table ="berlin_sumo_comp_2012q2_2013y_11m_08d_14h_70pc_42";
	
	Map<Integer, Integer> histogram = new HashMap<>();
	
	class TazElement implements Comparable<TazElement>{
		int id;
		double x;
		double y;
		@Override
		public int compareTo(TazElement o) {
			return this.id-o.id;
		}
	}
	
	class TazTimeElement implements Comparable<TazTimeElement>{
		TazElement from =null;
		TazElement to  =null;
		double ttOld=0;
		double ttNew=0;
		double scoreTTFrom=1;
		double scoreTTTo=1;

		double score(double oldVal, double newVal){
			if(oldVal ==0)
				return 0;
			return newVal-oldVal;

		}
		
		void setNewTTFrom(double tt){
			this.ttNew=tt;
			this.scoreTTFrom = score(this.ttOld, this.ttNew);
		}

		void setNewTTTo(double tt){
			this.scoreTTTo = score(this.ttOld, tt);
		}
		
		@Override
		public int compareTo(TazTimeElement arg0) {
			return this.scoreTTFrom-arg0.scoreTTFrom<0?-1:1;
		}
	}
	
	Map<Integer, TazElement> tazes = new HashMap<>();
	Map<Integer, Integer> tazMap = new HashMap<>();
	
	Map<String,TazTimeElement> analysis = new HashMap<>();
	
	private String generateKey(int from, int to){
		return from +"-"+ to;
		
	}
	
	void readTazes(String Region){
		String query ="";
		try{
			query = "SELECT taz_id, taz_num_id, st_X(taz_coordinate) as x, st_Y(taz_coordinate) as y FROM core."+Region+"_taz";
			ResultSet rs= this.dbCon.executeQuery(query, this);
			while(rs.next()){
				TazElement taz = new TazElement();
				taz.id 	= rs.getInt("taz_id");
				taz.x	= rs.getDouble("x");
				taz.y	= rs.getDouble("y");
				tazes.put(taz.id, taz);
				tazMap.put(rs.getInt("taz_num_id"),taz.id);
			}
			
		}
		catch(SQLException e){
			System.out.println("SQL error! Query: "+query);
			e.printStackTrace();
		}			
	}
	
	int[][] readMatrix(String name){
		int size = this.tazes.size();
		if(size==0)
			return null;
		int[][] mat = new int[size][size];
		String query ="";
		try{
			query = "SELECT matrix_values FROM core."+region+"_matrices WHERE matrix_name = '"+name+"'";
			ResultSet rs= this.dbCon.executeQuery(query, this);
			if(rs.next()){
				int[] numbers = TPS_DB_IO.extractIntArray(rs, "matrix_values");
				//parse array
				if(numbers.length== size*size){
					int k=0;
					for(int i=0; i< size; ++i){
						for(int j=0; j< size; ++j, ++k){
							mat[i][j] = numbers[k];
						}
					}
				}
				else{
					return null;
				}
			}			
		}
		catch(SQLException e){
			System.out.println("SQL error! Query: "+query);
			e.printStackTrace();
			return null;
		}			
		return mat;
	}
	
	void readSumoValues(String simkey){
		String query ="";
		try{
			String simName1= "temp.sumo_od_"+simkey;
			String simName2= "temp.sumo_od_entry_"+simkey;
			query = "SELECT taz_id_start, taz_id_end, travel_time_sec[3] FROM "+simName1+" as st JOIN "+simName2+" as se on st.entry_id = se.entry_id";
			ResultSet rs= this.dbCon.executeQuery(query, this);
			String key; 						
			int from, to;
			double tt;
			TazTimeElement ref;
			double binSize=60;
			while(rs.next()){
				from = tazMap.get(rs.getInt("taz_id_start"));
				to = tazMap.get(rs.getInt("taz_id_end"));
				tt = rs.getDouble("travel_time_sec");
				key = this.generateKey(from, to);
				ref = this.analysis.get(key);
				ref.setNewTTFrom(Math.round(tt));
				key = this.generateKey(to, from);
				ref = this.analysis.get(key);
				ref.setNewTTTo(Math.round(tt));
				
				int ttDiff =(int) ((ref.ttOld - tt)/binSize);
				int count =1;
				if(this.histogram.containsKey(ttDiff)){
					count+= this.histogram.get(ttDiff);
				}
				this.histogram.put(ttDiff, count);
			}
		}
		catch(SQLException e){
			System.out.println("SQL error! Query: "+query);
			e.printStackTrace();
		}	
	}
	
	void readCarMatrixValues(String matName){
			
			int [][]mat = this.readMatrix(matName);
			String key; 						
			int from, to;
			double tt;
			TazTimeElement ref;
			double binSize=60;
			for(int i=0; i<mat.length;++i){
				for(int j=0;j<mat[i].length;++j){
					from = i+1;
					to = j+1;
					tt = mat[i][j];
					key = this.generateKey(from, to);
					ref = this.analysis.get(key);
					ref.setNewTTFrom(Math.round(tt));
					key = this.generateKey(to, from);
					ref = this.analysis.get(key);
					ref.setNewTTTo(Math.round(tt));
					
					int ttDiff =(int) ((Math.abs(ref.ttOld - tt)/binSize));
					int count =1;
					if(this.histogram.containsKey(ttDiff)){
						count+= this.histogram.get(ttDiff);
					}
					this.histogram.put(ttDiff, count);
					
				}
			}
	}

	
	
	void loadTAZData(String tazName, String simkey){
		int [][] mat = this.readMatrix(tazName);
		for(int i=0; i<mat.length; ++i){
			for(int j=0 ; j<mat[i].length; ++j){
				TazTimeElement tazElem = new TazTimeElement();
				tazElem.from= this.tazes.get(i+1);
				tazElem.to = this.tazes.get(j+1);
				tazElem.ttOld = mat[i][j];
				this.analysis.put(this.generateKey(i+1, j+1), tazElem);
			}
		}
		//String matName= this.dbCon.readSingleParameter(simkey, "DB_NAME_MATRIX_TT_MIT");
		
		readCarMatrixValues(comp_Name);
		//readSumoValues(simkey);		
	}
	

	
	class AggregatedResult{
		TazElement ref;
		double scoreSumFrom= 0;
		double scoreSumTo= 0;
		TazTimeElement worst=null;
		TazTimeElement best=null;
		
		void addElem(TazTimeElement elem){			
			this.scoreSumFrom+= elem.scoreTTFrom;
			this.scoreSumTo+= elem.scoreTTTo;
			if(this.worst==null){
				this.worst	= elem;
				this.best 	= elem;
			}
			else{
				if(this.worst.compareTo(elem) < 0)
					this.worst= elem;
				if(this.best.compareTo(elem) >0)
					this.best= elem;
			}
		}
	}
	
	List<AggregatedResult> analyze(){
		List<AggregatedResult> res = new ArrayList<>();
		
		for( int i= 0; i<this.tazes.size(); ++i){
			AggregatedResult tazRes = new AggregatedResult();
			tazRes.ref = this.tazes.get(i+1);
			for( int j= 0; j<this.tazes.size(); ++j){
				String key = this.generateKey(i+1, j+1);
				if(i!=j){
					tazRes.addElem(this.analysis.get(key));
				}
			}
			res.add(tazRes);
		}
		
		return res;
	}
	
	void saveToDB(String tableName, List<AggregatedResult> res){
		String query ="";
		try{
			
			//check for old tables
			query = "SELECT count(*) > 0 AS exsits FROM pg_tables WHERE tablename='"+tableName+"' AND schemaname='core'";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			if(rs.next()){
				if(rs.getBoolean("exsits")){
					//clean up
					query = "select dropgeometrycolumn('core','"+tableName+"','taz_coordinate')";
					this.dbCon.execute(query, this);
					query = "select dropgeometrycolumn('core','"+tableName+"','line_best')";
					this.dbCon.execute(query, this);
					query = "select dropgeometrycolumn('core','"+tableName+"','line_worst')";
					this.dbCon.execute(query, this);
					
					query = "DROP TABLE core."+tableName;
					this.dbCon.execute(query, this);
				}
			}
			
			
			//create
			query = "CREATE TABLE core."+tableName+"(  taz_id integer NOT NULL, taz_id_worst integer, taz_id_best integer, score double precision, scoreTo double precision," +
					" CONSTRAINT "+tableName+"_pk PRIMARY KEY (taz_id) USING INDEX TABLESPACE index) WITH ( OIDS=FALSE);";
			this.dbCon.execute(query, this);
			
			//add geometry
			query = "SELECT AddGeometryColumn('core','"+tableName+"','taz_coordinate','4326','POINT',2)";
			this.dbCon.execute(query, this);
			query = "SELECT AddGeometryColumn('core','"+tableName+"','line_worst','4326','LINESTRING',2)";
			this.dbCon.execute(query, this);
			query = "SELECT AddGeometryColumn('core','"+tableName+"','line_best','4326','LINESTRING',2)";
			this.dbCon.execute(query, this);

			for (AggregatedResult agg : res) {
				query = "INSERT INTO core." + tableName + " VALUES(	" + agg.ref.id + "," + agg.worst.to.id + "," +
						agg.best.to.id + "," + agg.scoreSumFrom / (this.tazes.size() - 1) + "," +
						+agg.scoreSumTo / (this.tazes.size() - 1) + "," + "st_setsrid(st_makepoint(" + agg.ref.x + "," +
						agg.ref.y + "), 4326)," + "st_setsrid(st_makeline(st_makepoint(" + agg.ref.x + "," + agg.ref.y +
						"),st_makepoint(" + agg.worst.to.x + "," + agg.worst.to.y + ")), 4326)," +
						"st_setsrid(st_makeline(st_makepoint(" + agg.ref.x + "," + agg.ref.y + "),st_makepoint(" +
						agg.best.to.x + "," + agg.best.to.y + ")), 4326)" + ")";
				this.dbCon.execute(query, this);
			}					
		}
		catch(SQLException e){
			System.out.println("SQL error! Query: "+query);
			e.printStackTrace();
		}
	}
	
	public void printHistogram(){
		for(Entry<Integer, Integer> e : this.histogram.entrySet()){
			System.out.println(e.getKey()+"\t"+e.getValue());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SumoTravelTimeAnalysator worker = new SumoTravelTimeAnalysator();
		worker.readTazes(region);
		worker.loadTAZData(tt_name, simkey);
		//List<AggregatedResult> res = worker.analyze();
		//worker.saveToDB(target_table,res);
		worker.printHistogram();
	}

}
