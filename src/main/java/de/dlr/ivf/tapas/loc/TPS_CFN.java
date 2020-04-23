package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.TPS_VariableMap;

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
     * cfn4 values depending on settlement system and activity code
     */
    private final Map<TPS_SettlementSystem, Map<TPS_ActivityConstant, Double>> cfn4Map = new HashMap<>();


    /**
     * cfnX values depending on settlement system
     */
    private final Map<TPS_SettlementSystem, Double> defaultCFNXMap = new HashMap<>();


    /**
     * cfn4 values depending on settlement system, time and work
     */
    private TPS_VariableMap specialCFN4Map;

    /**
     * Settlement type
     */
    private final TPS_SettlementSystemType regType;

    /**
     * Activity type
     */
    private final TPS_ActivityCodeType actType;

    public TPS_CFN(TPS_SettlementSystemType regType, TPS_ActivityCodeType actType) {
        this.regType = regType;
        this.actType = actType;
    }

    /**
     * Adds a value for the regional and activity based cnf-map
     *
     * @param regionNumber the number of the region
     * @param activity     the activity number
     * @param value        the value for this set
     */
    public void addToCFN4Map(int regionNumber, int activity, double value) {
        TPS_SettlementSystem ref = TPS_SettlementSystem.getSettlementSystem(this.regType, regionNumber);
        Map<TPS_ActivityConstant, Double> actMap = this.cfn4Map.computeIfAbsent(ref, k -> new HashMap<>());
        TPS_ActivityConstant tmp = TPS_ActivityConstant.getActivityCodeByTypeAndCode(this.actType, activity);
        actMap.put(tmp, value);
    }

    /**
     * Adds a value for the (general) region based cnf-map
     *
     * @param regionNumber the number of the region
     * @param value        the value for this set
     */
    public void addToCFNXMap(int regionNumber, double value) {
        TPS_SettlementSystem ref = TPS_SettlementSystem.getSettlementSystem(this.regType, regionNumber);
        this.defaultCFNXMap.put(ref, value);
    }

    /**
     * @return default current cfn value
     */
    public double getCFN4Value(TPS_SettlementSystem regionType, TPS_ActivityConstant act) {
        TPS_SettlementSystem regRef = TPS_SettlementSystem.getSettlementSystem(this.regType,
                regionType.getCode(this.regType));
        TPS_ActivityConstant actRef = TPS_ActivityConstant.getActivityCodeByTypeAndCode(this.actType,
                act.getCode(this.actType));
        Map<TPS_ActivityConstant, Double> actMap = this.cfn4Map.get(regRef);
        if (actMap != null && actMap.containsKey(actRef)) return actMap.get(actRef);
        else {
            return 0.5;
        }
    }

    /**
     * @return current cfnx value
     */
    public double getCFNXValue(TPS_SettlementSystem regionType) {
        TPS_SettlementSystem ref = TPS_SettlementSystem.getSettlementSystem(this.regType,
                regionType.getCode(this.regType));
        return defaultCFNXMap.get(ref);
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
