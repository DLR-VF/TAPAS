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
import de.dlr.ivf.tapas.model.constants.TPS_LocationConstant.TPS_LocationCodeType;
import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import lombok.Builder;
import lombok.Singular;
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
@Builder
public class TPS_Location implements Locatable {

    /// Coordinate of the location
    private final TPS_Coordinate coordinate;
    /// The location code of the location
    private final TPS_LocationConstant locType;
    /// The list of possible activities (determined from the location code at initialisation)
    @Singular
    private Set<TPS_ActivityConstant> activities ;
    /// This location's data
    private final LocationData data;
    /// The id of the location
    private final int id;
    private final int tazId;
    private final int blockId;
    private final TPS_Block block;
    private final TPS_TrafficAnalysisZone taz;

    /**
     * Group Id of the location, which associates this location to a group, e.g.
     * shopping mall, where you can shop, eat and work. Should be set to -1, if
     * no information is available.
     */
    private final int groupId;


    /**
     * Returns the block of the location
     *
     * @return The block this location is located in; may be null
     */
    public TPS_Block getBlock() {
        return block;
    }

    public int getBlockId(){
        return this.blockId;
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
    public LocationData getData() {
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
        return this.tazId;
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
        return this.data != null;
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
     * @see Object#toString()
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
     * @param deltaOccupancy This location's new occupancy
     */
    public void updateOccupancy(int deltaOccupancy) {
        if (this.data != null && this.activities != null) {
            double oldWeight = this.data.getWeight();
            this.data.changeOccupancy(deltaOccupancy);
            double newWeight = this.data.getWeight();
            for (TPS_ActivityConstant actCode : this.activities) {
                if (!this.taz.allowsActivity(actCode)) {
                    throw new RuntimeException("");
                }
                this.taz.updateActivityOccupancy(actCode, oldWeight, newWeight);
            }
        }
    }
}
