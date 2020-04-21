package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet.Result;

public class TPS_SelectWithMultipleAccessModeGravity extends TPS_SelectWithMultipleAccessMode {
	class GravityWeightedResults extends WeightedResult{
		public GravityWeightedResults(Result result, double travelTime) {
			super(result, travelTime);
		}

		@Override
		/**
		 * 
		 */
		public int compareTo(WeightedResult arg0) {
			return -(this.getAdaptedWeight().compareTo(arg0.getAdaptedWeight()));
		}
		public Double getAdaptedWeight(){
			return this.result.sumWeight/this.travelTime;
		}
		
	}
	
	@Override
	public WeightedResult createLocationOption(Result result, double travelTime){
		return new GravityWeightedResults(result, travelTime);
	}
}
