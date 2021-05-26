
    /*
     * Copyright (c) 2020 DLR Institute of Transport Research
     * All rights reserved.
     *
     * This source code is licensed under the MIT license found in the
     * LICENSE file in the root directory of this source tree.
     */

package de.dlr.ivf.tapas.loc;

import de.dlr.ivf.tapas.persistence.TPS_RegionResultSet.Result;

    public class TPS_SelectWithMultipleAccessModeGravityPow2 extends TPS_SelectWithMultipleAccessMode {
        @Override
        public WeightedResult createLocationOption(TPS_RegionResultSet.Result result, double travelTime) {
            return new GravityWeightedResults(result, travelTime);
        }

        class GravityWeightedResults extends WeightedResult {
            public GravityWeightedResults(TPS_RegionResultSet.Result result, double travelTime) {
                super(result, travelTime);
            }

            @Override
            /**
             *
             */ public int compareTo(WeightedResult arg0) {
                return -(this.getAdaptedWeight().compareTo(arg0.getAdaptedWeight()));
            }

            public Double getAdaptedWeight() {
                return this.result.sumWeight / (this.travelTime*this.travelTime);
            }

        }
    }
