package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.util.Matrix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TPS_ParkAndRideAnalysator  extends TPS_ParkAndRideRouter {
	
	Matrix interchanges;	
	Matrix originalMIVTimes =null;
	Matrix pnrMIVTimes =null;
	Matrix mivDist =null;
	Map<Integer,Integer> interchangeCounter = new HashMap<>();
	int timeBinWitdh=60;
	int distBinWitdh=120;
	Map<Integer,Integer> tripTimeHisto = new HashMap<>();
	Map<Integer,Integer> connTimeHisto = new HashMap<>();
	Map<Integer,Integer> tripLengthHisto = new HashMap<>();
	Map<Integer,Boolean> isPnR = new HashMap<>();
	Map<Integer,PnRSpace> pnrOccupancy = new HashMap<>();
	
	class PnRSpace{
		int index;
		int[] parkingTimes= new int[60*24];
		
		public void bookParkingLot(int start, int duration) {
			if(start <0)
				start =0;
			start = Math.min(start, parkingTimes.length);
			int end = Math.min(start+duration, parkingTimes.length);
			for(int i= start; i<end;++i) {
				parkingTimes [i]+=1;
			}
		}
		
		public int maxParkingOccupancy() {
			int max=0;
			for (int parkingTime : parkingTimes) {
				max = Math.max(max, parkingTime);
			}
			return max;
		}

		public int peakTimeParking() {
			int max=0, indexOfMax=-1;
			for (int i =0; i<parkingTimes.length; ++i) {
				if(	max< parkingTimes[i]) {
					max = parkingTimes[i];
					indexOfMax = i;
				}
			}
			return indexOfMax;
		}
		
	}
	
	public void loadInterchanges(String name, String tableName){
		this.interchanges = this.readMatrix(name, tableName);
	}
	
	public void loadOriginalTimes(String name, String tableName){
		this.originalMIVTimes = this.readMatrix(name, tableName);
	}

	public void loadPNRTimes(String name, String tableName){
		this.pnrMIVTimes = this.readMatrix(name, tableName);
	}

	public void loadMIVDist(String name, String tableName){
		this.mivDist = this.readMatrix(name, tableName);
	}

	public void analyzeTrips(String name){
		//store into DB
		String query ="";
		
		try{
			
			query = "SELECT taz_id_start, taz_id_end, start_time_min, activity_start_min, activity_duration_min FROM "+name+" where mode = 2";
			
			ResultSet rs = this.dbCon.executeQuery(query,this);
			int from, to, interchange=0, num;
			double interchangeD;
			while(rs.next()){
				from =rs.getInt("taz_id_start");
				if(!interchangeCounter.containsKey(from)){
					interchangeCounter.put(from, 0);
				}
				from -=1;
				to =rs.getInt("taz_id_end")-1;
				interchangeD = (int) interchanges.getValue(from, to);
				if(interchangeD >=0.){
					interchange = (int) (interchangeD+1.5); //round plus index increment
					num=1;
					if(interchangeCounter.containsKey(interchange)){
						num+=interchangeCounter.get(interchange);
					}
					interchangeCounter.put(interchange, num);
				}
				if(this.originalMIVTimes!=null && this.pnrMIVTimes !=null && interchangeD >=0.) {
					
					double diff= this.pnrMIVTimes.getValue(from, to) - 
							this.originalMIVTimes.getValue(from, to);
					int bin= (int) (diff/timeBinWitdh);
					int numCount=1;
					if(connTimeHisto.containsKey(bin)) {
						numCount+=connTimeHisto.get(bin);
					}
					connTimeHisto.put(bin, numCount);
				}
				if(this.isPnR.get(from) != this.isPnR.get(to) && interchange >0) { //in or out the region
					if(this.isPnR.get(to)) {
						
						if(!this.pnrOccupancy.containsKey(interchange)) {
							this.pnrOccupancy.put(interchange, new PnRSpace());
						}
						PnRSpace tmp = this.pnrOccupancy.get(interchange);
						int start = rs.getInt("start_time_min");
						int duration = rs.getInt("activity_duration_min") + 2*(rs.getInt("activity_start_min")-start); //duration + two times travel time						
						tmp.bookParkingLot(start, duration);
					}
				}
			}
			rs.close();
			for(int i= 0; i< this.pnrMIVTimes.getNumberOfRows(); ++i) {
				for(int j= 0; j< this.pnrMIVTimes.getNumberOfColums(); ++j) {
					interchangeD = (int) interchanges.getValue(i, j);
					if(this.originalMIVTimes!=null && this.pnrMIVTimes !=null && interchangeD >=0.) {
						double diff= this.pnrMIVTimes.getValue(i, j) - 
								this.originalMIVTimes.getValue(i, j);
						int bin= (int) (diff/timeBinWitdh);
						int numCount=1;
						if(connTimeHisto.containsKey(bin)) {
							numCount+=connTimeHisto.get(bin);
						}
						connTimeHisto.put(bin, numCount);
					}					
				}
				
			}
			
		}
		catch(SQLException e){
			System.err.println("Error during sql-statement: "+query);
			e.printStackTrace();
			e.getNextException().printStackTrace();
		}		
	}
	
	public void loadPnRRegion(String table, String name){
		//store into DB
		String query ="";
		
		try{
			
			query = "SELECT ft_taz_id, is_park_and_ride from "+table+" where ft_name = '"+name+"'";
			
			ResultSet rs = this.dbCon.executeQuery(query,this);
			while(rs.next()){
				this.isPnR.put((rs.getInt("ft_taz_id")-1), rs.getBoolean("is_park_and_ride"));				
			}
			rs.close();			
		}
		catch(SQLException e){
			System.err.println("Error during sql-statement: "+query);
			e.printStackTrace();
			e.getNextException().printStackTrace();
		}		
	}
	
	public void storeInterchangesInDB(String tableName){
		//store into DB
		//delete old entry
		String query = "DROP TABLE IF EXISTS "+tableName ;
		this.dbCon.execute(query,this);
		//create table
		query = "CREATE TABLE "+tableName+" (taz_id integer, interchanges integer, max_parking integer, peak_hour integer, CONSTRAINT "+tableName+"_pkey PRIMARY KEY (taz_id)) WITH (  OIDS = FALSE);";
		this.dbCon.execute(query,this);

		//fill it!
		for(Entry<Integer, Integer> e: this.interchangeCounter.entrySet()){
			PnRSpace tmp = this.pnrOccupancy.get(e.getKey());
			if(tmp!=null) {
				query = "INSERT INTO "+tableName+
						" VALUES (	"+e.getKey() +","+e.getValue() +","+tmp.maxParkingOccupancy()
									+","+(tmp.peakTimeParking()/60)	+")";
			}
			else{
				query = "INSERT INTO "+tableName+
						" VALUES (	"+e.getKey()
									+","+e.getValue()
									+",0"
									+",0"
								+")";

			}
			this.dbCon.execute(query,this);
		}
	}
	
	public void printTimeHistoOnScreen() {
		System.out.println("TIME\tCOUNT");
		for(Entry<Integer,Integer> e :this.tripTimeHisto.entrySet()) {
			int bin = e.getKey();
			int c = e.getValue();
			System.out.println(bin+"\t"+c);			
		}
	}
	
	public void printConnHistoOnScreen() {
		System.out.println("TIME\tCOUNT");
		for(Entry<Integer,Integer> e :this.connTimeHisto.entrySet()) {
			int bin = e.getKey();
			int c = e.getValue();
			System.out.println(bin+"\t"+c);			
		}
	}	
	
	public static void main(String[] args) {
		TPS_ParkAndRideAnalysator worker = new TPS_ParkAndRideAnalysator();
		worker.loadInterchanges( "Berlin_PNR_INTERCHANGE_1193", "core.berlin_matrices");
		worker.loadOriginalTimes( "CAR_1193_2010_T0_TT_TOP3", "core.berlin_matrices");
		worker.loadPNRTimes( "Berlin_PNR_TT_1193", "core.berlin_matrices");
		worker.loadMIVDist( "WALK_1193TAZ_DIST", "core.berlin_matrices");
		worker.loadPnRRegion("core.berlin_taz_fees_tolls", "BERLIN_2010_PNR_HUNDEKOPF");
		worker.analyzeTrips("berlin_trips_2017y_08m_17d_13h_55m_36s_380ms");
		worker.storeInterchangesInDB("berlin_trips_2017y_08m_17d_13h_55m_36s_380ms_pnr_analyze2");
		worker.printConnHistoOnScreen();
	}

}
