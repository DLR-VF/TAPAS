package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * This class creates a new table in the database based on the table with the individuals living
 * in the area under investigation. The new table adds to the existing one a fixed location
 * to individuals whose main activity is work, school, university or vocational training.
 * People from other groups are not assigned a fixed location.
 */
public class TPS_PrimaryActivityAssignment extends TPS_BasicConnectionClass {
    final int distanceBinSize;
    List<Person> persons = new ArrayList<>();
    Map<Integer, List<Location>> locations = new TreeMap<>();
    //general variables
    Map<Integer, Integer> globalLocationCapacity = new HashMap<>();
    List<String> keys_var = new ArrayList<>();
    //variables for school
    Map<Integer, Integer> globalSchoolCapacity = new HashMap<>();
    List<Integer> schoolType = Arrays.asList(1002002001, 1002002002, 1002002003, 1002002004, 1002002005);
    TPS_DiscreteDistribution<Integer> secSchoolDistribution;
    //variables for work
    Map<Integer, Integer> workplaceCapacity = new HashMap<>();
    Map<Integer, Integer> workplaceModelTest = new HashMap<>(); //this is used to test the workplace choice model
    Map<Integer, Map<Integer, List<Integer>>> workplaceMap = new TreeMap<>();
    Map<Integer, List<Integer>> workplaceTazIds = new TreeMap<>();
    int[][] distMatrix = new TPS_MatrixComparison().loadMatrix("core.berlin_matrices", "WALK_IHK_DIST");
    int[][] matrix_with_categories = new int[distMatrix.length][distMatrix[0].length];
    Map<Integer, List<Integer>> distanceCategories = new TreeMap<>();
    Integer[] workplaceKeys;
    //variables for tertiary education (university and vocational training)
    Map<Integer, Integer> tertiaryEduIDs = new HashMap<>();
    TPS_DiscreteDistribution<Integer> tertiaryEduDistribution;

    /**
     * This constructor overrides the file the with login information and
     * also initializes the distance of the bins for the length distribution for workers
     */
    public TPS_PrimaryActivityAssignment() {
        super("T:\\Simulationen\\runtime_argos_admin.csv");
        this.distanceBinSize = 500;
    }

    public static void main(String[] args) {
        TPS_PrimaryActivityAssignment myPerson = new TPS_PrimaryActivityAssignment();
        List<String> keys = myPerson.pKeyList("public.berlin_persons_copy");
        myPerson.createTable("public.berlin_persons_copy_fix_location");

        for (String key : keys) {
            System.out.println(key);
            myPerson = new TPS_PrimaryActivityAssignment();
            myPerson.readLocationsTable("core.berlin_locations_1223");
            myPerson.matrixConversion();
            myPerson.completeWorkplaceMap();
            myPerson.readPersonsTable("public.berlin_persons_copy", "core.berlin_households_1223", key);
            myPerson.selectBestLocation();
            myPerson.insertPersonsIntoTable("public.berlin_persons_copy_fix_location");
        }
    }

    /**
     * This method assigns the next school from their home with available places to pupils in primary school
     *
     * @param p
     */
    private void assignPrimarySchoolLocation(Person p) {
        double dist;
        double minDist = Double.MAX_VALUE;
        Location best = null;

        for (Location l : locations.get(1002001001)) {
            if (l.loc_capacity > 0) {
                if (p.loc_code == l.loc_code) {
                    dist = distMatrix[p.taz_id - 1][l.loc_taz_id - 1];
                    if (dist < minDist) {
                        minDist = dist;
                        p.p_loc_id = l.loc_id;
                        best = l;
                    }
                }
            }
        }
        if (best != null) {
            best.loc_capacity--;
            globalLocationCapacity.put(best.loc_code, globalLocationCapacity.get(best.loc_code) - 1);
        } else {
            System.out.println("Person " + p.p_id + " has no location for his activity " + p.loc_code);
        }
    }

