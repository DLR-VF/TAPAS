package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_Distance;
import de.dlr.ivf.tapas.constants.TPS_Distance.TPS_DistanceCodeType;
import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.modechoice.TPS_ExpertKnowledgeTree;
import de.dlr.ivf.tapas.modechoice.TPS_ModeChoiceTree;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

/**
 * Class for the organisation of the modes available for choice as well as the choice of a mode considering the existing
 * alternatives
 *
 * @author mark_ma
 */
public class TPS_ModeSet implements ExtendedWritable {

    final double obscureDistanceCorrectionNumber = 500;
    /**
     * Mode Choice Tree for this mode set
     */
    private TPS_ModeChoiceTree modeChoiceTree;

    /**
     * Mode Choice Tree for this mode set
     */
    private final TPS_ExpertKnowledgeTree expertKnowledgeTree;

    private final TPS_ParameterClass parameterClass;

    /**
     * Standard constructor for a mode set
     *
     * @param modeChoiceTree      The choice tree for pivot point models
     * @param expertKnowledgeTree
     * @param parameterClass      parameter class reference
     */
    public TPS_ModeSet(TPS_ModeChoiceTree modeChoiceTree, TPS_ExpertKnowledgeTree expertKnowledgeTree, TPS_ParameterClass parameterClass) {
        super();
        this.modeChoiceTree = modeChoiceTree;
        this.expertKnowledgeTree = expertKnowledgeTree;
        this.parameterClass = parameterClass;
    }

    /**
     * returns the initial expert knowledge tree
     *
     * @return mode choice tree
     */
    public TPS_ExpertKnowledgeTree getExpertKnowledgeTree() {
        return expertKnowledgeTree;
    }

    /**
     * returns the initial mode choice tree
     *
     * @return mode choice tree
     */
    public TPS_ModeChoiceTree getModeChoiceTree() {
        return modeChoiceTree;
    }

    /**
     * Sets the initial mode choice tree.
     *
     * @param modeChoiceTree
     */
    public void setModeChoiceTree(TPS_ModeChoiceTree modeChoiceTree) {
        this.modeChoiceTree = modeChoiceTree;
    }

