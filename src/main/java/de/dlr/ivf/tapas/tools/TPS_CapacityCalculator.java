package de.dlr.ivf.tapas.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;


public class TPS_CapacityCalculator extends TPS_BasicConnectionClass {

	static final int UNKNOWN = -2;
	static final int WOHNEN = -1;
	static final int FREE_TIME_HOME_BOUND = 0;
	static final int WORK = 1;
	static final int SCHOOL = 2;
	static final int SHOPPING = 3;
	static final int PERSONAL_MATTERS = 4;
	static final int FREE_TIME_EXTERN = 5;
	static final int MISC = 6;
	static final int STUDY = 7;
	static final int USER = 8;
	static final String SEPARATOR="-";
	
	class TargetLocation{
		double x,y;
		int loc_id;
		int loc_code;
		String loc_enterprise;
		int loc_capacity=0;
		boolean fix=true;
		String loc_type,loc_unit;
		
	}
	
	class CapacityInfo{
		int tapasCode =UNKNOWN;
		double min=0;
		double max=0;
		String relatedOn = "";
	}
	
	public static String getCombinedCategory(String cat1, String cat2, String cat3){
		String returnVal =cat1; //cat1 can not be null!
		if(cat2!=null && cat2.length()>0){
			returnVal+=SEPARATOR+cat2;
			if(cat3!=null && cat3.length()>0){
				returnVal+=SEPARATOR+cat3;
			}
		}
		
		return returnVal;
	}	
	
	class CapacityType{
		String type;
		String[] cathegory = new String[3];
		List<CapacityInfo> infos = new ArrayList<>();

		double workFactor = 0;
		double userFactor = 0;
		double defaultCapaUser =0;
		double defaultCapaWorker=0;
		
		public boolean hasCatLevel(int level){
			return cathegory[level-1]!= null  && cathegory[level-1].length()>0;
		}
		
		/**
		 * Method to get the combined cathegory-key from the cathegory array up to a specific level. 
		 * For instance cathegory {"Einkauf","Lebensmittel","SB"} becomes at level 3 "Einkauf - Lebensmittel - SB" 
		 *
		 * @param maxLevel the maximum cathegory
		 * @return the combined String
		 */
		public String getCombinedCathegory(int maxLevel){
			return getCombinedCathegory(maxLevel, SEPARATOR);
		}
		
		public String getCombinedCathegory(int maxLevel, String separator){
			String returnVal ="";
			if(cathegory[0]!= null  && cathegory[0].length()>0){
				returnVal = cathegory[0];
				if(hasCatLevel(2) && maxLevel >1){
					returnVal = returnVal+ separator+cathegory[1];
					if(hasCatLevel(3) && maxLevel >2){
						returnVal = returnVal+ separator+cathegory[2];
					}
				}
			}
			return returnVal;
		}

	/**
		 * This method extracts the cathegories from a dash-separated combined string 
		 * @param combo
		 */
		public void extractCombinedCathegory(String combo){
            String[] tok = combo.split(SEPARATOR);
			if(tok.length<=3){
				for(int i = 0; i<tok.length; ++i) {
					cathegory[i]= tok[i].trim();
				}
			}			
		}
		
		public void addCapacityInfo(CapacityInfo info){
			this.infos.add(info);
			if(info.tapasCode==WORK)
				this.workFactor=info.max;
			else if(info.tapasCode != UNKNOWN)
				this.userFactor=info.max;				
		}
		
