/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_LocationConstant;
import de.dlr.ivf.tapas.constants.TPS_LocationConstant.TPS_LocationCodeType;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.TPS_FastMath;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.HashSet;
import java.util.Set;


/**
 * This class represents a location within a region. It is characterized by
 * a capacity, weight and an occupancy. Furthermore it is located at a
 * coordinate. There exist two references to higher regions: the location set
 * and the block. The block level is not known in all regions so this reference
 * may be null.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_Location implements Locatable {

    /// Reference to the block if this information is available
    private final TPS_Block block;
    /// Coordinate of the location
    private final TPS_Coordinate coordinate;
    /// The location code of the location
    private final TPS_LocationConstant locType;
    /// The list of possible activities (determined from the location code at initialisation)
    private Set<TPS_ActivityConstant> activities = null;
    /// This location's data
    private TPS_LocationData data = null;
    /// The id of the location
    private final int id;
    private final TPS_ParameterClass parameterClass;
    /**
     * Group Id of the location, which associates this location to a group, e.g.
     * shopping mall, where you can shop, eat and work. Should be set to -1, if
     * no information is available.
     */
    private final int groupId;
    /// The traffic analysis zone this location is located in
    private final TPS_TrafficAnalysisZone taz;

    /**
     * The constructor sets the id of the location. The occupancy is set to 0
     * and the weight is calculated.
     *
     * @param id             The id of the location
     * @param groupId        The id of the locations group this location belongs to
     * @param locType        The type of this location
     * @param x              The x-coordinate of this location
     * @param y              The y-coordinate of this location
     * @param parameterClass parameter container reference
     */
    public TPS_Location(int id, int groupId, TPS_LocationConstant locType, double x, double y, TPS_TrafficAnalysisZone taz, TPS_Block block, TPS_ParameterClass parameterClass) {
        this.id = id;
        this.groupId = groupId;
        this.locType = locType;
        this.coordinate = new TPS_Coordinate(0, 0);
        this.coordinate.setValues(x, y);
        if (locType != TPS_LocationConstant.HOME) {
            this.activities = new HashSet<>(TPS_TrafficAnalysisZone.LOCATION2ACTIVITIES_MAP.get(locType));
            //TPS_AbstractConstant.getConnectedConstants(locType, TPS_ActivityCode.class));
        }
        this.taz = taz;
        this.block = block;
        this.parameterClass = parameterClass;
    }

    /**
     * Returns the block of the location
     *
     * @return The block this location is located in; may be null
     */
    public TPS_Block getBlock() {
        return block;
    }

    /**
     * Returns the coordinate of the location
     *
     * @return The coordinates of this location
     */
    public TPS_Coordinate getCoordinate() {
        return coordinate;
    }

    /**
     * Returns this location's data
     *
     * @return Data of this location
     */
    public TPS_LocationData getData() {
        return data;
    }

    /**
     * Returns the id of the group this location belongs to
     *
     * @return The id of this location's group (may be -1 if not given)
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Returns the id of this location
     *
     * @return The id of this location
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the location type
     *
     * @return The type of this location
     */
    public TPS_LocationConstant getLocType() {
        return this.locType;
    }

    /**
     * Returns the id of the traffic assignment zone this location belongs to
     *
     * @return The id of the traffic assignment zone this location belongs to
     */
    @Override
    public int getTAZId() {
        return this.getTrafficAnalysisZone().getTAZId();
    }

    /**
     * Returns the traffic analysis zone this location belongs to
     *
     * @return The traffic analysis zone this location belongs to
     */
    public TPS_TrafficAnalysisZone getTrafficAnalysisZone() {
        return this.taz;
    }

    /**
     * Flag if the block of the location is known and set as reference
     *
     * @return true if the reference to the block is not null, false otherwise
     */
    public boolean hasBlock() {
        return this.getBlock() != null;
    }

    /**
     * Flag if data is attached to this location
     *
     * @return true if getData returns not null
     */
    public boolean hasData() {
        return getData() != null;
    }

    /**
     * Inits this location's capacity
     *
     * @param capacity    the capacity
     * @param fixCapacity true if the capacity is fixed
     */
    public void initCapacity(int capacity, boolean fixCapacity) {
        this.data = new TPS_LocationData(this.parameterClass);
        this.data.init(capacity, fixCapacity);
    }

    /**
     * Method to determine if two locations are within the same group.
     *
     * @param ref The location to check
     * @return true if both locations are in the same location group
     */
    public boolean isSameLocationGroup(TPS_Location ref) {
        return this.groupId >= 0 && ref.getGroupId() >= 0 && this.groupId == ref.getGroupId();
    }

    /**
     * Returns this object's string representation
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Location [id=" + this.getId() + ", groupId=" + this.getGroupId() + ", locType=" +
                this.getLocType().getCode(TPS_LocationCodeType.TAPAS) + ", coord=" + this.getCoordinate() + ", data=" +
                (this.data != null ? data.toString() : "null") + "]";
    }

    /**
     * Updates the occupancy of this location
     * <p>
     * A location may allow different activities. The location's weight has therefore to be adapted for
     * all supported activity types.
     *
     * @param occupancy This location's new occupancy
     */
    public void updateOccupancy(int occupancy) {
        if (this.data != null && this.activities != null) {
            double oldWeight = this.data.getWeight();
            this.data.setOccupancy(occupancy);
            double newWeight = this.data.getWeight();
            for (TPS_ActivityConstant actCode : this.activities) {
                if (!this.taz.allowsActivity(actCode)) {
                    throw new RuntimeException("");
                }
                this.taz.updateActivityOccupancy(actCode, oldWeight, newWeight);
            }
        }
    }

    public class TPS_LocationData {
        /// Capacity of the location
        private int capacity;

        /**
         * This flag indicates if the location has a fixed capacity, i.e.
         * occupancy <= capacity (e.g. theatres, enterprises, etc). If the flag
         * is false more people can choose this location than its capacity
         * provides (e.g. parks, museums, shops, etc.).
         */
        private boolean fixCapacity;

        /// number of persons that chose that location
        private int occupancy = 0;

        /// Weight represents the amount of free capacities
        private double weight = 0;

        private final TPS_ParameterClass parameterClass;

        TPS_LocationData(TPS_ParameterClass parameterClass) {
            this.parameterClass = parameterClass;
        }

        /**
         * This method calculates the weight of the location by a decreasing
         * exponential function, so the weight is always greater than 0.
         * <p>
         * Since very small weights produce nasty cast errors in sql an
         * overloadfactor is introduced: Locations with an overload of 10 get a
         * weight of 0!
         */
        public void calculateWeight() {
            double val;
            if (this.capacity == 0) {
                val = 0;
            } else if (this.occupancy <= 0) {
                val = this.capacity;
                this.occupancy = 0;
            } else if (fixCapacity) {
                val = this.capacity - this.occupancy;
            } else {
                if (this.occupancy < 10.0 * this.capacity) {
                    val = this.capacity * TPS_FastMath.exp(
                            -this.parameterClass.getDoubleValue(ParamValue.WEIGHT_OCCUPANCY) * this.occupancy /
                                    this.capacity);
                } else {
                    val = 0;
                }
            }
            this.weight = val;
        }

        /**
         * Returns the capacity of a location
         *
         * @return capacity
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * Returns the occupancy of the location
         *
         * @return occupancy
         */
        public int getOccupancy() {
            return occupancy;
        }

        /**
         * Returns the weight of the location
         *
         * @return weight (always greater than 0)
         */
        public double getWeight() {
            return weight;
        }

        /**
         * Flag if the capacity of the location is fix
         *
         * @return true if fix, false else
         */
        public boolean hasFixCapacity() {
            return fixCapacity;
        }

        /**
         * Initialises this location's capacity and occupancy and computes the respective weight
         *
         * @param capacity    The capacity of this location
         * @param fixCapacity Whether the capacity shall be fix
         */
        public void init(int capacity, boolean fixCapacity) {
            this.capacity = capacity;
            this.occupancy = 0;
            this.fixCapacity = fixCapacity;
            this.calculateWeight();
        }

        /**
         * Sets a new occupancy
         *
         * @param newValue The new occupancy
         * @return Whether the occupancy has changed
         */
        public boolean setOccupancy(int newValue) {
            synchronized (this) {
                if (occupancy != newValue) {
                    this.occupancy = newValue;
                    if (this.parameterClass.isTrue(ParamFlag.FLAG_UPDATE_LOCATION_WEIGHTS)) {
                        this.calculateWeight();
                    }
                    // this.weight= this.capacity;
                    return true;
                }
                return false;
            }
        }


        /**
         * Returns this object's string representation
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "[" + this.getOccupancy() + "/" + this.getCapacity() + ", fix=" + this.hasFixCapacity() +
                    ", weight=" + this.getWeight() + "]";
        }
    }
}
