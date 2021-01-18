package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.TPS_FastMath;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamMatrixMap;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;

public class TPS_UtilityMNLFullComplexRamona extends TPS_UtilityMNL {

    private double costsForMIV(TPS_Mode mode, TPS_Plan plan, double distanceNet, double travelTime, TPS_ModeChoiceContext mcc, SimulationType simType) {
        double cost = 0;
        TPS_TrafficAnalysisZone comingFromTVZ;
        TPS_TrafficAnalysisZone goingToTVZ;
        double tmpFactor = 1;
        comingFromTVZ = mcc.fromStayLocation.getTrafficAnalysisZone();
        goingToTVZ = mcc.toStayLocation.getTrafficAnalysisZone();
        //fuel cost per km
        if (mcc.carForThisPlan != null) {

            if (goingToTVZ.isRestricted() && mcc.carForThisPlan.isRestricted()) {
                return Double.NaN;
            }
            tmpFactor = (mcc.carForThisPlan.getCostPerKilometer(simType) +
                    mcc.carForThisPlan.getVariableCostPerKilometer(simType));
            //TODO: Bad hack to modify the costs according to the av-reduction!
            if (mcc.carForThisPlan.getAutomation() >= mode.getParameters().getIntValue(
                    ParamValue.AUTOMATIC_VEHICLE_LEVEL) && SimulationType.SCENARIO.equals(simType)) {
                //calculate time perception modification
                double rampUp = mode.getParameters().getDoubleValue(ParamValue.AUTOMATIC_VEHICLE_RAMP_UP_TIME);
                if (travelTime > rampUp) {
                    double timeMod = distanceNet > mode.getParameters().getIntValue(
                            ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_THRESHOLD) ? mode.getParameters().getDoubleValue(
                            ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_FAR) : mode.getParameters().getDoubleValue(
                            ParamValue.AUTOMATIC_VEHICLE_TIME_MOD_NEAR);
                    tmpFactor *= timeMod; // modify the costs according to the av-reduction from the first meter!
                }
            }

        } else { //calc cost for a generic car
            boolean carIsRestricted = true;
            if (Randomizer.random() < mode.getParameters().getDoubleValue(ParamValue.PASS_PROBABILITY_HOUSEHOLD_CAR)) {
                TPS_Car coDriver = plan.getPerson().getHousehold().getLeastRestrictedCar();
                if (coDriver != null) { //does the household has a car???
                    carIsRestricted = plan.getPerson().getHousehold().getLeastRestrictedCar().isRestricted();
                } else {
                    carIsRestricted = Randomizer.random() < mode.getParameters().getDoubleValue(
                            ParamValue.PASS_PROBABILITY_RESTRICTED);
                    //return Double.NaN; // Don't ride with strangers, 'Cause they're only there to do you harm
                }
            } else {
                carIsRestricted = Randomizer.random() < mode.getParameters().getDoubleValue(
                        ParamValue.PASS_PROBABILITY_RESTRICTED);
            }

            if (goingToTVZ.isRestricted() && carIsRestricted) {
                return Double.NaN;
            }

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
        boolean carMustPayToll = true;
        if (mcc.carForThisPlan != null && mcc.carForThisPlan.hasPaidToll || plan.mustPayToll) carMustPayToll = false;

        if (goingToTVZ.hasToll(simType) && carMustPayToll) {
            // toll is relevant as entering a cordon toll zone
            cost += goingToTVZ.getTollFee(simType);
        }

        if (mode.getParameters().isTrue(ParamFlag.FLAG_USE_EXIT_MAUT) && !goingToTVZ.hasToll(simType) &&
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
     * @author hein_mh
     *
     */ public double getCostOfMode(TPS_Mode mode, TPS_Plan plan, double distanceNet, double travelTime, TPS_ModeChoiceContext mcc,/*TPS_Location locComingFrom, TPS_Location locGoingTo, double startTime, double durationStay, TPS_Car car, boolean fBike,*/ SimulationType simType/*, TPS_Stay stay*/) {
        double cost = 0;
        double[] parameters = this.parameterMap.get(mode);

        double expInterChanges = 0;
        distanceNet = mode.getDistance(mcc.fromStayLocation, mcc.toStayLocation, simType,
                mcc.carForThisPlan); //correct the distance to the actual value!

        boolean feederArea = false;
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
                        cost = mode.getParameters().getDoubleValue(ParamValue.PT_COST_PER_KM_BASE);
                    } else {
                        cost = mode.getParameters().getDoubleValue(ParamValue.PT_COST_PER_KM);
                    }
                    if (mode.getParameters().isFalse(ParamFlag.FLAG_FIX_PT_COSTS)) {//cost per kilometer!
                        cost *= distanceNet * 0.001;
                    }
                }
                //RAMONA MOD
                
            	int TVZfrom, TVZto;
            	TVZfrom = mcc.fromStayLocation.getTAZId();
            	TVZto = mcc.toStayLocation.getTAZId();
            	int time = mcc.startTime;
            	double accessReduction = 0;
            	double egressReduction = 0;
            	double interchangeReduction = 0;
            	double interchanges = mode.getParameters().paramMatrixMapClass
                        .getValue(ParamMatrixMap.INTERCHANGES_PT, mcc.fromStayLocation.getTAZId(),
                                mcc.toStayLocation.getTAZId(), mcc.startTime) * TPS_DB_IO.INTERCHANGE_FACTOR;
            	
            	if(TVZfrom !=TVZto) { //only for non -intra-cell-traffic!
            		// access time mod if applicable
	                if(mcc.fromStayLocation.feederService) {
	                    if (mode.getParameters().isDefined(ParamMatrixMap.ARRIVAL_PT)) {
	                        accessReduction = mode.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.ARRIVAL_PT, TVZfrom, TVZto,
	                                simType, time);
	                        //calc the reduction
	                        if(accessReduction>180)
	                        	accessReduction = 180-accessReduction;
	                    }
	            		feederArea = true;
	                }
	                
	                // egress time mod if applicable
	                if(mcc.toStayLocation.feederService) {
	                    if (mode.getParameters().isDefined(ParamMatrixMap.EGRESS_PT)) {
	                        egressReduction = mode.getParameters().paramMatrixMapClass.getValue(ParamMatrixMap.EGRESS_PT, TVZfrom, TVZto,
	                                simType, time);
	                        //calc the reduction
	                        if(egressReduction>120)
	                        	accessReduction = 120-egressReduction;
	                    }
	            		feederArea = true;
	                }
	                
	                if(mcc.fromStayLocation.feederService && mcc.toStayLocation.feederService) { //both sides
	                	if(interchanges>=2) { //double reduction
	                		interchangeReduction =-4*60;
	                	}
	                	else if(interchanges >=1) {//single reduction
	                		interchangeReduction =-2*60;
	                	}
	                }
	                else if(mcc.toStayLocation.feederService) {
	                	if(interchanges >=1) {//single reduction
	                		interchangeReduction =-2*60;
	                	}	                	
	                }
	                else if(mcc.fromStayLocation.feederService ) {
	                	if(interchanges >=1) {//single reduction
	                		interchangeReduction =-2*60;
	                	}	  
	                }
	                if(Math.abs(accessReduction+egressReduction+interchangeReduction)<travelTime)
	                	travelTime +=accessReduction+egressReduction+interchangeReduction;
            	}
                expInterChanges = TPS_FastMath.exp(interchanges);

                break;
            case TRAIN: //car sharing-faker
                if ((mode.getParameters().isDefined(ParamFlag.FLAG_USE_CARSHARING) && mode.getParameters().isTrue(
                        ParamFlag.FLAG_USE_CARSHARING) && plan.getPerson().isCarPooler()) //cs user?
                        || (mode.getParameters().isDefined(ParamFlag.FLAG_USE_ROBOTAXI) && mode.getParameters().isTrue(
                        ParamFlag.FLAG_USE_ROBOTAXI)) // is robotaxi
                ) { //no cs user!
                    //service area in scenario?
                    if (!mcc.toStayLocation.getTrafficAnalysisZone().isCarSharingService(SimulationType.SCENARIO) ||
                            !mcc.fromStayLocation.getTrafficAnalysisZone().isCarSharingService(
                                    SimulationType.SCENARIO)) {
                        return Double.NaN;
                    }
                    // time equals MIT + 3 min more access
                    travelTime = TPS_Mode.get(ModeType.MIT).getTravelTime(mcc.fromStayLocation, mcc.toStayLocation,
                            mcc.startTime, simType, TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY,
                            plan.getPerson(), mcc.carForThisPlan);
                    travelTime += mode.getParameters().getDoubleValue(
                            ParamValue.CARSHARING_ACCESS_ADDON); //Longer Access for Carsharing
                    cost = mode.getCost_per_km(simType) * distanceNet * 0.001;
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

        //Mod Ramona: if we activate PTBoost, we add a constant bonus on the utility function   
        if(!mode.getParameters().getString(ParamString.UTILITY_FUNCTION_KEY).equals("PTBoost"))
        	feederArea = false;
        	
        return parameters[0] +  // mode constant
                parameters[1] * travelTime + // beta travel time
                parameters[2] * cost + // beta costs
                parameters[3] * plan.getPerson().getAge() + //alter
                parameters[4] * plan.getPerson().getAge() * plan.getPerson().getAge() + //quadratisches alter
                parameters[5] * plan.getPerson().getHousehold().getCarNumber() + // anzahl autos
                parameters[6] * expInterChanges + //umstiege (nur ÖV)
                //ab jetzt binär-Betas, also Ja/nein
                (plan.getPerson().mayDriveACar() ? parameters[7] : 0) + //führerschein
                (plan.getPerson().hasAbo() ? parameters[8] : 0) + //Öffi -abo
                (work ? parameters[9] : 0) + //tourpart mit Arbeit
                (education ? parameters[10] : 0) + //tourpart mit Bildung
                (shopping ? parameters[11] : 0) + //tourpart mit Einkauf
                (errant ? parameters[12] : 0) + //tourpart mit Erledigung
                (leisure ? parameters[13] : 0) +
                (feederArea ? 0.5 : 0); //RAMONA constant for feederArea
    }


}
