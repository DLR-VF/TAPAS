package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet;
import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet.Result;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.SortedSet;
import java.util.TreeSet;

public abstract class TPS_SelectLocationWeigthBased extends TPS_LocationSelectModel {
	abstract class WeightedResult implements Comparable<WeightedResult>{
		Result result;
		Double travelTime;
		public WeightedResult(Result result,double travelTime){
			this.result=result;
			this.travelTime = travelTime;
		}
		public abstract Double getAdaptedWeight();
		
		
	}

	class GravityWeightedResults extends WeightedResult{
		public GravityWeightedResults(Result result, double weight, double travelTime) {
			super(result, travelTime);
		}

		@Override
		/**
		 * The most attractive Location has to be in front! -> Descending
		 */
		public int compareTo(WeightedResult arg0) {
			return -(this.getAdaptedWeight().compareTo(arg0.getAdaptedWeight()));
		}
		public Double getAdaptedWeight(){
			return this.result.sumWeight/this.travelTime;
		}
		
	}
	
	
	@Override
	public TPS_Location selectLocationFromChoiceSet(TPS_RegionResultSet choiceSet, TPS_Plan plan, TPS_PlanningContext pc, TPS_LocatedStay locatedStay) {
		if(this.PM==null){
			TPS_Logger.log(SeverenceLogLevel.FATAL, "TPS_LocationSelectModel not properly initialized! Persistance manager is null?!?! Driving home!");
			return plan.getPerson().getHousehold().getLocation();
		}
		if(this.region == null){
			TPS_Logger.log(SeverenceLogLevel.FATAL, "TPS_LocationSelectModel not properly initialized! Region is null?!?! Driving home!");
			return plan.getPerson().getHousehold().getLocation();
		}
		TPS_Stay stay = locatedStay.getStay();
		TPS_ActivityConstant actCode = stay.getActCode();
		TPS_TourPart tourpart = (TPS_TourPart) locatedStay.getStay().getSchemePart();
		TPS_Stay comingFrom = tourpart.getStayHierarchy(stay).getPrevStay();
		TPS_Stay goingTo = tourpart.getStayHierarchy(stay).getNextStay();
		TPS_Location locComingFrom = plan.getLocatedStay(comingFrom).getLocation();
		TPS_Location locGoingTo = plan.getLocatedStay(goingTo).getLocation();
		TPS_SettlementSystem regionType = locComingFrom.getTrafficAnalysisZone().getBbrType();
		
		// different cnf4-params according to the type of trip
		double cfn4 = region.getCfn().getCFN4Value(regionType,actCode);
		double cnfX = region.getCfn().getCFNXValue(regionType);

		SortedSet <WeightedResult> weightedChoiceSet = new TreeSet<>();
		TPS_Location locRepresentant = null;
		double sumWeight = 0;
		for (Result result : choiceSet.getResultIterable()) {
			if(result.sumWeight<=0)
				continue;
			// Draw a location at random. This location represents the zone for this round.
			TPS_TrafficAnalysisZone taz = result.taz;
			locRepresentant = result.loc;
			//here we can switch between different travel time models
			TPS_ModeChoiceContext prevMCC = new TPS_ModeChoiceContext();
			prevMCC.isBikeAvailable = pc.isBikeAvailable;
			prevMCC.carForThisPlan = pc.carForThisPlan;
			prevMCC.duration = stay.getOriginalDuration();
			prevMCC.startTime = stay.getOriginalStart();
			prevMCC.fromStayLocation = locComingFrom;
			prevMCC.fromStay = comingFrom;
			prevMCC.toStayLocation = locRepresentant;
			prevMCC.toStay = stay;
			TPS_ModeChoiceContext nextMCC = new TPS_ModeChoiceContext();
			nextMCC.isBikeAvailable = pc.isBikeAvailable;
			nextMCC.carForThisPlan = pc.carForThisPlan;
			nextMCC.duration = stay.getOriginalDuration();
			nextMCC.startTime = stay.getOriginalStart();
			nextMCC.fromStayLocation = locRepresentant;
			nextMCC.fromStay = stay;
			nextMCC.toStayLocation = locGoingTo;
			nextMCC.toStay = goingTo;

			double weightedTT = this.getTravelTime(plan, pc, prevMCC, nextMCC, taz, this.PM.getParameters());
			if(weightedTT>0){
				//here we can switch between different Opportunity-weighting
				WeightedResult weightedResult = this.createLocationOption(result, weightedTT); //weight wird mit steigender Reisezeit abgewertet
				sumWeight+=weightedResult.getAdaptedWeight();
				weightedChoiceSet.add(weightedResult);
			}
		}
		
		//Collections.sort(weightedChoiceSet); //sort descending!!
		
		if(weightedChoiceSet.size()>0) {
			double rand = Randomizer.random(); //uniform distribution from 0 to 1
			double posMicro = rand;
//			if(actCode.getCode(TPS_ActivityCodeType.TAPAS)==3) {
//				rand = 0; //hit
//			}
				
			//double posMicro = ( Math.log(1.0 - rand) / Math.log(1.0 - cfn4));
			if(this.PM.getParameters().isDefined(ParamValue.GAMMA_LOCATION_WEIGHT)){
				posMicro = Math.pow(posMicro, this.PM.getParameters().getDoubleValue(ParamValue.GAMMA_LOCATION_WEIGHT));
			}
			//double posMicro = 1.0 - rand;
			posMicro *= sumWeight; // norm to max weight
			posMicro *= cnfX; //apply region-specific restiction factor
			posMicro *= cfn4;  // apply activity based restiction factor
			
			double weightPos=0;
			for (WeightedResult entry : weightedChoiceSet) {
				weightPos += entry.getAdaptedWeight();
				if(weightPos>=posMicro) {
					locRepresentant = entry.result.loc;
					break;
				}
			}
		} else {
			TPS_Logger.log(SeverenceLogLevel.WARN, "No specific location found for activity "
					+ actCode.getCode(TPS_ActivityCodeType.ZBE));
			return region.selectDefaultLocation(plan, pc, locatedStay);
		}
		return locRepresentant;
	}
	
	
	/**
	 * Method to calculate the expected travel time to the locRepresentant.
	 * @param plan
	 * @param pc
	 * @param prevMCC
	 * @param nextMCC
	 * @param taz
	 * @return
	 */
	abstract public double getTravelTime(TPS_Plan plan, TPS_PlanningContext pc, TPS_ModeChoiceContext prevMCC, TPS_ModeChoiceContext nextMCC, TPS_TrafficAnalysisZone taz, TPS_ParameterClass parameterClass);
	
	abstract public WeightedResult createLocationOption(Result result, double travelTime);
}
