/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_InternalConstant;
import de.dlr.ivf.tapas.loc.Locatable;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.modechoice.TPS_UtilityFunction;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Collection;
import java.util.EnumMap;

/**
 * This class represents the basic features of a mode. It is characterized by a type and a name.
 * <p>
 * Every mode is a singleton so the type and the name have to be unique. This is realized by the visibility of the
 * constructor. Only classes in this package have access.
 * <p>
 * see {@link TPS_ModeSet}
 * <p>
 * Furthermore there are two abstract methods to calculate the distance and the travel time between two locations.
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public abstract class TPS_Mode {

    public static ModeType[] MODE_TYPE_ARRAY = new ModeType[]{ModeType.WALK, ModeType.BIKE, ModeType.MIT, ModeType.MIT_PASS, ModeType.TAXI, ModeType.PT, ModeType.CAR_SHARING};
    public static double NO_CONNECTION = -1;
    public static double VISUM_NO_CONNECTION = 99999.0;
    /**
     * Map stores all modes indexed by their mode type
     */
    private static final EnumMap<ModeType, TPS_Mode> MODE_MAP = new EnumMap<>(ModeType.class);
    /**
     * Instance to the utility function
     */
    private static TPS_UtilityFunction UTILITY_FUNCTION = null;
    public ParamValue velocity = null;
    public double cost_per_km = 0;
    public double cost_per_km_base = 0;
    public double variable_cost_per_km = 0;
    public double variable_cost_per_km_base = 0;
    public boolean useBase = false;
    private TPS_ParameterClass parameterClass;
    /**
     * In this map all internal representation of this constant are stored
     */
    private final EnumMap<TPS_ModeCodeType, TPS_InternalConstant<TPS_ModeCodeType>> map;
    private final String name;
    /**
     * This flag determines whether the choice for this mode has to be fix or not, i.e. when I decide to drive to work by car
     * then I have to take it back home too (fix = true). If I go to work by taxi then I can take the bus to go home (fix =
     * false).
     */
    private boolean isFix;
    /**
     * type (Name) of the mode
     */
    private ModeType modeType;

    /**
     * Basic constructor for a TPS_Mode
     *
     * @param name           of the mode
     * @param attributes     list of string containing the (name, code, type) triples; length must be divisible by 3
     * @param isFix          states if the mode is fix, like bike or car
     * @param parameterClass reference
     */
    protected TPS_Mode(String name, String[] attributes, boolean isFix, TPS_ParameterClass parameterClass) {
        if (attributes.length % 3 != 0) {
            throw new RuntimeException("Mode constant need n*3 attributes n*(name, code, type): " + attributes.length);
        }
        this.name = name;
        this.parameterClass = parameterClass;

        TPS_InternalConstant<TPS_ModeCodeType> tic;
        this.map = new EnumMap<>(TPS_ModeCodeType.class);

        for (int i = 0; i < attributes.length; i += 3) {
            tic = new TPS_InternalConstant<>(attributes[i], Integer.parseInt(attributes[i + 1]),
                    TPS_ModeCodeType.valueOf(attributes[i + 2]));
            this.map.put(tic.getType(), tic);
        }

        for (TPS_ModeCodeType type : TPS_ModeCodeType.values()) {
            if (!this.map.containsKey(type)) {
                throw new RuntimeException(
                        "mode code for " + this.getName() + " for type " + type.name() + " not defined");
            }
        }
        this.setAttribute(ModeType.valueOf(this.getName()));
        this.setFix(isFix);
    }

    /**
     * Empties the global static TPS_Mode map
     */
    public static void clearModeMap() {
        MODE_MAP.clear();
    }

    /**
     * This method returns the mode corresponding to the given mode type
     *
     * @param key key mode type
     * @return mode corresponding to the mode type
     */
    public static TPS_Mode get(ModeType key) {
        return MODE_MAP.get(key);
    }

    /**
     * Returns a collection/view of the values of the PERSON_GROUP_MAP, i.e. all stored person group constants
     * Note: changes in the map are reflected in the collection and vice-versa
     *
     * @return a collection/view of the values of the PERSON_GROUP_MAP
     */
    public static Collection<TPS_Mode> getConstants() {
        return MODE_MAP.values();
    }

    /**
     * Method returns utility function.
     * Must be initialized first by the initUtilityFunction
     *
     * @return utility function
     */
    public static TPS_UtilityFunction getUtilityFunction() {
        return UTILITY_FUNCTION;
    }

    /**
     * Method to check if the returned value is a connection. Inverse function is noConnection.
     *
     * @param val the value to check
     * @return true if connection is detected
     */
    public static boolean hasConnection(double val) {
        return val >= 0 || val > TPS_Mode.NO_CONNECTION;
    }

    /**
     * This method initializes the utility function.
     *
     * @param parameterClass reference the parameter class object
     */
    public static void initUtilityFunction(TPS_ParameterClass parameterClass) {
        try {
            //we have to state the modechoice package path
            String utilityFunctionName = parameterClass.getString(ParamString.UTILITY_FUNCTION_CLASS);
            utilityFunctionName = utilityFunctionName.substring(utilityFunctionName.lastIndexOf('.') + 1);
            Class<?> c = Class.forName("de.dlr.ivf.tapas.modechoice." + utilityFunctionName);
            TPS_Mode.UTILITY_FUNCTION = (TPS_UtilityFunction) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // this is bad style, but the above four lines produce way to many exceptions, which are all related to "ClassNotFound"
            TPS_Logger.log(HierarchyLogLevel.APPLICATION, SeverenceLogLevel.FATAL,
                    "Error in instantiating the utility function: " + ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Method to check if the returned value is a "no connection". Inverse function is hasConnection.
     *
     * @param val the value to check
     * @return true if no connection is detected
     */
    public static boolean noConnection(double val) {
        return val < 0 || val <= TPS_Mode.NO_CONNECTION;
    }

    /**
     * Adds this TPS_Mode instance to the global static MODE_MAP
     */
    public void addModeToMap() {
        MODE_MAP.put(this.getAttribute(), this);
    }

    /**
     * Gets the Delta of costs for this mode
     */
    double calculateDelta(TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
        return TPS_Mode.getUtilityFunction().calculateDelta(this, plan, distanceNet, mcc);
    }

    /**
     * Returns the type of the mode
     *
     * @return mode type attribute
     */
    public ModeType getAttribute() {
        return modeType;
    }

    /**
     * Sets mode type attribute
     *
     * @param attribute mode type like BIKE, MIT etc., see {@link ModeType}
     */
    private void setAttribute(ModeType attribute) {
        this.modeType = attribute;
        switch (this.modeType) {
            case BIKE:
                this.velocity = ParamValue.VELOCITY_BIKE;
                this.cost_per_km = this.parameterClass.getDoubleValue(ParamValue.BIKE_COST_PER_KM);
                this.cost_per_km_base = this.parameterClass.getDoubleValue(ParamValue.BIKE_COST_PER_KM_BASE);
                this.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE);
                break;
            case MIT:
                this.velocity = ParamValue.VELOCITY_CAR;
                this.cost_per_km = this.parameterClass.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM);
                this.cost_per_km_base = this.parameterClass.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM_BASE);
                this.variable_cost_per_km = this.parameterClass.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM);
                this.variable_cost_per_km_base = this.parameterClass.getDoubleValue(
                        ParamValue.MIT_VARIABLE_COST_PER_KM_BASE);
                this.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);
                break;
            case MIT_PASS:
                this.velocity = ParamValue.VELOCITY_CAR;
                this.cost_per_km = this.parameterClass.getDoubleValue(ParamValue.PASS_COST_PER_KM);
                this.cost_per_km_base = this.parameterClass.getDoubleValue(ParamValue.PASS_COST_PER_KM_BASE);
                this.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);
                break;
            case TAXI:
                this.velocity = ParamValue.VELOCITY_CAR;
                this.cost_per_km = this.parameterClass.getDoubleValue(ParamValue.TAXI_COST_PER_KM);
                this.cost_per_km_base = this.parameterClass.getDoubleValue(ParamValue.TAXI_COST_PER_KM_BASE);
                this.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);
                break;
            case PT:
                this.velocity = ParamValue.VELOCITY_TRAIN;
                this.cost_per_km = this.parameterClass.getDoubleValue(ParamValue.PT_COST_PER_KM);
                this.cost_per_km_base = this.parameterClass.getDoubleValue(ParamValue.PT_COST_PER_KM_BASE);
                this.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE);
                break;
            case CAR_SHARING:
                this.velocity = ParamValue.VELOCITY_CAR;
                this.cost_per_km = this.parameterClass.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM);
                this.cost_per_km_base = this.parameterClass.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE);
                this.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE);
                break;
            case WALK:
                this.velocity = ParamValue.VELOCITY_FOOT;
                this.cost_per_km = this.parameterClass.getDoubleValue(ParamValue.WALK_COST_PER_KM);
                this.cost_per_km_base = this.parameterClass.getDoubleValue(ParamValue.WALK_COST_PER_KM_BASE);
                this.useBase = this.parameterClass.isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE);
                break;
        }
    }

    /**
     * @param type TPS_ModeCodeTyp like MCT or VOT
     * @return code of the constant corresponding to the given enum type
     */
    public int getCode(TPS_ModeCodeType type) {
        return this.map.get(type).getCode();
    }

    /**
     * This method returns the cost factor for this mode for the specified Scenario case. It must be multiplied with a cost per km value.
     *
     * @param simType scenario or base
     * @return cost factor per km.
     */
    public double getCost_per_km(SimulationType simType) {
        if (simType == SimulationType.BASE) {
            return cost_per_km_base;
        } else {
            return cost_per_km;
        }
    }

    /**
     * This method approximates the real distance between two locations by multiplying a specific factor to the beeline
     * distance. If this value is smaller than the minimum distance the minimum distance is returned
     *
     * @param beeline direct distance between two locations
     * @return approximated distance in meter or the minimum distance
     */
    double getDefaultDistance(double beeline) {
        return Math.max(this.parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                this.parameterClass.getDoubleValue(this.modeType.getBeelineFactor()) * beeline);
    }

    /**
     * This method calculates the real distance between two locations.
     *
     * @param start   location, where this trip starts
     * @param end     location, where this trip ends
     * @param simType Type of simulation (Base/Scenario)
     * @param car     The car for this trip, if available. Set it to null, if no car is used
     * @return calculated distance in meter
     */
    public abstract double getDistance(Locatable start, Locatable end, SimulationType simType, TPS_Car car);

    /**
     * Gets the ModeType for this mode
     *
     * @return The ModeType
     */
    public ModeType getModeType() {
        return modeType;
    }

    public String getName() {
        return name;
    }

    /**
     * This method returns the parameter class reference
     *
     * @return parameter class reference
     */
    public TPS_ParameterClass getParameters() {
        return this.parameterClass;
    }

    /**
     * This method sets the parameter class reference
     *
     * @param param parameter class reference
     */
    public void setParameters(TPS_ParameterClass param) {
        this.parameterClass = param;
    }

    /**
     * This method calculates the travel time between two locations.
     *
     * @param start       location, where this trip starts
     * @param end         location, where this trip ends
     * @param time        departure time of the trip in seconds after midnight
     * @param simType     simulation type
     * @param actCodeFrom activity code of the earlier episode
     * @param actCodeTo   activity code of the later episode
     * @param person      reference to the person on this trip
     * @return travel time in seconds
     */
    public abstract double getTravelTime(Locatable start, Locatable end, int time, SimulationType simType, TPS_ActivityConstant actCodeFrom, TPS_ActivityConstant actCodeTo, TPS_Person person, TPS_Car car);

    /**
     * This method returns the cost factor for the variable costs for the specified Scenario case.
     * It must be multiplied with a variable cost per km value.
     *
     * @param simType Scenario or base
     * @return cost factor per km.
     */
    public double getVariableCost_per_km(SimulationType simType) {
        if (simType == SimulationType.BASE) {
            return variable_cost_per_km_base;
        } else {
            return variable_cost_per_km;
        }
    }

    /**
     * If there is a specific velocity for this mode known, it can be got with this method.
     *
     * @return specific velocity in m/s
     */
    public ParamValue getVelocity() {
        if (velocity == null) throw new RuntimeException(
                "getVelocity called, but velocity not set jet in " + this.toString());
        return this.velocity;
    }

    /**
     * Flag determining whether the mode is fix or can be chosen
     *
     * @return fix flag
     */
    public boolean isFix() {
        return isFix;
    }

    /**
     * Sets whether the mode is fix or can be changed.
     *
     * @param isFix boolean value for isFix
     */
    private void setFix(boolean isFix) {
        this.isFix = isFix;
    }

    /**
     * This method check if the mode contains one of the given attributes
     *
     * @param type mode type attributes
     * @return true if the mode contains one attribute, false otherwise
     */
    public boolean isType(ModeType... type) {
        for (ModeType value : type) {
            if (this.isType(value)) return true;
        }
        return false;
    }

    /**
     * This method check if the mode contains the given attribute
     *
     * @param type mode type attribute
     * @return true if the attribute is contained, false otherwise
     */
    public boolean isType(ModeType type) {
        return type.equals(this.getAttribute());
    }

    /**
     * Flag to indicate if time differences to the base scenario are present.
     *
     * @return True if tie travel times of scenario and base differs.
     */
    public boolean isUseBase() {
        return useBase;
    }

    public String toString(String prefix) {
        return prefix + this.getClass().getSimpleName() + " [type=" + this.getAttribute().name() + "]";
    }

    /**
     * Method to check if the travel time is in a valid range
     *
     * @param tt travel time to be checked
     * @return true if tt is a valid travel time
     */
    boolean travelTimeIsInvalid(double tt) {
        boolean returnValue = Double.isNaN(tt) || Double.isInfinite(tt);
        if (!returnValue && (tt < 0.0 || tt >= 100000.0)) { // positive and less than a day + x
            returnValue = true;
        }
        return returnValue;
    }

    /**
     * There exist two different distributions for the modes. One for the mode choice and one for the values of time.
     */
    public enum TPS_ModeCodeType {
        /**
         * Mode distribution for the mode choice tree
         */
        MCT,
        /**
         * Mode distribution for the values of time
         */
        VOT
    }


    /**
     * This enum represents all known mode types of TAPAS
     */
    public enum ModeType {
        //FIXME order is critical and must match the order in the modechoice parameter and expert knowledge arrays!
        WALK(ParamValue.BEELINE_FACTOR_FOOT), BIKE(ParamValue.BEELINE_FACTOR_BIKE), MIT(
                ParamValue.BEELINE_FACTOR_MIT), MIT_PASS(ParamValue.BEELINE_FACTOR_MIT), TAXI(
                ParamValue.BEELINE_FACTOR_MIT), PT(ParamValue.BEELINE_FACTOR_PT), CAR_SHARING(ParamValue.BEELINE_FACTOR_MIT);

        private final ParamValue beelineFactor;

        ModeType(ParamValue beelineFactor) {
            this.beelineFactor = beelineFactor;
        }

        public ParamValue getBeelineFactor() {
            return beelineFactor;
        }


    }


}
