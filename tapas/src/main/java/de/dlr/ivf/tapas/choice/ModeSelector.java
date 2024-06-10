package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.mode.cost.MNLFullComplexContext;
import de.dlr.ivf.tapas.mode.cost.MNLFullComplexFunction;
import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;
import de.dlr.ivf.tapas.model.choice.DiscreteDistributionFactory;
import de.dlr.ivf.tapas.model.choice.DiscreteProbability;
import de.dlr.ivf.tapas.model.choice.ModeChoiceContext;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.legacy.TPS_ModeSet;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;
import de.dlr.ivf.tapas.model.scheme.TPS_TourPart;

import java.util.Map;
import java.util.function.Supplier;

public class ModeSelector {

    private final double minProbability = 0.0;

    private final TPS_ModeSet modeSet;
    private final boolean flagUseExitMaut;

    private final DiscreteDistributionFactory<TPS_Mode> modeDistributionFactory;

    private final Map<TPS_Mode, MNLFullComplexFunction> mnlFunctions;
    private final TravelTimeCalculator travelTimeCalculator;

    public ModeSelector(TPS_ModeSet modeSet, TPS_ParameterClass parameterClass, DiscreteDistributionFactory<TPS_Mode> modeDistributionFactory,
                        Map<TPS_Mode, MNLFullComplexFunction> mnlFunctions, TravelTimeCalculator travelTimeCalculator){

        this.modeSet = modeSet;
        this.flagUseExitMaut = parameterClass.isTrue(ParamFlag.FLAG_USE_EXIT_MAUT);
        this.modeDistributionFactory = modeDistributionFactory;
        this.mnlFunctions = mnlFunctions;
        this.travelTimeCalculator = travelTimeCalculator;
    }

    public TPS_Mode selectMode(ModeChoiceContext<TPS_Mode> modeChoiceContext, MNLFullComplexContext mnlContext){

        DiscreteDistribution<TPS_Mode> modeDistribution = modeDistributionFactory.emptyDistribution();
//        for(TPS_Mode mode : modeDistributionFactory.getDiscreteVariables()){
//            if(!modeChoiceContext.modeIsAvailable(mode)){
//                modeDistribution.addProbability(new DiscreteProbability<>(mode, minProbability));
//                continue;
//            }
//            //todo create a context for each mode for a travel time function visitor
////            double travelTime = travelTimeCalculator.getTravelTime(mode, modeChoiceContext.getFrom(), modeChoiceContext.getTo(),
////                    modeChoiceContext.getStartTime(), );
//            //modeDistribution.addProbability(new DiscreteProbability<>(mode, mnlFunctions.get(mode).apply()));
//
//        }
        return null;
    }

