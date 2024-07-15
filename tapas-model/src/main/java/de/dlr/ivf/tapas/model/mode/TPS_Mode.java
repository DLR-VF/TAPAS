/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.mode;

import de.dlr.ivf.tapas.model.constants.TPS_InternalConstant;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Collection;
/**
 * This class represents the basic features of a mode. It is characterized by a type and a name.
 * <p>
 * Every mode is a singleton so the type and the name have to be unique. This is realized by the visibility of the
 * constructor. Only classes in this package have access.
 * <p><
 * <p>
 * Furthermore there are two abstract methods to calculate the distance and the travel time between two locations.
 */

@Builder
@Getter
public class TPS_Mode {

    public static final ModeType[] MODE_TYPE_ARRAY = new ModeType[]{ModeType.WALK, ModeType.BIKE, ModeType.MIT, ModeType.MIT_PASS, ModeType.TAXI, ModeType.PT, ModeType.CAR_SHARING};
    public static final double NO_CONNECTION = -1;
    public static final double VISUM_NO_CONNECTION = 99999.0;

    private final double velocity;
    private final double costPerKm;
    private final double costPerKmBase;
    private final double variableCostPerKm;
    private final double variableCostPerKmBase;
    private final boolean useBase;
    private final double beelineFactor;
    @Singular
    private final Collection<TPS_InternalConstant<TPS_ModeCodeType>> internalConstants;
    private final String name;

    private final int id;


    /**
     * This flag determines whether the choice for this mode has to be fix or not, i.e. when I decide to drive to work by car
     * then I have to take it back home too (fix = true). If I go to work by taxi then I can take the bus to go home (fix =
     * false).
     */
    private final boolean isFix;
    /**
     * type (Name) of the mode
     */
    private final ModeType modeType;

    public boolean isUseBase(){
        return this.useBase;
    }

//
//    /**
//     * Method returns utility function.
//     * Must be initialized first by the initUtilityFunction
//     *
//     * @return utility function
//     */
//    public TPS_UtilityFunction getUtilityFunction() {
//        return UTILITY_FUNCTION;
//    }

    public double getCostPerKmBase() {
        return costPerKmBase;
    }

    public double getVariableCostPerKmBase() {
        return variableCostPerKmBase;
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

//    /**
//     * This method initializes the utility function.
//     *
//     */
//    public void initUtilityFunction(Class<? extends TPS_UtilityFunction> utilityFunctionClass) {
//        try {
//            UTILITY_FUNCTION = utilityFunctionClass.getDeclaredConstructor().newInstance();
//        } catch (Exception e) {
//            // this is bad style, but the above four lines produce way to many exceptions, which are all related to "ClassNotFound"
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * Method to check if the returned value is a "no connection". Inverse function is hasConnection.
     *
     * @param val the value to check
     * @return true if no connection is detected
     */
    public static boolean noConnection(double val) {
        return val < 0 || val <= TPS_Mode.NO_CONNECTION;
    }


//    /**
//     * Gets the Delta of costs for this mode
//     */
//    public double calculateDelta(TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
//        return UTILITY_FUNCTION.calculateDelta(plan, distanceNet, mcc);
//    }


    /**
     * This method returns the cost factor for this mode for the specified Scenario case. It must be multiplied with a cost per km value.
     *
     * @return cost factor per km.
     */
    public double getCostPerKm() {
        return this.costPerKm;
    }


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

    public double getBeelineFactor(){
        return this.beelineFactor;
    }

    /**
     * This method returns the cost factor for the variable costs for the specified Scenario case.
     * It must be multiplied with a variable cost per km value.
     *

     * @return cost factor per km.
     */
    public double getVariableCostPerKm() {

        return this.variableCostPerKm;
    }

    /**
     * If there is a specific velocity for this mode known, it can be got with this method.
     *
     * @return specific velocity in m/s
     */
    public double getVelocity() {
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

    public int getCodeVot() {
        return internalConstants.stream().filter(ic -> ic.getType() == TPS_ModeCodeType.VOT).findFirst().map(TPS_InternalConstant::getCode).get();
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
        WALK, BIKE, MIT, MIT_PASS, TAXI, PT, CAR_SHARING
    }


}
