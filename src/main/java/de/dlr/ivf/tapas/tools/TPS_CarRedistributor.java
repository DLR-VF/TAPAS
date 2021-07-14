/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

/**
 * In several scenarios households within a restricted access area poses a restricted car. This is highly unlikely!
 * This class scans for such households and exchanges the cars randomly with unrestricted cars from the outside.
 * Of course this is not a good model, but what can I do?
 *
 * @author hein_mh
 */
public class TPS_CarRedistributor extends TPS_BasicConnectionClass {

    Map<Integer, Boolean> carRestriction = new HashMap<>();
    Map<Integer, Boolean> tazRestriction = new HashMap<>();
    List<HouseHoldWithCars> householdsWithBadCars = new LinkedList<>();
    List<HouseHoldWithCars> householdsWithGoodCars = new LinkedList<>();
    List<HouseHoldWithCars> householdsToUpdate = new LinkedList<>();
    Map<Integer, Integer> carIDFrequency = new HashMap<>();

    public static void main(String[] args) {
        String region = "berlin";
        String carKey = "MID2008_Y2010_BERLIN";
        String hhKey = "MID2008_Y2010_ANT";
        String feeKey = "Berlin_2010";

        //automatic Strings
        String tazFeeTable = "core." + region + "_taz_fees_tolls";
        String carTable = "core." + region + "_cars";
        String hhTable = "core." + region + "_households";

        TPS_CarRedistributor worker = new TPS_CarRedistributor();
        worker.loadTazMap(tazFeeTable, feeKey);
        worker.loadCarMap(carTable, carKey);
        worker.recodeAllCars(hhTable, hhKey);
        worker.updateHouseHolds(hhTable, hhKey);
        worker.loadHouseHolds(hhTable, hhKey);
        worker.printStatistics();
        //worker.redistributeCars();
        //build a probability-vector
//		List<CarProbabaility> probs = new ArrayList<>();
//		double sum=0;
//
//		CarProbabaility tmp = worker.new CarProbabaility();
//		tmp.id= 1; //benzin
//		tmp.freqency = 75;
//		sum += tmp.freqency;
//		probs.add(tmp);
//
//		CarProbabaility tmp2 = worker.new CarProbabaility();
//		tmp2.id= 4; //diesel
//		tmp2.freqency = 15;
//		sum += tmp2.freqency;
//		probs.add(tmp2);
//
//		CarProbabaility tmp3 = worker.new CarProbabaility();
//		tmp3.id= 13; //Plug-ins
//		tmp3.freqency = 10;
//		sum += tmp3.freqency;
//		probs.add(tmp3);
//
//		double accumulation =0;
//		for(CarProbabaility car: probs){
//			car.prob = accumulation+(double)car.freqency/sum;
//			accumulation = car.prob;
//		}
//		worker.transformAllCars(hhTable, hhKey, probs);
//		worker.redistributeCars();
        //worker.transformCars();
//		worker.updateHouseHolds(hhTable, hhKey);
        //check if everything worked
//		worker.loadHouseHolds(hhTable, hhKey);

    }

