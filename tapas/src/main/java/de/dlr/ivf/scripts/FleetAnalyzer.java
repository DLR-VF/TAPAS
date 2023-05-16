/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.scripts;

import de.dlr.ivf.tapas.model.mode.ModeUtils;
import de.dlr.ivf.tapas.model.vehicle.CarSize;
import de.dlr.ivf.tapas.model.vehicle.FuelTypeName;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;


import java.util.*;
import java.util.Map.Entry;

/**
 * This script was used in the project of ecoMove to analyze a new car fleet.
 * Kept for historic reasons.
 */

public class FleetAnalyzer  {

    String key = "";
    String hh_key = "";
    String fleet_key = "";
    String regionName = "";
    int meterStepsForHistogram = 500;
    Map<EcoMoveCar, Double> segmentDistances = new HashMap<>();
    Map<EcoMoveCar, Integer> segmentCount = new HashMap<>();
    Map<EcoMoveCar, Integer> segmentCars = new HashMap<>();
    Map<Integer, EcoMoveCar> carFleet = new HashMap<>();
    Map<Integer, Integer> segmentCarTrips = new HashMap<>();
    Map<Integer, Integer> distHistogram = new HashMap<>();
    List<EcoMoveCar> knownCarTypes = new ArrayList<>();
    List<Trip> trips = new LinkedList<>();
    int tripCounter = 0;

    /**
     * @param args
     */
    public static void main(String[] args) {
        FleetAnalyzer worker = new FleetAnalyzer();
        worker.key = args[0];
        worker.readParameters();
        worker.readTrips();
        worker.analyzeSegments();
        worker.printResults();

    }

    public void analyzeSegments() {
        for (Trip trip : trips) {
            if (trip.mode != 2) {
                continue;
            }
            CarSize size = ModeUtils.getCarSize(trip.car_kba);
            FuelTypeName fuel = trip.fuelType;
            EcoMoveCar ref = null;
            boolean ecoCar = trip.car_kba >= 100;
            for (EcoMoveCar car : this.knownCarTypes) {
                if (car.fuelType.equals(fuel) && car.size.equals(size) && car.ecoCar == ecoCar) {
                    ref = car;
                    break;
                }
            }
            //new car found
            if (ref == null) {
                ref = new EcoMoveCar();
                ref.fuelType = fuel;
                ref.size = size;
                ref.ecoCar = ecoCar;
                //store new element in lists/maps
                this.knownCarTypes.add(ref);
                this.segmentCount.put(ref, 0);
                this.segmentDistances.put(ref, 0.0);

            }

            //adopt numbers
            this.segmentCount.put(ref, this.segmentCount.get(ref) + 1);
            this.segmentDistances.put(ref, this.segmentDistances.get(ref) + trip.dist);
            int tripByThisCar = 1;
            if (this.segmentCarTrips.containsKey(trip.car_id)) {
                tripByThisCar += this.segmentCarTrips.get(trip.car_id);
            }
            segmentCarTrips.put(trip.car_id, tripByThisCar);
            this.carFleet.put(trip.car_id, ref);

            int histogammSegment = (int) (trip.dist + 0.5); // incl. round
            histogammSegment -= histogammSegment % meterStepsForHistogram; // drop the remainer
            int number = 1;
            if (this.distHistogram.containsKey(histogammSegment)) {
                number = this.distHistogram.get(histogammSegment) + 1;
            }
            this.distHistogram.put(histogammSegment, number);
        }

        for (Entry<Integer, EcoMoveCar> entry : this.carFleet.entrySet()) {
            EcoMoveCar ref = entry.getValue();
            int count = 1;
            if (this.segmentCars.containsKey(ref)) {
                count += this.segmentCars.get(ref);
            }
            this.segmentCars.put(ref, count);
        }

    }