    /**
     * Calculates the mode distribution
     *
     * @param plan        day plan
     * @param distanceNet distance between the locations
     * @param mcc
     * @return mode chosen mode TPS_ModeSet
     */
    public TPS_DiscreteDistribution<TPS_Mode> getModeDistribution(TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc) {
        // getting the distribution of modes from the mode choice tree according to the attributes of the plan
        plan.setAttributeValue(TPS_Attribute.CURRENT_DISTANCE_CLASS_CODE_MCT,
                TPS_Distance.getCode(TPS_DistanceCodeType.MCT, distanceNet));
        //these attributes need to be set here again, because it is not guarantied, that they are set elsewhere
        TPS_ActivityConstant currentActCode = mcc.toStay.getActCode();
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_MCT,
                currentActCode.getCode(TPS_ActivityCodeType.MCT));
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_VOT,
                currentActCode.getCode(TPS_ActivityCodeType.VOT));
        plan.setAttributeValue(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
                currentActCode.getCode(TPS_ActivityCodeType.TAPAS));

        TPS_DiscreteDistribution<TPS_Mode> srcDis = TPS_Mode.getUtilityFunction(this.parameterClass).getDistributionSet(
                this, plan, distanceNet,
                mcc);//locComingFrom, currentStayLocation, stay.getOriginalStart(), durationStay, car, fBike,stay);
        plan.removeAttribute(TPS_Attribute.CURRENT_DISTANCE_CLASS_CODE_MCT);

        TPS_DiscreteDistribution<TPS_Mode> dstDis = null;

        // in the scenario case: calculating the differences in mode distribution
        if (this.parameterClass.isTrue(ParamFlag.FLAG_RUN_SZENARIO)) {
            dstDis = TPS_ModeDistribution.calculateDistribution(srcDis, plan, distanceNet, mcc);
        } else {
            dstDis = srcDis;
        }

        return dstDis;
    }

    /**
     * Returns the parameter class reference
     *
     * @return parameter class reference
     */
    public TPS_ParameterClass getParameters() {
        return this.parameterClass;
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
    public void selectMode(TPS_Plan plan, TPS_Stay prevStay, TPS_LocatedStay locatedStay, TPS_Stay nextStay, TPS_PlanningContext pc) {
        // log.debug("\t\t\t\t\t\t '--> In tpsSelectLocation.selectMode");


        TPS_ExtMode currentArrivalMode = locatedStay.getModeArr();
        TPS_ExtMode currentDepartureMode = locatedStay.getModeDep();

        if (currentArrivalMode == null || currentDepartureMode == null) {
            // The episodes for activities out of home receive this call in a hierarchical order. That means that either
            // 'comingFrom' or 'goingTo' point to an episode at home or they point to superior episodes. In the latter case
            // the mode eventually has to be adopted.
            TPS_LocatedStay prevStayLocated = plan.getLocatedStay(prevStay);
            TPS_LocatedStay nextStayLocated = plan.getLocatedStay(nextStay);
            TPS_ExtMode previousStayDepartureMode = prevStayLocated.getModeDep();
            TPS_ExtMode nextStayArrivalMode = nextStayLocated.getModeArr();
            TPS_Location previousStayLocation = prevStayLocated.getLocation();
            TPS_Location currentStayLocation = locatedStay.getLocation();

            // The mode "walking" is used to get distances on the net.
            double distanceNet = Math.max(this.parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                    TPS_Mode.get(ModeType.WALK)
                            .getDistance(previousStayLocation, currentStayLocation, SimulationType.SCENARIO, null) -
                            obscureDistanceCorrectionNumber);
            TPS_ModeChoiceContext mcc = new TPS_ModeChoiceContext();


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
                    TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
                            "Error: no possible mode found!");
                }
                locatedStay.setModeArr(currentArrivalMode);

                //now we have an arrival mode
                if (currentDepartureMode == null) {
                    if (currentArrivalMode.isFix()) { // do we have to stick to the mode
                        currentDepartureMode = currentArrivalMode;
                    } else {
                        TPS_TourPart tourpart;
                        if(locatedStay.getStay().getSchemePart() instanceof TPS_TourPart)
                            tourpart = (TPS_TourPart) locatedStay.getStay().getSchemePart();
                        else
                            tourpart = (TPS_TourPart) prevStayLocated.getStay().getSchemePart();
                        mcc.fromStayLocation = currentStayLocation;
                        mcc.toStayLocation = nextStayLocated.getLocation();
                        mcc.toStay = nextStay;
                        mcc.duration = mcc.toStay.getOriginalDuration();
                        mcc.startTime = mcc.toStay.getOriginalStart();
                        mcc.isBikeAvailable = tourpart.isBikeUsed();
                        mcc.carForThisPlan = tourpart.getCar();
                        currentDepartureMode = selectMode0(plan, distanceNet, mcc);
                    }
                    locatedStay.setModeDep(currentDepartureMode);
                    if (currentDepartureMode == null) {
                        TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
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
                        TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
                                "Error: no possible mode found!");
                    }
                    locatedStay.setModeArr(currentArrivalMode);
                }
            }
        }
    }

    public void selectModeSequentially(TPS_Plan plan, TPS_Stay prevStay, TPS_LocatedStay locatedStay, TPS_Stay nextStay, TPS_PlanningContext pc) {
        // log.debug("\t\t\t\t\t\t '--> In tpsSelectLocation.selectMode");
//asd

        TPS_ExtMode currentArrivalMode = locatedStay.getModeArr();
        TPS_ExtMode currentDepartureMode = locatedStay.getModeDep();

        if (currentArrivalMode == null || currentDepartureMode == null) {
            // The episodes for activities out of home receive this call in a hierarchical order. That means that either
            // 'comingFrom' or 'goingTo' point to an episode at home or they point to superior episodes. In the latter case
            // the mode eventually has to be adopted.
            TPS_LocatedStay prevStayLocated = plan.getLocatedStay(prevStay);
            TPS_LocatedStay nextStayLocated = plan.getLocatedStay(nextStay);
            TPS_ExtMode previousStayDepartureMode = prevStayLocated.getModeDep();
            TPS_ExtMode nextStayArrivalMode = nextStayLocated.getModeArr();
            TPS_Location previousStayLocation = prevStayLocated.getLocation();
            TPS_Location currentStayLocation = locatedStay.getLocation();

            // The mode "walking" is used to get distances on the net.
            double distanceNet = Math.max(this.parameterClass.getDoubleValue(ParamValue.MIN_DIST),
                    TPS_Mode.get(ModeType.WALK)
                            .getDistance(previousStayLocation, currentStayLocation, SimulationType.SCENARIO, null) -
                            obscureDistanceCorrectionNumber);
            TPS_ModeChoiceContext mcc = new TPS_ModeChoiceContext();


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
                    TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
                            "Error: no possible mode found!");
                }
                locatedStay.setModeArr(currentArrivalMode);

                //now we have an arrival mode
                if (currentDepartureMode == null) {
                    if (currentArrivalMode.isFix()) { // do we have to stick to the mode
                        currentDepartureMode = currentArrivalMode;
                    } else {
                        TPS_TourPart tourpart;
                        if(locatedStay.getStay().getSchemePart() instanceof TPS_TourPart)
                            tourpart = (TPS_TourPart) locatedStay.getStay().getSchemePart();
                        else
                            tourpart = (TPS_TourPart) prevStayLocated.getStay().getSchemePart();
                        mcc.fromStayLocation = currentStayLocation;
                        mcc.toStayLocation = nextStayLocated.getLocation();
                        mcc.toStay = nextStay;
                        mcc.duration = mcc.toStay.getOriginalDuration();
                        mcc.startTime = mcc.toStay.getOriginalStart();
                        mcc.isBikeAvailable = tourpart.isBikeUsed();
                        mcc.carForThisPlan = tourpart.getCar();
                        currentDepartureMode = selectMode0(plan, distanceNet, mcc);
                    }
                    locatedStay.setModeDep(currentDepartureMode);
                    if (currentDepartureMode == null) {
                        TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
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
                        TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
                                "Error: no possible mode found!");
                    }
                    locatedStay.setModeArr(currentArrivalMode);
                }
            }
        }
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

        TPS_DiscreteDistribution<TPS_Mode> dstDis = getModeDistribution(plan, distanceNet, mcc);
        //for calculating the weighted acceptance probability
        //mcc.toStay.setModeDistribution(dstDis);
        // if (TPS_Region.WRITE)
        // System.out.println(Arrays.toString(dstDis.getValues()));

        TPS_Mode primary;
        // selecting mode
        if (dstDis == null || dstDis.size() == 0) {
            if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE)) {
                TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
                        "Distribution is empty. Using 'WALK' as default.");
            }
            primary = TPS_Mode.get(ModeType.WALK);
        } else {
            primary = dstDis.drawKey();
        }

        //check if toll must be charged
        if (primary.isType(ModeType.MIT) && !plan.mustPayToll) {
            TPS_TrafficAnalysisZone comingFromTVZ;
            TPS_TrafficAnalysisZone goingToTVZ;
            comingFromTVZ = mcc.fromStayLocation.getTrafficAnalysisZone();
            goingToTVZ = mcc.toStayLocation.getTrafficAnalysisZone();
            boolean carMustPayToll = true;
            if (mcc.carForThisPlan != null && mcc.carForThisPlan.hasPaidToll) carMustPayToll = false;
            // set "must pay toll flag"
            if (goingToTVZ.hasToll(SimulationType.SCENARIO) && carMustPayToll) {
                // toll is relevant as entering a cordon toll zone
                plan.mustPayToll = true;
            }

            if (this.getParameters().isTrue(ParamFlag.FLAG_USE_EXIT_MAUT) && !goingToTVZ.hasToll(
                    SimulationType.SCENARIO) && comingFromTVZ.hasToll(SimulationType.SCENARIO) && carMustPayToll) {
                // scenario: toll is relevant as leaving a cordon toll zone
                plan.mustPayToll = true;
            }
        }


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
//				TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.SEVERE,
//						"Distribution is empty. Using 'WALK' as default.");
//				mode = TPS_Mode.get(ModeType.WALK);
//			} else {
//				mode = dstDis.getIndex(dstDis.draw(random));
//			}
//		}

        //TODO: should a forbidden bike be set to PT?
        //correct forbidden modes MIT->PT and BIKE-> WALK
        if (primary.isType(ModeType.MIT) && mcc.carForThisPlan == null) {
            primary = TPS_Mode.get(ModeType.PT);
        }
        if (primary.isType(ModeType.BIKE) && !mcc.isBikeAvailable) {
            primary = TPS_Mode.get(ModeType.PT);
        }
//		if(mode.equals(TPS_Mode.get(ModeType.WALK)) && distanceNet>TPS_UtilityFunction.walkDistanceBarrier){
//			TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.WARN,
//					"Unusual long walk! Probability: "+dstDis.getIndexedValue(TPS_Mode.get(ModeType.WALK))+"ranom value: "+random);
//		}
        TPS_Mode secondary = null;
        if (primary.isType(ModeType.PT)) {
            secondary = mcc.combinedMode;
        }
        return new TPS_ExtMode(primary, secondary);
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
        sb.append(prefix + "TAPAS ModeSet\n");
        sb.append(this.modeChoiceTree.toString(prefix + " ") + "\n");
        for (TPS_Mode mode : TPS_Mode.getConstants()) {
            sb.append(mode.toString(prefix + " ") + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
