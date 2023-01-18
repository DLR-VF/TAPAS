/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.person;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import javafx.print.Collation;

import java.util.*;

/**
 * Set with all households
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_HouseholdSet implements Iterable<TPS_Household>, ExtendedWritable {

    /**
     * Map with all households
     */
    private final SortedMap<Integer, TPS_Household> households;

    /**
     * Default constructor
     */
    public TPS_HouseholdSet() {
        this.households = new TreeMap<>();
    }

    /**
     * This method adds a household to the set of households.
     *
     * @param household
     * @return the household with its id
     * @throws RuntimeException this exception is thrown if the household already exist
     */
    public TPS_Household addHousehold(TPS_Household household) {
        int id = household.getId();
        if (this.containsHousehold(id)) {
            throw new RuntimeException("Household already exists: " + id + " " + this.households.keySet());
        }
        this.households.put(id, household);
        return household;
    }

    public int getNumberOfHouseholds(){
        return this.households.size();
    }

    /**
     * States if a household with the id supplied exists
     *
     * @param id id of the household
     * @return true if a household with this id exists, false otherwise
     */
    public boolean containsHousehold(Integer id) {
        return households.containsKey(id);
    }

    /**
     * Returns the household with the id specified
     *
     * @param id household id
     * @return household with this id, null if the household does not exist
     */
    public TPS_Household getHousehold(Integer id) {
        return households.get(id);
    }

    public Collection<TPS_Household> getHouseholds(){
        return this.households.values();
    }

    public void clearAllHouseholds() {
        this.households.clear();
    }

    public boolean isEmpty(){
        return this.households.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<TPS_Household> iterator() {
        return this.households.values().iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.util.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "HouseholdSet\n");
        for (TPS_Household hh : this.households.values()) {
            sb.append(hh.toString(prefix + " ") + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

}
