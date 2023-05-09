
    /*
     * Copyright (c) 2020 DLR Institute of Transport Research
     * All rights reserved.
     *
     * This source code is licensed under the MIT license found in the
     * LICENSE file in the root directory of this source tree.
     */

package de.dlr.ivf.tapas.legacy;

    import de.dlr.ivf.tapas.model.TPS_RegionResultSet.Result;
    import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

    public class TPS_SelectWithMultipleAccessModeGravityPow2 extends TPS_SelectWithMultipleAccessMode {
        public TPS_SelectWithMultipleAccessModeGravityPow2(TPS_ParameterClass parameterClass) {
            super(parameterClass);
        }

        @Override
    public WeightedResult createLocationOption(Result result, double travelTime, double param) {
        return new GravityWeightedResults(result, travelTime,param);
    }

    class GravityWeightedResults extends WeightedResult {
        public GravityWeightedResults(Result result, double travelTime, double param) {
            super(result, travelTime, param);
        }
        @Override
        /**
         *
         */
        public int compareTo(WeightedResult arg0) {
            return -(this.getAdaptedWeight().compareTo(arg0.getAdaptedWeight()));
        }

        public Double getAdaptedWeight() {
                return this.result.sumWeight /  Math.pow(this.travelTime,this.param);
        }
    }
}