    /**
     * Sets the departure and arrival modes for a stay
     *
     * @param plan        day plan
     * @param prevStay
     * @param locatedStay the stay the modes should be selected for
     * @param nextStay
     * @param pc
     */
    public TPS_Mode selectMode(TPS_Plan plan, Supplier<TPS_Stay> prevStay, TPS_LocatedStay locatedStay, Supplier<TPS_Stay> nextStay, TPS_PlanningContext pc, double distanceNet) {
        // log.debug("\t\t\t\t\t\t '--> In tpsSelectLocation.selectMode");


        TPS_ExtMode currentArrivalMode = locatedStay.getModeArr();
        TPS_ExtMode currentDepartureMode = locatedStay.getModeDep();

        if (currentArrivalMode == null || currentDepartureMode == null) {
            // The episodes for activities out of home receive this call in a hierarchical order. That means that either
            // 'comingFrom' or 'goingTo' point to an episode at home or they point to superior episodes. In the latter case
            // the mode eventually has to be adopted.
            TPS_LocatedStay prevStayLocated = plan.getLocatedStay(prevStay.get());
            TPS_LocatedStay nextStayLocated = plan.getLocatedStay(nextStay.get());
            TPS_ExtMode previousStayDepartureMode = prevStayLocated.getModeDep();
            TPS_ExtMode nextStayArrivalMode = nextStayLocated.getModeArr();
            TPS_Location previousStayLocation = prevStayLocated.getLocation();
            TPS_Location currentStayLocation = locatedStay.getLocation();

            TPS_ModeChoiceContext mcc = new TPS_ModeChoiceContext();


            TPS_Mode chosenMode;
            if (nextStayArrivalMode == null) { //does the next stay allow unrestricted mode choice
                if (previousStayDepartureMode == null) { //does the previous stay allow unrestricted mode choice
                    mcc.fromStayLocation = previousStayLocation;
                    mcc.toStayLocation = currentStayLocation;
                    mcc.toStay = locatedStay.getStay();
                    mcc.duration = mcc.toStay.getOriginalDuration();
                    mcc.startTime = mcc.toStay.getOriginalStart();
                    mcc.isBikeAvailable = pc.isBikeAvailable;
                    mcc.carForThisPlan = pc.carForThisPlan;
                    currentArrivalMode = selectMode0(plan, distanceNet, mcc);
                } else { // adopt arrival mode according to departure mode
                    currentArrivalMode = previousStayDepartureMode;
                }
                if (currentArrivalMode == null) {
                    TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE,
                            "Error: no possible mode found!");
                }
                locatedStay.setModeArr(currentArrivalMode);

                //now we have an arrival mode
                if (currentDepartureMode == null) {
                    if (currentArrivalMode.isFix()) { // do we have to stick to the mode
                        currentDepartureMode = currentArrivalMode;
                    } else {
                        TPS_TourPart tourpart = (TPS_TourPart) locatedStay.getStay().getSchemePart();
                        mcc.fromStayLocation = currentStayLocation;
                        mcc.toStayLocation = nextStayLocated.getLocation();
                        mcc.toStay = nextStay.get();
                        mcc.duration = mcc.toStay.getOriginalDuration();
                        mcc.startTime = mcc.toStay.getOriginalStart();
                        mcc.isBikeAvailable = tourpart.isBikeUsed();
                        mcc.carForThisPlan = tourpart.getCar();
                        currentDepartureMode = selectMode0(plan, distanceNet, mcc);
                    }
                    locatedStay.setModeDep(currentDepartureMode);
                    if (currentDepartureMode == null) {
                        TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE,
                                "Error: no possible mode found!");
                    }
                }
            } else { //adopt departure mode to the next stay arrival mode
                currentDepartureMode = nextStayArrivalMode;
                locatedStay.setModeDep(currentDepartureMode);
                if (currentArrivalMode == null) { // is the arrival mode set?
                    if (previousStayDepartureMode == null) { //does the previous stay allow unrestricted mode choice
                        if (currentDepartureMode.isFix()) { // do we have to stick to the mode
                            currentArrivalMode = currentDepartureMode;
                        } else {
                            TPS_TourPart tourpart = (TPS_TourPart) locatedStay.getStay().getSchemePart();
                            mcc.fromStayLocation = previousStayLocation;
                            mcc.toStayLocation = currentStayLocation;
                            mcc.toStay = locatedStay.getStay();
                            mcc.duration = mcc.toStay.getOriginalDuration();
                            mcc.startTime = mcc.toStay.getOriginalStart();
                            mcc.isBikeAvailable = tourpart
                                    .isBikeUsed(); // pc.isBikeAvailable; !!! why this - it should be same as before (pc.isBikeAvailable)
                            mcc.carForThisPlan = tourpart
                                    .getCar(); // pc.carForThisPlan; !!! why this - it should be same as before (pc.carForThisPlan)
                            currentArrivalMode = selectMode0(plan, distanceNet, mcc);
                        }
                    } else {// adopt arrival mode according to departure mode
                        currentArrivalMode = previousStayDepartureMode;
                    }
                    if (currentArrivalMode == null) {
                        TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE,
                                "Error: no possible mode found!");
                    }
                    locatedStay.setModeArr(currentArrivalMode);
                }
            }
        }
        return locatedStay.getModeArr().primary;
    }

    public TPS_ExtMode selectDepartureMode(TPS_Plan plan, TPS_LocatedStay departure_stay, TPS_LocatedStay arrival_stay, TPS_PlanningContext pc) {

        TPS_ExtMode departure_stay_arrival_mode = departure_stay.getModeArr();

        TPS_ExtMode chosen_mode;

        if (departure_stay_arrival_mode == null || !departure_stay_arrival_mode.isFix()) { //we are free to chose a mode to get to our arrival location

            TPS_Location departure_location = departure_stay.getLocation();
            TPS_Location arrival_location = arrival_stay.getLocation();

            // The mode "walking" is used to get distances on the net.
            //todo extract this
            double distanceNet = 0;//Math.max(this.parameterClass.getDoubleValue(ParamValue.MIN_DIST),
//                    TPS_Mode.get(ModeType.WALK)
//                            .getDistance(departure_location, arrival_location, SimulationType.SCENARIO, null) -
//                            obscureDistanceCorrectionNumber);

            TPS_ModeChoiceContext mcc = new TPS_ModeChoiceContext();

            mcc.fromStayLocation = departure_location;
            mcc.toStayLocation = arrival_location;
            mcc.toStay = arrival_stay.getStay();
            mcc.duration = mcc.toStay.getOriginalDuration();
            mcc.startTime = mcc.toStay.getOriginalStart();
            mcc.isBikeAvailable = pc.isBikeAvailable;
            mcc.carForThisPlan = pc.carForThisPlan;

            chosen_mode = selectMode0(plan, distanceNet, mcc);


        }else{
            chosen_mode = departure_stay_arrival_mode;
        }

        return chosen_mode;
    }

    /**
     * Method to determine a preliminary mode, just as if this stay would be the first stay to choose a mode
     * @param plan The plan, which is processed
     * @param locatedStay the actual Stay
     * @param beelineDistanceLocArr the beeline from the last location (block level)
     * @param beelineDistanceLocDep the beeline to the next location (block level)
     * @param beelineDistanceTAZArr the beeline from the last location (taz level)
     * @param beelineDistanceTAZDep the beeline to the next location (taz level)
     * @param car a car which could be used or null
     * @param bikeIsAvailable flag for bike availability
     */
	/*
	public void selectPreliminaryMode(TPS_Plan plan, TPS_LocatedStay locatedStay, TPS_Car car, boolean bikeIsAvailable) {

		// log.debug("\t\t\t\t\t\t '--> In tpsSelectLocation.selectMode");

		TPS_Location currentStayLocation = locatedStay.getLocation();
		TPS_Mode currentArrivalMode = locatedStay.getModeArr();
		TPS_Mode currentDepartureMode = locatedStay.getModeDep();
		// This method is only called with tour parts
		TPS_Stay currentStay = locatedStay.getStay();
		TPS_TourPart tourpart = (TPS_TourPart) currentStay.getSchemePart();
		TPS_Stay prevStay = tourpart.getStayHierarchy(currentStay).getPrevStay();
		TPS_Stay nextStay = tourpart.getStayHierarchy(currentStay).getNextStay();
		TPS_Location previousStayLocation = plan.getLocatedStay(prevStay).getLocation();


		// The MIV-mode is used to get distances on the net.
		double distanceNet = Math.max(TPS_Parameters.ParamValue.MIN_DIST.getDoubleValue(), TPS_Mode.get(ModeType.WALK).getDistance(previousStayLocation, currentStayLocation, SimulationType.SCENARIO));


		currentArrivalMode = selectMode0(	plan, distanceNet, bikeIsAvailable, car,
											previousStayLocation, currentStay, locatedStay.getLocation(), currentStay.getOriginalDuration());
		locatedStay.setModeArr(currentArrivalMode);

		if (currentArrivalMode.isFix()) { // do we have to stick to the mode
			currentDepartureMode = currentArrivalMode;
			locatedStay.setModeDep(currentDepartureMode);
		} else {
			currentDepartureMode = selectMode0(plan, distanceNet, tourpart.isBikeUsed(), tourpart.getCar(), currentStayLocation, nextStay, plan.getLocatedStay(nextStay).getLocation(), nextStay.getOriginalDuration());
			locatedStay.setModeDep(currentDepartureMode);
		}
	}
	*/

    /**
     * Selects a mode, default being public transport
     *
     * @param plan        day plan
     * @param distanceNet distance between the locations
     * @param mcc
     * @return mode chosen mode TPS_ModeSet
     */
    private TPS_ExtMode selectMode0(TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
        // log.debug("\t\t\t\t '--> In tpsSelectMode.selectMode");

        TPS_DiscreteDistribution<TPS_Mode> dstDis = this.modeSet.getModeDistribution(plan, distanceNet, mcc);
        //for calculating the weighted acceptance probability
        //mcc.toStay.setModeDistribution(dstDis);
        // if (TPS_Region.WRITE)
        // System.out.println(Arrays.toString(dstDis.getValues()));

        TPS_Mode primary;
        // selecting mode
        if (dstDis == null || dstDis.size() == 0) {
            if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE)) {
                TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE,
                        "Distribution is empty. Using 'WALK' as default.");
            }
            //todo check this
            primary = null;//TPS_Mode.get(ModeType.WALK);
        } else {
            primary = dstDis.drawKey();
        }

        TPS_TrafficAnalysisZone comingFromTVZ;
        TPS_TrafficAnalysisZone goingToTVZ;
        comingFromTVZ = mcc.fromStayLocation.getTrafficAnalysisZone();
        goingToTVZ = mcc.toStayLocation.getTrafficAnalysisZone();

        //check if toll must be charged
        //todo get a work around
        //if (primary.isType(ModeType.MIT) && !plan.mustPayToll) {