    /**
     * This method assigns a random school from a set of the next five schools to students in secondary school
     *
     * @param p
     */
    private void assignSecondarySchoolLocation(Person p) {
        int randomKey = secSchoolDistribution.draw();
        p.loc_code = randomKey;
        int actCap = globalSchoolCapacity.get(p.loc_code) - 1;
        secSchoolDistribution = new TPS_DiscreteDistribution<>(globalSchoolCapacity);
        SortedMap<Integer, List<Integer>> secondarySchool = new TreeMap<>();
        List<Integer> secondarySchoolList = new ArrayList<>();
        int[] secondarySchoolKeys = new int[5];
        int i = 0;

        globalSchoolCapacity.put(p.loc_code, actCap);
        if (globalSchoolCapacity.get(p.loc_code) == 0) {
            globalSchoolCapacity.remove(p.loc_code);
        }

        for (Location l : locations.get(p.loc_code)) {
            int dist = distMatrix[p.taz_id - 1][l.loc_taz_id - 1];
            List<Integer> tempSecondarySchoolList = secondarySchool.get(dist);
            if (tempSecondarySchoolList == null) {
                tempSecondarySchoolList = new ArrayList<>();
            }
            tempSecondarySchoolList.add(l.loc_id);
            secondarySchool.put(dist, tempSecondarySchoolList);
        }
        forloop:
        for (Integer key : secondarySchool.keySet()) {
            if (i == 5) {
                break forloop;
            }
            secondarySchoolKeys[i] = key;
            i++;
        }
        for (Integer key : secondarySchoolKeys) {
            secondarySchoolList.addAll(secondarySchool.get(key));
        }
        p.p_loc_id = getRandomElement(secondarySchoolList);
    }

    /**
     * This method assigns a totally random location to students or trainees in tertiary education
     *
     * @param p
     */
    private void assignTertiaryEduLocation(Person p) {
        int randomId = tertiaryEduDistribution.draw();

        //assignation of random-location
        p.p_loc_id = randomId;
        int actCap = tertiaryEduIDs.get(p.p_loc_id) - 1;
        tertiaryEduIDs.put(p.p_loc_id, actCap);
        tertiaryEduDistribution = new TPS_DiscreteDistribution<Integer>(tertiaryEduIDs);
    }

    /**
     * A work location is assigned to each worker drawing a taz_id from the discrete probability distribution and then
     * choosing a random location from the set of locations within that TAZ. This process is repeated until a location
     * is assigned to a worker, always taking into account the capacities.
     * In addition, the number of people in each distance category is counted and stored as value in workplaceModelTest
     *
     * @param p
     * @param probDistribution
     */
    private void assignWorkplaceLocation(Person p, TPS_DiscreteDistribution<Integer> probDistribution) {
        List<Integer> tazIdsList;
        int randomKeyWorkplace;
        int tazID = 0;

        do {
            randomKeyWorkplace = probDistribution.draw();
            tazIdsList = workplaceMap.get(p.taz_id).get(workplaceKeys[randomKeyWorkplace]);
            if (tazIdsList != null) {
                tazID = getRandomElement(tazIdsList);
                if (workplaceTazIds.get(tazID) != null) {
                    p.p_loc_id = getRandomElement(workplaceTazIds.get(tazID));
                }
            }
        } while (p.p_loc_id == -1 || p.p_loc_id == 0);

        int capacity = workplaceCapacity.get(p.p_loc_id);
        capacity--;
        if (capacity == 0) {
            workplaceCapacity.remove(p.p_loc_id);
            Iterator<Integer> iter = workplaceTazIds.get(tazID).listIterator();
            while (iter.hasNext()) {
                int elem = p.p_loc_id;
                if (iter.next() == elem) {
                    iter.remove();
                }
            }
        } else {
            workplaceCapacity.put(p.p_loc_id, capacity);
        }

        Integer personsCounter = workplaceModelTest.get(workplaceKeys[randomKeyWorkplace]);
        if (personsCounter == 0) {
            personsCounter = 1;
        } else {
            personsCounter++;
        }
        workplaceModelTest.put(workplaceKeys[randomKeyWorkplace], personsCounter);
    }

    /**
     * Pre-step to the assignation of a work location
     */
    private void completeWorkplaceMap() {
        List<Integer> taz_id_list = new ArrayList<>(workplaceMap.keySet());
        for (Map.Entry<Integer, Map<Integer, List<Integer>>> workplaceMap_entry : workplaceMap.entrySet()) {
            for (int taz : taz_id_list) {
                List<Integer> taz_ids_list = workplaceMap_entry.getValue().get(
                        matrix_with_categories[workplaceMap_entry.getKey() - 1][taz - 1]);
                if (taz_ids_list == null) {
                    taz_ids_list = new ArrayList<>();
                }
                taz_ids_list.add(taz);
                workplaceMap_entry.getValue().put(matrix_with_categories[workplaceMap_entry.getKey() - 1][taz - 1],
                        taz_ids_list);
            }
        }
    }

