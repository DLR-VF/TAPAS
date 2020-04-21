package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.loc.TPS_Coordinate;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.TPS_Geometrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class TPS_ExternalTrafficDistribution extends TPS_BasicConnectionClass {


    Map<Integer, TAZ> externalOrigins = new HashMap<>();
    Map<Integer, TAZ> internalDestinations = new HashMap<>();
    Map<Integer, Map<Integer, TAZ>> berlinDModel = new HashMap<>();
    Map<Integer, Double> dayDistributionStart = new HashMap<>();
    Map<Integer, Double> dayDistributionReturn = new HashMap<>();
    Map<Integer, Double> dayDistributionIntern = new HashMap<>();
    Map<String, ODPair> ODPairs = new HashMap<>();
    List<ODPair> ODPairsDayDistributed = new ArrayList<>();
    List<ODPair> ODPairsGravityDistributed = new ArrayList<>();
    int berlinZone = -1;

    Map<Integer, Integer> carTypeSumpType = new HashMap<>();
    List<Vehicle> carTypeDistribution = new ArrayList<>();
    Map<Integer, Integer> dModellZonen = new HashMap<>();

    public TPS_ExternalTrafficDistribution() {
        carTypeSumpType.put(0, -1);
        carTypeSumpType.put(2, 0);
        carTypeSumpType.put(17, 6);
        carTypeSumpType.put(20, 7);
        carTypeSumpType.put(23, 8);
        carTypeSumpType.put(26, 9);
        carTypeSumpType.put(29, 10);
        carTypeSumpType.put(32, 11);
        carTypeSumpType.put(33, 19);
        carTypeSumpType.put(34, 24);
        carTypeSumpType.put(35, 25);
        carTypeSumpType.put(36, 26);
        carTypeSumpType.put(37, 27);
        carTypeSumpType.put(38, 56);
    }

    public static void main(String[] args) {
        String post = ".mtx";
        TPS_ExternalTrafficDistribution worker = new TPS_ExternalTrafficDistribution();
        worker.loadCordonDistricts("core.berlin_taz_1223_umland", "quesadillas.zonierung_d_modell", 211000);
        worker.loadExternalPositions("quesadillas.zonierung_d_modell", "quesadillas.deutschland_modell_berlin_mapping");
        worker.loadTAPASDeutschlandmodellMapping(10, "core.berlin_locations_1223",
                "werkstatt.\"Berlin_im_D-Modell_zone_314767\"");
        for (Entry<Integer, Integer> e : worker.dModellZonen.entrySet()) {
            System.out.println(e.getKey() + " : " + e.getValue());
        }
        //worker.loadTAPASDeutschlandmodellMapping(10,"core.berlin_locations_ihk","werkstatt.\"Berlin_im_D-Modell_zone_314767\"");
        //set schönefeld as non-gravity
        worker.externalOrigins.get(1239).useGravity = false;
        String[][] scens = new String[3][2];
        int i = 0;
        scens[i][0] = "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2010_REF\\";
        scens[i][1] = "core.berlin_grundlast_2010_ref_1223";
        i++;
        scens[i][0] = "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2030_REF\\";
        scens[i][1] = "core.berlin_grundlast_2030_ref_1223";
        i++;
//		String[][] scens = new String[7][2];
//		int i=0;
//		scens[i][0]= "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2010_REF\\";
//		scens[i][1]= "core.berlin_grundlast_2010_ref4";
//		i++;
//		scens[i][0]= "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2030_REF\\";
//		scens[i][1]= "core.berlin_grundlast_2030_ref4";
//		i++;
        scens[i][0] = "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2040_REF\\";
        scens[i][1] = "core.berlin_grundlast_2040_ref_1223";
        i++;
//		scens[i][0]= "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2030_FS\\";
//		scens[i][1]= "core.berlin_grundlast_2030_fs4";
//		i++;
//		scens[i][0]= "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2040_FS\\";
//		scens[i][1]= "core.berlin_grundlast_2040_fs4";
//		i++;
//		scens[i][0]= "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2030_GR\\";
//		scens[i][1]= "core.berlin_grundlast_2030_gr4";
//		i++;
//		scens[i][0]= "T:\\Daten\\Aufbereitung\\Deutschlandmodell\\Schnittstelle_Berlin\\2040_GR\\";
//		scens[i][1]= "core.berlin_grundlast_2040_gr4";
        boolean createTable;
        for (i = 0; i < scens.length; ++i) {
            createTable = true;
            //ÖV
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillPTDistribution2010();
            worker.loadODPairs(scens[i][0] + "PT" + post, false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 5, false, createTable);
            createTable = false; //only first call creates the table

            //nahverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillCarDistribution2010();
            worker.loadODPairs(scens[i][0] + "Pkw_NV" + post, false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //fernverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillCarDistribution2010();
            worker.loadODPairs(scens[i][0] + "Pkw_FV" + post, false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //Personenwirtschaftsverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillTruck35Distribution2010();
            worker.loadODPairs(scens[i][0] + "PWV_PKW" + post, true);
            //worker.loadODPairs(scens[i][0]+"PWV_PKW_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //Leichte Nutzfahrzeuge
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillTruck35Distribution2010();
            worker.loadODPairs(scens[i][0] + "LNF" + post, true);
            //worker.loadODPairs(scens[i][0]+"LNF_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //LKW 3.5t bis 7.5t
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillTruck75Distribution2010();
            worker.loadODPairs(scens[i][0] + "L75" + post, true);
            //worker.loadODPairs(scens[i][0]+"L75_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //LKW 7.5t bis 12t
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillTruck12Distribution2010();
            worker.loadODPairs(scens[i][0] + "L12" + post, true);
            //worker.loadODPairs(scens[i][0]+"L12_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //Heavy Duty vehicles (Müllwagen, Betonmischer)
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillHDVDistribution2010();
            worker.loadODPairs(scens[i][0] + "HDV" + post, true);
            //worker.loadODPairs(scens[i][0]+"HDV_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //LKW >12t incl Sattelzugmaschienen
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillTrailerDistribution2010();
            worker.loadODPairs(scens[i][0] + "SZM" + post, true);
            //worker.loadODPairs(scens[i][0]+"SZM_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

//			//Reisebusse
//			worker.clearTrafficDistribution();
//			worker.loadDayDistribution();
//			worker.fillBusDistribution2010();
//			worker.loadODPairs(scens[i][0]+"RB_FV_2017327"+post,true);
//			worker.distributeOverDay();
//			worker.distributeOverLocations(0.8);
//			worker.saveToDB(scens[i][1], 2, false, createTable);
        }


    }

    public void loadExternalPositions(String tableName, String mappingTable) {
        StringBuilder query = new StringBuilder();
        try {
            int id, kreis;
            double sLon, sLat, dLon, dLat;
            int[] zonen;
            Map<Integer, DModellKreisMapping> KreisMap = new HashMap<>();

            //load the mapping
            query = new StringBuilder(
                    "SELECT id,source_lat,source_lon,destination_lat,destination_lon,mappings from " + mappingTable);
            ResultSet rs = this.dbCon.executeQuery(query.toString(), this);
            while (rs.next()) {
                id = rs.getInt("id");
                sLat = rs.getDouble("source_lat");
                sLon = rs.getDouble("source_lon");
                dLat = rs.getDouble("destination_lat");
                dLon = rs.getDouble("destination_lon");
                zonen = new int[0];
                Object array = rs.getArray("mappings").getArray();
                if (array instanceof int[]) {
                    zonen = (int[]) array;
                } else if (array instanceof Integer[]) {
                    Integer[] IArray = (Integer[]) array;
                    zonen = new int[IArray.length];
                    for (int i = 0; i < IArray.length; i++) {
                        zonen[i] = IArray[i];
                    }
                } else {
                    System.err.println("Cannot cast to int array");
                }
                for (int j : zonen) {
                    if (id != 0) {
                        dModellZonen.put(j, id);

                        //create point informations
                        TAZ entry = new TAZ();
                        //now convert this id to a zone of the above mapping
                        entry.id = id;
                        entry.taz_id = entry.id;
                        entry.coordSource.setValues(sLon, sLat);
                        entry.coordDestination.setValues(dLon, dLat);
                        entry.cappa = 0;
                        this.externalOrigins.put(entry.id, entry);
                        this.internalDestinations.put(entry.id, entry);
                    } else {
                        dModellZonen.put(j,
                                -j); //the 0-zone is a funny thing: it contains the taz WITHIN the A10 in Berlin
                        //we will load the additional information later
                    }
                }
            }
            rs.close();

            Set<Integer> idsToLookFor = new TreeSet<>();
            for (Integer v : dModellZonen.values()) {
                if (v < 0) {
                    idsToLookFor.add(-v);
                }
            }

            //now load the included cells
            query = new StringBuilder(
                    "select vbz_6561, vbz_412, " + "st_X(st_transform(st_centroid(the_geom),4326)) as lon, " +
                            "st_Y(st_transform(st_centroid(the_geom),4326)) as lat " + "from	" + tableName +
                            " as p " + "where vbz_6561 = any(ARRAY[");
            for (Integer v : idsToLookFor) {
                query.append(v).append("\n,");
            }
            query = new StringBuilder(query.substring(0, query.length() - 1) + "])");

            rs = this.dbCon.executeQuery(query.toString(), this);
            while (rs.next()) {
                id = rs.getInt("vbz_6561");
                sLon = rs.getDouble("lon");
                sLat = rs.getDouble("lat");
                TAZ entry = new TAZ();
                // now convert this id to a zone of the above mapping
                if (dModellZonen.containsKey(id)) {
                    entry.id = dModellZonen.get(id);
                    entry.taz_id = entry.id;
                } else {
                    System.err.println("ARG!");
                    entry.id = id;
                    entry.taz_id = -id;
                }
                entry.coordSource.setValues(sLon, sLat);
                entry.coordDestination.setValues(sLon, sLat);
                entry.cappa = 0;
                this.externalOrigins.put(entry.id, entry);
                this.internalDestinations.put(entry.id, entry);

            }

            System.out.println("found " + this.externalOrigins.size() + " origin entires");
            rs.close();
            //now find the kreis mapping
            query = new StringBuilder("select vbz_6561, vbz_412 " + "from	" + tableName + " as p ");

            rs = this.dbCon.executeQuery(query.toString(), this);
            while (rs.next()) {

                id = rs.getInt("vbz_6561");
                kreis = rs.getInt("vbz_412");
                if (dModellZonen.containsKey(id)) {
                    //find the mapping entry
                    TAZ entry = this.externalOrigins.get(dModellZonen.get(id));
                    if (entry != null) {

                        //now check the "Kreis mapping"
                        DModellKreisMapping tmp = KreisMap.get(kreis);
                        if (tmp == null) { //first time this kreis occures
                            tmp = new DModellKreisMapping();
                            tmp.d_vbz_412 = kreis;
                            DVZMappingElement tmpKreis = new DVZMappingElement();
                            tmpKreis.externalOrigin = entry.id;
                            tmpKreis.count = 1;
                            tmp.vzSet.put(entry.id, tmpKreis);
                            KreisMap.put(kreis, tmp);
                        } else {
                            DVZMappingElement tmpKreis = tmp.vzSet.get(entry.id);
                            if (tmpKreis == null) { //first time this external origin occures
                                tmpKreis = new DVZMappingElement();
                                tmpKreis.externalOrigin = entry.id;
                                tmpKreis.count = 1;
                                tmp.vzSet.put(entry.id, tmpKreis);
                            } else { //just update the count
                                tmpKreis.count += 1;
                            }
                        }
                    }
                } else { //we found berlin!
                    berlinZone = kreis;
                }
            }

            rs.close();

            for (DModellKreisMapping e : KreisMap.values()) {
                DVZMappingElement best = null;
                for (DVZMappingElement tmp : e.vzSet.values()) {
                    if (best == null || best.count < tmp.count) {
                        best = tmp;
                    }
                }
                // now i have the best mapping value
                dModellZonen.put(e.d_vbz_412, best.externalOrigin);
            }
            System.out.println("Mapped " + KreisMap.size() + " district values to peripheral districts");


        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void loadCordonDistricts(String tableName, String dmod, int berlinId) {
        String query = "";
        try {
            int id, taz;
            double sLon, sLat, dLon, dLat;

            //load the external mapping
            query = "SELECT z.tapas_taz_id,z.vbz_no, vbz_6561, z.nuts_id ISNULL as isberlin, " +
                    "st_X(st_transform(st_centroid(z.the_geom),4326)) as lon, " +
                    "st_Y(st_transform(st_centroid(z.the_geom),4326)) as lat " + "  from " + tableName + " as z join " +
                    dmod + " as d on z.vbz_no=d.ver_nr";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                if (!rs.getBoolean("isberlin")) {
                    id = (int) (rs.getDouble("vbz_6561") + 0.1);
                    sLat = rs.getDouble("lat");
                    sLon = rs.getDouble("lon");
                    dLat = rs.getDouble("lat");
                    dLon = rs.getDouble("lon");
                    taz = rs.getInt("tapas_taz_id");
                    dModellZonen.put(id, taz);

                    //create point informations
                    TAZ entry = new TAZ();
                    //now convert this id to a zone of the above mapping
                    entry.id = taz;
                    entry.taz_id = taz;
                    entry.coordSource.setValues(sLon, sLat);
                    entry.coordDestination.setValues(dLon, dLat);
                    entry.cappa = 0;
                    this.externalOrigins.put(entry.id, entry);
                    this.internalDestinations.put(entry.id, entry);
                }
            }
            rs.close();


            System.out.println("found " + this.externalOrigins.size() + " origin entires");
            berlinZone = berlinId;


        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void loadTAPASDeutschlandmodellMapping(int minCapacity, String locName, String zonierungsName) {

        String query =
                "with l as ( select loc_id, loc_taz_id, loc_capacity, loc_coordinate, st_transform(loc_coordinate,31467) as trans from " +
                        locName + " where loc_code = 1001000000 and loc_capacity>" + minCapacity + ") " +
                        "select \"NO\" as id, " + "loc_id, " + "loc_taz_id, " + "loc_capacity, " +
                        "st_X(loc_coordinate) as lon, " + "st_Y(loc_coordinate) as lat " + "from	l " + "join " +
                        zonierungsName + " as b " + "on within(trans,b.the_geom)";
        try {
            ResultSet rs = this.dbCon.executeQuery(query, this);
            int id, count = 0;
            double lon, lat;
            Map<Integer, TAZ> globalList = new HashMap<>();
            while (rs.next()) {
                id = rs.getInt("id");
                lon = rs.getDouble("lon");
                lat = rs.getDouble("lat");

                TAZ entry = new TAZ();
                entry.id = rs.getInt("loc_id");
                entry.taz_id = rs.getInt("loc_taz_id");
                entry.coordSource.setValues(lon, lat);
                entry.coordDestination.setValues(lon, lat);
                entry.cappa = rs.getInt("loc_capacity");
                Map<Integer, TAZ> list = this.berlinDModel.get(id);
                if (list == null) {
                    list = new HashMap<>();
                }
                list.put(entry.id, entry);
                globalList.put(entry.id, entry);
                this.internalDestinations.put(entry.id, entry);
                this.berlinDModel.put(id, list);
                count++;
            }
            rs.close();
            this.berlinDModel.put(this.berlinZone, globalList);

            System.out.println(
                    "found " + this.berlinDModel.size() + " destination entires which are mapped to " + count +
                            " work locations");
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void loadODPairs(String fileName, boolean useInternalTraffic) {
        double inboundVol = 0, outboundVol = 0;
        int numRelations = 0;
        String key;
        ODPair pair;

        FileReader in = null;
        BufferedReader input = null;
        String line;
        int i, fromTVZ, toTVZ, berlinInBound, berlinOutBound;
        double volume;
        boolean stringChanged;
        String[] delimiter = {" ", "\t", ";"}; //It try and I try and I try! I can't get no  ... satisfaction!
        String line2;
        int totalVolume = 0;
        String[] tok;
        try {
            for (i = 0; i < delimiter.length && totalVolume == 0; ++i) {
                in = new FileReader(fileName);
                input = new BufferedReader(in);
                while ((line = input.readLine()) != null) {
                    if (line.startsWith("$") || line.startsWith("*") || line.contains("\"")) { // comment or cell name
                        continue;
                    }
                    stringChanged = true;
                    while (stringChanged && line.length() > 0) {
                        line2 = line.trim().replaceAll("  ", " ");
                        if (line2.length() == line.length()) {
                            stringChanged = false;
                        }
                        line = line2;
                    }


                    tok = line.trim().split(delimiter[i]);
                    //check format
                    if (tok.length != 3) continue;

                    //get from
                    fromTVZ = Integer.parseInt(tok[0].trim());
                    //get to
                    toTVZ = Integer.parseInt(tok[1].trim());
                    //				if(toTVZ==23565)
                    //					System.out.println("Hit berlin!");
                    //get volume
                    volume = Double.parseDouble(tok[2].trim());
                    totalVolume += volume;
                    //todo: import kreis-zell maping

                    //filter unused relations: exactly one cell must be berlin bound!
                    berlinOutBound = this.berlinDModel.containsKey(fromTVZ) ? 1 : 0;
                    berlinInBound = this.berlinDModel.containsKey(toTVZ) ? 1 : 0;

                    if (berlinInBound == 0 && berlinOutBound == 0) {
                        continue;
                    }

                    if (berlinInBound == 1 && berlinOutBound == 1 && !useInternalTraffic) {
                        continue;
                    }


                    //map the tvz to a "boundary cell"
                    if (berlinInBound != berlinOutBound) {
                        if (berlinInBound > 0) {
                            if (dModellZonen.containsKey(fromTVZ)) {
                                fromTVZ = dModellZonen.get(fromTVZ); //get the mapping

                            } else {
                                //System.err.println("Cannot find mapping for inbound (o) taz: "+fromTVZ);
                                continue;
                            }
                        }

                        if (berlinOutBound > 0) {
                            if (dModellZonen.containsKey(toTVZ)) {
                                toTVZ = dModellZonen.get(toTVZ); //get the mapping
                            } else {
                                //System.err.println("Cannot find mapping for outbound (d) taz: "+toTVZ);
                                continue;
                            }
                        }
                    }

                    //				if(fromTVZ == -111209727 || toTVZ == -111209727){
                    //					System.out.println("Hit wandlitz!");
                    //				}

                    key = fromTVZ + "-" + toTVZ;
                    //find existing relation and add the volume if found!
                    pair = this.ODPairs.get(key);
                    if (pair != null) {
                        pair.volume += volume;
                    } else {
                        //add the new relation!
                        pair = new ODPair();
                        pair.idOrigin = fromTVZ;
                        pair.idDestination = toTVZ;
                        pair.volume = volume;
                        pair.inBound = berlinInBound > 0;
                        pair.internalTraffic = berlinInBound > 0 && berlinOutBound > 0;
                        this.ODPairs.put(key, pair);
                        numRelations++;
                    }
                    if (pair.inBound) inboundVol += volume;
                    else outboundVol += volume;

                }
                //input.close();
                in.close();
            }

        } catch (IOException | NumberFormatException e) {
            System.err.println(" Could not close : " + fileName);
            e.printStackTrace();
        } finally {
            try {
                if (input != null) input.close();
                if (in != null) in.close();
            }//try
            catch (IOException e) {
                System.err.println(" Could not close : " + fileName);
                e.printStackTrace();
            }//catch
        }//finally
        String fileNameShort = (new File(fileName).getName());

        System.out.println(
                fileNameShort + ": Loaded " + numRelations + " od-relations. Total inbound volume: " + inboundVol +
                        " total outbound volume: " + outboundVol);


//		//currently I have no data so lets put some dummy data!
//
//		for(Entry<Integer, TAZ> o: this.externalOrigins.entrySet()){
//			for(Entry<Integer, Map<Integer, TAZ>> d: this.berlinDModel.entrySet()){
//				//there...
//				ODPair pair = new ODPair();
//				pair.idOrigin= o.getKey();
//				pair.idDestination = d.getKey();
//				pair.volume =500;
//				pair.inBound = true;
//				this.ODPairs.add(pair);
//				//..and back again
//				pair = new ODPair();
//				pair.idOrigin= d.getKey();
//				pair.idDestination = o.getKey();
//				pair.volume =500;
//				pair.inBound = false;
//				this.ODPairs.add(pair);
//			}
//		}
    }

    public void distributeOverDay() {
        //go through the OD-List
        for (ODPair p : this.ODPairs.values()) {

            //check if the origin is outside berlin -> distribute according to start list
            if (p.inBound) {
                for (Entry<Integer, Double> e : this.dayDistributionStart.entrySet()) {
                    ODPair pair = new ODPair();
                    pair.idOrigin = p.idOrigin;
                    pair.idDestination = p.idDestination;
                    pair.inBound = p.inBound;
                    pair.internalTraffic = p.internalTraffic;
                    pair.hourOfDay = e.getKey();
                    pair.volume = p.volume * e.getValue();
                    this.ODPairsDayDistributed.add(pair);
                }
            } else if (p.internalTraffic) {
                for (Entry<Integer, Double> e : this.dayDistributionIntern.entrySet()) {
                    ODPair pair = new ODPair();
                    pair.idOrigin = p.idOrigin;
                    pair.idDestination = p.idDestination;
                    pair.inBound = p.inBound;
                    pair.internalTraffic = p.internalTraffic;
                    pair.hourOfDay = e.getKey();
                    pair.volume = p.volume * e.getValue();
                    this.ODPairsDayDistributed.add(pair);
                }
            } else {
                //check if the origin is inside berlin -> distribute according to return list
                for (Entry<Integer, Double> e : this.dayDistributionReturn.entrySet()) {
                    ODPair pair = new ODPair();
                    pair.idOrigin = p.idOrigin;
                    pair.idDestination = p.idDestination;
                    pair.inBound = p.inBound;
                    pair.internalTraffic = p.internalTraffic;
                    pair.hourOfDay = e.getKey();
                    pair.volume = p.volume * e.getValue();
                    this.ODPairsDayDistributed.add(pair);
                }
            }
        }
    }

    public void loadDayDistribution() {
//
//		/*
//		 * This Method "creates" Static valued derived from the MiD:
//		 * The weighted shares of all Trips in Berlin starting home and ending at home
//		 * Since I cannot filter for commuter trips this must be sufficient!
//		 * DB: perseus.mid2008.wege
//		 * Filter: Bland=11
//		 * Stichtag = {2,3,4} (DiMiDo)
//		 * Anteil gewichtete Anzahl der Wege gruppiert nach Stunde Startzeit
//		 * Start: Startpunkt Zu Hause
//		 * Return: Zielpunkt: Zu Hause
//		 */
//		this.dayDistributionStart.put( 0, 	0.0);
//		this.dayDistributionStart.put( 1, 	0.0);
//		this.dayDistributionStart.put( 2, 	0.0);
//		this.dayDistributionStart.put( 3, 	0.0);
//		this.dayDistributionStart.put( 4, 	0.016319554);
//		this.dayDistributionStart.put( 5, 	0.039152948);
//		this.dayDistributionStart.put( 6, 	0.063814429);
//		this.dayDistributionStart.put( 7, 	0.253985158);
//		this.dayDistributionStart.put( 8, 	0.169123629);
//		this.dayDistributionStart.put( 9, 	0.127186955);
//		this.dayDistributionStart.put(10, 	0.097285095);
//		this.dayDistributionStart.put(11, 	0.067713099);
//		this.dayDistributionStart.put(12, 	0.04052658);
//		this.dayDistributionStart.put(13, 	0.026566243);
//		this.dayDistributionStart.put(14, 	0.037838191);
//		this.dayDistributionStart.put(15, 	0.016196232);
//		this.dayDistributionStart.put(16, 	0.010166324);
//		this.dayDistributionStart.put(17, 	0.019645697);
//		this.dayDistributionStart.put(18, 	0.002431023);
//		this.dayDistributionStart.put(19, 	0.007391158);
//		this.dayDistributionStart.put(20, 	0.00224679);
//		this.dayDistributionStart.put(21, 	0.002410896);
//		this.dayDistributionStart.put(22, 	0.0);
//		this.dayDistributionStart.put(23, 	0.0);
//
//		this.dayDistributionReturn.put( 0, 	0.007975497);
//		this.dayDistributionReturn.put( 1, 	0.0);
//		this.dayDistributionReturn.put( 2, 	0.006796908);
//		this.dayDistributionReturn.put( 3, 	0.005046985);
//		this.dayDistributionReturn.put( 4, 	0.0);
//		this.dayDistributionReturn.put( 5, 	0.001412515);
//		this.dayDistributionReturn.put( 6, 	0.002733216);
//		this.dayDistributionReturn.put( 7, 	0.004537778);
//		this.dayDistributionReturn.put( 8, 	0.011349728);
//		this.dayDistributionReturn.put( 9, 	0.024948635);
//		this.dayDistributionReturn.put(10, 	0.0351713);
//		this.dayDistributionReturn.put(11, 	0.038384415);
//		this.dayDistributionReturn.put(12, 	0.058681634);
//		this.dayDistributionReturn.put(13, 	0.054708503);
//		this.dayDistributionReturn.put(14, 	0.053853918);
//		this.dayDistributionReturn.put(15, 	0.078192265);
//		this.dayDistributionReturn.put(16, 	0.151311135);
//		this.dayDistributionReturn.put(17, 	0.110716488);
//		this.dayDistributionReturn.put(18, 	0.119048915);
//		this.dayDistributionReturn.put(19, 	0.130643535);
//		this.dayDistributionReturn.put(20, 	0.02524086);
//		this.dayDistributionReturn.put(21, 	0.017362978);
//		this.dayDistributionReturn.put(22, 	0.047978302);
//		this.dayDistributionReturn.put(23, 	0.013904491);


        /*
         * This Method "creates" Static valued derived from the MiD:
         * The weighted shares of all Trips in Berlin starting home and ending at home
         * Since I cannot filter for commuter trips this must be sufficient!
         * DB: perseus.mid2008.wege
         * Filter: Bland=11
         * Stichtag = {2,3,4} (DiMiDo)
         * Anteil gewichtete Anzahl der Wege gruppiert nach Stunde Startzeit
         * Start: Startpunkt Zu Hause
         * Return: Zielpunkt: Zu Hause
         */
        this.dayDistributionStart.put(0, 0.001538715);
        this.dayDistributionStart.put(1, 0.000714403);
        this.dayDistributionStart.put(2, 0.000659449);
        this.dayDistributionStart.put(3, 0.000851789);
        this.dayDistributionStart.put(4, 0.003517063);
        this.dayDistributionStart.put(5, 0.018464582);
        this.dayDistributionStart.put(6, 0.043523658);
        this.dayDistributionStart.put(7, 0.07561686);
        this.dayDistributionStart.put(8, 0.057811727);
        this.dayDistributionStart.put(9, 0.057536957);
        this.dayDistributionStart.put(10, 0.061658515);
        this.dayDistributionStart.put(11, 0.057646865);
        this.dayDistributionStart.put(12, 0.061548607);
        this.dayDistributionStart.put(13, 0.058004067);
        this.dayDistributionStart.put(14, 0.064653514);
        this.dayDistributionStart.put(15, 0.074462824);
        this.dayDistributionStart.put(16, 0.089135572);
        this.dayDistributionStart.put(17, 0.085618509);
        this.dayDistributionStart.put(18, 0.070533604);
        this.dayDistributionStart.put(19, 0.049348794);
        this.dayDistributionStart.put(20, 0.026405451);
        this.dayDistributionStart.put(21, 0.01923394);
        this.dayDistributionStart.put(22, 0.015634445);
        this.dayDistributionStart.put(23, 0.00588009);

        this.dayDistributionReturn.put(0, 0.001538715);
        this.dayDistributionReturn.put(1, 0.000714403);
        this.dayDistributionReturn.put(2, 0.000659449);
        this.dayDistributionReturn.put(3, 0.000851789);
        this.dayDistributionReturn.put(4, 0.003517063);
        this.dayDistributionReturn.put(5, 0.018464582);
        this.dayDistributionReturn.put(6, 0.043523658);
        this.dayDistributionReturn.put(7, 0.07561686);
        this.dayDistributionReturn.put(8, 0.057811727);
        this.dayDistributionReturn.put(9, 0.057536957);
        this.dayDistributionReturn.put(10, 0.061658515);
        this.dayDistributionReturn.put(11, 0.057646865);
        this.dayDistributionReturn.put(12, 0.061548607);
        this.dayDistributionReturn.put(13, 0.058004067);
        this.dayDistributionReturn.put(14, 0.064653514);
        this.dayDistributionReturn.put(15, 0.074462824);
        this.dayDistributionReturn.put(16, 0.089135572);
        this.dayDistributionReturn.put(17, 0.085618509);
        this.dayDistributionReturn.put(18, 0.070533604);
        this.dayDistributionReturn.put(19, 0.049348794);
        this.dayDistributionReturn.put(20, 0.026405451);
        this.dayDistributionReturn.put(21, 0.01923394);
        this.dayDistributionReturn.put(22, 0.015634445);
        this.dayDistributionReturn.put(23, 0.00588009);

        this.dayDistributionIntern.put(0, 0.001538715);
        this.dayDistributionIntern.put(1, 0.000714403);
        this.dayDistributionIntern.put(2, 0.000659449);
        this.dayDistributionIntern.put(3, 0.000851789);
        this.dayDistributionIntern.put(4, 0.003517063);
        this.dayDistributionIntern.put(5, 0.018464582);
        this.dayDistributionIntern.put(6, 0.043523658);
        this.dayDistributionIntern.put(7, 0.07561686);
        this.dayDistributionIntern.put(8, 0.057811727);
        this.dayDistributionIntern.put(9, 0.057536957);
        this.dayDistributionIntern.put(10, 0.061658515);
        this.dayDistributionIntern.put(11, 0.057646865);
        this.dayDistributionIntern.put(12, 0.061548607);
        this.dayDistributionIntern.put(13, 0.058004067);
        this.dayDistributionIntern.put(14, 0.064653514);
        this.dayDistributionIntern.put(15, 0.074462824);
        this.dayDistributionIntern.put(16, 0.089135572);
        this.dayDistributionIntern.put(17, 0.085618509);
        this.dayDistributionIntern.put(18, 0.070533604);
        this.dayDistributionIntern.put(19, 0.049348794);
        this.dayDistributionIntern.put(20, 0.026405451);
        this.dayDistributionIntern.put(21, 0.01923394);
        this.dayDistributionIntern.put(22, 0.015634445);
        this.dayDistributionIntern.put(23, 0.00588009);
    }

    public void distributeOverLocations(double calib) {
        double way = 0;
        Map<Integer, Integer> timeDistCheck = new HashMap<>();
        for (int i = 0; i < 24; ++i)
            timeDistCheck.put(i, 0);
        TAZ o;
        Map<Integer, TAZ> dSet, dSet2;
        for (ODPair p : this.ODPairsDayDistributed) {
            if (!p.internalTraffic) {
                //first calculate the weight of the locations
                //it doesnt matter if o and d are exchanged: "gravity" is symmetric
                if (p.inBound) {
                    o = this.externalOrigins.get(p.idOrigin);
                    dSet = this.berlinDModel.get(p.idDestination);
                } else {
                    o = this.externalOrigins.get(p.idDestination);
                    dSet = this.berlinDModel.get(p.idOrigin);
                }
                double sum = 0;
                for (TAZ d : dSet.values()) {
                    if (o.useGravity) d.tempWeight = d.cappa * Math.pow(TPS_Geometrics
                            .getDistance(d.coordDestination.getValue(0), d.coordDestination.getValue(1),
                                    o.coordSource.getValue(0), o.coordSource.getValue(1)), -calib);
                    else d.tempWeight = d.cappa;
                    sum += d.tempWeight;
                }
                //normalize
                sum = 1.0 / sum;
                for (TAZ d : dSet.values()) {
                    d.tempWeight *= sum;
                }

                //now distribute the volume
                int totalVolume = 0;
                for (TAZ d : dSet.values()) {

                    way += d.tempWeight * p.volume;
                    if (way >= 1.0) {
                        //we have some trips!
                        ODPair distributedPair = new ODPair();
                        distributedPair.inBound = p.inBound;
                        distributedPair.hourOfDay = p.hourOfDay;
                        if (p.inBound) {
                            distributedPair.idOrigin = p.idOrigin;
                            distributedPair.idDestination = d.id;
                        } else {
                            distributedPair.idOrigin = d.id;
                            distributedPair.idDestination = p.idDestination;

                        }

                        distributedPair.volume = (int) way; // this is the integer-part
                        totalVolume += distributedPair.volume;
                        way -= distributedPair.volume; // this is the remainer
                        this.ODPairsGravityDistributed.add(distributedPair);
                    }
                }
                timeDistCheck.put(p.hourOfDay, timeDistCheck.get(p.hourOfDay) + totalVolume);
                //System.out.println("Pair o:"+p.idOrigin+" d:"+p.idDestination+" h:"+p.hourOfDay+" v:"+p.volume+" distributed:"+totalVolume);
            } else {
                //first calculate the weight of the locations
                //just the capacity for internal traffics!
                dSet = this.berlinDModel.get(p.idOrigin);
                dSet2 = this.berlinDModel.get(p.idDestination);
                double sum = 0;
                for (TAZ d : dSet.values()) {
                    d.tempWeight = d.cappa;
                    sum += d.tempWeight;
                }
                //normalize
                sum = 1.0 / sum;
                for (TAZ d : dSet.values()) {
                    d.tempWeight *= sum;
                }

                //now distribute the volume
                int totalVolume = 0;
                for (TAZ oSet : dSet.values()) {
                    if (way + oSet.tempWeight * p.volume > 1.0) {
                        for (TAZ d : dSet2.values()) {

                            way += d.tempWeight * oSet.tempWeight * p.volume;
                            if (way >= 1.0 && oSet.id != d.id) {
                                //we have some trips!
                                ODPair distributedPair = new ODPair();
                                distributedPair.inBound = p.inBound;
                                distributedPair.internalTraffic = p.internalTraffic;
                                distributedPair.hourOfDay = p.hourOfDay;
                                distributedPair.idOrigin = oSet.id;
                                distributedPair.idDestination = d.id;
                                distributedPair.volume = (int) way; // this is the integer-part
                                totalVolume += distributedPair.volume;
                                way -= distributedPair.volume; // this is the remainer
                                this.ODPairsGravityDistributed.add(distributedPair);
                            }
                        }
                    } else {
                        way += oSet.tempWeight * p.volume;
                    }
                    timeDistCheck.put(p.hourOfDay, timeDistCheck.get(p.hourOfDay) + totalVolume);
                    //System.out.println("Pair o:"+p.idOrigin+" d:"+p.idDestination+" h:"+p.hourOfDay+" v:"+p.volume+" distributed:"+totalVolume);
                }
            }
        }
//		for(int i=0; i<24;++i){
//			System.out.println("Hour "+i+" volume: "+timeDistCheck.get(i));
//		}
    }

    public void clearTrafficDistribution() {
        this.carTypeDistribution.clear();
        this.dayDistributionStart.clear();
        this.dayDistributionReturn.clear();
        this.dayDistributionIntern.clear();
        this.ODPairsGravityDistributed.clear();
        this.ODPairsDayDistributed.clear();
        this.ODPairs.clear();
    }

    public void saveToDB(String tablename, int modeNumber, boolean is_resticted, boolean createTable) {
        String query = "";
        try {
            if (createTable) {
                query = "DROP TABLE IF EXISTS " + tablename;
                this.dbCon.execute(query, this);

                String[] tokens = tablename.split("\\.");
                query = "CREATE TABLE " + tablename + " (" + "  p_id integer NOT NULL," + "  hh_id integer NOT NULL," +
                        "  taz_id_start integer," + "  loc_id_start integer," + "  lon_start double precision," +
                        "  lat_start double precision," + "  taz_id_end integer," + "  loc_id_end integer," +
                        "  lon_end double precision," + "  lat_end double precision," +
                        "  start_time_min integer NOT NULL," + "  travel_time_sec double precision," +
                        "  mode integer," + "  car_type integer," + "  activity_duration_min integer," +
                        "  sumo_type integer," + "  is_restricted boolean," + "  CONSTRAINT " +
                        tokens[tokens.length - 1] + "_pkey PRIMARY KEY (p_id, hh_id, start_time_min)" +
                        "  USING INDEX TABLESPACE index)" + "WITH (  OIDS=FALSE);";
                this.dbCon.execute(query, this);
            }
            int num = -1;

            //get the right number
            query = "SELECT count(*) as c, MIN(p_id) as min from " + tablename;
            ResultSet rs = this.dbCon.executeQuery(query, this);
            if (rs.next()) {
                if (rs.getInt("c") > 0) {
                    num += rs.getInt("min");
                }
            }
            rs.close();


            query = "INSERT INTO " + tablename + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?," + modeNumber + ",?,1,?," +
                    is_resticted + ")";

            PreparedStatement pS = this.dbCon.getConnection(this).prepareStatement(query);
            int count = 0, maxBatch = 10000;
            for (ODPair p : this.ODPairsGravityDistributed) {
                if (Math.abs(p.idOrigin) == 20957 || Math.abs(p.idDestination) == 20957) {
                    p.idOrigin = p.idOrigin; //TODO
                }
                TAZ o = this.internalDestinations.get(p.idOrigin);
                TAZ d = this.internalDestinations.get(p.idDestination);
                int start = p.hourOfDay * 60 + (int) (Math.random() * 60.0);
                double time = TPS_Geometrics.getDistance(d.coordDestination.getValue(0), d.coordDestination.getValue(1),
                        o.coordSource.getValue(0), o.coordSource.getValue(1));
                time *= 1.4 / 7.7777; //1.4 ist der umwegefaktor, 28kmh= 7.7777m/s;
                Vehicle tmp;
                double vID, sum;
                int v;
                for (int i = 0; i < p.volume; ++i) {
                    // get the cartype
                    vID = Math.random();
                    sum = this.carTypeDistribution.get(0).weight;
                    v = 0;
                    do {
                        if (sum >= vID) {
                            break;
                        } else {
                            v++;
                            sum += this.carTypeDistribution.get(v).weight;
                        }
                    } while (v < this.carTypeDistribution.size() - 1);
                    tmp = this.carTypeDistribution.get(v);

                    pS.setInt(1, num);//p_id
                    pS.setInt(2, num);//hh_id
                    pS.setInt(3, o.taz_id); //taz_id_start
                    pS.setInt(4, o.id); //loc_id_start
                    pS.setDouble(5, o.coordSource.getValue(0)); //lon_start
                    pS.setDouble(6, o.coordSource.getValue(1)); //lat_start
                    pS.setInt(7, d.taz_id); //taz_id_end
                    pS.setInt(8, d.id); //loc_id_end
                    pS.setDouble(9, d.coordDestination.getValue(0)); //lon_end
                    pS.setDouble(10, d.coordDestination.getValue(1)); //lat_end
                    pS.setInt(11, start); // start_time_min
                    pS.setDouble(12, time); //travel_time_sec
                    pS.setInt(13, tmp.id);
                    pS.setInt(14, tmp.getSumoVehicle());
                    pS.addBatch();
                    count++;
                    if (count >= maxBatch) {
                        pS.executeBatch();
                        count = 0;
                    }
                    num--;
                }

            }
            if (count > 0) {
                pS.executeBatch();
            }

        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
            e.getNextException().printStackTrace();
        }

    }

    public void fillCarDistribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(2, 0.3);
        this.carTypeDistribution.add(tmp);
        tmp = new Vehicle(17, 0.5);
        this.carTypeDistribution.add(tmp);
        tmp = new Vehicle(20, 0.19);
        this.carTypeDistribution.add(tmp);
        tmp = new Vehicle(23, 0.01);
        this.carTypeDistribution.add(tmp);
    }

    public void fillPTDistribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(0, 1);
        this.carTypeDistribution.add(tmp);
    }

    public void fillTruck35Distribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(33, 1.0);
        this.carTypeDistribution.add(tmp);
    }

    public void fillTruck75Distribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(34, 1.0);
        this.carTypeDistribution.add(tmp);
    }

    public void fillTruck12Distribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(35, 1.0);
        this.carTypeDistribution.add(tmp);
    }

    public void fillHDVDistribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(36, 1.0);
        this.carTypeDistribution.add(tmp);
    }

    public void fillTrailerDistribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(37, 1.0);
        this.carTypeDistribution.add(tmp);
    }

    public void fillBusDistribution2010() {
        Vehicle tmp;
        tmp = new Vehicle(38, 1.0);
        this.carTypeDistribution.add(tmp);
    }

    class Vehicle {
        int id;
        boolean isRestricted = false;
        double weight;
        Vehicle(int id, double weight) {
            this.id = id;
            this.weight = weight;

        }

        int getSumoVehicle() {
            return TPS_ExternalTrafficDistribution.this.carTypeSumpType.get(id);
        }

    }

    class ODPair {
        int idOrigin = -1;
        int idDestination = -1;
        int hourOfDay = -1;
        boolean inBound = true;
        boolean internalTraffic = false;
        double volume = 0;
    }

    class TAZ {
        int id = -1;
        int taz_id = -1;
        double cappa = 0;
        double tempWeight = 0;
        boolean isRestricted = false;
        boolean useGravity = true;
        TPS_Coordinate coordSource = new TPS_Coordinate(0, 0);
        TPS_Coordinate coordDestination = new TPS_Coordinate(0, 0);
    }

    class DModellKreisMapping {
        int d_vbz_412 = -1;
        Map<Integer, DVZMappingElement> vzSet = new HashMap<>();

    }

    class DVZMappingElement implements Comparable<DVZMappingElement> {
        int externalOrigin = -1;
        int count = 0;


        public int compareTo(DVZMappingElement arg0) {
            return this.count - arg0.count;
        }
    }

}
