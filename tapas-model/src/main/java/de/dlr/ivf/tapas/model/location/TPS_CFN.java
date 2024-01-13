/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.location;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.TPS_VariableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates all tables for cfn* values.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_CFN {

    /**
     * cfn4 values depending on settlement system id and activity code
     */
    private final Map<Integer, Map<TPS_ActivityConstant, Double>> cfn4Map = new HashMap<>();


    /**
     * cfnX values depending on settlement system
     */
    private final Map<Integer, Double> defaultCFNXMap = new HashMap<>();


    /**
     * cfn4 values depending on settlement system, time and work
     */
    private TPS_VariableMap specialCFN4Map;

    /**
     * Activity type
     */
    private final TPS_ActivityCodeType actType;

    public TPS_CFN(TPS_ActivityCodeType actType) {
        this.actType = actType;
    }

    /**
     * Adds a value for the regional and activity based cnf-map
     *
     * @param settlementSystemId id of the settlement system
     * @param activity     the activity number
     * @param value        the value for this set
     */
    public void addToCFN4Map(int settlementSystemId, int activity, double value) {
        Map<TPS_ActivityConstant, Double> actMap = this.cfn4Map.computeIfAbsent(settlementSystemId, k -> new HashMap<>());
        TPS_ActivityConstant tmp = TPS_ActivityConstant.getActivityCodeByTypeAndCode(this.actType, activity);
        actMap.put(tmp, value);
    }

    /**
     * Adds a value for the (general) region based cnf-map
     *
     * @param settlementSystemId if od settlement system
     * @param value        the value for this set
     */
    public void addToCFNXMap(int settlementSystemId, double value) {
        this.defaultCFNXMap.put(settlementSystemId, value);
    }

    /**
     * @return default current cfn value
     */
    public double getCFN4Value(int settlementSystemId, TPS_ActivityConstant act) {
        TPS_ActivityConstant actRef = TPS_ActivityConstant.getActivityCodeByTypeAndCode(this.actType,
                act.getCode(this.actType));
        Map<TPS_ActivityConstant, Double> actMap = this.cfn4Map.get(settlementSystemId);
        if (actMap != null && actMap.containsKey(actRef)) return actMap.get(actRef);
        else {
            TPS_Logger.log(SeverityLogLevel.WARN,
                    "No CNF4-value found for activity " + act.getCode(TPS_ActivityCodeType.ZBE)+" in region "+settlementSystemId);
            return 0.5;
        }
    }

    /**
     * @return default current cfn value
     */
    public double getParamValue(int settlementSystemId, TPS_ActivityConstant act) {
        TPS_ActivityConstant actRef = TPS_ActivityConstant.getActivityCodeByTypeAndCode(this.actType,
                act.getCode(this.actType));
        Map<TPS_ActivityConstant, Double> actMap = this.cfn4Map.get(settlementSystemId);
        if (actMap != null && actMap.containsKey(actRef)) return actMap.get(actRef);
        else {

            return -1;
        }
    }

    /**
     * @return current cfnx value
     */
    public double getCFNXValue(int settlementSystemId) {
        return defaultCFNXMap.get(settlementSystemId);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.cfn4Map.toString() + "\n" + this.specialCFN4Map + "\n" + this.defaultCFNXMap;
    }
}
