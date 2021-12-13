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
import de.dlr.ivf.tapas.execution.sequential.statemachine.util.StateMachineUtils;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles the loading process of all households and persons from the database. It also assigns the cars to every household
 *
 */

public class TPS_HouseholdAndPersonLoader {

    //The persistence manager
    private final TPS_DB_IOManager pm;

    //the region we are simulating
    private final TPS_Region region;

    public TPS_HouseholdAndPersonLoader(TPS_DB_IOManager pm) {

        this.pm = pm;
        this.region = pm.getRegion();
    }


    /**
     * 1 - Generates all households with their associated persons.
     * 2 - Assigns cars to households.
     *
     * @return a list of all households for a simulation run.
     */
    public List<TPS_Household> initAndGetAllHouseholds() {

        Map<Integer, TPS_Car> car_map = loadAndGetCars(pm);

        List<TPS_Household> households = loadAndGetAllHouseholds(pm);

        //assign cars to households
        households.stream().parallel().forEach(household -> {
            assignCarsToHousehold(car_map, household);
            initializeCarMediator(household);
        });

        return households;
    }

    /**
     * Sets some person specific parameters eg. possession of drivers license
     *
     * @param person the {@link TPS_Person}  to apply the parameters to
     * @param rs the {@link ResultSet} that provides the data
     * @throws SQLException in case of incorrect data access or connection loss
     */

    private void initPersonParams(TPS_Person person, ResultSet rs) throws SQLException {

        if (this.pm.getParameters().isTrue(ParamFlag.FLAG_USE_DRIVING_LICENCE)) {
            int licCode = rs.getInt("driver_license");
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


    /**
     * Loads all {@link TPS_Car} from the database.
     *
     * @param pm the persistence manager
     * @return a {@link Map} representation of all {@link TPS_Car}
     */

    private Map<Integer, TPS_Car> loadAndGetCars(TPS_DB_IOManager pm) {

        var cars_table = this.pm.getParameters().getString(ParamString.DB_TABLE_CARS);
        var car_fleet_key = this.pm.getParameters().getString(ParamString.DB_CAR_FLEET_KEY);

        String query = "SELECT car_id,kba_no, fix_costs, engine_type, is_company_car, emission_type, " +
                "restriction,automation_level FROM " + cars_table + " WHERE car_key='" + car_fleet_key + "'";

        Map<Integer, TPS_Car> car_map = new HashMap<>();

        try (ResultSet rs = pm.executeQuery(query)) {
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
                car_map.put(tmp.getId(), tmp);
            }
        } catch (SQLException e) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL, e);
        }