    /**
     * This method creates a new empty table
     *
     * @param table
     */
    public void createTable(String table) {
        String query = "";
        try {
            query = "DROP TABLE IF EXISTS " + table;
            dbCon.execute(query, this);
            query = "CREATE TABLE " + table + " (" + "p_id integer NOT NULL, " + "p_hh_id integer NOT NULL, " +
                    "p_group integer, " + "p_sex integer, " + "p_age integer, " + "p_age_stba integer, " +
                    "p_work_id integer, " + "p_working integer, " + "p_abo integer, " + "p_budget_pt integer, " +
                    "p_budget_it integer, " + "p_budget_it_fi integer, " + "p_key character varying NOT NULL, " +
                    "p_driver_license integer, " + "p_has_bike boolean, " + "p_education integer, " +
                    "p_professional integer, " + "p_loc_id integer, " +
                    "CONSTRAINT berlin_persons_fix_location_pkey PRIMARY KEY(p_id,p_hh_id,p_key)); " + "ALTER TABLE " +
                    table + " OWNER TO tapas_admin_group; " + "GRANT ALL ON TABLE " + table +
                    " TO tapas_admin_group; " + "GRANT ALL ON TABLE " + table + " TO tapas_user_group;";

            dbCon.execute(query, this);

        } catch (Exception e) {
            System.err.println(this.getClass().getCanonicalName() + " SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

    /**
     * This method returns a random integer from a list
     *
     * @param list
     * @return
     */
    public int getRandomElement(List<Integer> list) {
        Random rand = new Random();
        if (!list.isEmpty()) {
            return list.get(rand.nextInt(list.size()));
        } else {
            return 0;
        }
    }

    /**
     * The table created in the last step has to be filled with all the individuals, like in the original table, plus
     * a fixed location for workers, pupils, students and trainees
     *
     * @param table
     */
    public void insertPersonsIntoTable(String table) {
        String query = "";

        try {
            query = "INSERT INTO " + table +
                    " (p_id, p_hh_id, p_group, p_sex, p_age, p_age_stba, p_work_id, p_working, p_abo, p_budget_pt," +
                    " p_budget_it, p_budget_it_fi, p_key, p_driver_license, p_has_bike, p_education, p_professional, p_loc_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            PreparedStatement ps = dbCon.getConnection(this).prepareStatement(query);
            int pos;
            int count = 0, chunk = 1024 * 16;
            for (Person p : persons) {
                pos = 1;
                ps.setInt(pos++, p.p_id);
                ps.setInt(pos++, p.p_hh_id);
                ps.setInt(pos++, p.p_group);
                ps.setInt(pos++, p.p_sex);
                ps.setInt(pos++, p.p_age);
                ps.setInt(pos++, p.p_age_stba);
                ps.setInt(pos++, p.p_work_id);
                ps.setInt(pos++, p.p_working);
                ps.setInt(pos++, p.p_abo);
                ps.setInt(pos++, p.p_budget_pt);
                ps.setInt(pos++, p.p_budget_it);
                ps.setInt(pos++, p.p_budget_it_fi);
                ps.setString(pos++, p.p_key);
                ps.setInt(pos++, p.p_driver_license);
                ps.setBoolean(pos++, p.p_has_bike);
                ps.setInt(pos++, p.p_education);
                ps.setInt(pos++, p.p_professional);
                ps.setInt(pos++, p.p_loc_id);
                ps.addBatch();
                count++;
                if (count % chunk == 0) {
                    ps.executeBatch();
                    System.out.println("Stored " + count + " persons.");
                }
            }
            ps.executeBatch();
            System.out.println("Stored " + count + " persons.");
        } catch (SQLException e) {
            System.err.println(this.getClass().getCanonicalName() + " SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

    private void matrixConversion() {
        for (int i = 0; i < distMatrix.length; i++) {
            for (int j = 0; j < distMatrix[i].length; j++) {
                matrix_with_categories[i][j] = whichClass(distMatrix[i][j]);
            }
        }
    }

    /**
     * Since we have keys in our tables to differentiate projects or data sources, we want to do the process
     * for each key separately
     */
    public List<String> pKeyList(String table) {
        String query = "";
        try {
            query = "SELECT DISTINCT p_key FROM " + table;
            ResultSet rs = dbCon.executeQuery(query, this);
            while (rs.next()) {
                String s = rs.getString("p_key");
                keys_var.add(s);
            }
        } catch (Exception e) {
            System.err.println("Error: " + query);
            e.printStackTrace();
        }
        return keys_var;
    }

    /**
     * This method creates a TreeMap (distanceCategories) with the mean value of distance categories as keys and
     * null as values (for now). First of all, the maximum distance between TAZs (Traffic Analysis Zones) is calculated.
     * This is going to be the maximum value used for the categories. To test the workplace choice model another map
     * is created (workplaceModelTest). The keys are the same as in the TreeMap and the values are (for now) set to 0.
     */
    private void prepareDistanceCategories() {
        int maxDistBetweenTAZ = Integer.MIN_VALUE;
        int tmpDist = 0;
        int catNum = maxDistBetweenTAZ / this.distanceBinSize;

        for (int i = 0; i < distMatrix.length; i++) {
            for (int j = 0; j < distMatrix[0].length; j++) {
                int distBetweenTAZ = distMatrix[i][j];
                if (distBetweenTAZ > maxDistBetweenTAZ) {
                    maxDistBetweenTAZ = distBetweenTAZ;
                }
            }
        }
        for (int i = 0; i <= catNum; i++) {
            int midDist = (tmpDist + (tmpDist + this.distanceBinSize)) / 2;
            distanceCategories.put(midDist, null);
            workplaceModelTest.put(midDist, 0);
            tmpDist += this.distanceBinSize;
        }
        System.out.println(distanceCategories);
    }

    /**
     * This method creates a discrete probability distribution based on the capacities of the different types
     * of secondary schools (according to the German system)
     */
    private void prepareSecondarySchoolLocations() {
        secSchoolDistribution = new TPS_DiscreteDistribution<>(globalSchoolCapacity);
    }

    /**
     * This method creates a discrete probability distribution based on the capacities of the different locations
     * for tertiary education
     */
    private void prepareTertiaryEduLocations() {
        tertiaryEduDistribution = new TPS_DiscreteDistribution<>(tertiaryEduIDs);
    }

    /**
     * This method creates a discrete probability distribution based on the workplace choice model
     *
     * @return
     */
    private TPS_DiscreteDistribution<Integer> probDistributionWorkplace() {
        double beta1 = 0.108801184642458;
        double beta2 = 0.229681522400213;
        workplaceKeys = distanceCategories.keySet().toArray(new Integer[0]);
        Double[] workplaceValuesDist = new Double[distanceCategories.size()];

        //fill the array
        for (int i = 0; i < distanceCategories.size(); i++) {
            workplaceValuesDist[i] = Math.exp(-beta1 * (workplaceKeys[i] / 1000)) - Math.exp(
                    -beta2 * (workplaceKeys[i] / 1000));
        }
        return new TPS_DiscreteDistribution<>(new ArrayList<>(distanceCategories.keySet()),
                Arrays.asList(workplaceValuesDist));
    }

    /**
     * This method implies different actions in relation to the locations of activities
     * (e.g., workplaces, shops, schools). First of all, the table with the locations is read
     * from the database. Every location is stored in a map (locations) where the key is the type of location (loc_code)
     * and the value is a list of locations of the corresponding type.
     * workplaceMap is a TreeMap with the taz_id as key and a map with the distance categories (mean value) as key and
     * a list (for now) set to null as value.
     * In addition, the capacities of the locations are stored in different maps. There is a map for all the locations
     * together and maps for schools, for workplaces and for tertiary education.
     * workplaceTazIds is a map with lists of locations according to the TAZ where they are located
     *
     * @param table this is the table with the locations of activities
     */
    public void readLocationsTable(String table) {
        String query = "";
        int locCounter = 0;
        try {
            query = "WITH l AS (SELECT loc_id, loc_code, loc_type, ST_X(loc_coordinate) AS lon, " +
                    "ST_Y(loc_coordinate) AS lat, loc_taz_id, loc_capacity FROM " + table + ") " +
                    "SELECT lon, lat, loc_capacity, loc_taz_id, loc_id, loc_code, loc_type FROM l";

            ResultSet rs = dbCon.executeQuery(query, this);

            while (rs.next()) {
                Location location = new Location();
                location.loc_id = rs.getInt("loc_id");
                location.loc_code = rs.getInt("loc_code");
                location.loc_type = rs.getString("loc_type");
                location.loc_capacity = rs.getInt("loc_capacity");
                location.x = rs.getDouble("lon");
                location.y = rs.getDouble("lat");
                location.loc_taz_id = rs.getInt("loc_taz_id");

                if (workplaceMap.get(location.loc_taz_id) == null) {
                    Map<Integer, List<Integer>> tempDistanceCategories = new TreeMap<>(distanceCategories);
                    workplaceMap.put(location.loc_taz_id, tempDistanceCategories);
                }

                List<Location> tmpList = this.locations.computeIfAbsent(location.loc_code, k -> new ArrayList<>());
//                List<Location> tmpList = this.locations.get(location.loc_code);
//                if(tmpList == null){
//                    tmpList = new ArrayList<>();
//                    this.locations.put(location.loc_code,tmpList);
//                }
                tmpList.add(location);

                // capacities for all locations
                int cappa = location.loc_capacity;
                if (globalLocationCapacity.containsKey(location.loc_code)) {
                    cappa += globalLocationCapacity.get(location.loc_code);
                }
                globalLocationCapacity.put(location.loc_code, cappa);

                // capacities for workplace locations and fill workplaceTazIds
                List<Integer> locIdsList = workplaceTazIds.get(location.loc_taz_id);
                if (location.loc_code == 1001000000 && location.loc_capacity > 0) {
                    workplaceCapacity.put(location.loc_id, location.loc_capacity);
                    if (locIdsList == null) {
                        locIdsList = new ArrayList<>();
                        workplaceTazIds.put(location.loc_taz_id, locIdsList);
                    }
                    locIdsList.add(location.loc_id);
                }

                // capacities for tertiary education
                if (location.loc_code == 1002004002 || location.loc_code == 1002004001) {
                    tertiaryEduIDs.put(location.loc_id, location.loc_capacity);
                }
                locCounter++;
            }
            rs.close();
            System.out.println("Number of locations = " + locCounter);

            //removes the codes with 0 capacity
            Iterator<Map.Entry<Integer, Integer>> it = globalLocationCapacity.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> entry = it.next();
                if (entry.getValue() == 0) {
                    it.remove();
                }
            }

            //creates a Map for the school codes and capacities
            for (Map.Entry<Integer, Integer> entry : globalLocationCapacity.entrySet()) {
                if (schoolType.contains(entry.getKey())) {
                    globalSchoolCapacity.put(entry.getKey(), entry.getValue());
                }
            }

            //pre-step of random-assignation of loc_codes for pupils in secondary schools
            prepareSecondarySchoolLocations();

            //pre-step of random-assignation of loc_ids for students in tertiary education
            prepareTertiaryEduLocations();

            //pre-step to prepare data containers for distance related information for workplace assignment
            prepareDistanceCategories();
        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " readlocationsTable: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

    /**
     * This method reads the table with the individuals along with the table with the households (needed to get
     * the coordinates of the household) from the database. Everyone is stored in a list of the type Person (persons)
     *
     * @param p_table
     * @param hh_table
     * @param key
     */
    public void readPersonsTable(String p_table, String hh_table, String key) {
        persons.clear();
        String query = "";
        try {
            final int chunks = 20;
            int count = 0;
            for (int counter = 0; counter < chunks; counter++) {
                query = "WITH p AS (SELECT p_id, p_hh_id, p_group, p_sex, p_age, p_age_stba, p_work_id, p_working, " +
                        "p_abo, p_budget_pt, p_budget_it, p_budget_it_fi, p_key, p_driver_license, p_has_bike, p_education, p_professional " +
                        "FROM " + p_table + " WHERE p_key = '" + key + "'), " +
                        "h AS (SELECT hh_id, ST_X(hh_coordinate) AS lon, ST_Y(hh_coordinate) AS lat, hh_taz_id FROM " +
                        hh_table + " WHERE hh_key = '" + key + "') " +
                        "SELECT p_id, p_hh_id, p_group, p_sex, p_age, p_age_stba, p_work_id, p_working, p_abo, p_budget_pt, p_budget_it, " +
                        "p_budget_it_fi, p_key, p_driver_license, p_has_bike, p_education, p_professional, lon, lat, hh_taz_id " +
                        "FROM p, h WHERE p.p_hh_id = h.hh_id AND p.p_id%" + chunks + " = " + counter;

                ResultSet rs = dbCon.executeQuery(query, this);

                while (rs.next()) {
                    Person person = new Person();
                    person.p_id = rs.getInt("p_id");
                    person.p_hh_id = rs.getInt("p_hh_id");
                    person.p_group = rs.getInt("p_group");
                    person.p_sex = rs.getInt("p_sex");
                    person.p_age = rs.getInt("p_age");
                    person.p_age_stba = rs.getInt("p_age_stba");
                    person.p_work_id = rs.getInt("p_work_id"); //-1
                    person.p_working = rs.getInt("p_working");
                    person.p_abo = rs.getInt("p_abo");
                    person.p_budget_pt = rs.getInt("p_budget_pt");
                    person.p_budget_it = rs.getInt("p_budget_it");
                    person.p_budget_it_fi = rs.getInt("p_budget_it_fi");
                    person.p_key = rs.getString("p_key");
                    person.p_driver_license = rs.getInt("p_driver_license");
                    person.p_has_bike = rs.getBoolean("p_has_bike");
                    person.p_education = rs.getInt("p_education");
                    person.p_professional = rs.getInt("p_professional");
                    person.p_loc_id = -1;
                    person.x = rs.getDouble("lon");
                    person.y = rs.getDouble("lat");
                    person.taz_id = rs.getInt("hh_taz_id");
                    person.loc_code = ((person.p_group == 3) || (person.p_group == 4) || (person.p_group == 5) ||
                            (person.p_group == 9) || (person.p_group == 10) || (person.p_group == 11) ||
                            (person.p_group == 12) || (person.p_group == 17) || (person.p_group == 18) ||
                            (person.p_group == 19) || (person.p_group == 20)) ? 1001000000 //Workers
                            : ((person.p_group == 1) && (person.p_age <= 12)) ? 1002001001 //elementary school
                            : ((person.p_group == 2) &&
                            ((person.p_education == 1) || (person.p_education == 2) || (person.p_education == 3))) ||
                            ((person.p_group == 2) &&
                                    (person.p_working != 0)) ? 1002004001 //Vocational training - stimmt das??
                            : ((person.p_group == 2) && (person.p_working == 0)) ? 1002004002 //Students
                            : ((person.p_group == 1) && (person.p_age > 12)) ? 0 //secondary school
                            : -1;
                    persons.add(person);

                    count++;
                    if (count % 10000 == 0) {
                        System.out.println("Chunk: " + counter + " read " + count + " persons.");
                    }
                }
                rs.close();
                System.out.println("Chunk: " + counter + " read " + count + " persons.");
            }
        } catch (SQLException e) {
            System.err.println(
                    this.getClass().getCanonicalName() + " readPersonsTable: SQL-Error during statement: " + query);
            e.printStackTrace();
        }
    }

    /**
     * This method sums up all the methods used to select a proper location, considering the status (worker, student,...)
     * of the individual
     */
    public void selectBestLocation() {
        int count = 0;
        TPS_DiscreteDistribution<Integer> workplaceDistribution = probDistributionWorkplace();

        for (Person p : persons) {
            if (p.p_loc_id != -1) // do not process persons with a fixed location
                continue;
            switch (p.loc_code) {
                case 1002001001:
                    assignPrimarySchoolLocation(p); //primary school
                    break;
                case 0:
                    assignSecondarySchoolLocation(p); //secondary school
                    break;
                case 1002004002:
                    assignTertiaryEduLocation(p); //students
                    break;
                case 1001000000:
                    assignWorkplaceLocation(p, workplaceDistribution); //workers
                    break;
                case 1002004001:
                    assignTertiaryEduLocation(p); //vocational training
                    break;
            }
            count++;
            if (count % 100 == 0) System.out.println("Processed persons: " + count);
        }
        System.out.println("Processed persons: " + count);
    }

    /**
     * This two methods convert the values in meters of the distance matrix (matrix with distances between TAZs) to
     * categories; this means, mean values of the distance classes
     */
    private int whichClass(int a) {
        return (a / this.distanceBinSize * this.distanceBinSize) +
                (this.distanceBinSize / 2); //integer effect! division always floors to the next number
    }

    /**
     * This class represents an individual plus the xy-coordinates of their household
     */
    class Person {
        //columns of table
        int p_id;
        int p_hh_id;
        int p_group;
        int p_sex;
        int p_age;
        int p_age_stba;
        int p_work_id;
        int p_working;
        int p_abo;
        int p_budget_pt;
        int p_budget_it;
        int p_budget_it_fi;
        String p_key;
        int p_driver_license;
        Boolean p_has_bike;
        int p_education;
        int p_professional;
        int p_loc_id;
        //extra-needed-variables
        double x;
        double y;
        int taz_id;
        int loc_code;
    }

    class Location {
        int loc_id;
        int loc_code;
        String loc_type;
        int loc_capacity;
        double x;
        double y;
        int loc_taz_id;
    }
}