		public void setDefaultCapa(int code, double val){
			if(code==WORK){
				this.defaultCapaWorker=val;
				if(this.workFactor!=0 && this.userFactor!=0)
					this.defaultCapaUser= val*this.userFactor/this.workFactor;
			}
			else if(code != UNKNOWN){
				this.defaultCapaUser=val;
				if(this.workFactor!=0 && this.userFactor!=0)
					this.defaultCapaWorker= val*this.workFactor/this.userFactor;
			}
		}
		
		
		/**
		 * Function to calculate a capacity from a given capacity of a different info.
		 * @return the rounded capacity
		 */
		public int capacityConvert(int cappa, int sourceTypeIndex, int destinationTypeIndex){
			double area = cappa/this.infos.get(sourceTypeIndex).max;
			return (int)((this.infos.get(destinationTypeIndex).max*area)+0.5);
		}
	}
	
	
	private String makeSQLArray(String separatedArrary, String separator){
		StringBuilder returnVal = new StringBuilder("ARRAY[");
		
		if(separatedArrary!=null && separatedArrary.length()>0){ //everything ok
            String[] tok = separatedArrary.split(separator);
			for(int i=0; i<tok.length; ++i){ //build strings
				returnVal.append("'").append(tok[i].trim()).append("'");
				if((i+1)<tok.length) // is there another element?
					returnVal.append(",");
			}
		}
		else{
			returnVal.append("NULL");
		}
		
		returnVal.append("]");
		return returnVal.toString();
	}
	
