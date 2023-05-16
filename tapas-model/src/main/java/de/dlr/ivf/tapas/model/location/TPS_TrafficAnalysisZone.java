/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.location;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_LocationConstant;
import de.dlr.ivf.tapas.model.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import lombok.Getter;

import java.util.*;

@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
@Getter
public class TPS_TrafficAnalysisZone implements Comparable<TPS_TrafficAnalysisZone>, Locatable {

    /// Mapping between corresponding codes

//    public static MultiMap<TPS_LocationConstant, TPS_ActivityConstant, List<TPS_ActivityConstant>> LOCATION2ACTIVITIES_MAP = new MultiMap<>(new ArrayList<>());

    // locations storage
    public HashMap<TPS_ActivityConstant, TypedWeightedLocationDistribution> locationsByActivity = new HashMap<>();
    /// community type, used during choice of locations
    private TPS_SettlementSystem bbrType;

    private final Map<TPS_LocationConstant, Collection<TPS_ActivityConstant>> locationToActivityMap = new HashMap<>();
    /// Collection with all blocks of this traffic analysis zone
    private final SortedMap<Integer, TPS_Block> blocks;
    /// Coordinate of the center point of this traffic analysis zone
    private TPS_Coordinate center;
    /// id of the traffic analysis zone
    private final int id;
    /// score value for the quality of pt access and service in the traffic analysis zone
    private double score = 3;
    /// Category of the score for the quality of pt in the traffic analysis zone
    private int scoreCat = 3;
    /// reference to external id code
    private int externalId = -1;
    /// Map which stores the values for the corresponding SimulationType
    private final Map<SimulationType, ScenarioTypeValues> simulationTypeValues;
    /// Local variable for the TAZData (locations etc.)
    private TPS_TAZData data;
    private boolean isRestricted = false; // restricted zone, e.g. only for electric vehicles etc.
    private boolean isPNR = false; //is Park and Ride

    /**
     * Constructor
     *
     * @param id The id of this TAZ
     */
    public TPS_TrafficAnalysisZone(int id) {
        this.id = id;
        this.blocks = new TreeMap<>();
        this.simulationTypeValues = new HashMap<>();
        this.simulationTypeValues.put(SimulationType.BASE, new ScenarioTypeValues());
        this.simulationTypeValues.put(SimulationType.SCENARIO, new ScenarioTypeValues());
    }

    /**
     * This method adds the given location
     *
     * @param loc The location to add
     */
    public void addLocation(TPS_Location loc) {
        TPS_LocationConstant locType = loc.getLocType();
        // TODO: necessary?
        // check data
        if (loc.hasData() && !this.hasData()) {
            this.data = new TPS_TAZData();
        } else if (!loc.hasData() && this.hasData()) {
            throw new RuntimeException(
                    "You want to add a location without additional data to a taz with additional data");
        }
        // TODO: necessary? end

        // add to the activity storages
        Collection<TPS_ActivityConstant> actCodes = locationToActivityMap.get(locType);
        //TPS_AbstractConstant.getConnectedConstants(locType,TPS_ActivityCode.class);
        if (actCodes != null) {
            for (TPS_ActivityConstant actCode : actCodes) {
                if (!this.locationsByActivity.containsKey(actCode)) {
                    this.locationsByActivity.put(actCode, new TypedWeightedLocationDistribution());
                }
                this.locationsByActivity.get(actCode).addLocation(loc);
            }
        }
    }

    /**
     * Returns whether this activity can be performed within this TAZ
     * (whether at least one location that supports this activity is included in this TAZ)
     *
     * @param actCode The code of the activity it is asked for
     * @return Whether at least one location allows the activity to be performed
     */
    public boolean allowsActivity(TPS_ActivityConstant actCode) {
        return this.locationsByActivity.containsKey(actCode);
    }

    @Override
    public int compareTo(TPS_TrafficAnalysisZone o) {
        return Integer.compare(this.id, o.id);
    }

    /**
     * Returns whether the named block is contained
     *
     * @param blockId The id of the block
     * @return true if the block is contained, false otherwise
     */
    public boolean containsBlock(int blockId) {
        return this.blocks.containsKey(blockId);
    }

    /**
     * Returns the weight sum of locations that support the given activity
     *
     * @param actCode The code of the activity
     * @return The sum of weights of the locations that support this activity
     */
    public double getActivityWeightSum(TPS_ActivityConstant actCode) {
        if (!allowsActivity(actCode)) {
            return 0;
        }
        return this.locationsByActivity.get(actCode).getWeightSum();
    }

    /**
     * Returns the community / region type of the traffic analysis zone according to BBR
     *
     * @return The BBR (community) type
     */
    public TPS_SettlementSystem getBbrType() {
        return bbrType;
    }

