package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.model.TPS_Geometrics;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/*this class is very similar to TPS_ExternalTrafficDistribution

 */
public class TPS_ExternalTrafficDistributionKoFiF{

    enum TrafficType{INBOUND,OUTBOUND,INTERNAL,DRIVETHROUGH, UNKNOWN};

    class LocationOption{
        String id;
        int cappa,taz;
        double x,y, weight;
    }

    class ExternalCell{
        String id;
        double[] x = new double[2]; //0 = from, 1= to
        double[] y = new double[2]; //0 = from, 1= to
    }

    class ODPair {
        String idOrigin = "-1";
        String idDestination = "-1";
        int hourOfDay = -1;
        TrafficType type = TrafficType.UNKNOWN;
        double volume = 0;
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
            int id = -1;
            if(TPS_ExternalTrafficDistributionKoFiF.this.carTypeSumpType.containsKey(id))
                id = TPS_ExternalTrafficDistributionKoFiF.this.carTypeSumpType.get(id);
            return id;
        }

    }

    Map<String, List<LocationOption>> external2TapasLoc = new HashMap<>();
    Map<String,ExternalCell> externalLoc = new HashMap<>();

    Map<Integer, Double> dayDistributionStart = new HashMap<>();
    Map<Integer, Double> dayDistributionReturn = new HashMap<>();
    Map<Integer, Double> dayDistributionIntern = new HashMap<>();
    Map<String, ODPair> ODPairs = new HashMap<>();
    List<ODPair> ODPairsDayDistributed = new ArrayList<>();
    List<ODPair> ODPairsGravityDistributed = new ArrayList<>();
    Map<Integer, Integer> carTypeSumpType = new HashMap<>();
    List<Vehicle> carTypeDistribution = new ArrayList<>();
    Map<String,LocationOption> internalLocs = new HashMap<>();

    public TPS_ExternalTrafficDistributionKoFiF() {
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

    public static void main(String[] args) {
        TPS_ExternalTrafficDistributionKoFiF worker = new TPS_ExternalTrafficDistributionKoFiF();
        worker.loadCordonDistrictsKoFIF("werkstatt.kofif_visum_traffic", "quesadillas.zonierung_d_modell", "werkstatt.niedersachsen_cordon_sumo", "core.view_region_niedersachsen_locations", "DEFAULT", 1001000000);


        String[][] scens = new String[1][2];
        int i = 0;
        scens[i][0] = "werkstatt.kofif_visum_traffic";
        scens[i][1] = "core.region_niedersachsen_grundlast_2030_promising";
        i++;
        boolean createTable;
        for (i = 0; i < scens.length; ++i) {
            createTable = true;
            //ÖV
/*
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            worker.fillBusDistribution2017();
            worker.loadODPairsFromDB(scens[i][0], "PNV" , false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 5, false, createTable);
*/
            //nahverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillCarDistribution2017();
            //worker.fillCarDistribution2030Ref();
            //worker.fillCarDistribution2030Lifelike();
            worker.fillCarDistribution2030Promising();
            //worker.fillCarDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "PNV", false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            createTable = false; //only first call creates the table
            //some more car trips
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillCarDistribution2017();
            //worker.fillCarDistribution2030Ref();
            //worker.fillCarDistribution2030Lifelike();
            worker.fillCarDistribution2030Promising();
            //worker.fillCarDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "PVT", false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            createTable = false; //only first call creates the table
            //fernverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillCarDistribution2017();
            //worker.fillCarDistribution2030Ref();
            //worker.fillCarDistribution2030Lifelike();
            worker.fillCarDistribution2030Promising();
            //worker.fillCarDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "PFV", false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //Binnenverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck35Distribution2017();
            worker.fillTruck35Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "PQZV", true);
            //worker.loadODPairs(scens[i][0]+"PWV_PKW_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            //just load and display - no saving!
            //worker.saveToDB(scens[i][1], 2, false, createTable);

            //Personenwirtschaftsverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck35Distribution2017();
            worker.fillTruck35Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "PWV", false);
            //worker.loadODPairs(scens[i][0]+"PWV_PKW_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            //Binnenverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck35Distribution2017();
            worker.fillTruck35Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "PWV-B-6561", true);
            //worker.loadODPairs(scens[i][0]+"PWV_PKW_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //Leichte Nutzfahrzeuge

            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck35Distribution2017();
            worker.fillTruck35Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "LNF", false);
            //worker.loadODPairs(scens[i][0]+"PWV_PKW_HD"+post,true);
            //worker.loadODPairs(scens[i][0]+"LNF_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            //binnenverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck35Distribution2017();
            worker.fillTruck35Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "LNF-B-6561", true);
            //worker.loadODPairs(scens[i][0]+"PWV_PKW_HD"+post,true);
            //worker.loadODPairs(scens[i][0]+"LNF_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //LKW 3.5t bis 7.5t
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck75Distribution2017();
            worker.fillTruck75Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "L75", false);
            //worker.loadODPairs(scens[i][0]+"L75_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            //binnenverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck75Distribution2017();
            worker.fillTruck75Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "L75-B-6561", true);
            //worker.loadODPairs(scens[i][0]+"L75_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);


            //LKW 7.5t bis 12t
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck12Distribution2017();
            worker.fillTruck12Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "L12", false);
            //worker.loadODPairs(scens[i][0]+"L12_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            //binnenverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTruck12Distribution2017();
            worker.fillTruck12Distribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "L12-B-6561", true);
            //worker.loadODPairs(scens[i][0]+"L75_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //Heavy Duty vehicles (Müllwagen, Betonmischer)
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillHDVDistribution2017();
            worker.fillHDVDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "HDV", false);
            //worker.loadODPairs(scens[i][0]+"HDV_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            //Binnenverkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillHDVDistribution2017();
            worker.fillHDVDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "HDV-B-6561", true);
            //worker.loadODPairs(scens[i][0]+"HDV_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //LKW >12t incl Sattelzugmaschinen
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTrailerDistribution2017();
            worker.fillTrailerDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "SZM", false);
            //worker.loadODPairs(scens[i][0]+"SZM_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);

            //Binnenvehkehr
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillTrailerDistribution2017();
            worker.fillTrailerDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "SZM-B-6561", true);
            //worker.loadODPairs(scens[i][0]+"SZM_HD"+post,true);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
            //Reisebusse
            worker.clearTrafficDistribution();
            worker.loadDayDistribution();
            //worker.fillBusDistribution2017();
            worker.fillBusDistribution2030HighTech();
            worker.loadODPairsFromDB(scens[i][0], "RB", false);
            worker.distributeOverDay();
            worker.distributeOverLocations(0.8);
            worker.saveToDB(scens[i][1], 2, false, createTable);
        }


    }

    public void distributeOverLocations(double calib) {
        double way = 0, way2=0;
        Map<Integer, Integer> timeDistCheck = new HashMap<>();
        for (int i = 0; i < 24; ++i)
            timeDistCheck.put(i, 0);

        int totalVolume = 0;
        List<LocationOption> e = new ArrayList<>(), f = new ArrayList<>();
        for (ODPair p : this.ODPairsDayDistributed) {

            switch (p.type) {
                case INBOUND:
                    //now distribute the volume
                    totalVolume = 0;
                    e = this.external2TapasLoc.get(p.idDestination);

                    for (LocationOption l : e) {

                        way += l.weight * p.volume;
                        if (way >= 1.0) {
                            //we have some trips!
                            ODPair distributedPair = new ODPair();
                            distributedPair.type = p.type;
                            distributedPair.hourOfDay = p.hourOfDay;
                            distributedPair.idOrigin = p.idOrigin;
                            distributedPair.idDestination = l.id;
                            distributedPair.volume = (int) way; // this is the integer-part
                            totalVolume += distributedPair.volume;
                            way -= distributedPair.volume; // this is the remainer
                            this.ODPairsGravityDistributed.add(distributedPair);
                        }
                    }
                    break;
                case OUTBOUND:
                    //now distribute the volume
                    totalVolume = 0;
                    e = this.external2TapasLoc.get(p.idOrigin);

                    for (LocationOption l : e) {

                        way += l.weight * p.volume;
                        if (way >= 1.0) {
                            //we have some trips!
                            ODPair distributedPair = new ODPair();
                            distributedPair.type = p.type;
                            distributedPair.hourOfDay = p.hourOfDay;
                            distributedPair.idOrigin = l.id;
                            distributedPair.idDestination = p.idDestination;
                            distributedPair.volume = (int) way; // this is the integer-part
                            totalVolume += distributedPair.volume;
                            way -= distributedPair.volume; // this is the remainer
                            this.ODPairsGravityDistributed.add(distributedPair);
                        }
                    }
                    break;
                case INTERNAL:
                    //now distribute the volume
                    totalVolume = 0;
                    e = this.external2TapasLoc.get(p.idOrigin);
                    f = this.external2TapasLoc.get(p.idDestination);
                    for (LocationOption l : e) {

                        way += l.weight * p.volume;
                        way2 = 0;
                        if (way >= 1.0) {
                            for (LocationOption m : f) {
                                way2 += m.weight * way;
                                if (way2 >= 1) {
                                    //we have some trips!
                                    ODPair distributedPair = new ODPair();
                                    distributedPair.type = p.type;
                                    distributedPair.hourOfDay = p.hourOfDay;
                                    distributedPair.idOrigin = l.id;
                                    distributedPair.idDestination = m.id;
                                    distributedPair.volume = (int) way2; // this is the integer-part
                                    totalVolume += distributedPair.volume;
                                    way2 -= distributedPair.volume; // this is the remainer
                                    this.ODPairsGravityDistributed.add(distributedPair);
                                }
                            }
                            way -= way2; // this is the overall remainer for this od-relation
                        }
                    }
                    break;

                case DRIVETHROUGH: //up to now: same as internal
                    ODPair distributedPair = new ODPair();
                    distributedPair.type = p.type;
                    distributedPair.hourOfDay = p.hourOfDay;
                    distributedPair.idOrigin = p.idOrigin;
                    distributedPair.idDestination = p.idDestination;
                    distributedPair.volume = p.volume; // this is the integer-part
                    this.ODPairsGravityDistributed.add(distributedPair);
                    break;
            }
            timeDistCheck.put(p.hourOfDay, timeDistCheck.get(p.hourOfDay) + totalVolume);

        }
		for(int i=0; i<24;++i){
			System.out.println("Hour "+i+" volume: "+timeDistCheck.get(i));
		}
    }

    public void distributeOverDay() {
        //go through the OD-List
        for (ODPair p : this.ODPairs.values()) {
            Map<Integer, Double> dayDistribution = new HashMap<>(); // initialize just in case
            switch(p.type){
                case INBOUND:
                    dayDistribution = this.dayDistributionStart;
                    break;
                case OUTBOUND:
                    dayDistribution =this.dayDistributionReturn;
                    break;
                case INTERNAL:
                case DRIVETHROUGH: //up to now: same as internal
                    dayDistribution =this.dayDistributionIntern;
                    break;
            }
            for (Map.Entry<Integer, Double> e : dayDistribution.entrySet()) {
                ODPair pair = new ODPair();
                pair.idOrigin = p.idOrigin;
                pair.idDestination = p.idDestination;
                pair.type = p.type;
                pair.hourOfDay = e.getKey();
                pair.volume = p.volume * e.getValue();
                this.ODPairsDayDistributed.add(pair);
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

    public void fillCarDistribution2017() {

        addVehicleToDistribution(  1, 0.2382);
        addVehicleToDistribution(  7, 0.0111);
        addVehicleToDistribution( 25, 0.0014);
        addVehicleToDistribution( 31, 0.0005);
        addVehicleToDistribution( 43, 0.1614);
        addVehicleToDistribution( 49, 0.0822);
        addVehicleToDistribution( 67, 0.0022);
        addVehicleToDistribution( 73, 0.0005);
        addVehicleToDistribution( 85, 0.1558);
        addVehicleToDistribution( 91, 0.1116);
        addVehicleToDistribution(109, 0.0011);
    }

    public void fillCarDistribution2030Ref() {

        addVehicleToDistribution(  1, 0.1770);
        addVehicleToDistribution(  7, 0.0057);
        addVehicleToDistribution( 25, 0.0185);
        addVehicleToDistribution( 31, 0.0031);
        addVehicleToDistribution( 43, 0.1003);
        addVehicleToDistribution( 49, 0.0329);
        addVehicleToDistribution( 67, 0.0375);
        addVehicleToDistribution( 73, 0.0121);
        addVehicleToDistribution( 85, 0.1965);
        addVehicleToDistribution( 91, 0.0762);
        addVehicleToDistribution(109, 0.0434);
        addVehicleToDistribution(115, 0.0401);
    }

    public void fillCarDistribution2030Lifelike() {

        addVehicleToDistribution(  1, 0.1623);
        addVehicleToDistribution(  6, 0.0080);
        addVehicleToDistribution(  7, 0.0052);
        addVehicleToDistribution( 12, 0.0005);
        addVehicleToDistribution( 25, 0.0151);
        addVehicleToDistribution( 30, 0.0035);
        addVehicleToDistribution( 31, 0.0025);
        addVehicleToDistribution( 36, 0.0006);
        addVehicleToDistribution( 43, 0.0849);
        addVehicleToDistribution( 48, 0.0168);
        addVehicleToDistribution( 49, 0.0279);
        addVehicleToDistribution( 54, 0.0054);
        addVehicleToDistribution( 67, 0.0279);
        addVehicleToDistribution( 72, 0.0099);
        addVehicleToDistribution( 73, 0.0090);
        addVehicleToDistribution( 78, 0.0032);
        addVehicleToDistribution( 85, 0.1576);
        addVehicleToDistribution( 90, 0.0462);
        addVehicleToDistribution( 91, 0.0611);
        addVehicleToDistribution( 96, 0.0161);
        addVehicleToDistribution(109, 0.0307);
        addVehicleToDistribution(114, 0.0132);
        addVehicleToDistribution(115, 0.0283);
        addVehicleToDistribution(120, 0.0121);
    }

    public void fillCarDistribution2030Promising() {
        addVehicleToDistribution(  1, 0.1411);
        addVehicleToDistribution(  6, 0.0193);
        addVehicleToDistribution(  7, 0.0045);
        addVehicleToDistribution( 12, 0.0012);
        addVehicleToDistribution( 25, 0.0102);
        addVehicleToDistribution( 30, 0.0084);
        addVehicleToDistribution( 31, 0.0017);
        addVehicleToDistribution( 36, 0.0014);
        addVehicleToDistribution( 43, 0.0628);
        addVehicleToDistribution( 48, 0.0400);
        addVehicleToDistribution( 49, 0.0206);
        addVehicleToDistribution( 54, 0.0126);
        addVehicleToDistribution( 67, 0.0139);
        addVehicleToDistribution( 72, 0.0239);
        addVehicleToDistribution( 73, 0.0045);
        addVehicleToDistribution( 78, 0.0077);
        addVehicleToDistribution( 85, 0.1014);
        addVehicleToDistribution( 90, 0.1058);
        addVehicleToDistribution( 91, 0.0393);
        addVehicleToDistribution( 96, 0.0384);
        addVehicleToDistribution(109, 0.0122);
        addVehicleToDistribution(114, 0.0316);
        addVehicleToDistribution(115, 0.0113);
        addVehicleToDistribution(120, 0.0291);
    }

    public void fillCarDistribution2030HighTech() {
        //2017
        addVehicleToDistribution(  6, 0.0885);
        addVehicleToDistribution( 12, 0.0057);
        addVehicleToDistribution( 30, 0.0185);
        addVehicleToDistribution( 36, 0.0031);
        addVehicleToDistribution( 48, 0.1003);
        addVehicleToDistribution( 54, 0.0329);
        addVehicleToDistribution( 72, 0.0375);
        addVehicleToDistribution( 78, 0.0121);
        addVehicleToDistribution( 90, 0.1965);
        addVehicleToDistribution( 96, 0.0762);
        addVehicleToDistribution(114, 0.0434);
        addVehicleToDistribution(120, 0.0401);
    }

    public void fillTruck35Distribution2017() {
        addVehicleToDistribution(146, 1.0);
    }

    public void fillTruck35Distribution2030HighTech() {
        addVehicleToDistribution(168, 1.0);
    }

    public void fillTruck75Distribution2017() {
        addVehicleToDistribution(157, 1.0);
    }

    public void fillTruck75Distribution2030HighTech() {
        addVehicleToDistribution(173, 1.0);
    }

    public void fillTruck12Distribution2017() {
        addVehicleToDistribution(158, 1.0);
    }

    public void fillTruck12Distribution2030HighTech() {
        addVehicleToDistribution(174, 1.0);
    }


    public void fillHDVDistribution2017() {
        addVehicleToDistribution(159, 1.0);
    }

    public void fillHDVDistribution2030HighTech() {
        addVehicleToDistribution(175, 1.0);
    }

    public void fillTrailerDistribution2017() {
        addVehicleToDistribution(160, 1.0);
    }

    public void fillTrailerDistribution2030HighTech() {
        addVehicleToDistribution(176, 1.0);
    }

    public void fillBusDistribution2017() {
        addVehicleToDistribution(161, 1.0);
    }

    public void fillBusDistribution2030HighTech() {
        addVehicleToDistribution(177, 1.0);
    }

    private void addVehicleToDistribution(int id, double distribution){
        Vehicle tmp;
        tmp = new Vehicle(id, distribution);
        this.carTypeDistribution.add(tmp);
    }

    public void loadODPairsFromDB(String tableName, String trafficTypeFilter, boolean useInternalTraffic) {
        double inboundVol = 0, outboundVol = 0, internalVol = 0, driveThrougVolume = 0;
        int numRelations = 0;
        String key;
        ODPair pair;
        ResultSet rs;

        String fromTVZ, toTVZ;
        double volume;
        String query ="";
        int totalVolume = 0;
//        try {
//            query = "SELECT demo_from, demo_to, volume as volume FROM "+tableName+" where demo_vehicle = '"+trafficTypeFilter+"';";
//            rs = this.dbCon.executeQuery(query, this);
//            while(rs.next()){
//
//                //get from
//                fromTVZ = rs.getString("demo_from");
//                //get to
//                toTVZ = rs.getString("demo_to");
//                //				if(toTVZ==23565)
//                //					System.out.println("Hit berlin!");
//                //get volume
//                volume = rs.getDouble("volume");
//                totalVolume += volume;
//
//
//                //see if we found a TAPAS-mapping
//                if (!external2TapasLoc.containsKey(fromTVZ) && !fromTVZ.startsWith("w")) {
//                    System.err.println("Cannot find mapping for inbound  taz: "+fromTVZ);
//                    continue;
//                }
//                if (!external2TapasLoc.containsKey(toTVZ) && !toTVZ.startsWith("w")) {
//                    System.err.println("Cannot find mapping for outbound taz: "+toTVZ);
//                    continue;
//                }
//
//
//                key = fromTVZ + "-" + toTVZ;
//                //find existing relation and add the volume if found!
//                pair = this.ODPairs.get(key);
//                if (pair != null) {
//                    pair.volume += volume;
//                } else {
//                    //add the new relation!
//                    pair = new ODPair();
//                    pair.idOrigin = fromTVZ;
//                    pair.idDestination = toTVZ;
//                    pair.volume = volume;
//                    if(fromTVZ.startsWith("w") && !toTVZ.startsWith("w")){
//                        pair.type = TrafficType.INBOUND;
//                    }
//                    else if (!fromTVZ.startsWith("w") && toTVZ.startsWith("w")){
//                        pair.type = TrafficType.OUTBOUND;
//                    }
//                    else if(!fromTVZ.startsWith("w") && !toTVZ.startsWith("w")){
//                        pair.type = TrafficType.INTERNAL;
//                    }
//                    else{
//                        pair.type = TrafficType.DRIVETHROUGH;
//                    }
//
//                    this.ODPairs.put(key, pair);
//                    numRelations++;
//                }
//
//                switch(pair.type){
//                    case INBOUND:
//                        inboundVol += volume;
//                        break;
//                    case OUTBOUND:
//                        outboundVol += volume;
//                        break;
//                    case INTERNAL:
//                        internalVol += volume;
//                        break;
//                    case DRIVETHROUGH:
//                        driveThrougVolume += volume;
//                        break;
//
//                }
//            }
//
//
//        } catch (SQLException e) {
//            System.err.println("Error in sqlstatement: " + query);
//            e.printStackTrace();
//        }

        System.out.println("Relations: "+numRelations );
        System.out.println("Total volume: "+totalVolume );
        System.out.println("Inbound  volume: "+inboundVol );
        System.out.println("Outbound volume: "+outboundVol );
        System.out.println("Internal volume: "+internalVol );
        System.out.println("Drive Tr volume: "+driveThrougVolume );
    }


    public void loadCordonDistrictsKoFIF(String trafficTableName, String dmod, String sumoCoordinates, String region, String loc_key, int loc_code) {
        String query = "";
//        try {
//            int id, taz, cappa;
//            double sLon, sLat;
//            ResultSet rs;
//            String cellName;
//            String zonename1 = "vbz_6561", zonename2 = "vbz_412";
//            Set<String > cordons = new TreeSet<>();
//
//            //first load the cells from the external traffic
//            query = "SELECT demo_from as id, st_x(st_transform(from_coord,4326)) as x,st_y(st_transform(from_coord,4326)) as y FROM "+trafficTableName+" group by demo_from,x,y";
//            rs = this.dbCon.executeQuery(query, this);
//            while(rs.next()){
//                ExternalCell tmp = new ExternalCell();
//                tmp.id = rs.getString("id");
//                if(tmp.id.startsWith("w"))
//                    cordons.add(tmp.id);
//                tmp.x[0] = rs.getDouble("x");
//                tmp.y[0] = rs.getDouble("y");
//                tmp.x[1] = rs.getDouble("x");
//                tmp.y[1] = rs.getDouble("y");
//                this.externalLoc.put(tmp.id,tmp);
//            }
//            rs.close();
//
//            //just in case we are not symmetric
//            query = "SELECT demo_to as id, st_x(st_transform(to_coord,4326)) as x,st_y(st_transform(to_coord,4326)) as y FROM "+trafficTableName+" group by demo_to,x,y";
//            rs = this.dbCon.executeQuery(query, this);
//
//            while(rs.next()){
//
//                ExternalCell tmp = new ExternalCell();
//                tmp.id = rs.getString("id");
//                if(tmp.id.startsWith("w"))
//                    cordons.add(tmp.id);
//                tmp.x[0] = rs.getDouble("x");
//                tmp.y[0] = rs.getDouble("y");
//                tmp.x[1] = rs.getDouble("x");
//                tmp.y[1] = rs.getDouble("y");
//                this.externalLoc.put(tmp.id,tmp);
//            }
//            rs.close();
//            System.out.println("found " + this.externalLoc.size() + " origin entires and "+cordons.size()+" cordons");
//
//            //load the sumo coordinates for the cordon cells
//            query = "SELECT full_id, lon_out,lat_out, lon_in, lat_in FROM "+sumoCoordinates;
//            rs = this.dbCon.executeQuery(query, this);
//            int count=0;
//            Set<String > cordonsCorrected = new TreeSet<>();
//            while(rs.next()){
//
//                //get the ID, which might end with an "-0" or "-1"
//
//                String sumoID = rs.getString("full_id");
//
//                if (cordons.contains(sumoID)) {
//                    cordonsCorrected.add(sumoID);
//                    count++;
//                    ExternalCell tmp = this.externalLoc.get(sumoID);
//
//                    tmp.x[0] = rs.getDouble("lon_in");
//                    tmp.y[0] = rs.getDouble("lat_in");
//                    tmp.x[1] = rs.getDouble("lon_out");
//                    tmp.y[1] = rs.getDouble("lat_out");
//                }
//                if(cordons.contains(sumoID+"-0")){
//                    cordonsCorrected.add(sumoID+"-0");
//                    count++;
//                    ExternalCell tmp = this.externalLoc.get(sumoID+"-0");
//
//                    tmp.x[0] = rs.getDouble("lon_in");
//                    tmp.y[0] = rs.getDouble("lat_in");
//                    tmp.x[1] = rs.getDouble("lon_out");
//                    tmp.y[1] = rs.getDouble("lat_out");;
//                }
//                if(cordons.contains(sumoID+"-1")){
//                    count++;
//                    cordonsCorrected.add(sumoID+"-1");
//                    ExternalCell tmp = this.externalLoc.get(sumoID+"-1");
//
//                    tmp.x[0] = rs.getDouble("lon_in");
//                    tmp.y[0] = rs.getDouble("lat_in");
//                    tmp.x[1] = rs.getDouble("lon_out");
//                    tmp.y[1] = rs.getDouble("lat_out");;
//                }
//            }
//            rs.close();
//
//            System.out.println("Updated  " + count + " cordon coordinates Unique: "+cordonsCorrected.size());
//            System.out.println(cordons.toString());
//            System.out.println(cordonsCorrected.toString());
//            //load the external mapping
//            query = "with cells as (" +
//                    "SELECT demo_from FROM "+trafficTableName+" where not demo_from like 'w%' group by demo_from)," +
//                    "locations as (select loc_id,loc_taz_id,loc_capacity, loc_coordinate  from "+region+" where \"key\" = '"+loc_key+"' and loc_code = " +loc_code+" )," +
//                    "zones as (select (zdm." + zonename1 +")::integer as " + zonename1 +", zdm." + zonename2 +",st_transform(zdm.the_geom,4326) as zone_geom from "+dmod+" zdm where zdm.bland =3)," +
//                    "locations2Zones as (select loc_id,loc_taz_id, loc_capacity, loc_coordinate, " + zonename1 +", " + zonename2 +" from locations join zones on st_within(loc_coordinate, zone_geom))" +
//                    "select demo_from, loc_id,loc_taz_id,loc_capacity, st_x(loc_coordinate) x, st_y(loc_coordinate) y, " + zonename1 +", " + zonename2 +" from cells join locations2Zones on (cells.demo_from)::integer = " + zonename1 +";";
//            rs = this.dbCon.executeQuery(query, this);
//            count=0;
//            while (rs.next()) {
//                   cellName = rs.getString("demo_from") ;
//                   LocationOption tmp = new LocationOption();
//                   tmp.x = rs.getDouble("x");
//                   tmp.y = rs.getDouble("y");
//                   tmp.id = Integer.toString(rs.getInt("loc_id"));
//                   tmp.cappa = rs.getInt("loc_capacity");
//                   tmp.taz = rs.getInt("loc_taz_id");
//                   List<LocationOption> locs = this.external2TapasLoc.get(cellName);
//                   if(locs == null){
//                       locs = new ArrayList<>();
//                   }
//                   locs.add(tmp);
//                   count++;
//                   this.external2TapasLoc.put(cellName,locs);
//                   this.internalLocs.put(tmp.id,tmp);
//            }
//            rs.close();
//
//            //load the external mapping for the "greater cells"
//            query = "with cells as (" +
//                    "SELECT demo_from FROM "+trafficTableName+" where not demo_from like 'w%' group by demo_from)," +
//                    "locations as (select loc_id,loc_taz_id,loc_capacity, loc_coordinate  from "+region+" where \"key\" = '"+loc_key+"' and loc_code = " +loc_code+" )," +
//                    "zones as (select (zdm." + zonename1 +")::integer as " + zonename1 +", zdm." + zonename2 +",st_transform(zdm.the_geom,4326) as zone_geom from "+dmod+" zdm where zdm.bland =3)," +
//                    "locations2Zones as (select loc_id,loc_taz_id, loc_capacity, loc_coordinate, " + zonename1 +", " + zonename2 +" from locations join zones on st_within(loc_coordinate, zone_geom))" +
//                    "select demo_from, loc_id,loc_taz_id,loc_capacity, st_x(loc_coordinate) x, st_y(loc_coordinate) y, " + zonename1 +", " + zonename2 +" from cells join locations2Zones on (cells.demo_from)::integer = " + zonename2 +";";            rs = this.dbCon.executeQuery(query, this);
//            count=0;
//            while (rs.next()) {
//                cellName = rs.getString("demo_from") ;
//                LocationOption tmp = new LocationOption();
//                tmp.x = rs.getDouble("x");
//                tmp.y = rs.getDouble("y");
//                tmp.id = Integer.toString(rs.getInt("loc_id"));
//                tmp.cappa = rs.getInt("loc_capacity");
//                tmp.taz = rs.getInt("loc_taz_id");
//                List<LocationOption> locs = this.external2TapasLoc.get(cellName);
//                if(locs == null){
//                    locs = new ArrayList<>();
//                }
//                locs.add(tmp);
//                count++;
//                this.external2TapasLoc.put(cellName,locs);
//                this.internalLocs.put(tmp.id,tmp);
//            }
//            rs.close();
//
//            //now the symmetric values: We have to load all and then filter by unknowns and add them to the external2TapasLoc-map
//            Map<String, List<LocationOption>> external2TapasLocDestinations = new HashMap<>();
//            //load the external mapping
//            query = "with cells as (" +
//                    "SELECT demo_to FROM "+trafficTableName+" where not demo_to like 'w%' group by demo_to)," +
//                    "locations as (select loc_id,loc_taz_id,loc_capacity, loc_coordinate  from "+region+" where \"key\" = '"+loc_key+"' and loc_code = " +loc_code+" )," +
//                    "zones as (select (zdm." + zonename1 +")::integer as " + zonename1 +", zdm." + zonename2 +",st_transform(zdm.the_geom,4326) as zone_geom from "+dmod+" zdm where zdm.bland =3)," +
//                    "locations2Zones as (select loc_id,loc_taz_id, loc_capacity, loc_coordinate, " + zonename1 +", " + zonename2 +" from locations join zones on st_within(loc_coordinate, zone_geom))" +
//                    "select demo_to, loc_id,loc_taz_id,loc_capacity, st_x(loc_coordinate) x, st_y(loc_coordinate) y, " + zonename1 +", " + zonename2 +" from cells join locations2Zones on (cells.demo_to)::integer = " + zonename1 +";";
//            rs = this.dbCon.executeQuery(query, this);
//
//            while (rs.next()) {
//                cellName = rs.getString("demo_to") ;
//                LocationOption tmp = new LocationOption();
//                tmp.x = rs.getDouble("x");
//                tmp.y = rs.getDouble("y");
//                tmp.id = Integer.toString(rs.getInt("loc_id"));
//                tmp.cappa = rs.getInt("loc_capacity");
//                tmp.taz = rs.getInt("loc_taz_id");
//                List<LocationOption> locs = external2TapasLocDestinations.get(cellName);
//                if(locs == null){
//                    locs = new ArrayList<>();
//                }
//                locs.add(tmp);
//                count++;
//                external2TapasLocDestinations.put(cellName,locs);
//            }
//            rs.close();
//
//            //load the external mapping for the "greater cells"
//            query = "with cells as (" +
//                        "SELECT demo_to FROM "+trafficTableName+" where not demo_to like 'w%' group by demo_to)," +
//                        "locations as (select loc_id,loc_taz_id,loc_capacity, loc_coordinate  " +
//                                        "from "+region+" where \"key\" = '"+loc_key+"' and loc_code = " +loc_code+" )," +
//                        "zones as (select (zdm." + zonename1 +")::integer as " + zonename1 +", zdm." + zonename2 +",st_transform(zdm.the_geom,4326) as zone_geom " +
//                                    "from "+dmod+" zdm where zdm.bland =3)," +
//                        "locations2Zones as (select loc_id,loc_taz_id, loc_capacity, loc_coordinate, " + zonename1 +", " + zonename2 +" " +
//                                                "from locations join zones on st_within(loc_coordinate, zone_geom))" +
//                    "select demo_to, loc_id,loc_taz_id,loc_capacity, st_x(loc_coordinate) x, st_y(loc_coordinate) y, " + zonename1 +", " + zonename2 +
//                    " from cells join locations2Zones on (cells.demo_to)::integer = " + zonename2 +";";
//            rs = this.dbCon.executeQuery(query, this);
//            count=0;
//            while (rs.next()) {
//                cellName = rs.getString("demo_to") ;
//                LocationOption tmp = new LocationOption();
//                tmp.x = rs.getDouble("x");
//                tmp.y = rs.getDouble("y");
//                tmp.id = Integer.toString(rs.getInt("loc_id"));
//                tmp.cappa = rs.getInt("loc_capacity");
//                tmp.taz = rs.getInt("loc_taz_id");
//                List<LocationOption> locs = external2TapasLocDestinations.get(cellName);
//                if(locs == null){
//                    locs = new ArrayList<>();
//                }
//                locs.add(tmp);
//                count++;
//                external2TapasLocDestinations.put(cellName,locs);
//            }
//            rs.close();
//
//            for(Map.Entry<String, List<LocationOption>> e:external2TapasLocDestinations.entrySet()){
//                if(this.external2TapasLoc.containsKey(e.getKey())) //skip known ones
//                    continue;
//                this.external2TapasLoc.put(e.getKey(),e.getValue());
//                for(LocationOption l: e.getValue()){
//                    this.internalLocs.put(l.id,l);
//                }
//                count+=e.getValue().size();
//            }
//
//            //normalize the weight according to the volume of each entry of the map
//            for(Map.Entry<String, List<LocationOption>> e:this.external2TapasLoc.entrySet()){
//                double sum=0;
//                //find the sum of all capacities
//                for(LocationOption l: e.getValue()){
//                    sum+=l.cappa;
//                }
//                //normalize by this value
//                for(LocationOption l: e.getValue()){
//                    l.weight= ((double)l.cappa) / sum;
//                }
//            }
//
//
//            System.out.println("found " + this.external2TapasLoc.size() + " origin entires with "+count+"locations");
//
//            for(Map.Entry<String, ExternalCell> e:this.externalLoc.entrySet()) {
//                if (this.external2TapasLoc.containsKey(e.getKey()))
//                    continue;
//                System.out.println(e.getKey() + " not found");
//            }
//
//
//        } catch (SQLException e) {
//            System.err.println("Error in sqlstatement: " + query);
//            e.printStackTrace();
//        }
    }

    private static int koFiFStringIDtoIntID(String input){
        try{
            return Integer.parseInt(input.replace("w","").replace("-1","").replace("-0",""));
        }catch (NumberFormatException e){
            System.err.println("Unparsable: " + input);
            e.printStackTrace();
        }
        return -1;
    }

    public void saveToDB(String tablename, int modeNumber, boolean is_resticted, boolean createTable) {
        String query = "";
//        try {
//            if (createTable) {
//                query = "DROP TABLE IF EXISTS " + tablename;
//                this.dbCon.execute(query, this);
//
//                String[] tokens = tablename.split("\\.");
//                query = "CREATE TABLE " + tablename + " (" + "  p_id integer NOT NULL," + "  hh_id integer NOT NULL," +
//                        "  taz_id_start integer," + "  loc_id_start integer," + "  lon_start double precision," +
//                        "  lat_start double precision," + "  taz_id_end integer," + "  loc_id_end integer," +
//                        "  lon_end double precision," + "  lat_end double precision," +
//                        "  start_time_min integer NOT NULL," + "  travel_time_sec double precision," +
//                        "  mode integer," + "  car_type integer," + "  activity_duration_min integer," +
//                        "  sumo_type integer," + "  is_restricted boolean," + "  CONSTRAINT " +
//                        tokens[tokens.length - 1] + "_pkey PRIMARY KEY (p_id, hh_id, start_time_min))" +
//                        "WITH (  OIDS=FALSE);";
//                this.dbCon.execute(query, this);
//                query = "GRANT ALL ON TABLE " + tablename + " TO tapas_admin_group;" + //Krass! Tappas_admin_group darf das!
//                        "GRANT SELECT ON TABLE " + tablename + " TO tapas_user_group;" +
//                        "GRANT REFERENCES ON TABLE " + tablename + " TO tapas_user_group;" +
//                        "GRANT TRIGGER ON TABLE " + tablename + " TO tapas_user_group;";
//                this.dbCon.execute(query, this);
//            }
//            int num = -1;
//
//            //get the right number
//            query = "SELECT count(*) as c, MIN(p_id) as min from " + tablename;
//            ResultSet rs = this.dbCon.executeQuery(query, this);
//            if (rs.next()) {
//                if (rs.getInt("c") > 0) {
//                    num += rs.getInt("min");
//                }
//            }
//            rs.close();
//
//
//            query = "INSERT INTO " + tablename + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?," + modeNumber + ",?,1,?," +
//                    is_resticted + ")";
//
//            PreparedStatement pS = this.dbCon.getConnection(this).prepareStatement(query);
//            int count = 0, maxBatch = 10000;
//            for (ODPair p : this.ODPairsGravityDistributed) {
//                //these values must be filled in the switch-case field
//                double lonStart =0,latStart=0,lonEnd=0,latEnd=0;
//                int tazStart =-1, locIDStart=-1, tazEnd =-1, locIDEnd=-1;
//                switch(p.type){
//                    case INBOUND: {
//                        ExternalCell origin = this.externalLoc.get(p.idOrigin);
//                        LocationOption destination = this.internalLocs.get(p.idDestination);
//                        lonStart = origin.x[0];
//                        latStart = origin.y[0];
//                        tazStart = koFiFStringIDtoIntID(origin.id);
//                        locIDStart = koFiFStringIDtoIntID(origin.id);
//                        lonEnd = destination.x;
//                        latEnd = destination.y;
//                        tazEnd = destination.taz;
//                        locIDEnd = koFiFStringIDtoIntID(destination.id);
//                    }
//                        break;
//                    case OUTBOUND: {
//                        ExternalCell destination = this.externalLoc.get(p.idDestination);
//                        LocationOption origin = this.internalLocs.get(p.idOrigin);
//                        lonStart = origin.x;
//                        latStart = origin.y;
//                        tazStart = origin.taz;
//                        locIDStart = koFiFStringIDtoIntID(origin.id);
//                        lonEnd = destination.x[1];
//                        latEnd = destination.y[1];
//                        tazEnd = koFiFStringIDtoIntID(destination.id);
//                        locIDEnd = koFiFStringIDtoIntID(destination.id);
//                    }
//                        break;
//                    case INTERNAL:{
//                        LocationOption destination = this.internalLocs.get(p.idDestination);
//                        LocationOption origin = this.internalLocs.get(p.idOrigin);
//                        lonStart = origin.x;
//                        latStart = origin.y;
//                        tazStart = origin.taz;
//                        locIDStart = koFiFStringIDtoIntID(origin.id);
//                        lonEnd = destination.x;
//                        latEnd = destination.y;
//                        tazEnd = destination.taz;
//                        locIDEnd = koFiFStringIDtoIntID(destination.id);
//                    }
//                        break;
//                    case DRIVETHROUGH:{
//                        ExternalCell origin = this.externalLoc.get(p.idOrigin);
//                        ExternalCell destination = this.externalLoc.get(p.idDestination);
//                        lonStart = origin.x[0];
//                        latStart = origin.y[0];
//                        tazStart = koFiFStringIDtoIntID(origin.id);
//                        locIDStart = koFiFStringIDtoIntID(origin.id);
//                        lonEnd = destination.x[1];
//                        latEnd = destination.y[1];
//                        tazEnd = koFiFStringIDtoIntID(destination.id);
//                        locIDEnd = koFiFStringIDtoIntID(destination.id);
//                    }
//
//                        break;
//                }
//
//
//
//
//                int start = p.hourOfDay * 60 + (int) (Math.random() * 60.0);
//                double time = TPS_Geometrics.getDistance(lonStart, latStart,
//                        lonEnd, latEnd);
//                time *= 1.2 / 7.7777; //1.2 ist der umwegefaktor, 28kmh= 7.7777m/s;

//                double vID, sum;
//                int v;
//                for (int i = 0; i < p.volume; ++i) {
//                    // get the cartype
//                    vID = Math.random();
//                    sum = this.carTypeDistribution.get(0).weight;
//                    v = 0;
//                    do {
//                        if (sum >= vID) {
//                            break;
//                        } else {
//                            v++;
//                            sum += this.carTypeDistribution.get(v).weight;
//                        }
//                    } while (v < this.carTypeDistribution.size() - 1);
//                    tmp = this.carTypeDistribution.get(v);
//
//                    pS.setInt(1, num);//p_id
//                    pS.setInt(2, num);//hh_id
//                    pS.setInt(3, tazStart); //taz_id_start
//                    pS.setInt(4, locIDStart); //loc_id_start
//                    pS.setDouble(5, lonStart); //lon_start
//                    pS.setDouble(6, latStart); //lat_start
//                    pS.setInt(7, tazEnd); //taz_id_end
//                    pS.setInt(8, locIDEnd); //loc_id_end
//                    pS.setDouble(9, lonEnd); //lon_end
//                    pS.setDouble(10, latEnd); //lat_end
//                    pS.setInt(11, start); // start_time_min
//                    pS.setDouble(12, time); //travel_time_sec
//                    pS.setInt(13, tmp.id);
//                    pS.setInt(14, tmp.getSumoVehicle());
//                    pS.addBatch();
//                    count++;
//                    if (count >= maxBatch) {
//                        pS.executeBatch();
//                        count = 0;
//                    }
//                    num--;
//                }
//
//            }
//            if (count > 0) {
//                pS.executeBatch();
//            }
//
//        } catch (SQLException e) {
//            System.err.println("Error in sqlstatement: " + query);
//            e.printStackTrace();
//            e.getNextException().printStackTrace();
//        }

    }

}