	public void catMatchingCSVImport(String filename, String tablename) {
		FileReader in = null;	BufferedReader input = null; 
		String line;
		String query;
		try {
			in = new FileReader (filename);
			input = new BufferedReader (in);
			//query= "DELETE FROM "+tablename;
			//this.dbCon.execute(query, this);
			input.readLine();//header
			while((line = input.readLine()) != null){
				boolean fakeDescription=false;
				if(line.endsWith(";")){ //add a ending space to avoid tructiating of the last entry
					line+=" ";
					fakeDescription = true;
				}
                String[] tok1 = line.split(";");
				if(tok1.length==10){
					query= "INSERT INTO "+tablename+" "
							+ "kategorie, unterkategorie_1, unterkategorie_2, default_cappa,"
							+ "user_factor, worker_factor, name_match, wz_key, name, comment) "
							+ "VALUES ("+
						   "'"+ tok1[0]+"', "+
						   "'"+ tok1[1]+"', "+
						   "'"+ tok1[2]+"', "+
						   (tok1[3].length()==0?0:tok1[3])+", "+
						   (tok1[4].length()==0?0:tok1[4])+", "+
						   (tok1[5].length()==0?0:tok1[5])+", "+
						   "'"+ tok1[6]+"', "+
						   makeSQLArray(tok1[7], ",")+", "+
					   	   "'"+ tok1[8]+"', ";
					if(!fakeDescription){
						query += "'"+ tok1[9]+"')";
					}
					else{
						query += "NULL)";
					}
					this.dbCon.execute(query, this);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try	{	
				if(input != null)input.close();	
				if(in!= null)	in.close();	
			} catch (IOException ex) {
				TPS_Logger.log(SeverenceLogLevel.ERROR, " Could not close : "+filename);
			} 
		}//finally
	}
	
	private void aggregateCat(String[] array, Map<String,CapacityType> catMap, Map<String,CapacityType> sourceMap, int catLevel){
		for (String s : array) {
			//create entries
			CapacityType catAggregate = new CapacityType();
			catAggregate.type = s;
			catAggregate.extractCombinedCathegory(s);
			CapacityInfo work = new CapacityInfo();
			work.tapasCode = WORK;
			CapacityInfo user = new CapacityInfo();

			int numMinWork = 0, numMaxWork = 0;
			int numMinUser = 0, numMaxUser = 0;
			int numEntry = 0;
			double defaultCapaUser = 0, defaultCapaWork = 0, factorU = 0, factorW = 0;
			//aggregate infos
			for (Entry<String, CapacityType> e : sourceMap.entrySet()) {
				CapacityType tmp = e.getValue();
				if (tmp.hasCatLevel(catLevel) && tmp.getCombinedCathegory(catLevel).equals(s)) {
					numEntry++;
					defaultCapaUser += tmp.defaultCapaUser;
					defaultCapaWork += tmp.defaultCapaWorker;
					factorU += tmp.userFactor;
					factorW += tmp.workFactor;
					for (int j = 0; j < tmp.infos.size(); ++j) {
						CapacityInfo tmpInfo = tmp.infos.get(j);
						if (tmpInfo.tapasCode == WORK) {
							numMaxWork++; //we always have max!
							work.max += tmpInfo.max;
							if (tmpInfo.min > 0) {
								numMinWork++;
								work.min += tmpInfo.min;
							}
						} else if (tmpInfo.tapasCode != UNKNOWN) {
							//set code if unknown
							if (user.tapasCode == UNKNOWN) {
								user.tapasCode = tmpInfo.tapasCode;
							}
							numMaxUser++; //we always have max!
							user.max += tmpInfo.max;
							if (tmpInfo.min > 0) {
								numMinUser++;
								user.min += tmpInfo.min;
							}
						}
					}
				}
			}
			//finish and add infos
			if (numMaxWork > 0) {
				if (numMinWork > 0) work.min /= numMinWork;
				work.max /= numMaxWork;
				catAggregate.addCapacityInfo(work);
			}
			if (numMaxUser > 0) {
				if (numMinUser > 0) user.min /= numMinUser;
				user.max /= numMaxUser;
				catAggregate.addCapacityInfo(user);
			}
			catAggregate.defaultCapaUser = defaultCapaUser / numEntry;
			catAggregate.defaultCapaWorker = defaultCapaWork / numEntry;
			catAggregate.userFactor = factorU / numEntry;
			catAggregate.workFactor = factorW / numEntry;
			//add Type
			catMap.put(s, catAggregate);
		}

	}
	
	public void aggregateValues(Map<String,CapacityType> map){
		Set<String> cats1 = new HashSet<>();
		Set<String> cats2 = new HashSet<>();
		Set<String> cats3 = new HashSet<>();
		//find level sets
		for(Entry<String, CapacityType> e : map.entrySet()){
			CapacityType tmp = e.getValue();
			cats1.add(tmp.getCombinedCathegory(1));
			if(tmp.hasCatLevel(2)){
				cats2.add(tmp.getCombinedCathegory(2));
			}
			if(tmp.hasCatLevel(3)){
				cats3.add(tmp.getCombinedCathegory(3));
			}
		}
		
		//aggregate level 3
        String[] array = cats3.toArray(new String[0]);
		aggregateCat(array, this.cat3Map,map,3);

		//aggregate level 2
		array = cats2.toArray(new String[0]);
		aggregateCat(array, this.cat2Map,map,2);

		//aggregate level 1
		array = cats1.toArray(new String[0]);
		aggregateCat(array, this.cat1Map,map,1);

	}
	
	Map<String,CapacityType> typeMap = new HashMap<>();
	Map<String,CapacityType> cat1Map = new HashMap<>();
	Map<String,CapacityType> cat2Map = new HashMap<>();
	Map<String,CapacityType> cat3Map = new HashMap<>();
	Map<String,CapacityType> wzMap = new HashMap<>();
	Map<String,Integer> codeMap = new HashMap<>();
	List<TargetLocation> locations = new LinkedList<>();
	
	int maxID=1;
	
	public void printMappingValues(Map<String,CapacityType> map){
		for(CapacityType e: map.values()){
			System.out.println("Type "+e.getCombinedCathegory(3)+" ("+e.type+"):");
			for(int i=0; i<e.infos.size();++i){
				CapacityInfo tmp = e.infos.get(i);
				System.out.println("\t"+i+": "+tmp.tapasCode+" "+tmp.min+"\t"+tmp.max);
			}
		}
	}
	
	public void readBosserhofValues(){
		String query ="";
		
		try {
			query = "select typ, bezugsgroesse, kategorie, unterkategorie_1, unterkategorie_2, code, min, max from core.global_bosserhof_mapping order by id";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				String typ = rs.getString("typ");
				String bezugsgroesse = rs.getString("bezugsgroesse");
				String kategorie = rs.getString("kategorie");
				String unterkategorie_1 = rs.getString("unterkategorie_1");
				String unterkategorie_2 = rs.getString("unterkategorie_2");
				int code = rs.getInt("code");
				double min = rs.getDouble("min");
				double max = rs.getDouble("max");

				//build the info
				CapacityInfo info = new CapacityInfo();
				info.relatedOn= bezugsgroesse;
				info.tapasCode= code;
				info.min=min;
				info.max=max;

				//check for existing cappa type
				CapacityType cappaTyp;
				if(typeMap.containsKey(typ))
					cappaTyp = typeMap.get(typ); //use existing one
				else{
					cappaTyp = new CapacityType(); //create new one
					cappaTyp.type = typ;
					cappaTyp.cathegory[0]= kategorie;
					if(unterkategorie_1!=null && unterkategorie_1.length()>0 && !unterkategorie_1.equals("''"))
						cappaTyp.cathegory[1]= unterkategorie_1;
					if(unterkategorie_2!=null && unterkategorie_2.length()>0 && !unterkategorie_2.equals("''"))
						cappaTyp.cathegory[2]= unterkategorie_2;	
					typeMap.put(typ, cappaTyp); //add it
				}
				cappaTyp.addCapacityInfo(info); //add info
			}
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}		
	}
	
	public void generateCodeMap(){
		String query ="";
		
		try {
			query = "select distinct "
					+ "kategorie, unterkategorie_1, unterkategorie_2 "
					+ "FROM core.global_bosserhof_ws_mapping order by kategorie, unterkategorie_1, unterkategorie_2";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			String lastKategorie = "";
			String lastUnterkategorie_1 = "";
			String lastUnterkategorie_2 = "";
			int code = 1000000000;
			
			
			while(rs.next()){
				String kategorie = rs.getString("kategorie");
				if(kategorie.equals("Arbeit")){
					@SuppressWarnings("unused")
					int i=0;
				}
				String unterkategorie_1 = rs.getString("unterkategorie_1");
				String unterkategorie_2 = rs.getString("unterkategorie_2");
				if(!kategorie.equals(lastKategorie)){
					code+=1000000;
					code/=1000000;
					code*=1000000;
				}
				if(!unterkategorie_1.equals(lastUnterkategorie_1)){
					code+=1000;
					code/=1000;
					code*=1000;
				}
				if(!unterkategorie_2.equals(lastUnterkategorie_2))
					code+=1;
				lastKategorie = kategorie;
				lastUnterkategorie_1 = unterkategorie_1;
				lastUnterkategorie_2 = unterkategorie_2;
				String entry = getCombinedCategory( kategorie,unterkategorie_1,unterkategorie_2);
				codeMap.put(entry, code);
			}
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}		

	}
	
	public void printCodeMap(){
		for(Entry<String, Integer> e: codeMap.entrySet()){
			System.out.println(e.getKey()+ ":\t"+e.getValue());
		}
	}
	
	public void updateLocCodesInBosserhofMapingValues(){
		String query ="";
		for(Entry<String, Integer> e: codeMap.entrySet()){
			String[] cat = e.getKey().split(SEPARATOR);
			if(cat.length==3){
				query = "UPDATE core.global_bosserhof_ws_mapping set loc_code="+e.getValue()+
						" WHERE kategorie='"+cat[0]+"' and "+
						"unterkategorie_1='"+cat[1]+"' and "+
						"unterkategorie_2='"+cat[2]+"'";
			}
			if(cat.length==2){
				query = "UPDATE core.global_bosserhof_ws_mapping set loc_code="+e.getValue()+
						" WHERE kategorie='"+cat[0]+"' and "+
						"unterkategorie_1='"+cat[1]+"' and "+
						"unterkategorie_2=NULL";
			}
			if(cat.length==1){
				query = "UPDATE core.global_bosserhof_ws_mapping set loc_code="+e.getValue()+
						" WHERE kategorie='"+cat[0]+"' and "+
						"unterkategorie_1=NULL and "+
						"unterkategorie_2=NULL";
			}

			this.dbCon.executeUpdate(query, this);
		}
	}
	
	public void readBosserhofMapingValues(){
		String query ="";
		
		try {
			query = "select "
					+ "name, kategorie, unterkategorie_1, unterkategorie_2, default_cappa,"
					+ "user_factor, worker_factor, wz_key FROM core.global_bosserhof_ws_mapping order by kategorie, unterkategorie_1, unterkategorie_2";
			ResultSet rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				String typ = rs.getString("name");
				String kategorie = rs.getString("kategorie");
				String unterkategorie_1 = rs.getString("unterkategorie_1");
				String unterkategorie_2 = rs.getString("unterkategorie_2");
				double defaultCappa = rs.getDouble("default_cappa");
				double userFactor = rs.getDouble("user_factor");
				double workFactor = rs.getDouble("worker_factor");

				//create the info
				CapacityInfo infoUser = new CapacityInfo();
				infoUser.max = userFactor;
				infoUser.tapasCode = USER;
				//create the info
				CapacityInfo infoWorker = new CapacityInfo();
				infoWorker.max = workFactor;
				infoWorker.tapasCode = WORK;
				
				//check for existing cappa type
				CapacityType cappaTyp = new CapacityType(); //create new one
				cappaTyp.type = typ;
				cappaTyp.cathegory[0]= kategorie;
				if(unterkategorie_1!=null && unterkategorie_1.length()>0 && !unterkategorie_1.equals("''"))
					cappaTyp.cathegory[1]= unterkategorie_1;
				if(unterkategorie_2!=null && unterkategorie_2.length()>0 && !unterkategorie_2.equals("''"))
					cappaTyp.cathegory[2]= unterkategorie_2;	
				wzMap.put(cappaTyp.getCombinedCathegory(3), cappaTyp); //add it
				
				cappaTyp.addCapacityInfo(infoUser); //add info
				cappaTyp.addCapacityInfo(infoWorker); //add info
				cappaTyp.setDefaultCapa(USER, defaultCappa);

			}
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}		
	}
	