    public void printResults() {
        System.out.println("Car\tDistances\tCount\tCars");
        for (Entry<EcoMoveCar, Double> entry : this.segmentDistances.entrySet()) {
            int count = this.segmentCount.get(entry.getKey());
            int countCars = this.segmentCars.get(entry.getKey());
            System.out.printf("%s %s %s\t%.3f\t%d\t%d\n", entry.getKey().size.toString(),
                    entry.getKey().fuelType.toString(), entry.getKey().ecoCar ? "ecoCar" : "NonEco",
                    entry.getValue() / 1000.0, count, countCars);
        }
        System.out.println("Distance\tCount");
        for (Entry<Integer, Integer> entry : this.distHistogram.entrySet()) {
            System.out.printf("%d\t%d\n", entry.getKey(), entry.getValue());
        }
    }

    public void readParameters() {
        String query = "";
//        try {
//            query = "SELECT param_key , param_value FROM simulation_parameters WHERE sim_key= '" + this.key + "'";
//            ResultSet rs = dbCon.executeQuery(query, this);
//
//            while (rs.next()) {
//                if (rs.getString("param_key").equals("DB_HOUSEHOLD_AND_PERSON_KEY")) {
//                    hh_key = rs.getString("param_value");
//                }
//
//                if (rs.getString("param_key").equals("DB_CAR_FLEET_KEY")) {
//                    fleet_key = rs.getString("param_value");
//                }
//
//                if (rs.getString("param_key").equals("DB_REGION")) {
//                    regionName = rs.getString("param_value");
//                }
//            }
//
//        } catch (SQLException e) {
//            System.err.println(
//                    this.getClass().getCanonicalName() + " readParameters: SQL-Error during statement: " + query);
//            e.printStackTrace();
//        }
    }

    public void readTrips() {
        String query = "";
//        try {
//            boolean recordsFound;
//            int step = 0;
//            int chunk = 1000000;
//            int pID = -1, hhID = -1;
//            do {
//                recordsFound = false;
//                query = "SELECT trip.p_id, trip.hh_id, trip.start_time_min, trip.mode, trip.car_type, trip.distance_real_m as dist,  cars.kba_no, cars.engine_type FROM " +
//                        regionName + "_trips_" + key + " as trip LEFT OUTER JOIN core." + regionName +
//                        "_cars as cars ON (trip.car_type=cars.car_id AND cars.car_key='" + fleet_key +
//                        "') WHERE trip.mode =2 ORDER BY hh_id, p_id, start_time_min LIMIT " + chunk + " OFFSET " +
//                        (step * chunk);
//                ResultSet rs = dbCon.executeQuery(query, this);
//                step++;
//                while (rs.next()) {
//                    recordsFound = true;
//                    int actPID = rs.getInt("p_id");
//                    int actHHID = rs.getInt("hh_id");
//                    if (pID != actPID || hhID != actHHID) {
//                        this.tripCounter++;
//                        pID = actPID;
//                        hhID = actHHID;
//                    }
//                    Trip trip = new Trip(actPID, actHHID, rs.getInt("start_time_min"));
//                    trip.car_id = rs.getInt("car_type");
//                    trip.car_kba = rs.getInt("kba_no");
//                    trip.fuelType = TPS_Car.FUEL_TYPE_ARRAY[rs.getInt("engine_type")];
//                    trip.dist = rs.getDouble("dist");
//                    trip.mode = rs.getInt("mode");
//
//                    this.trips.add(trip);
//
//                }
//                rs.close();
//            } while (recordsFound);
//
//        } catch (SQLException e) {
//            System.err.println(this.getClass().getCanonicalName() + " readTrips: SQL-Error during statement: " + query);
//            e.printStackTrace();
//        }
    }

    public class EcoMoveCar {
        CarSize size;
        boolean ecoCar = false;
        FuelTypeName fuelType;
    }

    public class Trip {
        int pID, hhID, startTime;
        int mode, car_id, car_kba;
        FuelTypeName fuelType;
        double dist;

        public Trip(int pID, int hhID, int startTime) {
            this.pID = pID;
            this.hhID = hhID;
            this.startTime = startTime;
        }
    }

}