    /**
     * Sets community / region type according to the BBR
     *
     * @param bbrType
     */
    public void setBbrType(TPS_SettlementSystem bbrType) {
        this.bbrType = bbrType;
    }

    /**
     * Returns the block corresponding to the given blockId.
     * If the block doesn't exist it is created.
     *
     * @param blockId The id of the block to return
     * @return block with this blockId
     */
    public TPS_Block getBlock(int blockId) {
        TPS_Block block = this.blocks.get(blockId);
        if (block == null) {
            block = new TPS_Block(blockId);
            block.setTrafficAnalysisZone(this);
            this.blocks.put(blockId, block);
        }
        return block;
    }

    /**
     * TODO check
     *
     * @return <code>null</code>
     */
    @Override
    public TPS_Block getBlock() {
        return null;
    }

    /**
     * Returns the coordinate of the center of this TAZ
     *
     * @return The center coordinate
     */
    public TPS_Coordinate getCenter() {
        return center;
    }

    /**
     * Returns the coordinate of the center of this TAZ
     *
     * @return The center coordinate
     */
    public TPS_Coordinate getCoordinate() {
        return this.getCenter();
    }

    /**
     * Returns the data attached to this zone.
     *
     * @return This TAZ's data
     */
    public TPS_TAZData getData() {
        return data;
    }

    /**
     * Returns the external id of this TAZ.
     *
     * @return the external id or -1, if no external id is known
     */
    public int getExternalId() {
        return externalId;
    }

    /**
     * Sets the external id number of this TAZ.
     *
     * @param externalId The external id number. Set to -1, if unknown.
     */
    public void setExternalId(int externalId) {
        this.externalId = externalId;
    }

    /**
     * Returns the parking fee for an hour for the traffic analysis zone in the
     * scenario or base situation as specified
     *
     * @param scenario
     * @return Parking fee
     */
    public double getParkingFee(SimulationType scenario) {
        return this.getSimulationTypeValues(scenario).getFeeParking();
    }


    /**
     * Returns the score indicating the PT quality in the traffic analysis zone
     *
     * @return score
     */
    public double getScore() {
        return score;
    }

    /**
     * Sets the score for the pt quality and access in the traffic analysis zone
     *
     * @param score The (PT) score of this TAZ
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Returns the category of the score indicating the pt quality in the
     * traffic analysis zone
     *
     * @return score category
     */
    public int getScoreCat() {
        return scoreCat;
    }

    /**
     * Sets the category of the score for the pt quality and access in the traffic analysis zone
     *
     * @param scoreCat The score category of this TAZ
     */
    public void setScoreCat(int scoreCat) {
        this.scoreCat = scoreCat;
    }

    /**
     * Returns the scenario specific values for the scenario given
     *
     * @param type scenario the values should be given for
     * @return set of scenario values
     */
    public ScenarioTypeValues getSimulationTypeValues(SimulationType type) {
        return this.simulationTypeValues.get(type);
    }

    /**
     * Returns the ID of this TAZ
     *
     * @return This TAZ's ID
     */
    @Override
    public int getTAZId() {
        return id;
    }

    /**
     * Returns the toll fee in EURO per entrance for the traffic analysis zone in
     * the specified scenario
     *
     * @param scenario scenario to get the fee for
     * @return toll fee in EURO
     */
    public double getTollFee(SimulationType scenario) {
        return this.getSimulationTypeValues(scenario).getFeeToll();
    }

    /**
     * Returns this TAZ
     *
     * @return This TAZ
     */
    @Override
    public TPS_TrafficAnalysisZone getTrafficAnalysisZone() {
        return this;
    }

    /**
     * TODO check
     *
     * @return <code>false</code>
     */
    @Override
    public boolean hasBlock() {
        return false;
    }

    /**
     * Returns whether this TAZ has data
     *
     * @return Whether this TAZ has data
     */
    public boolean hasData() {
        return this.getData() != null;
    }

    /**
     * Returns if a parking fee has to be payed in the scenario specified
     *
     * @param scenario scenario the fee should be determined for
     * @return true if parking fee is applicable, false else
     */
    public boolean hasParkingFee(SimulationType scenario) {
        return this.getSimulationTypeValues(scenario).hasParkingFee();
    }

//	todo remove
//	/** Initializer for the array of fees and their value
//	 * @param has dummy boolean
//	 * @param type dummy type
//	 */
//	public void nitFeesTolls(boolean[] has, int[] type) {
//		this.initFeesTolls(has[0], type[0], has[1], type[1], has[2], type[2], has[3], type[3], has[4],has[5]);
//	}