//        if (!plan.mustPayToll) {
//            boolean carMustPayToll = true;
//            if (mcc.carForThisPlan != null && mcc.carForThisPlan.hasPaidToll) carMustPayToll = false;
//            // set "must pay toll flag"
//            if (goingToTVZ.hasToll(SimulationType.SCENARIO) && carMustPayToll) {
//                // toll is relevant as entering a cordon toll zone
//                plan.mustPayToll = true;
//            }
//
//            if (this.flagUseExitMaut && !goingToTVZ.hasToll(
//                    SimulationType.SCENARIO) && comingFromTVZ.hasToll(SimulationType.SCENARIO) && carMustPayToll) {
//                // scenario: toll is relevant as leaving a cordon toll zone
//                plan.mustPayToll = true;
//            }
//        }


//		if(mode.equals(TPS_Mode.get(ModeType.MIT_PASS))){
//			dstDis = this.getModeDistribution(plan, distanceNet, fBike, car, locComingFrom, stay, currentStayLocation, durationStay) ;
//
//
//			//for calculating the weighted acceptance probability
//			stay.setModeDistribution(dstDis);
//
//			// if (TPS_Region.WRITE)
//			// System.out.println(Arrays.toString(dstDis.getValues()));
//
//
//			random = Randomizer.random();
//			// selecting mode
//			if (dstDis == null || dstDis.size()==0) {
//				TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverityLogLevel.SEVERE,
//						"Distribution is empty. Using 'WALK' as default.");
//				mode = TPS_Mode.get(ModeType.WALK);
//			} else {
//				mode = dstDis.getIndex(dstDis.draw(random));
//			}
//		}


        //correct forbidden modes MIT->PT and BIKE-> WALK
        //todo revise this
//        if (primary.isType(ModeType.MIT) && mcc.carForThisPlan == null) {
//                primary = TPS_Mode.get(ModeType.PT);
//        }
//
//        if (primary.isType(ModeType.BIKE) && !mcc.isBikeAvailable) {
//            primary = TPS_Mode.get(ModeType.PT);
//        }
//
        TPS_Mode secondary = null;
        //todo revise this
        //        if (primary.isType(ModeType.PT)) {
//            secondary = mcc.combinedMode;
//        }
        return new TPS_ExtMode(primary, secondary);
    }

    public Modes getModes(){
        return modeSet.modes();
    }
}