	public void updateLocationCapacity(String tableName){
		String query;
		for(Entry<String, CapacityType> e : this.wzMap.entrySet()){

			CapacityType capaTyp = e.getValue();
			if(capaTyp.cathegory[0].equals("Arbeit"))
				continue;
			double capa= capaTyp.defaultCapaUser/capaTyp.userFactor;
			query = "update "+tableName+" set capacity_value = "+capa+
					" WHERE capacity_value isnull and general_name = '"+capaTyp.getCombinedCathegory(3)+"'";
			this.dbCon.executeUpdate(query, this);
		}
	}
	
	public void loadLocations(String origin){
		String query ="";
		
		try {
			
			query = "select capacity_value as capa, unit as loc_unit, loc_id, general_type as loc_enterprise, general_name as loc_type, x_coord as x1, y_coord as y1 FROM  "+origin;
			ResultSet rs = this.dbCon.executeQuery(query, this);
			
			while(rs.next()){
				String loc_type = rs.getString("loc_type");
				
				loc_type = loc_type.replaceAll("ä", "ae");
				loc_type = loc_type.replaceAll("ö", "oe");
				loc_type = loc_type.replaceAll("ü", "ue");
				loc_type = loc_type.replaceAll("Ä", "Ae");
				loc_type = loc_type.replaceAll("Ö", "Oe");
				loc_type = loc_type.replaceAll("Ü", "Ue");
				if(loc_type.equals("private Erledigung-Dienstleistung-Versicherungen")){ //hot-fix
					loc_type = "private Erledigung-Dienstleistung-Versicherung";
				}
				if(loc_type.equals("private Erledigung-Familie-Besuch")){ //this cathegorty contains only bull shit
					continue;
				}

				TargetLocation tmp = new TargetLocation();
				CapacityType capaInfo = this.wzMap.get(loc_type);
				if(capaInfo==null){
					System.err.println("No capacity Info for "+loc_type);
					continue;
				}
				//tmp.loc_id=rs.getInt("loc_id")*2;
				tmp.loc_id=maxID*2;
				maxID++;
				tmp.loc_unit= rs.getString("loc_unit");
				
				//unit convert
				switch(tmp.loc_unit){
				case "Schüler":
				case "Schüler gymn. Oberstufe":
				case "Studierende":
				case "Schüler geistig behindert":
				case "Schüler Klassen 5-6":
				case "Schüler Klassen 1-4":
				case "Sitzplätze":
				case "Kunden":
				case "Personen":
				case "Schüler Klassen 7-10":
					tmp.loc_capacity=(int)(rs.getDouble("capa"));
					break;
				case "Quadratmeter":
				case "Quadratmeter Gastronomie":
					tmp.loc_capacity=(int)(rs.getDouble("capa")*capaInfo.userFactor);
					break;
				case "Besucher 2010":
				case "Besucher am Standort":
					tmp.loc_capacity=(int)(rs.getDouble("capa"))/365;
					break;
				case "Fälle=*Besucher*Quartal":
				case "Fälle=*Patienten*Quartal":
					tmp.loc_capacity=(int)(rs.getDouble("capa"))/(365/4);
					break;
				case "Parzellen":
					tmp.loc_capacity=(int)(rs.getDouble("capa")*300*capaInfo.userFactor); //ein Kleingarten = 300m²
					break;
				default:
					tmp.loc_capacity=(int)(rs.getDouble("capa")*capaInfo.userFactor);
					break;
				}

				tmp.loc_enterprise = rs.getString("loc_enterprise");
				if(tmp.loc_enterprise!=null && tmp.loc_enterprise.length()>25)
					tmp.loc_enterprise=tmp.loc_enterprise.substring(0, 25);
				tmp.loc_type = rs.getString("loc_type");
				tmp.x	= rs.getDouble("x1");
				tmp.y	= rs.getDouble("y1");
				tmp.loc_code= codeMap.get(loc_type); 
				locations.add(tmp);
				//generate workers/users from wzMap
				TargetLocation tmpWork = new TargetLocation();
				tmpWork.loc_id=tmp.loc_id+1;
				tmpWork.loc_unit= tmp.loc_unit;
				tmpWork.loc_capacity= (int)(tmp.loc_capacity*capaInfo.workFactor/capaInfo.userFactor);
				tmpWork.loc_enterprise = tmp.loc_enterprise;
				tmpWork.loc_type = "Arbeit";
				tmpWork.x	= tmp.x;
				tmpWork.y	= tmp.y;
				tmpWork.loc_code= codeMap.get("Arbeit");
				if(tmpWork.loc_capacity>0)
					locations.add(tmpWork);
												
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}
	}	
	
	public void loadNexigaLocations(String origin){
		String query ="";
		
		try {
			
			query = "select beschaeftigte as capa, 'worker' as loc_unit, id_extern as loc_id, general_type as loc_enterprise, general_name as loc_type, st_X(st_transform(the_geom,4326)) as x1, st_Y(st_transform(the_geom,4326)) as y1 FROM  "+origin;
			ResultSet rs = this.dbCon.executeQuery(query, this);
			
			while(rs.next()){
				String loc_type = rs.getString("loc_type");
				loc_type = loc_type.replaceAll(" - ", "-");
				if(loc_type.equals("private Erledigung-Dienstleistung-Versicherungen")){ //hot-fix
					loc_type = "private Erledigung-Dienstleistung-Versicherung";
				}
				if(loc_type.equals("private Erledigung-Familie-Besuch")){ //this cathegorty contains only bull shit
					continue;
				}
				TargetLocation tmp = new TargetLocation();
				CapacityType capaInfo = this.wzMap.get(loc_type);
				if(capaInfo==null){
					System.err.println("No capacity Info for "+loc_type);
					continue;
				}
				//tmp.loc_id=rs.getInt("loc_id")*2;
				//if(rs.getInt("loc_id")==58402){
				//	System.out.println("Found TU");
				//}
				tmp.loc_id=maxID*2;
				maxID++;
				tmp.loc_unit= rs.getString("loc_unit");
				
				//unit convert
				if(loc_type.equals("Arbeit"))
					tmp.loc_capacity=(int)(rs.getDouble("capa"));
				else
					tmp.loc_capacity=(int)(rs.getDouble("capa")*capaInfo.userFactor/capaInfo.workFactor);

				tmp.loc_enterprise = rs.getString("loc_enterprise");
				if(tmp.loc_enterprise!=null && tmp.loc_enterprise.length()>25)
					tmp.loc_enterprise=tmp.loc_enterprise.substring(0, 25);
				tmp.loc_type = rs.getString("loc_type");
				tmp.x	= rs.getDouble("x1");
				tmp.y	= rs.getDouble("y1");
				tmp.loc_code= codeMap.get(loc_type); 
				locations.add(tmp);
				
				if(!loc_type.equals("Arbeit")){
					//generate workers/users from wzMap
					TargetLocation tmpWork = new TargetLocation();
					tmpWork.loc_id=tmp.loc_id+1;
					if (tmpWork.loc_id==58402)
						System.out.println("FOUND PoPloa");
					tmpWork.loc_unit= tmp.loc_unit;
					tmpWork.loc_capacity= (int)(rs.getDouble("capa"));
					tmpWork.loc_enterprise = tmp.loc_enterprise;
					tmpWork.loc_type = "Arbeit";
					tmpWork.x	= tmp.x;
					tmpWork.y	= tmp.y;
					tmpWork.loc_code= codeMap.get("Arbeit");
					if(tmpWork.loc_capacity>0)
						locations.add(tmpWork);
				}
												
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}
	}
	
	public void emptyLocations(String destination){
		//empty target
		String query="DELETE FROM "+destination;
		this.dbCon.execute(query, this);
	}		

	public void printLocations(){
		for(TargetLocation e: locations){
			if(e.loc_code>0)
				System.out.println(e.loc_id+": "+e.loc_enterprise+ " "+e.loc_type+ " "+e.loc_capacity+ " "+e.loc_code);
			else
				System.err.println(e.loc_id+": "+e.loc_enterprise+ " "+e.loc_type+ " "+e.loc_capacity+ " "+e.loc_code);
		}
	}
	
	public void storeLocations(String destination, boolean updateTAZ, String tazMultiline, boolean updateBlocks, String blockMultiline){
		String query ="";
		
		try {
			
			
			query = "INSERT INTO "+destination+
					"(loc_id,loc_code,loc_taz_id,loc_group_id,loc_enterprise,loc_capacity,loc_has_fix_capacity,loc_type,loc_unit,loc_coordinate) "+
					"VALUES (?,?,-1,-1,?,?,?,?,?,st_setsrid(st_makepoint(?,?),4326))";
					
			PreparedStatement pSt = this.dbCon.getConnection(this).prepareStatement(query);
			int chunk=0, chunksize=10000;
			
			for(TargetLocation e: locations){
				int i=1;
				pSt.setInt(i++, e.loc_id);
				pSt.setInt(i++, e.loc_code);
				pSt.setString(i++, e.loc_enterprise);
				pSt.setInt(i++, e.loc_capacity);
				pSt.setBoolean(i++, e.fix);
				pSt.setString(i++, e.loc_type);
				pSt.setString(i++, e.loc_unit);
				pSt.setDouble(i++, e.x);
				pSt.setDouble(i++, e.y);
				pSt.addBatch();
				chunk++;
				//commit chunk if necessary
				if(chunk>=chunksize){
					chunk=0;
					pSt.executeBatch();
				}
			}
			//commit remainers
			if(chunk>0){
				pSt.executeBatch();
			}
			if(updateTAZ){
				//delete locs not in Region
				query = "DELETE from "+destination+" WHERE not st_within(loc_coordinate, (SELECT st_union(the_geom) from "+tazMultiline+"))";
				this.dbCon.execute(query, this);
				query="UPDATE "+destination+" as tab SET loc_taz_id = (SELECT taz.gid from "+tazMultiline+" as taz where st_within(tab.loc_coordinate,taz.the_geom))";
				this.dbCon.executeUpdate(query, this);
			}
			if(updateBlocks){
				query="UPDATE "+destination+" as tab SET loc_blk_id = (SELECT blk.blk_id from "+blockMultiline+" as blk where blk.blk_taz_id=tab.loc_taz_id order by st_distance(blk.blk_coordinate, tab.loc_coordinate) LIMIT 1)";
				this.dbCon.executeUpdate(query, this);
			}
			
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
			System.err.println("Next exception:");
			e.getNextException().printStackTrace();
		}
	}
	
	public void storeCodeMap(String table){
		String query ="";
		
		try {
			
			query= "SELECT max(id) as id FROM "+table;

			ResultSet rs = this.dbCon.executeQuery(query, this);
			if(rs.next()){
				int id = rs.getInt("id")+1;
				query = "INSERT INTO "+table+"( id, class, name_general, code_general, type_general, name_tapas, code_tapas, type_tapas)"
						+ "VALUES (?,'de.dlr.ivf.tapas.constants.TPS_LocationCode', ?,?,'GENERAL',?,?,'TAPAS')";
				PreparedStatement pSt = this.dbCon.getConnection(this).prepareStatement(query);
				for(Entry<String, Integer> e: codeMap.entrySet()){
					int i=1;
					pSt.setInt(i++, id++);
					pSt.setString(i++, e.getKey());
					pSt.setInt(i++, e.getValue());
					pSt.setString(i++, e.getKey());
					pSt.setInt(i++, e.getValue());
					pSt.addBatch();
				}
				pSt.executeBatch();
			}
			
			
					

			
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}
	}
	
	public void addHouseHoldsForVisit(String hhTable, String hhKey, String tazTable){
		String query ="";
		
		try {
			
			query= "select sum(hh_persons) as sum, hh_taz_id from "+hhTable+" where hh_key='"+hhKey+"' group by hh_taz_id";

			ResultSet rs = this.dbCon.executeQuery(query, this);
			while(rs.next()){
				//System.out.println("taz: "+ rs.getInt("hh_taz_id"));
				//System.out.println("num: "+ rs.getInt("sum"));
				TargetLocation loc = new TargetLocation();
				loc.fix=false;
				loc.loc_id = maxID*2;
				maxID++;
				loc.loc_code = this.codeMap.get("private Erledigung-Familie-Besuch");
				loc.loc_enterprise = "Personen in TAZ "+rs.getInt("hh_taz_id");
				loc.loc_type= "private Erledigung-Familie-Besuch";
				loc.loc_unit= "Personen";
				loc.loc_capacity= rs.getInt("sum");
				query = "SELECT st_X(taz_coordinate) as x, st_Y(taz_coordinate) as y from "+tazTable+" where taz_id ="+rs.getInt("hh_taz_id");
				ResultSet rs2 = this.dbCon.executeQuery(query, hhTable);
				if(rs2.next()){
					loc.x=rs2.getDouble("x");
					loc.y=rs2.getDouble("y");
				}			
				rs2.close();
				this.locations.add(loc);
			}
			rs.close();
			
					

			
		} catch (SQLException e) {
			System.err.println("Error in sqlstatement: "+query);
			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String[] args) {
		TPS_CapacityCalculator worker = new TPS_CapacityCalculator();
//		worker.readBosserhofValues();
//		System.out.println("Bosserhoff mapping:");
//		worker.printMappingValues(worker.typeMap);
		worker.readBosserhofMapingValues();
		//worker.printMappingValues(worker.wzMap);
		worker.aggregateValues(worker.wzMap);
		//System.out.println("Cat 3 mapping");
		//worker.printMappingValues(worker.cat3Map);
		//System.out.println("Cat 2 mapping");
		//worker.printMappingValues(worker.cat2Map);
		//System.out.println("Cat 1 mapping");
		//worker.printMappingValues(worker.cat1Map);
		worker.generateCodeMap();
		worker.printCodeMap();
		//worker.updateLocationCapacity("core.berlin_locations_import");
		//worker.updateLocCodesInBosserhofMapingValues();
		//worker.storeCodeMap("core.global_location_codes");
		//worker.locations.clear();
		//worker.loadNexigaLocations("quesadillas.berlin_locations_import_25833");
		
		//worker.loadLocations("core.berlin_locations_import");
		//worker.addHouseHoldsForVisit("core.main_roehn_households","MID2008-Y2010", "core.main_roehn_taz");
		//worker.printLocations();
		//worker.emptyLocations("core.main_roehn_locations");
		//worker.storeLocations("core.main_roehn_locations",true,"core.main_roehn_taz_multiline",false,"");
		//worker.catMatchingCSVImport("H:\\restore\\infas\\cat_matching.csv", "core.global_bosserhof_ws_mapping");
	}

}