    /**
     * Returns if a toll fee has to be payed in the scenario specified
     *
     * @param scenario The scenario the fee should be determined for
     * @return true if toll fee is applicable, false else
     */
    public boolean hasToll(SimulationType scenario) {
        return this.getSimulationTypeValues(scenario).hasToll();
    }

    /**
     * Constructor of the traffic analysis zone with information concerning the
     * costs associated with a visit (toll, parking fee, bbr region type)
     *
     * @param hasBasisToll       flag if the traffic analysis zone has a toll fee in the base scenario
     * @param basisTollType      type of toll fee in the base scenario
     * @param hasBasisParkingFee flag if the traffic analysis zone has a parking fee in the base scenario
     * @param basisParkingType   type of parking fee in the base scenario
     * @param hasScenToll        flag if the traffic analysis zone has a toll fee in the scenario
     * @param scenTollType       type of toll fee in the scenario
     * @param hasScenParkingFee  flag if the traffic analysis zone has a parking fee in the scenario
     * @param scenParkingType    type of parking fee in the scenario
     * @param parameterClass     parameter class reference
     */
    public void initFeesTolls(boolean hasBasisToll, int basisTollType, boolean hasBasisParkingFee, int basisParkingType, boolean hasScenToll, int scenTollType, boolean hasScenParkingFee, int scenParkingType, boolean isCarSharingBase, boolean isCarSharingScen, TPS_ParameterClass parameterClass) {

        this.getSimulationTypeValues(SimulationType.BASE).setHasToll(hasBasisToll);
        this.getSimulationTypeValues(SimulationType.BASE).setTypeToll(basisTollType);
        this.getSimulationTypeValues(SimulationType.BASE).setHasParkingFee(hasBasisParkingFee);
        this.getSimulationTypeValues(SimulationType.BASE).setTypeParking(basisParkingType);
        this.getSimulationTypeValues(SimulationType.BASE).setCarsharingServiceArea(isCarSharingBase);

        this.getSimulationTypeValues(SimulationType.SCENARIO).setHasToll(hasScenToll);
        this.getSimulationTypeValues(SimulationType.SCENARIO).setTypeToll(scenTollType);
        this.getSimulationTypeValues(SimulationType.SCENARIO).setHasParkingFee(hasScenParkingFee);
        this.getSimulationTypeValues(SimulationType.SCENARIO).setTypeParking(scenParkingType);
        this.getSimulationTypeValues(SimulationType.SCENARIO).setCarsharingServiceArea(isCarSharingScen);

        for (SimulationType st : SimulationType.values()) {
            // make sure that toll category is not null if hasToll is true;
            // default: 1
            if (this.getSimulationTypeValues(st).hasToll() && this.getSimulationTypeValues(st).getTypeToll() == 0) {
                this.getSimulationTypeValues(st).setTypeToll(1);
            }
            // make sure that ParkingType is not null if hasParkingFee is true;
            // default: 1
            if (this.getSimulationTypeValues(st).hasParkingFee() && this.getSimulationTypeValues(st).getTypeParking() ==
                    0) {
                this.getSimulationTypeValues(st).setTypeParking(1);
            }
            this.setCostParkingPerHour(st, parameterClass);
            this.setCostToll(st, parameterClass);
        }

    }

    /**
     * Returns the parking fee for an hour for the traffic analysis zone in the
     * scenario or base situation as specified
     *
     * @param scenario
     * @return Parking fee
     */
    public boolean isCarSharingService(SimulationType scenario) {
        return this.getSimulationTypeValues(scenario).isCarsharingServiceArea();
    }

    public boolean isPNR() {
        return isPNR;
    }

    public void setPNR(boolean isPNR) {
        this.isPNR = isPNR;
    }

    public boolean isRestricted() {
        return isRestricted;
    }

    public void setRestricted(boolean isRestricted) {
        this.isRestricted = isRestricted;
    }

    /**
     * Chooses the given number of locations that support te given activity
     *
     * @param actCode The code of the activity
     * @param number  The number of locations to select
     * @return The list of the selected locations
     */
    public ArrayList<TPS_Location> selectActivityLocations(TPS_ActivityConstant actCode, int number) {
        if (!allowsActivity(actCode)) {
            return null;
        }
        return this.locationsByActivity.get(actCode).selectActivityLocations(number);
    }

    /**
     * This method sets the center coordinate to the given value
     * TODO: check whether this could be done within the constructor
     *
     * @param x
     * @param y
     */
    public void setCenter(double x, double y) {
        if (this.center == null) {
            this.center = new TPS_Coordinate(x, y);
        } else {
            this.center.setValues(x, y);
        }
    }

