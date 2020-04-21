package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TPS_UtilityMNLFullComplexIntermodal extends TPS_UtilityMNL {

    static BufferedWriter writer = null;
    static boolean writeStats = false;

    @Override

    /**
     * Utility function, which implements the mnl-model according to the complex  model developped by Alexander Kihm.
	 * See https://wiki.dlr.de/confluence/display/MUM/Modalwahl+in+TAPAS
     * @author hein_mh
     *
     */ public double getCostOfMode(TPS_Mode mode, TPS_Plan plan, double distanceNet, double travelTime,
                                    TPS_ModeChoiceContext mcc, SimulationType simType) {
        if (writeStats && writer == null) {
            try {
                writer = new BufferedWriter(new FileWriter("d:\\measures.txt"));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


        double cost = 0;
        double[] parameters = this.parameterMap.get(mode);

        double modeConstant = parameters[0];
        double expInterChanges = 0;
        distanceNet = mode.getDistance(mcc.fromStayLocation, mcc.toStayLocation, simType,
                mcc.carForThisPlan); //correct the distance to the actual value!

        boolean work = false, education = false, shopping = false, errant = false, leisure = false;
        if (mcc.toStay.getSchemePart().isTourPart()) { // homeparts do not have attributes listed below!
            TPS_TourPart tourPart = (TPS_TourPart) mcc.toStay.getSchemePart();
            work = tourPart.hasWorkActivity;
            education = tourPart.hasEducationActivity;
            shopping = tourPart.hasShoppingActivity;
            errant = tourPart.hasErrantActivity;
            leisure = tourPart.hasLeisureActivity;
        }

        switch (mode.getAttribute()) {
            case WALK:
                cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
                break;
            case BIKE:
                cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
                break;
            case MIT:
                TPS_TrafficAnalysisZone comingFromTVZ = mcc.fromStayLocation.getTrafficAnalysisZone();
                TPS_TrafficAnalysisZone goingToTVZ = mcc.toStayLocation.getTrafficAnalysisZone();
                if (goingToTVZ.isRestricted() && mcc.carForThisPlan.isRestricted()) {
                    return Double.NaN;
                }
                //fuel cost per km
                double tmpFactor;
                if (mcc.carForThisPlan != null) {
                    tmpFactor = (mcc.carForThisPlan.getCostPerKilometer(simType) +
                            mcc.carForThisPlan.getVariableCostPerKilometer(simType));
                } else {
                    tmpFactor = (mode.getCost_per_km(simType) + mode.getVariableCost_per_km(simType));
                }
                tmpFactor *= 0.001; //km to m
                cost = distanceNet * tmpFactor;

                //should I add costs for a PT-ticket for this ride?
                if ((goingToTVZ.isPNR() || comingFromTVZ.isPNR()) && !plan.getPerson().hasAbo()) {
                    cost += mode.getParameters().getDoubleValue(ParamValue.PNR_COST_PER_TRIP);
                    cost += mode.getParameters().getDoubleValue(ParamValue.PNR_COST_PER_HOUR) * mcc.duration *
                            2.7777777777e-4;// stay time
                }

                // location based cost differences
                // determine whether target zone has parking fees or toll (only relevant if entering zone)
                // momentary situation compared to base situation
                if (goingToTVZ.hasToll(simType)) {
                    // toll is relevant as entering a cordon toll zone
                    cost += goingToTVZ.getTollFee(simType);
                }

                if (mode.getParameters().isTrue(ParamFlag.FLAG_USE_EXIT_MAUT)) {
                    if (!goingToTVZ.hasToll(simType) && comingFromTVZ.hasToll(simType)) {// scenario:
                        // toll is relevant as leaving a cordon toll zone
                        cost += comingFromTVZ.getTollFee(simType);
                    }
                }

                // diff parking fees
                if (goingToTVZ.hasParkingFee(simType)
					/*
					&&actCode!=1&&actCode!=2&&actCode!=7 //not for work, school or university 
					*/) {// scenario
                    cost += goingToTVZ.getParkingFee(simType) * mcc.duration * 2.7777777777e-4;// stay
                }

                break;
            case MIT_PASS:
                cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
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
                boolean workT = false, educationT = false, shoppingT = false, errantT = false, leisureT = false;
                double sizeT = 1;
                boolean workK = false, educationK = false, shoppingK = false, errantK = false, leisureK = false;
                double sizeK = 0;

                StringBuilder sb = new StringBuilder();


                // !!!
                // TODO: check what "Hauptzweck" is; should be the part of the tour with the highest priority
                // !!!
                if (mcc.isBikeAvailable || (mcc.carForThisPlan != null && plan.getPerson().mayDriveACar())) {
                    if (mcc.toStay.getSchemePart().isTourPart()) { // homeparts do not have attributes listed below!
                        TPS_TourPart tourPart = (TPS_TourPart) mcc.toStay.getSchemePart();
                        sizeT = 0.;//(double) tourPart.size();
                        for (@SuppressWarnings("unused") TPS_Stay s : tourPart.getStayIterator()) {
                            sizeT += 1.;
                        }
                        workT = tourPart.hasWorkActivity;
                        educationT = tourPart.hasEducationActivity;
                        shoppingT = tourPart.hasShoppingActivity;
                        errantT = tourPart.hasErrantActivity;
                        leisureT = tourPart.hasLeisureActivity;
                    }
                    for (TPS_TourPart tourPart : plan.getScheme().getTourPartIterator()) {
                        sizeK += 1;
                        workK |= tourPart.hasWorkActivity;
                        educationK |= tourPart.hasEducationActivity;
                        shoppingK |= tourPart.hasShoppingActivity;
                        errantK |= tourPart.hasErrantActivity;
                        leisureK |= tourPart.hasLeisureActivity;
                    }
                }

                // Rad+ÖPNV_TAPAS_ÖPNV_B
                double pCombineWithBike = 0;
                int ptBikeAccessTAZId = -1;
                int ptBikeEgressTAZId = -1;
                if (mcc.isBikeAvailable) {
                    ptBikeAccessTAZId = (int) mode.getParameters().paramMatrixMapClass
                            .getValue(ParamMatrixMap.PTBIKE_ACCESS_TAZ,
                                    mcc.fromStayLocation.getTrafficAnalysisZone().getTAZId(),
                                    mcc.toStayLocation.getTrafficAnalysisZone().getTAZId(),mcc.startTime);
                    if (ptBikeAccessTAZId > 0) {
                        ptBikeEgressTAZId = (int) mode.getParameters().paramMatrixMapClass
                                .getValue(ParamMatrixMap.PTBIKE_EGRESS_TAZ,
                                        mcc.fromStayLocation.getTrafficAnalysisZone().getTAZId(),
                                        mcc.toStayLocation.getTrafficAnalysisZone().getTAZId(),mcc.startTime);
                    }
                    if (ptBikeEgressTAZId > 0) {
                        double combinesWithBikeB = 0.141320637281871 * (double) plan.getPerson().getAge() +
                                -0.00160474148006888 * (double) plan.getPerson().getAge() * plan.getPerson().getAge() +
                                1.27962291294572 * ((plan.getPerson().isPupil() ||
                                        plan.getPerson().isStudent()) ? 1. : 0.) +

                                -0.629866549978136 * (double) plan.getPerson().getHousehold().getNumGrownups() +
                                0.148756452170525 * (double) plan.getPerson().getHousehold().getNumChildren() +
                                0.155346976954901 * (double) plan.getPerson().getHousehold().getNumHalfEmployed() +
                                4.78901315350237E-08 * plan.getPerson().getHousehold().getIncome() *
                                        plan.getPerson().getHousehold().getIncome() +
                                0.77164373393662 * (double) plan.getPerson().getHousehold().getNumCarDrivers() +
                                //0 * (plan.getPerson().getHousehold().getCarNumber()==0 ? 1. : 0) +// !!! prüfen
                                -0.643042179466235 * (plan.getPerson().getHousehold().getCarNumber() == 1 ? 1. : 0.) +
                                -0.725748521675366 * (plan.getPerson().getHousehold().getCarNumber() == 2 ? 1. : 0.) +

                                0.0463649704603541 * distanceNet / 1000. +

                                0.497463612431901 * sizeK + 0 * (workK ? 1. : 0) +
                                0.839470117785873 * (educationK ? 1. : 0.) +
                                1.3512939071016 * (errantK ? 1. : 0.) +
                                1.01952548420693 * (shoppingK ? 1. : 0.) +
                                0.468623879577511 * (leisureK ? 1. : 0.) +

                                -0.40157254397825 * sizeT + 0 * (workT ? 1. : 0) +
                                -2.02239144139734 * (educationT ? 1. : 0.) +
                                -2.07278998520244 * (errantT ? 1. : 0.) +
                                -2.12972593375702 * (shoppingT ? 1. : 0.) +
                                -1.68198344209646 * (leisureT ? 1. : 0.) +

                                -0.574793618624035 * sizeK +// !!!prüfen - Tour?
                                0.34876419149304 * (educationK ? 1. : 0.) +// !!!prüfen - Tour?
                                0.40438631333948 * (leisureK ? 1. : 0.) +// !!!prüfen - Tour?
                                -7.76513723740481;

                        pCombineWithBike = 1. / (1. + Math.exp(-combinesWithBikeB));
                    }
                }
                if (writeStats)
                    sb.append(mcc.isBikeAvailable).append(';').append(pCombineWithBike).append(';')
                            .append(ptBikeAccessTAZId).append(';').append(ptBikeEgressTAZId).append(';');

                // Car+ÖPNV_TAPAS_ÖPNV_B
                double pCombineWithCar = 0;
                int ptCarAccessTAZId = -1;
                if (mcc.carForThisPlan != null && plan.getPerson().mayDriveACar()) {
                    ptCarAccessTAZId = (int) mode.getParameters().paramMatrixMapClass
                            .getValue(ParamMatrixMap.PTCAR_ACCESS_TAZ,
                                    mcc.fromStayLocation.getTrafficAnalysisZone().getTAZId(),
                                    mcc.toStayLocation.getTrafficAnalysisZone().getTAZId(),mcc.startTime);
                    if (mcc.toStayLocation.getTrafficAnalysisZone().isRestricted() &&
                            mcc.carForThisPlan.isRestricted()) {
                        ptCarAccessTAZId = -1;
                    }
                    if (ptCarAccessTAZId > 0) {
                        double combinesWithCarB =
                                0.473495331672679 * (plan.getPerson().mayDriveACar() ? 1. : 0.) +
                                        -2.46390987118912 * (
                                                plan.getPerson().getHousehold().getCarNumber() == 0 ? 1. : 0.) +
                                        // !!! prüfen
                                        -0.295204809067696 * (plan.getPerson().hasAbo() ? 1. : 0.) +

                                        -0.663841245241534 *
                                                (double) plan.getPerson().getHousehold().getNumMalePersons() +
                                        0.000175464954796569 * plan.getPerson().getHousehold().getIncome() +
                                        0 * (plan.getPerson().getHousehold().getCarNumber() == 0 ? 1. : 0.) +
                                        // !!! prüfen
                                        -0.164594273391178 * (
                                                plan.getPerson().getHousehold().getCarNumber() == 1 ? 1. : 0.) +
                                        // !!! prüfen
                                        1.00960568985536 * (
                                                plan.getPerson().getHousehold().getCarNumber() == 2 ? 1. : 0.) +
                                        // !!! prüfen

                                        0.07887060144215 * distanceNet / 1000. + // !!! prüfen

                                        -0.211469360657672 * sizeK +// !!!prüfen - Kette?
                                        0 * (workK ? 1. : 0) + -0.180768877177397 * (educationK ? 1. : 0.) +
                                        -1.55855402377174 * (errantK ? 1. : 0.) +
                                        // !!! prüfen - Hauptzweck, errant=priv. Erledigungen?
                                        -1.80437905346604 * (shoppingK ? 1. : 0.) +// !!! prüfen - Hauptzweck?
                                        -0.814605797582529 * (leisureK ? 1. : 0.) +// !!! prüfen - Hauptzweck?

                                        0.293226404357034 * sizeT +// !!!prüfen - Tour?
                                        0 * (workT ? 1. : 0.) +// !!! prüfen - Hauptzweck?
                                        -0.879662952772845 * (educationT ? 1. : 0.) +
                                        // !!! prüfen - Hauptzweck?
                                        1.71873275466625 * (errantT ? 1. : 0.) +
                                        // !!! prüfen - Hauptzweck, errant=priv. Erledigungen?
                                        1.41425149891885 * (shoppingT ? 1. : 0.) +// !!! prüfen - Hauptzweck?
                                        0.298604609664767 * (leisureT ? 1. : 0.) +// !!! prüfen - Hauptzweck?

                                        0.737956530296847 * (educationK ? 1. : 0.) +// !!!prüfen - Tour?
                                        0.544761874732552 * (leisureK ? 1. : 0.) +// !!!prüfen - Tour?
                                        -4.45118725324756;

                        pCombineWithCar = 1. / (1. + Math.exp(-combinesWithCarB));
                    }
                }
                if (writeStats)
                    sb.append(mcc.carForThisPlan != null && plan.getPerson().mayDriveACar()).append(';')
                            .append(pCombineWithCar).append(';').append(ptCarAccessTAZId).append(';');

                double val = Math.random();
                int combi = 0;
                double cpCombineWithBike = pCombineWithBike *
                        mode.getParameters().getDoubleValue(ParamValue.PTBIKE_MODE_PROB_FACTOR);
                double cpCombineWithCar = pCombineWithCar *
                        mode.getParameters().getDoubleValue(ParamValue.PTCAR_MODE_PROB_FACTOR);
                if (val < cpCombineWithBike) {
                    combi = 1; // with bike
                    mcc.combinedMode = TPS_Mode.get(ModeType.BIKE);
                    modeConstant = mode.getParameters().getDoubleValue(ParamValue.PTBIKE_MODE_CONSTANT);
                } else if ((val - cpCombineWithBike) < cpCombineWithCar) {
                    combi = 2; // with car
                    mcc.combinedMode = TPS_Mode.get(ModeType.MIT);
                    modeConstant = mode.getParameters().getDoubleValue(ParamValue.PTCAR_MODE_CONSTANT);
                }
                if (writeStats)
                    sb.append(val).append(';').append(combi).append(';');

                double numInterchanges = 0;
                switch (combi) {
                    case 0:
                        break;
                    case 1:
                        // pt+bike
                        TPS_TrafficAnalysisZone accessTAZ = mcc.fromStayLocation.getTrafficAnalysisZone().getRegion()
                                .getTrafficAnalysisZone(ptBikeAccessTAZId);
                        TPS_TrafficAnalysisZone egressTAZ = mcc.fromStayLocation.getTrafficAnalysisZone().getRegion()
                                .getTrafficAnalysisZone(ptBikeEgressTAZId);

                        if (accessTAZ == null || egressTAZ == null) {
                            ptBikeAccessTAZId = (int) mode.getParameters().paramMatrixMapClass
                                    .getValue(ParamMatrixMap.PTBIKE_ACCESS_TAZ,
                                            mcc.fromStayLocation.getTrafficAnalysisZone().getTAZId(),
                                            mcc.toStayLocation.getTrafficAnalysisZone().getTAZId(),mcc.startTime);
                            ptBikeEgressTAZId = (int) mode.getParameters().paramMatrixMapClass
                                    .getValue(ParamMatrixMap.PTBIKE_EGRESS_TAZ,
                                            mcc.fromStayLocation.getTrafficAnalysisZone().getTAZId(),
                                            mcc.toStayLocation.getTrafficAnalysisZone().getTAZId(),mcc.startTime);
                            accessTAZ = mcc.fromStayLocation.getTrafficAnalysisZone().getRegion()
                                    .getTrafficAnalysisZone(ptBikeAccessTAZId);
                            egressTAZ = mcc.fromStayLocation.getTrafficAnalysisZone().getRegion()
                                    .getTrafficAnalysisZone(ptBikeEgressTAZId);
                        }

                        double travelTimeBike = TPS_Mode.get(ModeType.BIKE)
                                .getTravelTime(mcc.fromStayLocation, accessTAZ, mcc.startTime, simType,
                                        TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY, plan.getPerson(),
                                        mcc.carForThisPlan);
                        double travelTimeBike1 = travelTimeBike;
                        double travelTimePT = mode.getParameters().paramMatrixMapClass
                                .getValue(ParamMatrixMap.TRAVEL_TIME_PT, ptBikeAccessTAZId, ptBikeEgressTAZId, simType,
                                        (int)(mcc.startTime + travelTimeBike));//TPS_Mode.get(ModeType.PT).getTravelTime(accessTAZ, egressTAZ,
						// mcc.startTime + travelTimeBike, simType, TPS_ActivityCode.DUMMY, TPS_ActivityCode.DUMMY,
						// plan.getPerson(), mcc.carForThisPlan);
                        travelTimeBike += TPS_Mode.get(ModeType.BIKE).getTravelTime(egressTAZ, mcc.toStayLocation,
                                (int)(mcc.startTime + (travelTimeBike + travelTimePT)) , simType, TPS_ActivityConstant.DUMMY,
                                TPS_ActivityConstant.DUMMY, plan.getPerson(), mcc.carForThisPlan);

                        double distanceNetBike = TPS_Mode.get(ModeType.BIKE)
                                .getDistance(mcc.fromStayLocation, accessTAZ, simType, null) +
                                TPS_Mode.get(ModeType.BIKE).getDistance(egressTAZ, mcc.toStayLocation, simType, null);
                        double distanceNetPT = mode.getDistance(accessTAZ, egressTAZ, simType, mcc.carForThisPlan);

                        double costPT = 0;
                        double costBike = TPS_Mode.get(ModeType.BIKE).getCost_per_km(simType) * distanceNetBike * 0.001;
                        if (!plan.getPerson().hasAbo()) {
                            if (simType == SimulationType.BASE) {
                                costPT = mode.getParameters()
                                        .getDoubleValue(ParamValue.PTBIKE_COST_PER_KM_BASE); // + Rad!!!
                            } else {
                                costPT = mode.getParameters().getDoubleValue(ParamValue.PTBIKE_COST_PER_KM); // + Rad!!!
                            }
                            if (mode.getParameters().isFalse(ParamFlag.FLAG_FIX_PT_COSTS)) {//cost per kilometer!
                                costPT *= distanceNetPT * 0.001;
                            }
                        }

                        double tttmp = travelTimePT + travelTimeBike;

                        if (writeStats)
                            sb.append(travelTimeBike1).append(';').append(travelTimePT).append(';')
                                    .append(travelTimeBike - travelTimeBike1).append(';').append(travelTime)
                                    .append(';');

                        if (travelTime * 1.5 < tttmp/*||tttmp-600>travelTime*/) {
                            combi = 0;
                        } else {
                            travelTime = travelTimePT + travelTimeBike;
                            distanceNet = distanceNetBike + distanceNetPT;
                            cost = costBike + costPT;
                            // travel time
                            // !!! todo: correct number of interchanges
                            numInterchanges = mode.getParameters().paramMatrixMapClass
                                    .getValue(ParamMatrixMap.PTBIKE_INTERCHANGES, ptBikeAccessTAZId, ptBikeEgressTAZId,
                                            (int)(mcc.startTime + travelTimeBike1));
                            numInterchanges = Math.max(0, numInterchanges - 100.);
                            expInterChanges = TPS_FastMath.exp(numInterchanges * TPS_DB_IO.INTERCHANGE_FACTOR);
                            if (writeStats)
                                sb.append(travelTime).append(';').append(distanceNet).append(';').append(cost)
                                        .append(';').append(numInterchanges).append(';');
                        }
                        break;
                    case 2:
                        // pt+car
                        accessTAZ = mcc.fromStayLocation.getTrafficAnalysisZone().getRegion()
                                .getTrafficAnalysisZone(ptCarAccessTAZId);
                        double travelTimeCar = TPS_Mode.get(ModeType.MIT)
                                .getTravelTime(mcc.fromStayLocation, accessTAZ, mcc.startTime, simType,
                                        TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY, plan.getPerson(),
                                        mcc.carForThisPlan);
                        travelTimePT = mode.getParameters().paramMatrixMapClass
                                .getValue(ParamMatrixMap.TRAVEL_TIME_PT, ptCarAccessTAZId,
                                        mcc.toStayLocation.getTrafficAnalysisZone().getTAZId(), simType,
                                        (int)(mcc.startTime + travelTimeCar));
                        double distanceNetCar = TPS_Mode.get(ModeType.MIT)
                                .getDistance(mcc.fromStayLocation, accessTAZ, simType, mcc.carForThisPlan);
                        distanceNetPT = mode.getDistance(accessTAZ, mcc.toStayLocation, simType, mcc.carForThisPlan);

                        if (writeStats)
                            sb.append(travelTimeCar).append(';').append(travelTimePT).append(';').append(travelTime)
                                    .append(';');

                        costPT = 0;
                        if (!plan.getPerson().hasAbo()) {
                            if (simType == SimulationType.BASE) {
                                costPT = mode.getParameters().getDoubleValue(ParamValue.PT_COST_PER_KM_BASE);
                            } else {
                                costPT = mode.getParameters().getDoubleValue(ParamValue.PT_COST_PER_KM);
                            }
                            if (mode.getParameters().isFalse(ParamFlag.FLAG_FIX_PT_COSTS)) {//cost per kilometer!
                                costPT *= distanceNetPT * 0.001;
                            }
                        }

                        if (mcc.carForThisPlan != null) {
                            tmpFactor = (mcc.carForThisPlan.getCostPerKilometer(simType) +
                                    mcc.carForThisPlan.getVariableCostPerKilometer(simType));
                        } else {
                            tmpFactor = (mode.getCost_per_km(simType) + mode.getVariableCost_per_km(simType));
                        }
                        tmpFactor *= 0.001; //km to m
                        double costCar = distanceNetCar * tmpFactor;

                        comingFromTVZ = mcc.fromStayLocation.getTrafficAnalysisZone();
                        goingToTVZ = accessTAZ;//mcc.toStayLocation.getTrafficAnalysisZone();
                        //should I add costs for a PT-ticket for this ride?
                        if ((goingToTVZ.isPNR() || comingFromTVZ.isPNR()) && !plan.getPerson().hasAbo()) {
                            costCar += mode.getParameters().getDoubleValue(ParamValue.PNR_COST_PER_TRIP);
                            costCar += mode.getParameters().getDoubleValue(ParamValue.PNR_COST_PER_HOUR) *
                                    mcc.duration * 2.7777777777e-4;// stay time
                        }

                        // location based cost differences
                        // determine whether target zone has parking fees or toll (only relevant if entering zone)
                        // momentary situation compared to base situation
                        if (goingToTVZ.hasToll(simType)) {
                            // toll is relevant as entering a cordon toll zone
                            costCar += goingToTVZ.getTollFee(simType);
                        }

                        if (mode.getParameters().isTrue(ParamFlag.FLAG_USE_EXIT_MAUT)) {
                            if (!goingToTVZ.hasToll(simType) && comingFromTVZ.hasToll(simType)) {// scenario:
                                // toll is relevant as leaving a cordon toll zone
                                costCar += comingFromTVZ.getTollFee(simType);
                            }
                        }

                        // diff parking fees
                        if (goingToTVZ.hasParkingFee(simType)
						/*
						&&actCode!=1&&actCode!=2&&actCode!=7 //not for work, school or university 
						*/) {// scenario
                            costCar += goingToTVZ.getParkingFee(simType) * mcc.duration * 2.7777777777e-4;// stay
                        }

                        costCar += mode.getCost_per_km(simType) * distanceNetCar * 0.001; // car

                        tttmp = travelTimePT + travelTimeCar;
                        if (travelTime * 1.5 < tttmp/*||tttmp-600>travelTime*/) {
                            combi = 0;
                        } else {
                            travelTime = travelTimePT + travelTimeCar;
                            distanceNet = distanceNetCar + distanceNetPT;
                            cost = costCar + costPT;
                            // travel time
                            numInterchanges = mode.getParameters().paramMatrixMapClass
                                    .getValue(ParamMatrixMap.PTCAR_INTERCHANGES, ptCarAccessTAZId,
                                            mcc.toStayLocation.getTAZId(), (int)(mcc.startTime + travelTimeCar));
                            //numInterchanges = Math.max(0, numInterchanges-200.);
                            expInterChanges = TPS_FastMath.exp(numInterchanges * TPS_DB_IO.INTERCHANGE_FACTOR);
                            if (writeStats)
                                sb.append(travelTime).append(';').append(distanceNet).append(';').append(cost)
                                        .append(';').append(numInterchanges).append(';');
                        }
                        break;
                }

                if (combi == 0) {
                    // foot
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
                    numInterchanges = mode.getParameters().paramMatrixMapClass
                            .getValue(ParamMatrixMap.INTERCHANGES_PT, mcc.fromStayLocation.getTAZId(),
                                    mcc.toStayLocation.getTAZId(), mcc.startTime);
                    expInterChanges = TPS_FastMath.exp(numInterchanges * TPS_DB_IO.INTERCHANGE_FACTOR);
                    if (writeStats)
                        sb.append(travelTime).append(';').append(distanceNet).append(';').append(cost).append(';')
                                .append(numInterchanges).append(';');
                }
                if (writeStats) {
                    try {
                        sb.append('\n');
                        writer.write(sb.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case TRAIN: //car sharing-faker
                if (mode.getParameters().isDefined(ParamFlag.FLAG_USE_CARSHARING) &&
                        mode.getParameters().isTrue(ParamFlag.FLAG_USE_CARSHARING) &&
                        plan.getPerson().isCarPooler()) { //no cs user!
                    //service area in scenario?
                    if (!mcc.toStayLocation.getTrafficAnalysisZone().isCarSharingService(SimulationType.SCENARIO) ||
                            !mcc.fromStayLocation.getTrafficAnalysisZone()
                                    .isCarSharingService(SimulationType.SCENARIO)) {
                        return Double.NaN;
                    }
                    // time equals MIT + 3 min more access
                    travelTime = TPS_Mode.get(ModeType.MIT)
                            .getTravelTime(mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime, simType,
                                    TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY, plan.getPerson(),
                                    mcc.carForThisPlan);
                    travelTime += mode.getParameters()
                            .getDoubleValue(ParamValue.CARSHARING_ACCESS_ADDON); //Longer Access for Carsharing
                    cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
                } else {
                    return Double.NaN;
                }
                break;
        }

        // put together
        return modeConstant +  // mode constant
                parameters[1] * travelTime + // beta travel time
                parameters[2] * cost + // beta costs
                parameters[3] * plan.getPerson().getAge() + //alter
                parameters[4] * plan.getPerson().getAge() * plan.getPerson().getAge() + //quadratisches alter
                parameters[5] * plan.getPerson().getHousehold().getCarNumber() + // anzahl autos
                parameters[6] * expInterChanges + //umstiege (nur ÖV)
                // ab jetzt binär-Betas, also Ja/nein
                (plan.getPerson().mayDriveACar() ? parameters[7] : 0) + //führerschein
                (plan.getPerson().hasAbo() ? parameters[8] : 0) + //Öffi -abo
                (work ? parameters[9] : 0) + //tourpart mit Arbeit
                (education ? parameters[10] : 0) + //tourpart mit Bildung
                (shopping ? parameters[11] : 0) + //tourpart mit Einkauf
                (errant ? parameters[12] : 0) + //tourpart mit Erledigung
                (leisure ? parameters[13] : 0);
    }


}
