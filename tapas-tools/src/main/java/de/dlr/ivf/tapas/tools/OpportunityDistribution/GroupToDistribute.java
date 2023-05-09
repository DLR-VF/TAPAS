/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public class GroupToDistribute {

    private static final String opportunityDistributionTableSuffix = "_opportunity_distribution";
    private static final String dlm_table = "core.gis_dlm_fot";
    private static final String preFixLocation = "location_";
    private static final String preFixDLM = "dlm_";
    List<LocationInDB> opportunities = new LinkedList<>();
    HashMap<Integer, LocationInDB> opportunityOutput = new HashMap<>();
    HashMap<Integer, BlockInDB> blocks = new HashMap<>();
    HashMap<String, ConversionUnitToCapacity> factors = new HashMap<>();
    int locID;
    int newID;
    int workEnterpriseId;
    String region;
    int totalNumerOfPickedOpportunities = 0;
    private HashMap<Integer, String> sqlQueryForRegions = new HashMap<>();

    public GroupToDistribute(String region) {
        this.region = region;

        //init the id and enterprise counter
        String query = "";
//todo revise this
//        try {
//            //get the max id
//            query = "SELECT loc_id FROM core." + region + "_locations ORDER BY loc_ID DESC LIMIT 1";
//            ResultSet rs = dbCon.executeQuery(query, this);
//            //get the data and store them internally, to avoid database conflicts with nested requests.
//            if (rs.next()) {
//                this.locID = rs.getInt("loc_id");
//                this.newID = this.locID + 1;
//
//            }
//            rs.close();
//
//            //get the max enterprise even if it is deprecated
//            query = "SELECT DISTINCT loc_enterprise FROM core." + region + "_locations WHERE loc_code = 4";
//            rs = dbCon.executeQuery(query, this);
//            //get the data and store them internally, to avoid database conflicts with nested requests.
//            while (rs.next()) {
//                //get the max number after the starting 'w', unfortunately 1000000 is "bigger" than 9, this we have to search the whole record set
//                this.workEnterpriseId = Math.max(this.workEnterpriseId,
//                        Integer.parseInt(rs.getString("loc_enterprise").substring(1)));
//            }
//            rs.close();
//            //increment
//            this.workEnterpriseId++;
//
//            query = " SELECT type, unit, work_factor_min, work_factor_max, person_factor_min, person_factor_max FROM core.global_unit_to_capacity_factors";
//            rs = dbCon.executeQuery(query, this);
//            //get the data and store them internally, to avoid database conflicts with nested requests.
//            while (rs.next()) {
//                ConversionUnitToCapacity thisOne = new ConversionUnitToCapacity(rs);
//
//
//                this.factors.put(thisOne.getKeyValue(), thisOne);
//            }
//            rs.close();
//
//
//        } catch (SQLException e) {
//            System.err.println(
//                    this.getClass().getCanonicalName() + " GroupToDistribute: SQL-Error during statement: " + query);
//            e.printStackTrace();
//
//        }
    }

    public static String generateConversionUnitToCapacityKey(String type, String unit) {
        if (type != null && unit != null) return type + "-" + unit;
        else return null;
    }

    public void adaptCapacities() {
        String query = "";
        //todo revise this
//        try {
//            query = "SELECT loc_id, loc_blk_id, loc_taz_id, loc_code, loc_type, loc_unit, loc_capacity, st_X(loc_coordinate) AS x, st_Y(loc_coordinate) AS y FROM core." +
//                    this.region + "_locations";
//            ResultSet rs = dbCon.executeQuery(query, this);
//            while (rs.next()) {
//                this.opportunities.add(new LocationInDB(rs));
//            }
//            rs.close();
//
//            for (LocationInDB loc : this.opportunities) {
//                String key = generateConversionUnitToCapacityKey(loc.getType(), loc.getUnit());
//                if (key != null) {
//                    loc.setCapacity(this.factors.get(key).getCapacity(loc, false));
//                } else {
//                    System.out.println("No key for location " + loc.getId());
//                }
//            }
//
//        } catch (SQLException e) {
//            System.err.println(
//                    this.getClass().getCanonicalName() + " initOpportunities: SQL-Error during statement: " + query);
//            e.printStackTrace();
//        }
    }

    //TODO: make this more sophisticated
    private int areaToOpportunity(int code, double area, boolean workPlace) {
        double factor;
        switch (code) {
            case 2111: //Residential area
                factor = 0.1;
                break;
            case 2112: //Industrial area
                factor = 0.4;
                break;
            case 2113: //Mixed area
                factor = 0.3;
                break;
            case 2114: //Special area (Hospitals, City Hall etc.)
                factor = 0.2;
                break;
            default: // Should never happen
                factor = 0.1;
                break;
        }
        return (int) (factor * area / 1000);
    }

    //TODO: make this sophisticated!
    private int capacityToOpportunity(LocationInDB loc, boolean workPlace) {
        if (loc.getType() != null && loc.getUnit() != null) {
            String key = generateConversionUnitToCapacityKey(loc.getType(), loc.getUnit());
            if (key != null) {
                ConversionUnitToCapacity converter = this.factors.get(key);
                return converter.getCapacity(loc, workPlace);
            }
        }
        return 0;
    }

    public void exportOpportunityOutputToCSV(String fileName) {
        try {
            FileWriter out = new FileWriter(fileName);
            for (Entry<Integer, LocationInDB> entry : this.opportunityOutput.entrySet()) {
                LocationInDB tmp = entry.getValue();
                int actID = newID++; //generate a new id for this new location

                String locationDescription =
                        actID + ";" + tmp.getCode() + ";" + tmp.getTaz_id() + ";" + tmp.getBlk_id() + ";" +
                                tmp.getCapacity() + ";" + tmp.getCoordinate()[0] + ";" + tmp.getCoordinate()[1];

                out.write(locationDescription + "\n");
            }
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void exportUpdatedCapacitiesToCSV(String fileName) {
        try {
            FileWriter out = new FileWriter(fileName);
            out.write("loc_id;loc_capacity\n");
            for (LocationInDB loc : this.opportunities) {

                out.write(loc.getId() + ";" + loc.getCapacity() + "\n");
            }
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillLocationList(int map_id, int volume, int[] locationCodes) {
        StringBuilder query = new StringBuilder();
        //todo revise this
//        try {
//            if (locationCodes != null) {
//                //we go for location codes!
//                query = new StringBuilder(
//                        "SELECT loc_id, loc_blk_id, loc_taz_id, loc_code, loc_type, loc_unit, loc_capacity, st_X(loc_coordinate) AS x, st_Y(loc_coordinate) AS y FROM core." +
//                                this.region + "_locations " + "WHERE ST_WITHIN(loc_coordinate,(" +
//                                this.sqlQueryForRegions.get(map_id) + ")) AND " + "loc_code = ANY(ARRAY[");
//                for (int i = 0; i < locationCodes.length; ++i) {
//                    query.append(i).append(",");
//                }
//                query.deleteCharAt(query.length() - 1); //delete last comma
//                query.append("])");
//                ResultSet rs = dbCon.executeQuery(query.toString(), this);
//                while (rs.next()) {
//                    LocationInDB loc = new LocationInDB(rs);
//
//                    int numberOfOpportunities = capacityToOpportunity(loc, true);
//                    //now add this locationID as often as we have this opportunities
//                    for (int i = 0; i < numberOfOpportunities; ++i)
//                        this.opportunities.add(loc);
//                }
//                rs.close();
//
//                volume = pickOpportunitiesFromList(volume, preFixLocation);
//            }
//
//            //we go for a dlm-request or have to fill up opportunities with dlm data
//            if (volume > 0) { //something to do!
//                //select all areas from dlm within the given region
//                query = new StringBuilder("SELECT gid, objart, area(the_geom) as area FROM " + dlm_table +
//                        " WHERE ST_WITHIN(the_geom, ST_TRANSFORM((" + this.sqlQueryForRegions.get(map_id) +
//                        "),31467))  AND objart >=2111 AND objart<=2114");
//                ResultSet rs = dbCon.executeQuery(query.toString(), this);
//                while (rs.next()) {
//                    LocationInDB loc = new LocationInDB();
//                    int gID = rs.getInt("gid");
//                    int code = rs.getInt("objart");
//                    double area = rs.getDouble("area");
//                    loc.setId(gID + this.locID); //this is a safe id
//                    loc.setCode(code);    //fortunately this code does not overlap with
//                    //the tapas codes, so we can distinguish these
//                    //dlm locations later on
//                    loc.setCapacity((int) area);
//                    loc.setFixedCapacity(true);
//
//                    //taz, coordinate and blk are set after this loop!
//
//                    int numberOfOpportunities = areaToOpportunity(code, area, true);
//                    //now add this locationID as often as we have this opportunities
//                    for (int i = 0; i < numberOfOpportunities; ++i)
//                        this.opportunities.add(loc);
//                }
//                rs.close();
//                int i = 0;
//                for (LocationInDB loc : this.opportunities) {
//                    ++i;
//                    if (i % 100 == 0) System.out.println(
//                            "Processed " + i + " form " + this.opportunities.size() + " DLM-objects for mapping id " +
//                                    map_id);
//                    if (loc.getId() > this.locID) { // dlm location: find taz, address and block
//                        BlockInDB myBlock;
//                        if (!this.blocks.containsKey(loc.getId())) { //do we know this location already?
//                            myBlock = findBlockAndAdressForDLMObject(loc.getId());
//                        } else {
//                            myBlock = this.blocks.get(loc.getId());
//                        }
//                        if (myBlock != null) { // could be null if there is no block in the dlm object
//                            loc.setBlk_id(myBlock.getBlk_id());
//                            loc.setTaz_id(myBlock.getTaz_id());
//                            loc.getCoordinate()[0] = myBlock.getCoord()[0];
//                            loc.getCoordinate()[1] = myBlock.getCoord()[1];
//                        } else {
//                            //huh repair geometry from dlm and find the taz
//                            query = new StringBuilder(
//                                    "SELECT taz_id, st_X(taz_coordinate) AS x, st_Y(taz_coordinate) AS y FROM core." +
//                                            this.region +
//                                            "_taz ORDER BY ST_DISTANCE_SPHERE(ST_TRANSFORM((SELECT the_geom FROM " +
//                                            dlm_table + " AS dlm WHERE dlm.gid=" + (loc.getId() - this.locID) +
//                                            "),4326), taz_coordinate)  LIMIT 1");
//                            rs = dbCon.executeQuery(query.toString(), this);
//                            if (rs.next()) {
//                                loc.setTaz_id(rs.getInt("taz_id"));
//                                loc.getCoordinate()[0] = rs.getDouble("x");
//                                loc.getCoordinate()[1] = rs.getDouble("y");
//                            }
//                            rs.close();
//                        }
//                    }
//                }
//
//                volume = pickOpportunitiesFromList(volume, preFixDLM);
//            }
//        } catch (SQLException e) {
//            System.err.println(
//                    this.getClass().getCanonicalName() + " fillLocationList: SQL-Error during statement: " + query);
//            e.printStackTrace();
//
//        }
    }

    private BlockInDB findBlockAndAdressForDLMObject(int id) {
        //todo revise this
//        try {
//            boolean adressNotFound = true;
//            int blkID = -1, tazID = -1;
//            double[] coordinates = new double[2];
//            String query =
//                    "SELECT blk_id, blk_taz_id, st_X(blk_coordinate) AS x, st_Y(blk_coordinate) AS y FROM core." +
//                            region +
//                            "_blocks WHERE ST_WITHIN(ST_TRANSFORM(blk_coordinate, 31467),(SELECT the_geom FROM core.gis_dlm_fot WHERE gid=" +
//                            (id - this.locID) + "))";
//            ResultSet rs = dbCon.executeQuery(query, this);
//            List<BlockInDB> actBlocks = new LinkedList<>();
//            //collect all possible blocks
//            while (rs.next()) {
//                BlockInDB myBlock = new BlockInDB();
//                myBlock.setBlk_id(rs.getInt("blk_id"));
//                myBlock.setTaz_id(rs.getInt("blk_taz_id"));
//                myBlock.getCoord()[0] = rs.getDouble("x");
//                myBlock.getCoord()[1] = rs.getDouble("y");
//                actBlocks.add(myBlock);
//            }
//            rs.close();
//            //now look if we have an address, if not take the last block coordinate
//            while (adressNotFound && actBlocks.size() > 0) {
//                //store actual data
//                blkID = actBlocks.get(0).getBlk_id();
//                tazID = actBlocks.get(0).getTaz_id();
//                coordinates[0] = actBlocks.get(0).getCoord()[0];
//                coordinates[1] = actBlocks.get(0).getCoord()[1];
//                //remove
//                actBlocks.remove(0);
//                //look if we find an adress in this block
//                query = "SELECT st_X(the_geom) AS x, st_Y(the_geom) AS y FROM core." + region +
//                        "_buildings WHERE ST_WITHIN( the_geom,(SELECT the_geom FROM core." + region +
//                        "_blocks_multiline WHERE blocknr=" + blkID + ")) LIMIT 1";
//                rs = dbCon.executeQuery(query, this);
//                if (rs.next()) {
//                    adressNotFound = false;
//                    coordinates[0] = rs.getDouble("x");
//                    coordinates[1] = rs.getDouble("y");
//                }
//            }
//            if (blkID >= 0) {
//                //finally we have everything we need!
//                BlockInDB myBlock = new BlockInDB();
//                myBlock.setBlk_id(blkID);
//                myBlock.setTaz_id(tazID);
//                myBlock.getCoord()[0] = coordinates[0];
//                myBlock.getCoord()[1] = coordinates[1];
//                this.blocks.put(id, myBlock);
//                return myBlock;
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
        return null;
    }

    /**
     * This method initates a adapter to load a specific TAZ mapping for the given region and fills the internal sql-queries
     *
     * @param name the name of the mapping
     */
    public void initRegionsFromTazGrouper(String name) {
        //todo revise this
//        GroupToTAZ grouper = new GroupToTAZ(this.dbCon);
//        grouper.initRegion(this.region);
//        grouper.loadMapping(name);
//        this.sqlQueryForRegions.clear();
//        this.opportunities.clear();
//        for (Integer i : grouper.getMappingValues()) {
//            this.sqlQueryForRegions.put(i, grouper.getShapeSQLQuery(i));
//        }
    }

    private int pickOpportunitiesFromList(int volume, String idPrefix) {
        //now select locations as often as volume or as the list is empty
        Random rand = new Random();
        while (volume > 0 && this.opportunities.size() > 0) {

            int index = rand.nextInt(this.opportunities.size());
            LocationInDB loc = this.opportunities.get(index);

            int capacity = 1;
            int locationID = loc.getId();

            if (this.opportunityOutput.containsKey(locationID)) {
                loc = this.opportunityOutput.get(locationID);
                capacity += loc.getCapacity();
            }
            loc.setCapacity(capacity);
            loc.setFixedCapacity(true);

            //adjust the values
            this.opportunityOutput.put(locationID, loc);
            volume--;
            this.opportunities.remove(index);
            totalNumerOfPickedOpportunities++;
        }

        return volume;
    }

    public void processOpportunities(String opportunityDistribution) {
        String query = "";
        //todo revise this
//        try {
//            query = "SELECT map_id,volume, loc_codes FROM core." + this.region + opportunityDistributionTableSuffix +
//                    " WHERE name='" + opportunityDistribution + "' ORDER BY level";
//            ResultSet rs = dbCon.executeQuery(query, this);
//            HashMap<Integer, VolumeDistribution> mappingValues = new HashMap<>();
//            //get the data and store them internally, to avoid database conflicts with nested requests.
//            int entry = 0;
//            while (rs.next()) {
//                int id = rs.getInt("map_id");
//                int volume = rs.getInt("volume");
//                int[] locationCodes = null;
//                if (rs.getArray("loc_codes") != null) {
//                    locationCodes = TPS_DB_IO.extractIntArray(rs, "loc_codes");
//                }
//                mappingValues.put(++entry, new VolumeDistribution(id, volume, locationCodes));
//            }
//            rs.close();
//            //fill the internal list
//            for (Entry<Integer, VolumeDistribution> es : mappingValues.entrySet()) {
//                fillLocationList(es.getValue().getMap_id(), es.getValue().getVolume(),
//                        es.getValue().getLocationCodes());
//            }
//
//        } catch (SQLException e) {
//            System.err.println(
//                    this.getClass().getCanonicalName() + " initOpportunities: SQL-Error during statement: " + query);
//            e.printStackTrace();
//        }
    }

    /**
     * This method initiates a adapter to load a specific TAZ mapping for the given region and fills the internal
     * sql-queries
     *
     * @param sqlQueryForRegions
     */
    public void setSqlRegionsDirectly(HashMap<Integer, String> sqlQueryForRegions) {
        this.sqlQueryForRegions = sqlQueryForRegions;
    }

    class LocationInDB {
        int id;
        int blk_id;
        int taz_id;
        int code;
        int capacity;
        String type = "";
        String unit = "";
        boolean fixedCapacity;
        double[] coordinate = new double[2];

        /**
         * Standard constructor
         */
        public LocationInDB() {
            //standard constructor
        }
        /**
         * Constructor from a result set. it must be well formated!
         *
         * @param rs the resultset, which points to a valid Location in db
         */
        public LocationInDB(ResultSet rs) {
            try {
                if (rs == null) return;
                this.setId(rs.getInt("loc_id"));
                this.setBlk_id(rs.getInt("loc_blk_id"));
                this.setTaz_id(rs.getInt("loc_taz_id"));
                this.setType(rs.getString("loc_type"));
                this.setUnit(rs.getString("loc_unit"));
                this.setCode(rs.getInt("loc_code")); //this will be adjusted later
                this.setCapacity(rs.getInt("loc_capacity")); //this will be adjusted later
                this.setFixedCapacity(true); //this will become a work location so fix the capacity
                this.getCoordinate()[0] = rs.getDouble("x");
                this.getCoordinate()[1] = rs.getDouble("y");
            } catch (SQLException e) {
                e.printStackTrace();
                e.getNextException().printStackTrace();
            }
        }

        /**
         * @return the blk_id
         */
        public int getBlk_id() {
            return blk_id;
        }

        /**
         * @param blk_id the blk_id to set
         */
        public void setBlk_id(int blk_id) {
            this.blk_id = blk_id;
        }

        /**
         * @return the capacity
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * @param capacity the capacity to set
         */
        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        /**
         * @return the code
         */
        public int getCode() {
            return code;
        }

        /**
         * @param code the code to set
         */
        public void setCode(int code) {
            this.code = code;
        }

        /**
         * @return the coordinate
         */
        public double[] getCoordinate() {
            return coordinate;
        }

        /**
         * @return the id
         */
        public int getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(int id) {
            this.id = id;
        }

        /**
         * @return the taz_id
         */
        public int getTaz_id() {
            return taz_id;
        }

        /**
         * @param taz_id the taz_id to set
         */
        public void setTaz_id(int taz_id) {
            this.taz_id = taz_id;
        }

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * @return the unit
         */
        public String getUnit() {
            return unit;
        }

        /**
         * @param unit the unit to set
         */
        public void setUnit(String unit) {
            this.unit = unit;
        }

        /**
         * @return the fixedCapacity
         */
        public boolean isFixedCapacity() {
            return fixedCapacity;
        }

        /**
         * @param fixedCapacity the fixedCapacity to set
         */
        public void setFixedCapacity(boolean fixedCapacity) {
            this.fixedCapacity = fixedCapacity;
        }
    }

    class ConversionUnitToCapacity {
        String type = "";
        String unit = "";
        double work_min = 0;
        double work_max = 1;
        double person_min = 0;
        double person_max = 1;

        /**
         * constructor from a db entry
         *
         * @param rs well formated valid result set
         */
        public ConversionUnitToCapacity(ResultSet rs) {

            try {
                if (rs == null) return;
                this.setType(rs.getString("type"));
                this.setUnit(rs.getString("unit"));
                this.setWork_min(rs.getDouble("work_factor_min"));
                this.setWork_max(rs.getDouble("work_factor_max"));
                this.setPerson_min(rs.getDouble("person_factor_min"));
                this.setPerson_max(rs.getDouble("person_factor_max"));
            } catch (SQLException e) {
                e.printStackTrace();
                e.getNextException().printStackTrace();
            }
        }

        public int getCapacity(LocationInDB loc, boolean workplace) {
            if (loc.getType() != null && loc.getType().equals(this.type) && loc.getUnit() != null &&
                    loc.getUnit().equals(this.unit)) {
                double value = loc.getCapacity();
                double factorSpread, factorOffset;
                factorOffset = workplace ? this.work_min : this.person_min;
                factorSpread = workplace ? (this.work_max - this.work_min) : (this.person_max - this.person_min);
                value *= (factorOffset + Math.random() * factorSpread);

                return (int) (value + 0.5); // incl. round
            } else {
                //error: mismatch in type and unit
                return 0;
            }

        }

        /**
         * Function, which generated the unique key for this class. The combination of type and unit must be unique!
         *
         * @return the key
         */
        public String getKeyValue() {
            return generateConversionUnitToCapacityKey(this.type, this.unit);
        }

        /**
         * @return the person_max
         */
        public double getPerson_max() {
            return person_max;
        }

        /**
         * @param person_max the person_max to set
         */
        public void setPerson_max(double person_max) {
            this.person_max = person_max;
        }

        /**
         * @return the person_min
         */
        public double getPerson_min() {
            return person_min;
        }

        /**
         * @param person_min the person_min to set
         */
        public void setPerson_min(double person_min) {
            this.person_min = person_min;
        }

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * @return the unit
         */
        public String getUnit() {
            return unit;
        }

        /**
         * @param unit the unit to set
         */
        public void setUnit(String unit) {
            this.unit = unit;
        }

        /**
         * @return the work_max
         */
        public double getWork_max() {
            return work_max;
        }

        /**
         * @param work_max the work_max to set
         */
        public void setWork_max(double work_max) {
            this.work_max = work_max;
        }

        /**
         * @return the work_min
         */
        public double getWork_min() {
            return work_min;
        }

        /**
         * @param work_min the work_min to set
         */
        public void setWork_min(double work_min) {
            this.work_min = work_min;
        }
    }

    class BlockInDB {
        int blk_id;
        int taz_id;
        double[] coord = new double[2];

        /**
         * @return the blk_id
         */
        public int getBlk_id() {
            return blk_id;
        }

        /**
         * @param blk_id the blk_id to set
         */
        public void setBlk_id(int blk_id) {
            this.blk_id = blk_id;
        }

        /**
         * @return the coord
         */
        public double[] getCoord() {
            return coord;
        }

        /**
         * @return the taz_id
         */
        public int getTaz_id() {
            return taz_id;
        }

        /**
         * @param taz_id the taz_id to set
         */
        public void setTaz_id(int taz_id) {
            this.taz_id = taz_id;
        }

    }

    class VolumeDistribution {
        private int map_id;
        private int volume;
        private int[] locationCodes;

        public VolumeDistribution(int mapId, int volume, int[] locationCodes) {
            this.map_id = mapId;
            this.volume = volume;
            this.locationCodes = locationCodes;
        }

        /**
         * @return the locationCodes
         */
        public int[] getLocationCodes() {
            return locationCodes;
        }

        /**
         * @param locationCodes the locationCodes to set
         */
        public void setLocationCodes(int[] locationCodes) {
            this.locationCodes = locationCodes;
        }

        /**
         * @return the map_id
         */
        public int getMap_id() {
            return map_id;
        }

        /**
         * @param map_id the map_id to set
         */
        public void setMap_id(int map_id) {
            this.map_id = map_id;
        }

        /**
         * @return the volume
         */
        public int getVolume() {
            return volume;
        }

        /**
         * @param volume the volume to set
         */
        public void setVolume(int volume) {
            this.volume = volume;
        }
    }

}