        return car_map;
    }


    /**
     * Generates all {@link TPS_Household} and all associated {@link TPS_Person} objects.
     * The data is being loaded from a database in one go with customized {@link ResultSet} parameters
     *
     * @param pm the persistence manager
     * @return a {@link List} of generated {@link TPS_Household}s
     */
    private List<TPS_Household> loadAndGetAllHouseholds(TPS_DB_IOManager pm) {

        String households_table = this.pm.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD);
        String households_subset_table = this.pm.getParameters().getString(ParamString.DB_TABLE_HOUSEHOLD_TMP);
        String persons_table = this.pm.getParameters().getString(ParamString.DB_TABLE_PERSON);
        String household_and_person_key = this.pm.getParameters().getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY);

        List<TPS_Household> households = null;

        try {
            //get counts of households for initial size of household array
            String query = "SELECT COUNT(*) cnt FROM " + households_table + " WHERE hh_key = '" + household_and_person_key + "'";

            ResultSet rs = pm.executeQuery(query);
            int household_count = 0;
            if (rs.next())
                household_count = rs.getInt(1);

            rs.close();

            //get hh sample size
            household_count = (int) (household_count * this.pm.getParameters().getDoubleValue(ParamValue.DB_HH_SAMPLE_SIZE));

            households = household_count > 0 ? new ArrayList<>(household_count) : new ArrayList<>();

            /*
              the query that will fetch all households including all persons belonging to each household
              we read everything in one loop so the result set must be sorted by household ids

              the structure of the following result set:
                  household_id | person_id | ...
                         1     |    1      | ...
                         1     |    2      | ...
                         1     |    3      | ...
                         2     |    4      | ...
             */

            query = "WITH households AS(" +
                    "SELECT hh_id, hh_cars, hh_key, hh_car_ids, hh_income, hh_taz_id, hh_type, ST_X(hh_coordinate) as x, ST_Y(hh_coordinate) as y " +
                    "FROM "+households_table+" WHERE hh_key ='"+household_and_person_key+"' ORDER BY RANDOM() LIMIT "+household_count+")" +
                    "SELECT households.*, p_id, has_bike, sex, \"group\", age, pt_abo, budget_pt, status,budget_it, working, driver_license, education FROM households " +
                    "INNER JOIN " + persons_table + " persons ON households.hh_id = persons.hh_id " +
                    "AND persons.key = households.hh_key " +
                    "ORDER BY households.hh_id";

            //set fetching parameters
            Connection con = this.pm.getDbConnector().getConnection(this);
            con.setAutoCommit(false);
            Statement st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.FETCH_FORWARD);
            st.setFetchSize(10000);

            rs = st.executeQuery(query);

            int last_hh_id = Integer.MIN_VALUE;
            int current_hh_id;

            TPS_Household household_in_process = StateMachineUtils.EmptyHouseHold();

            while (rs.next()) {

                current_hh_id = rs.getInt("hh_id");

                //check whether we hit a new household in the result set
                if (current_hh_id != last_hh_id) {
                    last_hh_id = current_hh_id;

                    // read cars for the new household
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
                    int income = rs.getInt("hh_income");
                    int type = rs.getInt("hh_type");
                    TPS_Location loc = new TPS_Location(-1 * current_hh_id, -1, TPS_LocationConstant.HOME, rs.getDouble("x"),
                            rs.getDouble("y"), taz, null, this.pm.getParameters());
                    loc.initCapacity(0, false);

                    household_in_process = new TPS_Household(current_hh_id, income, type, loc, cars);

                    households.add(household_in_process);
                }

                //now add all persons to the household
                boolean hasBike = rs.getBoolean("has_bike") && Randomizer.random() < this.pm.getParameters().getDoubleValue(
                        ParamValue.AVAILABILITY_FACTOR_BIKE);

                // a better model
                double working = rs.getInt("working") / 100.0;
                double budget = (rs.getInt("budget_it") + rs.getInt("budget_pt")) / 100.0;

                TPS_Person person = new TPS_Person(rs.getInt("p_id"),
                        rs.getInt("group"),
                        rs.getInt("status"),
                        TPS_Sex.getEnum(rs.getInt("sex")),
                        rs.getInt("age"),
                        rs.getBoolean("pt_abo"), hasBike, budget, working, false,
                        rs.getInt("education"), this.pm.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES));

                initPersonParams(person, rs);
                household_in_process.addMember(person);
            }

            rs.close();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return households;
    }

    /**
     * Assigns {@link TPS_Car} references to a given {@link TPS_Household}.
     *
     * @param carMap the {@link Map} containing all {@link TPS_Car} references.
     * @param household the {@link TPS_Household} to assign the cars to.
     */
    private void assignCarsToHousehold(Map<Integer, TPS_Car> carMap, TPS_Household household) {
        if (household.getMembers(TPS_Household.Sorting.NONE).size() > 0) {
            //get the car values
            for (int i = 0; i < household.getNumberOfCars(); ++i) {
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

    private void initializeCarMediator(TPS_Household household){

        TPS_HouseholdCarMediator car_mediator = new TPS_HouseholdCarMediator(household);
        household.setCarMediator(car_mediator);

    }
}