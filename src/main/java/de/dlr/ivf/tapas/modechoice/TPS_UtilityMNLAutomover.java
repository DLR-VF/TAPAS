/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.TPS_FastMath;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamMatrixMap;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;

public class TPS_UtilityMNLAutomover extends TPS_UtilityMNLFullComplex {

    @Override
    /**
     * Utility function, which implements the mnl-model according to the complex  model developed by Alexander Kihm. See https://wiki.dlr.de/confluence/display/MUM/Modalwahl+in+TAPAS
     *
     */ public double getCostOfMode(TPS_Mode mode, TPS_Plan plan, double travelTime, TPS_ModeChoiceContext mcc,/*TPS_Location locComingFrom, TPS_Location locGoingTo, double startTime, double durationStay, TPS_Car car, boolean fBike,*/ SimulationType simType/*, TPS_Stay stay*/) {
        double cost = 0;
        double[] parameters = this.parameterMap.get(mode);

        double expInterChanges = 0;
        double distanceNet = mode.getDistance(mcc.fromStayLocation, mcc.toStayLocation, simType,
                mcc.carForThisPlan); //correct the distance to the actual value!

        switch (mode.getAttribute()) {
            case WALK:
                cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
                break;
            case BIKE:
                cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
                break;
            case MIT:
                cost = costsForMIV(mode, plan, distanceNet, travelTime, mcc, simType);
                break;
            case MIT_PASS:
                if (mode.getParameters().isTrue(ParamFlag.FLAG_CHARGE_PASSENGERS_WITH_EVERYTHING)) {
                    cost = costsForMIV(mode, plan, distanceNet, travelTime, mcc, simType);
                } else {
                    cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
                }
                break;
            case TAXI:
                //Todo: HACK: No separate access /egress-times for taxi. Assumption: 5min waiting but no egress!
                //Car has a general 4min waiting so add one minute
//                travelTime += 60; // add one more minute for TAXI
                //TODO: make hard coded values configurable
                /*
                 * Taxi tarif Berlin:
                 * 3€ base charge
                 * first 7 km: 1.58€
                 * after that: 1.20€
                 */
//                double baseCharge = 3.0;
//                double shortCharge = Math.min(7000, distanceNet) * 0.001 * 1.58;
//                double longCharge = Math.max(0, distanceNet - 7000) * 0.001 * 1.20;
//                cost = baseCharge + shortCharge + longCharge;
                cost = distanceNet * 0.001 * 0.6;
                break;
            case PT:

                if (!plan.getPerson().hasAbo()) {
                    if (simType == SimulationType.BASE) {
                        cost = mode.getParameters().getDoubleValue(ParamValue.PT_COST_PER_KM_BASE);
                    } else {
                        cost = mode.getParameters().getDoubleValue(ParamValue.PT_COST_PER_KM);
                    }
                    if (mode.getParameters().isFalse(ParamFlag.FLAG_FIX_PT_COSTS)) {//cost per kilometer!
                        cost *= distanceNet * 0.001;
                    }
                }

                expInterChanges = TPS_FastMath.exp(mode.getParameters().paramMatrixMapClass
                        .getValue(ParamMatrixMap.INTERCHANGES_PT, mcc.fromStayLocation.getTAZId(),
                                mcc.toStayLocation.getTAZId(), mcc.startTime) * TPS_DB_IO.INTERCHANGE_FACTOR);

                break;
            case TRAIN: //shared automatic vehicle-faker
                cost = distanceNet * 0.001 * 0.4;
                break;
//                if ((mode.getParameters().isDefined(ParamFlag.FLAG_USE_CARSHARING) && mode.getParameters().isTrue(
//                        ParamFlag.FLAG_USE_CARSHARING) && plan.getPerson().isCarPooler()) //cs user?
//                        || (mode.getParameters().isDefined(ParamFlag.FLAG_USE_ROBOTAXI) && mode.getParameters().isTrue(
//                        ParamFlag.FLAG_USE_ROBOTAXI)) // is robotaxi
//                ) { //no cs user!
//                    //service area in scenario?
//                    if (!mcc.toStayLocation.getTrafficAnalysisZone().isCarSharingService(SimulationType.SCENARIO) ||
//                            !mcc.fromStayLocation.getTrafficAnalysisZone().isCarSharingService(
//                                    SimulationType.SCENARIO)) {
//                        return Double.NaN;
//                    }
//                    // time equals MIT + 3 min more access
//                    travelTime = TPS_Mode.get(ModeType.MIT).getTravelTime(mcc.fromStayLocation, mcc.toStayLocation,
//                            mcc.startTime, simType, TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY,
//                            plan.getPerson(), mcc.carForThisPlan);
//                    travelTime += mode.getParameters().getDoubleValue(
//                            ParamValue.CARSHARING_ACCESS_ADDON); //Longer Access for Carsharing
//                    cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
//                } else {
//                    cost = Double.NaN;
//                }
//                break;
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
                (plan.getPerson().mayDriveACar() ? parameters[7] : 0) + //führerschein
                (plan.getPerson().hasAbo() ? parameters[8] : 0) + //Öffi -abo
                (work ? parameters[9] : 0) + //tourpart mit Arbeit
                (education ? parameters[10] : 0) + //tourpart mit Bildung
                (shopping ? parameters[11] : 0) + //tourpart mit Einkauf
                (errant ? parameters[12] : 0) + //tourpart mit Erledigung
                (leisure ? parameters[13] : 0);
    }


}