    /**
     * Calculates the hourly fee in Euro for parking in the TAZ according to the
     * scenario and the parking type in the TAZ
     *
     * @param type type indicating if the fees should be calculated for the scenario or the base situation
     */
    private void setCostParkingPerHour(SimulationType type, TPS_ParameterClass parameterClass) {
        ScenarioTypeValues stv = this.simulationTypeValues.get(type);
        if (stv.hasParkingFee()) {
            if (type == SimulationType.SCENARIO) {
                if (stv.getTypeParking() == 2) {
                    stv.setFeeParking(parameterClass.getDoubleValue(ParamValue.PARKING_FEE_CAT_2));
                } else if (stv.getTypeParking() == 3) {
                    stv.setFeeParking(parameterClass.getDoubleValue(ParamValue.PARKING_FEE_CAT_3));
                } else {
                    stv.setFeeParking(parameterClass.getDoubleValue(ParamValue.PARKING_FEE_CAT_1));
                }
            } else {
                if (stv.getTypeParking() == 2) {
                    stv.setFeeParking(parameterClass.getDoubleValue(ParamValue.PARKING_FEE_CAT_2_BASE));
                } else if (stv.getTypeParking() == 3) {
                    stv.setFeeParking(parameterClass.getDoubleValue(ParamValue.PARKING_FEE_CAT_3_BASE));
                } else {
                    stv.setFeeParking(parameterClass.getDoubleValue(ParamValue.PARKING_FEE_CAT_1_BASE));
                }
            }
        }
    }

    /**
     * Calculates the hourly fee in € for entering in the tvz according to the
     * scenario and the toll type in the traffic analysis zone; toll is assumed
     * to be cordon and undifferentiated by car type and person type
     *
     * @param type           type indicating if the fees should be calculated for the scenario or the base situation
     * @param parameterClass parameter class reference
     */
    private void setCostToll(SimulationType type, TPS_ParameterClass parameterClass) {
        ScenarioTypeValues stv = this.simulationTypeValues.get(type);
        if (stv.hasToll()) {
            if (type == SimulationType.SCENARIO) {
                if (stv.getTypeToll() == 2) {
                    stv.setFeeToll(parameterClass.getDoubleValue(ParamValue.TOLL_CAT_2));
                } else if (stv.getTypeToll() == 3) {
                    stv.setFeeToll(parameterClass.getDoubleValue(ParamValue.TOLL_CAT_3));
                } else {
                    stv.setFeeToll(parameterClass.getDoubleValue(ParamValue.TOLL_CAT_1));
                }
            } else {
                if (stv.getTypeToll() == 2) {
                    stv.setFeeToll(parameterClass.getDoubleValue(ParamValue.TOLL_CAT_2_BASE));
                } else if (stv.getTypeToll() == 3) {
                    stv.setFeeToll(parameterClass.getDoubleValue(ParamValue.TOLL_CAT_3_BASE));
                } else {
                    stv.setFeeToll(parameterClass.getDoubleValue(ParamValue.TOLL_CAT_1_BASE));
                }
            }
        }
    }

    /**
     * Returns this object's string representation
     *
     * @see Object#toString()
     */
    public String toString() {
        return "traffic analysis zone [id=" + this.getTAZId() + ", bbrType=" + this.bbrType.getId() + ", basis=" +
                this.getSimulationTypeValues(SimulationType.BASE).toString() + ", scenario=" +
                this.getSimulationTypeValues(SimulationType.SCENARIO).toString() + "]\n";
    }

    /**
     * Updates the weights (occupancies) of the activity within this TAZ
     *
     * @param actCode   The code of the activity
     * @param oldWeight The earlier weight of the location that is responsible for this update
     * @param weight    The new weight of the location that is responsible for this update
     */
    public void updateActivityOccupancy(TPS_ActivityConstant actCode, double oldWeight, double weight) {
        if (!allowsActivity(actCode)) {
            throw new RuntimeException("");
        }
        this.locationsByActivity.get(actCode).updateOccupancy(oldWeight, weight);
    }

    public class TPS_TAZData {
        /**
         * Chooses for the type of activity to be performed one of the
         * appropriate locations within the traffic analysis zone. This location
         * represents all possible locations within the traffic analysis zone
         * for this round of the location choice. The probability of a specific
         * location to be chosen depends on the weight of the location in
         * comparison to the sum of weights of the locations in the traffic
         * analysis zone. Method is accounting for different location types
         * being applicable for the same activity type (e.g. leisure in parks
         * and bars)
         *
         * @param actCode activity type to be performed
         * @return representative of this traffic analysis zone
         */
        public TPS_Location generateLocationRepr(TPS_ActivityConstant actCode) {
            // return null if there is no location with the wanted activity
            if (!TPS_TrafficAnalysisZone.this.locationsByActivity.containsKey(actCode)) {
                return null;
            }
            return TPS_TrafficAnalysisZone.this.locationsByActivity.get(actCode).select();
        }
    }




}
