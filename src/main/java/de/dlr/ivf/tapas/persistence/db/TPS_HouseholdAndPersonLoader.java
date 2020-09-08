package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.constants.TPS_LocationConstant;
import de.dlr.ivf.tapas.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.constants.TPS_Sex;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.loc.TPS_Region;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.StateMachineUtils;
import de.dlr.ivf.tapas.runtime.util.IPInfo;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TPS_HouseholdAndPersonLoader {

    private TPS_DB_IOManager pm;
    private TPS_Region region;

public TPS_HouseholdAndPersonLoader(TPS_DB_IOManager pm){

    this.pm = pm;
    this.region = pm.getRegion();
}

public List<TPS_Household> initAndGetHouseholds(){

    String hhtable = this.pm.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD);
    String persTable = this.pm.getParameters().getString(ParamString.DB_TABLE_PERSON);
    String carsTable = this.pm.getParameters().getString(ParamString.DB_TABLE_CARS);
    String hh_pers_key = this.pm.getParameters().getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY);

    String query = "";
    try {

        //check if simulation is running
        query = "SELECT sim_started FROM " + this.pm.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS) +
                " WHERE sim_key = '" + this.pm.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
        ResultSet sRs = pm.executeQuery(query);
        boolean sim_started = false;
        if (!sRs.next()) {
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "simulation key removed from db!");
        }else {
            sim_started = sRs.getBoolean("sim_started");
            sRs.close();
        }
        if (sim_started) {
            if (TPS_Logger.isLogging(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO))
                TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Loading all households, persons and cars");

            //fill car map
            Map<Integer, TPS_Car> carMap = new HashMap<>();
            query = "SELECT car_id,kba_no, fix_costs, engine_type, is_company_car, emission_type, " +
                    "restriction,automation_level FROM " + carsTable + " WHERE car_key='" +
                    this.pm.getParameters().getString(ParamString.DB_CAR_FLEET_KEY) + "'";
            initCars(carMap, query, pm);

            //get counts of households for initial size of household array
            query = "SELECT COUNT(*) cnt FROM "+hhtable+" WHERE hh_key = '"+hh_pers_key+"'";

            ResultSet rs = pm.executeQuery(query);
            int size = 0;
            if(rs.next()) {
                size = rs.getInt(1);
                rs.close();
            }
            List<TPS_Household>households = size > 0 ? new ArrayList<>(size) : new ArrayList<>();

            //now our query that will fetch all households including all persons belonging to each household
            //we read everything in one loop so the result set must be sorted by household ids
            query = "SELECT hh_id, hh_cars, hh_car_ids, hh_income, hh_taz_id, hh_type, ST_X(hh_coordinate) as x, ST_Y(hh_coordinate) as y, " +
                    "p_id, p_has_bike, p_sex, p_group, p_age, p_abo, p_budget_pt, p_budget_it, p_working, p_work_id, p_driver_license, p_hh_id, p_education FROM " + hhtable +" households "+
                    "INNER JOIN "+ persTable +" persons ON households.hh_id = persons.p_hh_id " +
                    "WHERE households.hh_key = '"+this.pm.getParameters().getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY)+"' " +
                    "AND persons.p_key = '"+this.pm.getParameters().getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY)+"' " +
                    "ORDER BY households.hh_id";

            //start fetching
            Connection con = this.pm.getDbConnector().getConnection(this);
            con.setAutoCommit(false);
            Statement st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.FETCH_FORWARD);
            st.setFetchSize(10000);
            rs = st.executeQuery(query);
            int last_hh_id = -2;
            int current_hh_id;
            TPS_Household hh = StateMachineUtils.EmptyHouseHold();
            while(rs.next()){

                //first create the household if it does not exist
                current_hh_id = rs.getInt("hh_id");
                if(current_hh_id != last_hh_id){
                    last_hh_id = current_hh_id;
                    // read cars
                    int carNum = rs.getInt("hh_cars");
                    TPS_Car[] cars = null;

                    if (carNum > 0) {
                        int[] carId = TPS_DB_IO.extractIntArray(rs.getArray("hh_car_ids").getArray());
                        if (carNum != carId.length) {
                            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.ERROR,
                                    "HH_id: " + current_hh_id + " expected cars: " + carNum + " found cars: " + carId.length);
                        }
                        // init the cars
                        cars = new TPS_Car[carId.length];
                        for (int i = 0; i < cars.length; ++i) {
                            cars[i] = new TPS_Car(carId[i]);
                            //store default values
                            cars[i].init(TPS_Car.FUEL_TYPE_ARRAY[1], 1, TPS_Car.EMISSION_TYPE_ARRAY[1], 0.0, false,
                                    false, this.pm.getParameters(), i);

                        }
                    }
                    // read other attributes
                    TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rs.getInt("hh_taz_id"));
                    int income = rs.getInt("hh_income"); // TODO: why int?
                    int type = rs.getInt("hh_type");
                    TPS_Location loc = new TPS_Location(-1 * current_hh_id, -1, TPS_LocationConstant.HOME, rs.getDouble("x"),
                            rs.getDouble("y"), taz, null, this.pm.getParameters());
                    loc.initCapacity(0, false);

                    hh = new TPS_Household(current_hh_id, income, type, loc, cars);

                    //during further loops the created household will have its members filled and when we get into this part again,
                    //we assign the cars to the previous household
                    if(households.size() > 0)
                        assignCarToLastHousehold(carMap, households.get(households.size()-1));
                    households.add(hh);
                }

                //now add all persons to the household
                boolean hasBike = rs.getBoolean("p_has_bike") && Randomizer.random() < this.pm.getParameters().getDoubleValue(
                        ParamValue.AVAILABILITY_FACTOR_BIKE);// TODO: make
                // a better model
                double working = rs.getInt("p_working") / 100.0;
                double budget = (rs.getInt("p_budget_it") + rs.getInt("p_budget_pt")) / 100.0;

                TPS_Person person = new TPS_Person(rs.getInt("p_id"), TPS_Sex.getEnum(rs.getInt("p_sex")),
                        TPS_PersonGroup.getPersonGroupByTypeAndCode(TPS_PersonGroup.TPS_PersonGroupType.TAPAS, rs.getInt("p_group")),
                        rs.getInt("p_age"), rs.getBoolean("p_abo"), hasBike, budget, working, false, rs.getInt("p_work_id"),
                        rs.getInt("p_education"), this.pm.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES));

                initPersonParams(person, rs);
                hh.addMember(person);
            }//asd
            rs.close();
            con.setAutoCommit(true);

            //now set the sim status to finished
            //TODO implement a more concise technique
            query = "UPDATE "+this.pm.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS)+" SET sim_finished = true, sim_progress = "+households.size()+", timestamp_finished = now()  WHERE sim_key = '"+this.pm.getParameters().getString(ParamString.RUN_IDENTIFIER) + "'";
            pm.execute(query);
            query = "UPDATE " + this.pm.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP) + " SET hh_started = true, hh_finished = true, server_ip = inet '"+ IPInfo.getEthernetInetAddress().getHostAddress()+"'";
            pm.execute(query);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Finished loading all households, persons and cars");
            return households;
        }

    } catch (SQLException e) {
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.ERROR, "error during one of th sqls: " + query,
                e);
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.ERROR, "next exception:", e.getNextException());
    } catch (IOException e) {
        e.printStackTrace();
    }
    return null;
}


    private void initPersonParams(TPS_Person person, ResultSet rs) throws SQLException {


        if (this.pm.getParameters().isTrue(ParamFlag.FLAG_USE_DRIVING_LICENCE)) {
            int licCode = rs.getInt("p_driver_license");
            if (licCode == 1) {
                person.setDrivingLicenseInformation(TPS_DrivingLicenseInformation.CAR);
            } else {
                person.setDrivingLicenseInformation(TPS_DrivingLicenseInformation.NO_DRIVING_LICENSE);
            }
        } else {
            person.setDrivingLicenseInformation(TPS_DrivingLicenseInformation.UNKNOWN);
        }

        // case for robotaxis: all if wanted
        boolean isCarPooler = Randomizer.random() < this.pm.getParameters().getDoubleValue(
                ParamValue.AVAILABILITY_FACTOR_CARSHARING);
        if (this.pm.getParameters().isFalse(ParamFlag.FLAG_USE_ROBOTAXI)) {
            // no robotaxis: must be able to drive a car and be older than MIN_AGE_CARSHARING
            isCarPooler &= person.getAge() >= this.pm.getParameters().getIntValue(ParamValue.MIN_AGE_CARSHARING) &&
                    person.mayDriveACar();
        }
        person.setCarPooler(isCarPooler);
    }

    private void initCars(Map<Integer,TPS_Car> carMap, String query, TPS_DB_IOManager pm) throws SQLException {
        ResultSet rs = pm.executeQuery(query);
        while (rs.next()) {
            TPS_Car tmp = new TPS_Car(rs.getInt("car_id"));
            int engineType = rs.getInt("engine_type");
            int emissionType = rs.getInt("emission_type");
            if (engineType >= 0 && engineType < TPS_Car.FUEL_TYPE_ARRAY.length && emissionType >= 0 &&
                    emissionType < TPS_Car.EMISSION_TYPE_ARRAY.length) {
                tmp.init(TPS_Car.FUEL_TYPE_ARRAY[engineType], rs.getInt("kba_no"),
                        TPS_Car.EMISSION_TYPE_ARRAY[emissionType], rs.getDouble("fix_costs"),
                        rs.getBoolean("is_company_car"), rs.getBoolean("restriction"),
                        this.pm.getParameters(), -1);
            }
            int automationLevel = rs.getInt("automation_level");
            if (this.pm.getParameters().getDoubleValue(ParamValue.GLOBAL_AUTOMATION_PROBABILITY) >
                    Math.random()) {
                automationLevel = this.pm.getParameters().getIntValue(ParamValue.GLOBAL_AUTOMATION_LEVEL);
            }
            tmp.setAutomation(automationLevel);
            carMap.put(tmp.getId(), tmp);
        }
        rs.close();
    }

    private void assignCarToLastHousehold(Map<Integer,TPS_Car> carMap, TPS_Household household){
        if (household.getMembers(TPS_Household.Sorting.NONE).size() > 0) {
            //get the car values
            for (int i = 0; i < household.getCarNumber(); ++i) {
                TPS_Car car = household.getCar(i);
                if (carMap.containsKey(car.getId())) {
                    car.cloneCar(carMap.get(car.getId()));
                } else {
                    if (TPS_Logger.isLogging(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.WARN)) {
                        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.WARN,
                                "Unknown car id " + car.getId() + " in household " + household.getId());
                    }
                }
            }
        }
    }
}