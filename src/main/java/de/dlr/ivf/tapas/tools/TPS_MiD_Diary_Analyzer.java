package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class TPS_MiD_Diary_Analyzer extends TPS_BasicConnectionClass {
	
	static final int ONE_DAY = 24*60;
	static final String groupCol = "tbg_7_reihenfolge";
	static final String groupCol_name = "TBG_7";
	//static final int kindergartengruppe = 91;

	Map<String,Diary> diaryMap = new HashMap<>();
	Map<Integer,Integer> accompanyStat = new HashMap<>();
	Map<Integer,Integer> UncodedAccompanyStat = new HashMap<>();
	Map <Integer,Integer> purposeNonHomeEndTrip= new HashMap<>();
	Map <Integer,Integer> purposeNonHomeStartTrip= new HashMap<>();
	Map<Integer, Map<Integer,Integer>> diaryStatistics = new HashMap<>();
	Map<Integer, Map<Integer,Integer>> diarySumStatistics = new HashMap<>();
	Map<Integer, Map<Integer,Integer>> personGroupDistribution = new HashMap<>();
	Set <Integer> activities = new TreeSet<>();
	Set <Integer> diaryGroups = new TreeSet<>();

	int recoded=0;
	int notRecoded=0;
	int numOfDoubleWays = 0;
	int numOfExactDoubleWays = 0;
	int numOfDiariesStartingWithATrip=0;
	int numOfDiariesNotEndingAtHome=0;
	
	public void clearEverything(){
		this.diaryMap.clear();
		this.accompanyStat.clear();
		this.UncodedAccompanyStat.clear();
		this.purposeNonHomeEndTrip.clear();
		this.purposeNonHomeStartTrip.clear();
		this.diaryStatistics.clear();
		this.diarySumStatistics.clear();
		this.personGroupDistribution.clear();
		this.activities.clear();
		this.diaryGroups.clear();
		this.recoded=0;
		this.notRecoded=0;
		this.numOfDoubleWays = 0;
		this.numOfExactDoubleWays = 0;
		this.numOfDiariesStartingWithATrip=0;
		this.numOfDiariesNotEndingAtHome=0;
		
	}
	
	
 	class DiaryElement implements Comparable<DiaryElement>{
		int wsID;
		int tourNumber=0;
		int start_min;
		int end_min;
		int purpose;
		int purposeDetailed;		
		boolean stay;
		boolean home;
		int tripDestination=0;
		boolean workchain=false;
		String purposeDescription;
		List<String> accompaningPersons = new ArrayList<>();
		public int getDurtation(){
			return end_min-start_min;
		}
		@Override
		public int compareTo(DiaryElement arg0) {			
			return arg0.start_min-this.start_min;
		}
		public void printElement() {
			System.out.println("\tWay ID: "+this.wsID+" start: "+this.start_min+" end: "+this.end_min+
					" purpose: "+this.purpose+" detailed: "+this.purposeDetailed+" stay: "+this.stay+" home: "+this.home);
			
		}		
		
		public boolean isRoundTrip(){
			return tripDestination==5;
		}
	}

	
	class Diary{
		int schemeID=0;
		int hhID;
		int pID;
		int group =0;
		int pGroup=0;
		int personStatus =0;
		int totalTravelTime =0;
		boolean reported; 
		List<DiaryElement> activities = new ArrayList<>();
		public Diary(int hhID, int pID, int group, int pGroup, int personStatus){
			this.hhID = hhID;
			this.pID = pID;
			this.group = group;
			this.pGroup = pGroup;
			this.personStatus= personStatus;
			this.reported = false;
			DiaryElement startElem = new DiaryElement();
			startElem.wsID=-1;
			startElem.start_min=0;
			startElem.purpose= 0;
			startElem.purposeDetailed=0;
			startElem.purposeDescription="At home";
			startElem.home=true;
			startElem.stay=true;
			this.addDiaryElement(startElem);
		}
		
		public DiaryElement getClosestTrip(int startTime){
			DiaryElement returnVal = this.activities.get(0);
			
			for(DiaryElement e : this.activities){
				//is this trip closer than the actual best hit?
				if(Math.abs(startTime-e.start_min)<Math.abs(startTime-returnVal.start_min) && !e.stay){
					returnVal=e;
				}
			}
			
			if(returnVal.stay){ //found something?
				returnVal=null; //no: set to null
			}
			
			return returnVal;
		}
		
		public void addDiaryElement(DiaryElement e){
			e.wsID=this.activities.size(); //just the index, where it will be inserted
			this.activities.add(e);
		}
		
		public boolean addNextElement(int wsID, int start, int end, int purpose, int purposeDetailed, String description, boolean home,int tripDestination){
			if(start==end) //no duration?!?
				return false;
			DiaryElement pre = this.activities.get(this.activities.size()-1);
			DiaryElement stay;			
			if(pre.stay){
				stay = pre;
			}
			else{
				stay = new DiaryElement();
				stay.start_min=pre.end_min;
				stay.purpose= pre.home?0:pre.purpose;
				stay.purposeDetailed=pre.home?0:pre.purposeDetailed;
				stay.purposeDescription=pre.purposeDescription;
				stay.home=pre.home;
				stay.stay = true;
				//check for funny double entries!
				if(pre.start_min==start && pre.end_min==end){
					numOfDoubleWays++;
					if(pre.purpose==purpose&& pre.purposeDetailed==purposeDetailed){
						numOfExactDoubleWays++;
						return true;
					}
					if(pre.purposeDetailed==3307){
						pre.purpose=purpose;
						pre.purposeDetailed=purposeDetailed;
						pre.purposeDescription=description;
						return false;
					}
				}				
			}		
		
								

			stay.end_min = start;			
			DiaryElement trip = new DiaryElement();
			if (purpose == 9) { // rückweg vom vorherigen weg
				if (this.activities.size() >= 2) { // look if the previous stay was at home
					DiaryElement preStay = this.activities.get(this.activities.size() - 2);
					trip.home = preStay.home;
					if (trip.home){
						purpose = 8;
					}
					else{ //a trip back has the same purpose!
						purpose = preStay.purpose;
						purposeDetailed = preStay.purposeDetailed;
						description = "Back to "+preStay.purposeDescription;
						
					}
				} else {
					trip.home = false; // no trip back?!?!
				}
			} else {
				trip.home = home;
			}
			trip.purpose = purpose;
			trip.start_min = start;
			trip.purposeDetailed = purposeDetailed;
			trip.purposeDescription = description;
			trip.end_min = end;
			trip.stay = false;
			trip.tripDestination=tripDestination;

			
			
			
			//now something strange: we have trips "Return to home" starting and ending at home.			
			// I assume these are round trips (walks etc..)
			if(stay.home && (purpose == 8 || trip.tripDestination ==1)){
				//is the trip long enough to split? (3min= 1min trip 1min stay 1min back)
				int middle= start+(end-start)/2;
				boolean splitIt=true;
				if(start<middle && middle+1<end && splitIt){
					if(!stay.equals(pre))
						this.addDiaryElement(stay);
					//split the trip!
					//way to target
					DiaryElement inBetweenTrip = new DiaryElement();
					inBetweenTrip.start_min = start;
					inBetweenTrip.end_min = middle;
					inBetweenTrip.stay = false;
					inBetweenTrip.home = false;
					inBetweenTrip.purpose = purpose;
					inBetweenTrip.purposeDetailed = purposeDetailed;
					inBetweenTrip.purposeDescription = "Nachträglich eingefügte Auftrennung des Weges";
					inBetweenTrip.tripDestination=304;
					this.addDiaryElement(inBetweenTrip);
					//target
					DiaryElement inBetweenStay = new DiaryElement();
					inBetweenStay.start_min = middle;
					inBetweenStay.end_min = middle+1;
					inBetweenStay.stay = true;
					inBetweenStay.home = false;
					inBetweenStay.purpose = purpose;
					inBetweenStay.purposeDetailed = purposeDetailed;
					inBetweenStay.purposeDescription = "Nachträglich eingefügte Auftrennung des Weges";
					inBetweenStay.tripDestination=304;
					this.addDiaryElement(inBetweenStay);
					
					//way back home
					trip.start_min= middle+1;
					this.addDiaryElement(trip);
					
					this.totalTravelTime += end - start -1;
				}
				
			}
			else{
				//if (purpose != 8 && purpose != 9) { // 8/9 sind rückwege!
				if(!stay.equals(pre))
					this.addDiaryElement(stay);
				//}
				this.addDiaryElement(trip);			
				this.totalTravelTime += end - start;
			}
			return true;
		}
	
		/**
		 * Method to close a Diary with a stay at home.
		 */
		public void finishDiary(){
			DiaryElement stay;
			if(this.activities.size()==1){
				stay = this.activities.get(0);
				stay.start_min=0;
				stay.end_min=ONE_DAY;
				stay.purpose= 0;
				stay.purposeDetailed=0;
				stay.purposeDescription="At home";
				stay.home=true;
				stay.stay = true;
				//this.addDiaryElement(stay);
			}
			else{

				//check if the last trip was going home!
				DiaryElement pre = this.activities.get(this.activities.size()-1);
				if(!pre.home){// && !pre.isRoundTrip()){
					numOfDiariesNotEndingAtHome++;
//					if(purposeNonHomeEndTrip.get(pre.purpose)==null){
//						purposeNonHomeEndTrip.put(pre.purpose,1);
//					}
//					else{
//						purposeNonHomeEndTrip.put(pre.purpose,purposeNonHomeEndTrip.get(pre.purpose)+1);
//					}
					//System.out.println("Purp: "+pre.purpose+"/"+pre.purposeDetailed+" Dest: "+pre.tripDestination);
					//oops fit it: Insert a stay (5min) and a trip back home
					DiaryElement auxStay = new DiaryElement();
					auxStay.start_min=pre.end_min;				
					auxStay.end_min=auxStay.start_min+5;
					auxStay.purpose= pre.purpose;
					auxStay.purposeDetailed=pre.purposeDetailed;
					auxStay.purposeDescription=pre.purposeDescription;
					auxStay.home=false;
					auxStay.stay=true;
					this.addDiaryElement(auxStay);
					// now insert a trip home. 
					//Only problem: how long should it be?
					//Assumption: look back for the last trip from home and take this time!
					pre = new DiaryElement(); //this will be the preceeding trip for the rest of the code!
					pre.start_min=auxStay.end_min;
					//now look for the duration of the last trip from home
					int duration =5; //safety value
					for(int i = this.activities.size()-2; i>=0; --i){
						DiaryElement lastStayHome = this.activities.get(i); 
						if(lastStayHome.home && lastStayHome.stay){
							duration = this.activities.get(i+1).getDurtation();
							break;
						}
					}
					
					
					pre.end_min=pre.start_min+duration;
					pre.purpose= 8;
					pre.purposeDetailed=3307;
					pre.purposeDescription=" Automatic trip back home";
					pre.home=true;
					pre.stay=false;
					this.addDiaryElement(pre);
					
				}
				
				stay = new DiaryElement();
				
				stay.start_min=pre.end_min;				
				stay.end_min=Math.max(ONE_DAY, pre.end_min+1);
				stay.purpose= pre.home?0:pre.purpose;
				stay.purposeDetailed=pre.home?0:pre.purposeDetailed;
				stay.purposeDescription="At home";
				stay.home=true;
				stay.stay = true;
				this.addDiaryElement(stay);
				
				//Collections.sort(this.activities);
				
				//now update the tournumbers
				pre = this.activities.get(0);
				int actTourNumber;
				DiaryElement act=null;
				List<Integer> toursWithWork = new LinkedList<>();
				//first home stays are always tour 0 trips are 1...
				actTourNumber =1; // start with the first tour
				for (int i = 1; i< this.activities.size(); ++i){
					act = this.activities.get(i);					
					if(act.stay){ //a stay allways gets the tournumber of its last trip
						if(act.home)
							act.tourNumber=0;
						else
							act.tourNumber= pre.tourNumber;
					}
					else{
						act.tourNumber=actTourNumber; //update the tour number
					}
					
					if(act.purpose==1 || act.purpose==2){
						toursWithWork.add(actTourNumber);
					}					
					if( !act.stay && act.home) // a tour ending at home: all succeding trips are a new tour  
					{
						actTourNumber++;
					}
					pre=act;
				}
				//now fix all trips within a workchain
				if(toursWithWork.size()>0){
					for (int i = 1; i< this.activities.size()-1; ++i){
						act = this.activities.get(i);
						if(!(act.home&&act.stay) && toursWithWork.contains(act.tourNumber)){
							act.workchain=true;
						}
					}				
				}

				//now fix 0-min stays:
				this.totalTravelTime=0;
				int addTime;
				for(int i= 1; i<this.activities.size(); ++i){
					act = this.activities.get(i);
					pre = this.activities.get(i-1);
					//fix starts at midnight
					if(act.start_min==0){
						pre.end_min=act.start_min=1;
					}
					addTime = 1-act.getDurtation();
					if(act.getDurtation()<=0){
						act.end_min+=addTime;
						for(int j=i+1; j<this.activities.size(); ++j){
							this.activities.get(j).start_min+=addTime;
							this.activities.get(j).end_min+=addTime;
						}
					}
					if(!act.stay){
						this.totalTravelTime+=act.getDurtation();
					}
				}
				for(int i= 1; i<this.activities.size(); ++i){
					act = this.activities.get(i);
					pre = this.activities.get(i-1);
					if(act.start_min==pre.start_min)
						System.out.println("argh!");
				}
				
			}
			
			
		}

		public boolean checkDiary(){
			//the diary is ok if stay and trip are toggeling every time
			//this can be done by checking the indices: 
			//   all even indices must be stays and all odd ones trips...
			if(this.activities.size() %2 ==0 )
				return false; //we have to end with an stay! so the number must be odd!
			for(int i=0; i< this.activities.size(); i++){
				if( ( (i%2)==0 && !this.activities.get(i).stay) || //even and not a stay: bad!
					( (i%2)==1 &&  this.activities.get(i).stay) // odd and not a trip: bad!	
						){
					return false;
				}
				if(this.activities.get(i).getDurtation()<=0){
					if(this.activities.get(i).start_min==0 && this.activities.get(i).end_min==0)
						continue;//starts at midnight are ok!
					System.out.println("Error in diary!");
					this.activities.get(i).printElement();
					return false;
				}
			}
			//now check if the first and last stays are home
            return this.activities.size() > 0 && this.activities.get(0).home && this.activities.get(
                    this.activities.size() - 1).home;
		}
		
		public void printDiary(){
			System.out.println("Diary household: "+this.hhID+" Person: "+this.pID+ " Person group: "+this.pGroup+" Status: "+this.personStatus + " Diary group: "+this.group);
			for (DiaryElement act : this.activities)
				act.printElement();
		}
	}
	
	public static String makeDiaryKey(int hhID, int pID){
		return hhID+"-"+pID;
	}
	

	private int recodeAcompanyTrip(DiaryElement trip, DiaryElement theOtherOne ){
		if(theOtherOne != null){
//			System.out.println("This trip:");
//			trip.printElement();
//			System.out.println("The othewr one:");
//			theOtherOne.printElement();
			trip.purpose= theOtherOne.purpose;
			trip.purposeDetailed = theOtherOne.purposeDetailed;
			trip.purposeDescription = "Begleitung bei "+theOtherOne.purposeDescription;
			int purpose = theOtherOne.purpose*100000+theOtherOne.purposeDetailed;
			
			if(accompanyStat.containsKey(purpose)){
				accompanyStat.put(purpose, 1+accompanyStat.get(purpose));
			}
			else{
				accompanyStat.put(purpose, 1);
			}
			recoded++;
			return 1;
			
		}
		else{
			int purpose = trip.purpose*100000+trip.purposeDetailed;
			if(UncodedAccompanyStat.containsKey(purpose)){
				UncodedAccompanyStat.put(purpose, 1+UncodedAccompanyStat.get(purpose));
			}
			else{
				UncodedAccompanyStat.put(purpose, 1);
			}
			
			notRecoded++;
			return 0;			
		}		
	}
	
	public void readMIDDiary(String table,String filter){
		String query ="";
		Diary actualDiary, lastDiary =null;
		DiaryElement lastActivity = null;
		int doubleReturnDiaries =0;
		
		try {
			query = "select hhid, pid, wsid, st_dat,st_std, st_min, en_dat, en_std, en_min, "
					+ "w04, w04_dzw, w04_sons, w13, "+groupCol+",pg_tapas, hp_besch, "
					+ "w072_1, w072_2, w072_3, w072_4, w072_5, w072_6, w072_7, w072_8 "
					+ "from "+table+" where st_std !=301 and pg_tapas <>-999 and "+filter+" order by hhid,pid,wsid";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			String key, lastKey="???",description;
			int hhID, pID, wsID, start, end, purpose, purposeDetailed,group, pGroup, personStatus, tripDestination;
			boolean clean = true, home;
			
			while(rs.next()){
				
				hhID = rs.getInt("hhid");
				pID = rs.getInt("pid");
				wsID = rs.getInt("wsid");
				start = rs.getInt("st_dat")*ONE_DAY+rs.getInt("st_std")*60+rs.getInt("st_min");
				end = rs.getInt("en_dat")*ONE_DAY+rs.getInt("en_std")*60+rs.getInt("en_min");
				purpose = rs.getInt("w04");
				purposeDetailed = rs.getInt("w04_dzw");
				description = rs.getString("w04_sons");
				group = rs.getInt(groupCol);
				pGroup = rs.getInt("pg_tapas");
				personStatus = rs.getInt("hp_besch");
				home = purpose ==8 || rs.getInt("w13")==1;
				tripDestination = rs.getInt("w13");				
				if(purpose==9 && rs.getInt("w13")==1) //codiere Rückwege mit Ziel "Nach Hause" in "Rückwege nach hause"
					purpose=8;
				key = makeDiaryKey(hhID, pID);
				if(key.equals(lastKey)){ //same diary?
					actualDiary = this.diaryMap.get(key);
				}
				else{
					if(clean) {
						//finish the old one
						if(lastDiary!=null){
							lastDiary.finishDiary();
						}
					}
					else{
						this.diaryMap.remove(lastKey);						
					}
					clean = true;
					actualDiary = new Diary(hhID, pID, group,pGroup,personStatus);					
					this.diaryMap.put(key, actualDiary);
					lastActivity = null;
				}
				if(actualDiary.activities.size()==1 && (purpose ==8||purpose==9) &&tripDestination!=5){
					numOfDiariesStartingWithATrip++;
//					if(purposeNonHomeStartTrip.get(purpose)==null){
//						purposeNonHomeStartTrip.put(purpose,1);
//					}
//					else{
//						purposeNonHomeStartTrip.put(purpose,purposeNonHomeStartTrip.get(purpose)+1);
//					}
//					System.out.println(description);
				}
				else if(lastActivity != null && lastActivity.home && purpose==8){ // two consecutive "trips home"
					lastActivity.end_min= Math.max(lastActivity.end_min, end);
					System.err.println("Diary "+hhID+" pid "+pID +" has two consecutive trips home");
					doubleReturnDiaries++;
				}
				else{
					if(start!=end){
						clean &= actualDiary.addNextElement(wsID, start, end, purpose, purposeDetailed, description,home,tripDestination);
						int diaryIndex= actualDiary.activities.size()-1;
						lastActivity = actualDiary.activities.get(diaryIndex);
						//check for accompaning persons
						for(int i=1; i<=8; ++i){
							String column= "w072_"+i;
							if(rs.getInt(column)==1 && i!=pID){
								actualDiary.activities.get(diaryIndex).accompaningPersons.add(makeDiaryKey(hhID,i));
							}					
						}
					}
				}

				
				lastDiary = actualDiary;			
				activities.add(purpose); //collect all possible activities	
				diaryGroups.add(actualDiary.group);
				lastKey=key;
			}
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}	
		if(lastDiary!=null){
			lastDiary.finishDiary();
		}

		//check all diaries:
		List<String> keysToRemove = new LinkedList<>();
		for(Entry<String, Diary> e : this.diaryMap.entrySet() ){
			if(!e.getValue().checkDiary()){
				System.out.println("Diary "+e.getKey()+" is not correct!");
				e.getValue().printDiary();
				keysToRemove.add(e.getKey());
			}
			else{
				boolean ok=true;
				for(DiaryElement d: e.getValue().activities){
					if(d.purpose==11){
						ok=false; //set it to false
						DiaryElement bestHit=null;
						for(String k : d.accompaningPersons){ // go through acompaning persons 1st time
							Diary company = this.diaryMap.get(k);
							if(company != null){ //Diary found?
								DiaryElement companyAct= company.getClosestTrip(d.start_min);
								if(companyAct!=null){ //found a trip?
									//is this trip in the other list and the other trip not a acommpany? (100% hit)
									if(companyAct.accompaningPersons.contains(e.getKey()) && companyAct.purpose!=11) {
										//d.purpose= companyAct.purpose;
										//d.purposeDetailed = companyAct.purposeDetailed;
										//d.purposeDescription = "Begleitung bei "+companyAct.purposeDescription;
										ok=true;
										bestHit=companyAct;
										break;
									}						
									if(!ok && companyAct.purpose!=11){ //no 100% hit found jet!
										if(bestHit==null || Math.abs(companyAct.start_min-d.start_min)<Math.abs(bestHit.start_min-d.start_min)){
											bestHit= companyAct;
										}
									}
								}
							}
						}
						if(!ok && bestHit !=null){
							//see if best hit is ok
							if(Math.abs(bestHit.start_min-d.start_min)<10){
								ok=true;								
							}
							else{
								ok=false;
								bestHit=null;
							}
						}					
						this.recodeAcompanyTrip(d, bestHit);
					}
				}
				if (!ok) {
					keysToRemove.add(e.getKey());
				}
			}
		}
		//remove bad diaries
		for(String k: keysToRemove){
			this.diaryMap.remove(k);
		}
		//now scan all diaries for "begleitwege" and find their counterparts
		if(doubleReturnDiaries>0) System.err.println("Number of two consecutive trips home: "+doubleReturnDiaries);
	}
	
	
	public void calcDiaryStatistics(){
		Map<Integer,Integer> groupStatistics ;
		Map<Integer,Integer> groupSumStatistics ;
		Map<Integer,Integer> groupDistribution;
		int countGroup;
		for(Diary e: this.diaryMap.values()){
			
			if(!diaryStatistics.containsKey(e.group)){ // make new group
				groupStatistics = new HashMap<>();
				for(Integer i: this.activities){//fill with init-values for all activities
					groupStatistics.put(i, 0);
				}
				diaryStatistics.put(e.group, groupStatistics);
			}
			else{
				groupStatistics = diaryStatistics.get(e.group);
			}
			
			if(!diarySumStatistics.containsKey(e.group)){ // make new group
				groupSumStatistics = new HashMap<>();
				for(Integer i: this.activities){//fill with init-values for all activities
					groupSumStatistics.put(i, 0);
				}
				diarySumStatistics.put(e.group, groupSumStatistics);
			}
			else{
				groupSumStatistics = diarySumStatistics.get(e.group);
			}

			if(!personGroupDistribution.containsKey(e.pGroup)){ //make new person group
				groupDistribution = new HashMap<>();
				for(Integer i: this.diaryGroups){//fill with init-values for all groups
					groupDistribution.put(i, 0);
				}				
				personGroupDistribution.put(e.pGroup, groupDistribution);
			}
			else{
				groupDistribution = personGroupDistribution.get(e.pGroup);
			}
			//now fill the statistics
			for(DiaryElement d: e.activities){
				if(d.stay && !d.home){
					groupStatistics.put(d.purpose, groupStatistics.get(d.purpose)+1);
					groupSumStatistics.put(d.purpose, groupSumStatistics.get(d.purpose)+d.getDurtation());
				}
			}
			//and count one diary for this persongroup
			countGroup = groupDistribution.get(e.group);
			groupDistribution.put(e.group, countGroup+1);
		}
	}
	
	public void printStatistics(){
		Map<Integer,Integer> groupStatistics ;
		Map<Integer,Integer> groupSumStatistics ;
		int sumTime, sumActivities, numAct, numDuration;
		int avgDuration;
		System.out.println("Group\tActivity\tNum\tAvg Duration");
		for( Integer group : this.diarySumStatistics.keySet()){
			sumTime=0;
			sumActivities=0;
			groupStatistics = this.diaryStatistics.get(group);
			groupSumStatistics = this.diarySumStatistics.get(group);
			
			for(Integer act: this.activities){
				numAct = groupStatistics.get(act);
				numDuration = groupSumStatistics.get(act);
				if(numAct>0){
					avgDuration = numDuration/numAct;
				}
				else{
					avgDuration = 0;
				}
					
				System.out.println(group+"\t"+act+"\t"+numAct+"\t"+avgDuration);
				sumTime += numDuration;
				sumActivities += numAct;
			}
			System.out.println(group+"\t0\t"+sumActivities+"\t"+(sumTime/sumActivities));
		}
	}
	
	public void printSchemeClassSQLInserts(String table, boolean print){
		Map<Integer,Integer> groupCounter = new HashMap<>();
		Map<Integer,Integer> groupSumTime = new HashMap<>();
		Map<Integer,Integer> groupSumWay = new HashMap<>();
		Map<Integer,Double> groupAvgTime = new HashMap<>();
		Map<Integer,Double> groupAvgWay = new HashMap<>();
		Map<Integer,Double> groupStdDeviation = new HashMap<>();
		Map<Integer,Double> groupWithinStdDeviation = new HashMap<>();
		int counter, group, time,sumtime, sumways;
		double avg,stdDev,within;
		//calc sum of times
		for(Diary e: this.diaryMap.values()){
			group = e.group;
			//fetch old values
			counter = groupCounter.getOrDefault(group, 0);
			time = groupSumTime.getOrDefault(group, 0);
			sumways = groupSumWay.getOrDefault(group, 0);
			
			//calc new values
			counter++;
			time+= e.totalTravelTime;
			for(DiaryElement d : e.activities){
				if(!d.stay)
					sumways++;
			}
			
			//put new values
			groupCounter.put(group, counter);
			groupSumTime.put(group, time);
			groupSumWay.put(group, sumways);
		}
		
		//calc avg
		for(Integer i : this.diaryGroups){
			counter = groupCounter.get(i);
			sumtime = groupSumTime.get(i);
			sumways = groupSumWay.get(i);
			groupAvgTime.put(i, ((double)sumtime/(double)counter));
			groupAvgWay.put(i, ((double)sumways/(double)counter));
		}
		//calc standard deviation
		for(Diary e: this.diaryMap.values()){
			group = e.group;
			if(groupStdDeviation.containsKey(group)){
				stdDev = groupStdDeviation.get(group);
			}
			else{
				stdDev =0;
			}
			//update value
			stdDev += (e.totalTravelTime-groupAvgTime.get(group))*(e.totalTravelTime-groupAvgTime.get(group));			
			groupStdDeviation.put(group, stdDev);			
		}
		//normalize stdDev
		for(Integer i : this.diaryGroups){
			counter = groupCounter.get(i)-1;
			stdDev = groupStdDeviation.get(i);
			groupStdDeviation.put(i, (stdDev/(double)counter));
		}
		
		//now count number of diaries within std deviation
		for(Diary e: this.diaryMap.values()){
			group = e.group;
			stdDev = groupStdDeviation.get(group); 
			if(groupWithinStdDeviation.containsKey(group)){
				within = groupWithinStdDeviation.get(group);
			}
			else{
				within =0;
			}
			
			//update value
			if(	(e.totalTravelTime-groupAvgTime.get(group))*(e.totalTravelTime-groupAvgTime.get(group)) //the deviation
					<stdDev){
				within+=1;
			}
			groupWithinStdDeviation.put(group, within);
		}
		//normalize
		for(Integer i : this.diaryGroups){
			counter = groupCounter.get(i);
			within = groupWithinStdDeviation.get(i);
			groupWithinStdDeviation.put(i, (within/(double)counter));
		}
		
		// puh, now print the whole stuff
		
		if(print){
			PrintWriter pw = new PrintWriter(System.out	); //needed to get rid of stupid german localization of doubles!
			
	
			for(Integer i : this.diaryGroups){
				avg = groupAvgTime.get(i);
				within = groupWithinStdDeviation.get(i);
				//double ways = groupAvgWay.get(i);
				//pw.printf(Locale.ENGLISH,"%d;%f;%f;%f\n",i,avg,within, ways);
				pw.printf(Locale.ENGLISH,"INSERT INTO %s (scheme_class_id, avg_travel_time, proz_std_dev) VALUES (%d,%f,%f);\n",table,i,avg,within);
				
			}		
			pw.flush();
		}
		else{
			//there are only a few insert, so we do it directly withourt pepared Statements
			String query;
			for(Integer i : this.diaryGroups){
				avg = groupAvgTime.get(i);
				within = groupWithinStdDeviation.get(i);
				query= String.format(Locale.ENGLISH,"INSERT INTO %s (scheme_class_id, avg_travel_time, proz_std_dev) VALUES (%d,%f,%f);",table,i,avg,within);
				this.dbCon.execute(query, this);

			}
		}
	}
	
	
	private int getMappingKey(int purpose, int purposeDetailed, int personStatus){
		return purposeDetailed*10000+purpose*100+personStatus;
	}
	
	public void printDiariesSQLInserts(String table_episode, String table_schemes, boolean print){
		Map<Integer,Integer> activityMapping = new HashMap<>();
		//build the code mapping
		activityMapping.put(getMappingKey(0,0,0),10); //stay at home

		activityMapping.put(getMappingKey(1,  11,0),211); //Work: Begleitung
		activityMapping.put(getMappingKey(1,3307,0),211); //unspecified workers
		activityMapping.put(getMappingKey(1,3307,1),212); //full time workers
		activityMapping.put(getMappingKey(1,3307,2),213); //part time workers
		activityMapping.put(getMappingKey(2,3307,0),211); //Business trip: Begleitung
		activityMapping.put(getMappingKey(2,3307,0),211); //Business trip
		activityMapping.put(getMappingKey(3,11  ,0),413); //Bildung: Begleitung
		activityMapping.put(getMappingKey(3,3307,0),413); //Kindergarden
		activityMapping.put(getMappingKey(3,3307,1),412); //Trainee school
		activityMapping.put(getMappingKey(3,3307,2),412); //Trainee school
		activityMapping.put(getMappingKey(3,3307,4),412); //Trainee school
		activityMapping.put(getMappingKey(3,3307,5),410); //Pupil school
		activityMapping.put(getMappingKey(3,3307,6),411); //University
		activityMapping.put(getMappingKey(4 ,11 ,0),50);  //Shopping : Begleitung
		activityMapping.put(getMappingKey(4, 501,0),51);  //Shopping short term: Täglicher Bedarf
		activityMapping.put(getMappingKey(4, 502,0),53);  //Shopping long term: sonstige Ware (Kleidung, Möbel, Hausrat)
		activityMapping.put(getMappingKey(4, 503,0),50);  //Shopping Allgemeiner Einkaufsbummel
		activityMapping.put(getMappingKey(4, 504,0),52);  //Shopping mid term: Dienstleistung (Friseur Schuster)
		activityMapping.put(getMappingKey(4, 505,0),50);  //Shopping sonstiger Einkaufszweck
		activityMapping.put(getMappingKey(4, 599,0),50);  //Shopping Einkäufe ohne Angabe zum Detail
		activityMapping.put(getMappingKey(4, 999,0),50);  //Shopping keine Angabe
		activityMapping.put(getMappingKey(4,3307,0),50);  //Shopping kein Einkaufs-, Erledigungs- und Freizeitweg?!?!?
		activityMapping.put(getMappingKey(5,  11,0),522); //private Erledigung: Begleitung ...
		activityMapping.put(getMappingKey(5, 503,0),50);  //private Erledigung: Allgemeiner Einkaufsbummel
		activityMapping.put(getMappingKey(5, 504,0),52);  //private Erledigung: Dienstleistung (Friseur Schuster)
		activityMapping.put(getMappingKey(5, 601,0),522); //private Erledigung: Arzt
		activityMapping.put(getMappingKey(5, 602,0),522); //private Erledigung: Amt, Behörtde, Post, Bank, Geldautomat
		activityMapping.put(getMappingKey(5, 603,0),522); //private Erledigung: für andere Person
		activityMapping.put(getMappingKey(5, 604,0),522); //private Erledigung: sonstiges
		activityMapping.put(getMappingKey(5, 605,0),522); //private Erledigung: Betreuung
		activityMapping.put(getMappingKey(5, 699,0),522); //private Erledigung: keine Angabe des Details
		activityMapping.put(getMappingKey(5, 701,0),631); //private Erledigung: Besuch mit/bei Freunden
		activityMapping.put(getMappingKey(5, 705,0),414); //private Erledigung: Weiterbildung
		activityMapping.put(getMappingKey(5, 706,0),720); //private Erledigung: Restaurant
		activityMapping.put(getMappingKey(5, 711,0),722); //private Erledigung: Hund ausführen
		activityMapping.put(getMappingKey(5, 713,0),522); //private Erledigung: Kirche, Friedhof
		activityMapping.put(getMappingKey(5, 714,0),300); //private Erledigung: Ehrenamt, Verein, pol. Aufgaben 
		activityMapping.put(getMappingKey(5, 715,0),213); //private Erledigung: Jobben in der Freizeit
		activityMapping.put(getMappingKey(5, 716,0),881); //private Erledigung: Begleitung Kinder Spielplatz
		activityMapping.put(getMappingKey(5, 717,0),522); //private Erledigung: Hobby
		activityMapping.put(getMappingKey(5,3307,0),522); //private Erledigung: kein Einkaufs-, Erledigungs- und Freizeitweg?!?!?
		activityMapping.put(getMappingKey(6,  11,0),740); //Bringen+Holen: Begleitung
		activityMapping.put(getMappingKey(6, 503,0),50);  //Bringen+Holen: Allgemeiner Einkaufsbummel
		activityMapping.put(getMappingKey(6, 701,0),631); //Bringen+Holen: Besuch mit/bei Freunden
		activityMapping.put(getMappingKey(6, 702,0),640); //Bringen+Holen: Kulturelle Einrichtung
		activityMapping.put(getMappingKey(6, 703,0),724); //Bringen+Holen: Besuch einer Veranstaltung
		activityMapping.put(getMappingKey(6, 704,0),721); //Bringen+Holen: Sport
		activityMapping.put(getMappingKey(6, 705,0),414); //Bringen+Holen: Weiterbildung
		activityMapping.put(getMappingKey(6, 706,0),720); //Bringen+Holen: Restaurant
		activityMapping.put(getMappingKey(6, 707,0),740); //Bringen+Holen: Schrebergarten/Wochenendhaus
		activityMapping.put(getMappingKey(6, 708,0),640); //Bringen+Holen: Tagesausflug
		activityMapping.put(getMappingKey(6, 709,0),640); //Bringen+Holen: Urlaub
		activityMapping.put(getMappingKey(6, 710,0),722); //Bringen+Holen: Spaziergang
		activityMapping.put(getMappingKey(6, 711,0),722); //Bringen+Holen: Hund ausführen
		activityMapping.put(getMappingKey(6, 712,0),721); //Bringen+Holen: Joggen u.ä....
		activityMapping.put(getMappingKey(6, 713,0),522); //Bringen+Holen: Kirche und Friedhof
		activityMapping.put(getMappingKey(6, 714,0),300); //Bringen+Holen: Ehrenamt, Verein, pol. Aufgaben 
		activityMapping.put(getMappingKey(6, 715,0),213); //Bringen+Holen: Jobben i.d. Freizeit
		activityMapping.put(getMappingKey(6, 716,0),881); //Bringen+Holen: Begleitung von Kindern (Spielplatz)
		activityMapping.put(getMappingKey(6, 717,0),740); //Bringen+Holen: Hobby
		activityMapping.put(getMappingKey(6, 718,0),720); //Bringen+Holen: Jugendfreizeitheim
		activityMapping.put(getMappingKey(6, 719,0),723); //Bringen+Holen: Spielen (Spielplatz)
		activityMapping.put(getMappingKey(6, 720,0),740); //Bringen+Holen: Sonstiger Freizeitzweck
		activityMapping.put(getMappingKey(6, 799,0),740); //Bringen+Holen: keine Angabe
		activityMapping.put(getMappingKey(6, 999,0),740); //Bringen+Holen: keine Angabe
		activityMapping.put(getMappingKey(6,3307,0),799); //Bringen+Holen: kein Einkaufs-, Erledigungs- und Freizeitweg?!?!?
		activityMapping.put(getMappingKey(7,  11,0),740); //Freizeit: Begleitung
		activityMapping.put(getMappingKey(7, 503,0), 50); //Freizeit: allg. Einkaufsbummel
		activityMapping.put(getMappingKey(7, 701,0),631); //Freizeit: Besuch/Treffen
		activityMapping.put(getMappingKey(7, 702,0),640); //Freizeit: Kulturelle Einrichtung
		activityMapping.put(getMappingKey(7, 703,0),724); //Freizeit: Besuch einer Veranstaltung
		activityMapping.put(getMappingKey(7, 704,0),721); //Freizeit: Sport
		activityMapping.put(getMappingKey(7, 705,0),414); //Freizeit: Weiterbildung
		activityMapping.put(getMappingKey(7, 706,0),720); //Freizeit: Restaurant
		activityMapping.put(getMappingKey(7, 707,0),740); //Freizeit: Schrebergarten/Wochenendhaus
		activityMapping.put(getMappingKey(7, 708,0),640); //Freizeit: Tagesausflug
		activityMapping.put(getMappingKey(7, 709,0),640); //Freizeit: Urlaub
		activityMapping.put(getMappingKey(7, 710,0),722); //Freizeit: Spaziergang
		activityMapping.put(getMappingKey(7, 711,0),722); //Freizeit: Hund ausführen
		activityMapping.put(getMappingKey(7, 712,0),721); //Freizeit: Joggen...
		activityMapping.put(getMappingKey(7, 713,0),522); //Freizeit: Kirche und Friedhof
		activityMapping.put(getMappingKey(7, 714,0),300); //Freizeit: Ehrenamt, Verein, pol. Aufgaben 
		activityMapping.put(getMappingKey(7, 715,0),213); //Freizeit: Jobben i.d. Freizeit
		activityMapping.put(getMappingKey(7, 716,0),881); //Freizeit: Begleitung von Kindern (Spielplatz)
		activityMapping.put(getMappingKey(7, 717,0),740); //Freizeit: Hobby
		activityMapping.put(getMappingKey(7, 718,0),720); //Freizeit: Jugendfreizeitheim
		activityMapping.put(getMappingKey(7, 719,0),723); //Freizeit: Spielen (Spielplatz)
		activityMapping.put(getMappingKey(7, 720,0),740); //Freizeit: Sonstiges
		activityMapping.put(getMappingKey(7, 799,0),740); //Freizeit: keine Angabe zum Detail
		activityMapping.put(getMappingKey(7, 999,0),740); //Freizeit: keine Angabe
		activityMapping.put(getMappingKey(7,3307,0),740); //Freizeit: kein Einkaufs-, Erledigungs- und Freizeitweg?!?!?
		activityMapping.put(getMappingKey(8,  11,0),740); //Rückweg nach Hause: Begleitung
		activityMapping.put(getMappingKey(8,3307,0), 799); //Rückweg nach Hause
		activityMapping.put(getMappingKey(9,11  ,0), 799); //Rückweg vom vorherigen Weg (Begleitung)
		activityMapping.put(getMappingKey(9,3307,0), 799); //Rückweg vom vorherigen Weg
		activityMapping.put(getMappingKey(10,11  ,0),799); //andere Aktivität : Begleitung
		activityMapping.put(getMappingKey(10,3307,0),799); //andere Aktivität
		activityMapping.put(getMappingKey(11,3307,0),799); //Begleitung Erwachsener
		activityMapping.put(getMappingKey(31,3307,0),410); //zur Schule incl Vorschule
		activityMapping.put(getMappingKey(32,3307,0),413); //Kita/Kindergarten/Hort
		activityMapping.put(getMappingKey(97,3307,0),799); //verweigert
		activityMapping.put(getMappingKey(98,3307,0),799); //weiß nicht
		activityMapping.put(getMappingKey(99,3307,0),799); //keine Angabe
		
		//now we go through the diaries and convert the numbers:
		int schemeID =1, start, duration, act_code_zbe, tourNumber, key;
		boolean home, workchain;
		
		PrintWriter pw = new PrintWriter(System.out	); //needed to get rid of stupid german localization of doubles!
		String tmpString ="";
		tmpString = String.format(Locale.ENGLISH,"INSERT INTO %s"+
				" (scheme_id, start, duration, act_code_zbe, home, tournumber, workchain) "
				+ "VALUES (?,?,?,?,?,?,?);", table_episode);

		try {
			PreparedStatement pS = this.dbCon.getConnection(this).prepareStatement(tmpString);
			int batchSize=0, maxSize=10000;
		
			int schemes=0, diaries=0;
			for( Entry<String, Diary> e : this.diaryMap.entrySet()){
				Diary tmp = e.getValue();
				tmp.schemeID=schemeID;
				tmpString = String.format(Locale.ENGLISH,"INSERT INTO %s (scheme_id, scheme_class_id, homework) VALUES (%d,%d,false);",table_schemes, tmp.schemeID,tmp.group);
				if(print){
					pw.printf(tmpString+"\n");
				}
				else{
					this.dbCon.execute(tmpString, this);
				}
				schemes++;
				
				for(DiaryElement d : tmp.activities){
					diaries++;
					start = d.start_min;
					duration = d.getDurtation();
					key = getMappingKey(d.purpose,d.purposeDetailed,tmp.pGroup);
					if(!activityMapping.containsKey(key)){
						key = getMappingKey(d.purpose,d.purposeDetailed,0);
					}
					act_code_zbe = activityMapping.get(key);
					tourNumber=d.tourNumber;
					home= d.home && d.stay;
					workchain = d.workchain;
					if(!d.stay)
						act_code_zbe=80;
					//System.out.printf("Scheme %6d Start %4d Duration %4d Act %3d tour %2d home %s workchain %s\n", schemeID, start, duration, act_code_zbe, tourNumber, (home?"T":"F"), (workchain?"T":"F"));
					tmpString = String.format(Locale.ENGLISH,"INSERT INTO %s"+
							" (scheme_id, start, duration, act_code_zbe, home, tournumber, workchain) "
							+ "VALUES (%d,%d,%d,%d,%s,%d,%s); --hhid: %d, pid: %d",
							table_episode,tmp.schemeID,start,duration,act_code_zbe,Boolean.toString(home),tourNumber,Boolean.toString(workchain),tmp.hhID,tmp.pID);
					if(print){
						pw.printf(tmpString+"\n");
					}
					else{
						int index =1;
						pS.setInt(index++, tmp.schemeID);
						pS.setInt(index++, start);
						pS.setInt(index++, duration);
						pS.setInt(index++, act_code_zbe);
						pS.setBoolean(index++, home);
						pS.setInt(index++, tourNumber);
						pS.setBoolean(index++, workchain);
						pS.addBatch();
						batchSize++;
					}					
				}
				schemeID++;
				if(batchSize>=maxSize){
					pS.executeBatch();
					batchSize=0;
				}
			}
			pS.executeBatch();
			pw.flush();
			if(!print){
				System.out.println("Inserted "+schemes+" schemes with "+diaries+" diaries.");
			}
		}
		catch(SQLException e){
			System.err.println("Error in sqlstatement: "+tmpString);
			e.printStackTrace();
		}

		
		
	}
	
	public void printDistributionVectors(){
		Map<Integer,Integer> groupDistribution;
		Map<Integer,Integer> groupSumCount = new HashMap<>();
		System.out.println("PG\tDG\tNum\tProb");
		int count;
		double norm;
		//count the numbers for normalization
		for( Integer pgroup : this.personGroupDistribution.keySet()){
			groupDistribution = this.personGroupDistribution.get(pgroup);
			count =0;
			for(Integer dgroup: this.diaryGroups){
				count+=groupDistribution.get(dgroup);
			}
			groupSumCount.put(pgroup, count);
		}

		for( Integer pgroup : this.personGroupDistribution.keySet()){
			groupDistribution = this.personGroupDistribution.get(pgroup);			
			norm = 1.0/groupSumCount.get(pgroup);
			for(Integer dgroup: this.diaryGroups){
				System.out.println(pgroup+"\t"+dgroup+"\t"+groupDistribution.get(dgroup)+"\t"+(groupDistribution.get(dgroup)*norm));
			}
		}		
	}
	
	public void printDistributionVectorSQLInserts(String tablename, String name, boolean print){
		Map<Integer,Integer> groupDistribution;
		Map<Integer,Integer> groupSumCount = new HashMap<>();
		int count;
		double norm;
		//count the numbers for normalization
		for( Integer pgroup : this.personGroupDistribution.keySet()){
			groupDistribution = this.personGroupDistribution.get(pgroup);
			count =0;
			for(Integer dgroup: this.diaryGroups){
				count+=groupDistribution.get(dgroup);
			}
			groupSumCount.put(pgroup, count);
		}

		PrintWriter pw = new PrintWriter(System.out	); //needed to get rid of stupid german localization of doubles!
		String query = "";
		//clean up
		if(!print){
			query = "DELETE FROM "+tablename+" where name ='"+name+"'";
			this.dbCon.execute(query, this);
		}

		for( Integer pgroup : this.personGroupDistribution.keySet()){
			groupDistribution = this.personGroupDistribution.get(pgroup);
			norm = 1.0/groupSumCount.get(pgroup);
			for(Integer dgroup: this.diaryGroups){
				query = String.format(Locale.ENGLISH,"INSERT INTO %s (name, scheme_class_id, person_group, probability) VALUES ('%s',%d,%d,%f);",tablename,name,dgroup,pgroup,groupDistribution.get(dgroup)*norm);
				if(print){
					pw.printf(query+"\n");
				}
				else {
					this.dbCon.execute(query, this);
				}
			}
		}

		pw.flush();
	}
	
	public void cleanUpDB(String table){
		String query="DELETE FROM "+table;
		this.dbCon.execute(query, this);
	}

	
	public static void main(String[] args) {
		TPS_MiD_Diary_Analyzer worker = new TPS_MiD_Diary_Analyzer();
		HashMap<String,String> times = new HashMap<>();
//		times.put("true", "_Mo-So");
		//times.put("stichtag = ANY(ARRAY[1,2,3,4,5])","_Mo-Fr");
		times.put("stichtag = ANY(ARRAY[2,3,4])","_Di-Do");
		//times.put("stichtag = ANY(ARRAY[6,7])","_Sa-So");
//		times.put("stichtag = 1","_Mo");
//		times.put("stichtag = 2","_Di");
//		times.put("stichtag = 3","_Mi");
//		times.put("stichtag = 4","_Do");
//		times.put("stichtag = 5","_Fr");
//		times.put("stichtag = 6","_Sa");
//		times.put("stichtag = 7","_So");
		HashMap<String,String> regions = new HashMap<>();
				
//		regions.put("true", "");
//		regions.put("rtyp = 1", "_RTYP1");
//		regions.put("rtyp = 2", "_RTYP2");
//		regions.put("rtyp = 3", "_RTYP3");
//		regions.put("polgk =1", "_PolGK1");
//		regions.put("polgk =2", "_PolGK2");
//		regions.put("polgk =3", "_PolGK3");
//		regions.put("polgk =4", "_PolGK4");
//		regions.put("polgk =5", "_PolGK5");
		regions.put("polgk =6", "_PolGK6");
		
		
		for(Entry<String,String> t:times.entrySet()){
			for(Entry<String,String> r:regions.entrySet()){
				worker.readMIDDiary("core.global_mid_puf_wege", t.getKey()+" and "+r.getKey());
				//System.out.println("Read "+worker.diaryMap.size()+" diaries");
				//System.out.println("Found "+worker.numOfDoubleWays+" doublet ways. "+worker.numOfExactDoubleWays+" are exact doublets.");
				worker.calcDiaryStatistics();
				boolean printOnScreen =true;
				boolean delete = false;
				
				if(!printOnScreen&& delete){
					worker.cleanUpDB("core.global_scheme_classes_mid");
					worker.cleanUpDB("core.global_episodes_mid");
					worker.cleanUpDB("core.global_schemes_mid");
				}
				//worker.printSchemeClassSQLInserts("core.global_scheme_classes_mid",printOnScreen);
				//worker.printDiariesSQLInserts("core.global_episodes_mid","core.global_schemes_mid",printOnScreen);
				worker.printDistributionVectors();
				worker.printDistributionVectorSQLInserts("core.global_scheme_class_distributions_mid","MID_2008_"+groupCol_name+t.getValue()+r.getValue(),printOnScreen);	
				worker.clearEverything();
			}
		}
	}
}