    public void loadCarMap(String table, String key) {
        String query = "";

        try {
            query = "select car_id, restriction from " + table + " where car_key='" + key + "'";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                this.carRestriction.put(rs.getInt("car_id"), rs.getBoolean("restriction"));
                this.carIDFrequency.put(rs.getInt("car_id"), 0);
            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void loadHouseHolds(String table, String key) {
        String query = "";
        int numOfBadCars = 0;
        int numOfGoodCars = 0;
        boolean addHH;
        try {
            query = "select hh_id, hh_taz_id, hh_car_ids from " + table + " where hh_key='" + key + "' and hh_cars>0";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                HouseHoldWithCars hh = new HouseHoldWithCars();
                hh.id = rs.getInt("hh_id");
                //grrrr! int to Integer conversion does not work nicely!
                int[] tmp = TPS_DB_IO.extractIntArray(rs, "hh_car_ids");
                hh.carIDs = new Integer[tmp.length];
                for (int i = 0; i < tmp.length; ++i) {
                    hh.carIDs[i] = tmp[i];
                    this.carIDFrequency.put(tmp[i], this.carIDFrequency.get(tmp[i]) + 1);
                }
                addHH = false;
                if (this.tazRestriction.get(rs.getInt("hh_taz_id"))) {
                    //check if the household has restricted cars;
                    for (int i : hh.carIDs) {
                        if (this.carRestriction.get(i)) { //restricted car in restricted area -> bad!
                            addHH = true;
                            numOfBadCars++;
                        }
                    }
                    if (addHH) {
                        this.householdsWithBadCars.add(hh);
                    }
                } else {
                    //check if the household has unrestricted cars
                    for (int i : hh.carIDs) {
                        if (!this.carRestriction.get(i)) { //unrestricted car in unrestricted area -> good!
                            addHH = true;
                            numOfGoodCars++;
                        }
                    }
                    if (addHH) {
                        this.householdsWithGoodCars.add(hh);
                    }
                }
            }
            System.out.println("Bad cars:  " + numOfBadCars);
            System.out.println("Good cars: " + numOfGoodCars);
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void loadTazMap(String table, String name) {
        String query = "";

        try {
            query = "select ft_taz_id, is_restricted from " + table + " where ft_name='" + name + "'";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                this.tazRestriction.put(rs.getInt("ft_taz_id"), rs.getBoolean("is_restricted"));
            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
    }

    public void printStatistics() {
        for (Entry<Integer, Integer> e : this.carIDFrequency.entrySet()) {
            System.out.printf("ID %d: Freq : %d\n", e.getKey(), e.getValue());
        }
    }

    public void recodeAllCars(String table, String key) {

        String query = "";
        //Map<Integer, Integer> values = new HashMap<>();
        //int sum =0;
        try {
            query = "select hh_id, hh_taz_id, hh_car_ids from " + table + " where hh_key='" + key + "' and hh_cars>0";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                HouseHoldWithCars hh = new HouseHoldWithCars();
                hh.id = rs.getInt("hh_id");
                //grrrr! int to Integer conversion does not work nicely!
                int[] tmp = TPS_DB_IO.extractIntArray(rs, "hh_car_ids");
                hh.carIDs = new Integer[tmp.length];
                for (int i = 0; i < hh.carIDs.length; ++i) {
                    switch (tmp[i]) {
                        case 1:
                            hh.carIDs[i] = 2;
                            break;
                        case 4:
                            hh.carIDs[i] = 5;
                            break;
                        case 13:
                            hh.carIDs[i] = 8;
                            break;
                    }
                }
                this.householdsToUpdate.add(hh);
            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
        System.out.println("Households to update: " + this.householdsToUpdate.size());
    }

    public void redistributeCars() {
        int numOfExchangedCars = 0;
        //loop over all house holds
        for (HouseHoldWithCars badCarHH : this.householdsWithBadCars) {
            //loop over all cars
            for (int i = 0; i < badCarHH.carIDs.length; ++i) {
                //is this a restricted car?
                if (this.carRestriction.get(badCarHH.carIDs[i]) && this.householdsWithGoodCars.size() > 0) {
                    //find a random unrestricted partner household
                    int exchangePartner = (int) (Math.random() * this.householdsWithGoodCars.size());
                    boolean hasMoreGoodCars = false, carExchanged = false;
                    HouseHoldWithCars goodCarHH = this.householdsWithGoodCars.get(exchangePartner);
                    //find the first good car
                    for (int j = 0; j < goodCarHH.carIDs.length; ++j) {
                        if (!this.carRestriction.get(goodCarHH.carIDs[j])) {
                            if (!carExchanged) {
                                //exchange these cars
                                int tmp = goodCarHH.carIDs[j];
                                goodCarHH.carIDs[j] = badCarHH.carIDs[i];
                                badCarHH.carIDs[i] = tmp;
                                carExchanged = true;
                                numOfExchangedCars++;
                            } else { //this house holds more good cars!
                                hasMoreGoodCars = true;
                            }
                        }
                    }
                    //remove household from the good-list, if no good car is left
                    if (!hasMoreGoodCars) {
                        this.householdsWithGoodCars.remove(exchangePartner);
                    }
                    if (!this.householdsToUpdate.contains(goodCarHH)) { //add to update list if not previously known
                        this.householdsToUpdate.add(goodCarHH);
                    }
                }
            }
            this.householdsToUpdate.add(badCarHH);
        }
        System.out.println("Exchanged cars: " + numOfExchangedCars);
        System.out.println("Households to update: " + this.householdsToUpdate.size());
    }

    public void transformAllCars(String table, String key, List<CarProbabaility> probs) {

        String query = "";
        Map<Integer, Integer> values = new HashMap<>();
        for (CarProbabaility car : probs) {
            values.put(car.id, 0);
        }
        int sum = 0;
        try {
            query = "select hh_id, hh_taz_id, hh_car_ids from " + table + " where hh_key='" + key + "' and hh_cars>0";
            ResultSet rs = this.dbCon.executeQuery(query, this);
            while (rs.next()) {
                HouseHoldWithCars hh = new HouseHoldWithCars();
                hh.id = rs.getInt("hh_id");
                //grrrr! int to Integer conversion does not work nicely!
                int[] tmp = TPS_DB_IO.extractIntArray(rs, "hh_car_ids");
                hh.carIDs = new Integer[tmp.length];
                for (int i = 0; i < tmp.length; ++i) {
                    double random = Math.random();
                    int id = tmp[i];
                    for (CarProbabaility car : probs) {
                        if (car.prob > random) {
                            id = car.id;
                            values.put(car.id, values.get(car.id) + 1);
                            sum++;
                            break;
                        }
                    }
                    hh.carIDs[i] = id;
                }
                this.householdsToUpdate.add(hh);
            }
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            e.printStackTrace();
        }
        for (CarProbabaility car : probs) {
            System.out.println(
                    "Car id:" + car.id + " given prob:" + car.prob + " given freq:" + car.freqency + " drawn: " +
                            (double) values.get(car.id) / sum);
        }
        System.out.println("Households to update: " + this.householdsToUpdate.size());
    }

    public void transformCars() {
        int numOfExchangedCars = 0;

        //build a probability-vector
        List<CarProbabaility> probs = new ArrayList<>();
        double sum = 0;
        for (Entry<Integer, Integer> e : this.carIDFrequency.entrySet()) {
            if (!this.carRestriction.get(e.getKey())) { // an unrestricted car!
                CarProbabaility tmp = new CarProbabaility();
                tmp.id = e.getKey();
                tmp.freqency = e.getValue();
                if (tmp.freqency > 0) {
                    sum += e.getValue();
                    probs.add(tmp);
                }
            }
        }
        double accumulation = 0;
        for (CarProbabaility car : probs) {
            car.prob = accumulation + (double) car.freqency / sum;
            accumulation = car.prob;
        }

        //loop over all house holds
        for (HouseHoldWithCars badCarHH : this.householdsWithBadCars) {
            //loop over all cars
            for (int i = 0; i < badCarHH.carIDs.length; ++i) {
                //is this a restricted car?
                if (this.carRestriction.get(badCarHH.carIDs[i])) {
                    double random = Math.random();
                    int id = badCarHH.carIDs[i];
                    for (CarProbabaility car : probs) {
                        if (car.prob > random) {
                            id = car.id;
                            numOfExchangedCars++;
                            break;
                        }
                    }

                    badCarHH.carIDs[i] = id;
                }
            }
            this.householdsToUpdate.add(badCarHH);
        }
        System.out.println("Exchanged cars: " + numOfExchangedCars);
        System.out.println("Households to update: " + this.householdsToUpdate.size());
    }

    public void updateHouseHolds(String table, String key) {
        String query = "";
        try {
            query = "UPDATE " + table + " set hh_car_ids =? where hh_key='" + key + "' and hh_id=?";

            PreparedStatement pSt = this.dbCon.getConnection(this).prepareStatement(query);
            int chunk = 0, chunksize = 10000;
            for (HouseHoldWithCars updateHH : this.householdsToUpdate) {
                Array sqlArray = this.dbCon.getConnection(this).createArrayOf("integer", updateHH.carIDs);
                pSt.setArray(1, sqlArray);
                pSt.setInt(2, updateHH.id);
                pSt.addBatch();
                chunk++;
                if (chunk == chunksize) { //commit chunk
                    pSt.executeBatch();
                    chunk = 0;
                }
            }
            pSt.executeBatch(); //commit remainers
            //clean up
            this.householdsToUpdate.clear();
            this.householdsWithBadCars.clear();
            this.householdsWithGoodCars.clear();
        } catch (SQLException e) {
            System.err.println("Error in sqlstatement: " + query);
            if (e.getNextException() != null) {
                e.getNextException().printStackTrace();
            }
            e.printStackTrace();
        }
    }

    class HouseHoldWithCars {
        int id = 0;
        Integer[] carIDs = new Integer[0];
    }

    class CarProbabaility {
        int id;
        int freqency;
        double prob;
    }

}
