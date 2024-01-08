/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.choice.TravelDistanceCalculator;
import de.dlr.ivf.tapas.choice.TravelTimeCalculator;
import de.dlr.ivf.tapas.choice.traveltime.functions.SimpleMatrixMapFunction;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.mode.Modes;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.parameter.*;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;
import de.dlr.ivf.tapas.model.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.TPS_FastMath;


public class TPS_UtilityMNLFullComplex extends TPS_UtilityMNL {

    private final int automaticVehicleLevel;
    private final SimulationType simType;
    private final double rampUp;
    private final int avTimeModThreshhold;
    private final double avTimeModFar;
    private final double avTimeModNear;
    private final double passProbHouseHoldCar;
    private final double passProbRestricted;
    private final double pnrCostPerTrip;
    private final double pnrCostPerHour;
    private final boolean useExitMaut;
    private final TravelDistanceCalculator distanceCalculator;
    private final boolean chargePassWithEverything;

    private final boolean useFixPtCost;
    private final double interchangeFactor;
    private final SimpleMatrixMapFunction interchangeMatrixMap;
    private final boolean useRoboTaxi;
    private final boolean useCarSharing;
    private final double carSharingAccessAddon;
    private final int avMinDriverAge;

    public TPS_UtilityMNLFullComplex(TravelDistanceCalculator travelDistanceCalculator,TravelTimeCalculator travelTimeCalculator, TPS_ParameterClass parameterClass, Modes modes){
        super(travelTimeCalculator,parameterClass, modes);
        this.automaticVehicleLevel = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_LEVEL);
        this.simType = parameterClass.getSimulationType();
        this.rampUp = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_RAMP_UP_TIME);
        this.avTimeModThreshhold = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD);
        this.avTimeModFar = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR);
        this.avTimeModNear = parameterClass.getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR);
        this.passProbHouseHoldCar = parameterClass.getDoubleValue(ParamValue.PASS_PROBABILITY_HOUSEHOLD_CAR);
        this.passProbRestricted = parameterClass.getDoubleValue(ParamValue.PASS_PROBABILITY_RESTRICTED);
        this.pnrCostPerTrip = parameterClass.getDoubleValue(ParamValue.PNR_COST_PER_TRIP);
        this.pnrCostPerHour = parameterClass.getDoubleValue(ParamValue.PNR_COST_PER_HOUR);
        this.useExitMaut = parameterClass.isTrue(ParamFlag.FLAG_USE_EXIT_MAUT);
        this.distanceCalculator = travelDistanceCalculator;

        this.chargePassWithEverything = parameterClass.isTrue(ParamFlag.FLAG_CHARGE_PASSENGERS_WITH_EVERYTHING);
        this.useFixPtCost = parameterClass.isFalse(ParamFlag.FLAG_FIX_PT_COSTS);
        this.interchangeFactor = 0.01;
        this.interchangeMatrixMap = new SimpleMatrixMapFunction(parameterClass.paramMatrixMapClass.getMatrixMap(ParamMatrixMap.INTERCHANGES_PT, simType));
        this.useRoboTaxi = parameterClass.isDefined(ParamFlag.FLAG_USE_ROBOTAXI) && parameterClass.isTrue(ParamFlag.FLAG_USE_ROBOTAXI);
        this.useCarSharing = parameterClass.isDefined(ParamFlag.FLAG_USE_CARSHARING) && parameterClass.isTrue(ParamFlag.FLAG_USE_CARSHARING);
        this.carSharingAccessAddon = parameterClass.getDoubleValue(ParamValue.CARSHARING_ACCESS_ADDON);
        this.avMinDriverAge = parameterClass.getIntValue(ParamValue.AUTOMATIC_VEHICLE_MIN_DRIVER_AGE);
    }

    double costsForMIV(TPS_Mode mode, TPS_Plan plan, double distanceNet, double travelTime, TPS_ModeChoiceContext mcc, SimulationType simType) {
        double cost;
        TPS_TrafficAnalysisZone comingFromTVZ;
        TPS_TrafficAnalysisZone goingToTVZ;
        double tmpFactor;
        comingFromTVZ = mcc.fromStayLocation.getTrafficAnalysisZone();
        goingToTVZ = mcc.toStayLocation.getTrafficAnalysisZone();
        //fuel cost per km
        if (mcc.carForThisPlan != null) {

            if (goingToTVZ.isRestricted() && mcc.carForThisPlan.isRestricted()) {
                return Double.NaN;
            }
            tmpFactor = (mcc.carForThisPlan.costPerKilometer() +
                    mcc.carForThisPlan.variableCostPerKilometer());
            //TODO: Bad hack to modify the costs according to the av-reduction!

            if (mcc.carForThisPlan.getAutomationLevel() >= automaticVehicleLevel && SimulationType.SCENARIO == simType) {
                //calculate time perception modification
                if (travelTime > rampUp) {
                    double timeMod = distanceNet > avTimeModThreshhold ? avTimeModFar : avTimeModNear;
                    tmpFactor *= timeMod; // modify the costs according to the av-reduction from the first meter!
                }
            }

        } else { //calc cost for a generic car
            boolean carIsRestricted = true;

            if (Randomizer.random() < passProbHouseHoldCar) {
                TPS_Car coDriver = plan.getPerson().getHousehold().getLeastRestrictedCar();
                if (coDriver != null) { //does the household have a car???
                    carIsRestricted = plan.getPerson().getHousehold().getLeastRestrictedCar().isRestricted();
                } else {
                    carIsRestricted = Randomizer.random() < passProbRestricted;
                    //return Double.NaN; // Don't ride with strangers, 'Cause they're only there to do you harm
                }
            } else {
                carIsRestricted = Randomizer.random() < passProbRestricted;
            }

            if (goingToTVZ.isRestricted() && carIsRestricted) {
                return Double.NaN;
            }

            tmpFactor = (mode.getCostPerKm() + mode.getVariableCostPerKm());
        }
        tmpFactor *= 0.001; //km to m


        cost = distanceNet * tmpFactor;

        //should I add costs for a PT-ticket for this ride?
        if ((goingToTVZ.isPNR() || comingFromTVZ.isPNR()) && !plan.getPerson().hasAbo()) {
            cost += pnrCostPerTrip;
            cost += pnrCostPerHour * mcc.duration * 2.7777777777e-4;// stay time
        }

        // location based cost differences
        // determine whether target zone has parking fees or toll (only relevant if entering zone)
        // momentary situation compared to base situation
        boolean carMustPayToll = true;

        //todo revise this line
        //if (mcc.carForThisPlan != null && mcc.carForThisPlan.hasPaidToll || plan.mustPayToll) carMustPayToll = false;

        if (goingToTVZ.hasToll(simType) && carMustPayToll) {
            // toll is relevant as entering a cordon toll zone
            cost += goingToTVZ.getTollFee(simType);
        }
        if (useExitMaut && !goingToTVZ.hasToll(simType) &&
                comingFromTVZ.hasToll(simType) && carMustPayToll) {// scenario:
            //FIXME necessary?
        }
        // diff parking fees
        if (goingToTVZ.hasParkingFee(simType)
				/*
				&&actCode!=1&&actCode!=2&&actCode!=7 //not for work, school or university
				*/) {// scenario
            cost += goingToTVZ.getParkingFee(simType) * mcc.duration * 2.7777777777e-4;// stay
        }
        return cost;
    }

    @Override
    /**
     * Utility function, which implements the mnl-model according to the complex  model developed by Alexander Kihm. See https://wiki.dlr.de/confluence/display/MUM/Modalwahl+in+TAPAS
     *
     */
    public double getCostOfMode(TPS_Mode mode, TPS_Plan plan, double travelTime, TPS_ModeChoiceContext mcc, SimulationType simType) {
        double cost = 0;
        double[] parameters = this.parameterMap.get(mode);

        double expInterChanges = 0;
        double distanceNet = distanceCalculator.getDistance(mcc.fromStayLocation, mcc.toStayLocation, ModeType.MIT); //correct the distance to the actual value!

        switch (mode.getModeType()) {
            case WALK:
                cost = mode.getCostPerKm() * distanceNet * 0.001;
                break;
            case BIKE:
                cost = mode.getCostPerKm() * distanceNet * 0.001;
                break;
            case MIT:
                cost = costsForMIV(mode, plan, distanceNet, travelTime, mcc, simType);
                break;
            case MIT_PASS:
                if (chargePassWithEverything) {
                    cost = costsForMIV(mode, plan, distanceNet, travelTime, mcc, simType);
                } else {
                    cost = mode.getCostPerKm() * distanceNet * 0.001;
                }

                break;
            case TAXI:
                //Todo: HACK: No separate access /egress-times for taxi. Assumption: 5min waiting but no egress!
                //Car has a general 4min waiting so add one minute
                travelTime += 60; // add one more minute for TAXI
                //TODO: make hard coded values configurable
                /*
                 * Taxi tarif Berlin:
                 * 3€ base charge
                 * first 7 km: 1.58€
                 * after that: 1.20€
                 */
                double baseCharge = 3.0;
                double shortCharge = Math.min(7000, distanceNet) * 0.001 * 1.58;
                double longCharge = Math.max(0, distanceNet - 7000) * 0.001 * 1.20;
                cost = baseCharge + shortCharge + longCharge;
                break;
            case PT:

                if (!plan.getPerson().hasAbo()) {
                    if (simType == SimulationType.BASE) {
                        cost = mode.getCostPerKmBase();
                    } else {
                        cost = mode.getCostPerKm();
                    }
                    if (!useFixPtCost) {//cost per kilometer!
                        cost *= distanceNet * 0.001;
                    }
                }

                expInterChanges = TPS_FastMath.exp(
                        interchangeMatrixMap.apply(mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime)) * interchangeFactor;

                break;
            case CAR_SHARING: //car sharing-faker
                if ((useCarSharing && plan.getPerson().isCarPooler()) //cs user?
                        || useRoboTaxi // is robotaxi
                ) { //no cs user!
                    //service area in scenario?
                    if (!mcc.toStayLocation.getTrafficAnalysisZone().isCarSharingService(SimulationType.SCENARIO) ||
                            !mcc.fromStayLocation.getTrafficAnalysisZone().isCarSharingService(
                                    SimulationType.SCENARIO)) {
                        return Double.NaN;
                    }
                    // time equals MIT + 3 min more access
                    travelTime = travelTimeCalculator.getTravelTime(mode, mcc.fromStayLocation, mcc.toStayLocation,
                            mcc.startTime, TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY,
                            plan.getPerson(), mcc.carForThisPlan);
                    travelTime += carSharingAccessAddon; //Longer Access for Carsharing
                    cost = mode.getCostPerKm() * distanceNet * 0.001;
                } else {
                    cost = Double.NaN;
                }
                break;
        }

        if (Double.isNaN(cost)) return Double.NaN;

        // put together
        boolean work = false, education = false, shopping = false, errant = false, leisure = false;
        TPS_Stay ref = mcc.fromStay == null ? mcc.toStay : mcc.fromStay; //es muss einen geben!!!!
        if (ref.getSchemePart().isTourPart()) { // homeparts do not have attributes listed below!
            TPS_TourPart tourPart = (TPS_TourPart) ref.getSchemePart();
            work = tourPart.hasWorkActivity;
            education = tourPart.hasEducationActivity;
            shopping = tourPart.hasShoppingActivity;
            errant = tourPart.hasErrantActivity;
            leisure = tourPart.hasLeisureActivity;
        }


        return parameters[0] +  // mode constant
                parameters[1] * travelTime + // beta travel time
                parameters[2] * cost + // beta costs
                parameters[3] * plan.getPerson().getAge() + //alter
                parameters[4] * plan.getPerson().getAge() * plan.getPerson().getAge() + //quadratisches alter
                parameters[5] * plan.getPerson().getHousehold().getNumberOfCars() + // anzahl autos
                parameters[6] * expInterChanges + //umstiege (nur ÖV)
                //ab jetzt binär-Betas, also Ja/nein
                (plan.getPerson().mayDriveACar(mcc.carForThisPlan,avMinDriverAge, automaticVehicleLevel) ? parameters[7] : 0) + //führerschein
                (plan.getPerson().hasAbo() ? parameters[8] : 0) + //Öffi -abo
                (work ? parameters[9] : 0) + //tourpart mit Arbeit
                (education ? parameters[10] : 0) + //tourpart mit Bildung
                (shopping ? parameters[11] : 0) + //tourpart mit Einkauf
                (errant ? parameters[12] : 0) + //tourpart mit Erledigung
                (leisure ? parameters[13] : 0);
    }

    @Override
    public void setDistributionCalculator(ModeDistributionCalculator modeDistributionCalculator) {
        super.setDistributionCalculator(modeDistributionCalculator);
    }
}
