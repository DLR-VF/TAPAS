package de.dlr.ivf.tapas.configuration.json.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TravelTimeConfiguration(
        @JsonProperty double minDist,
        @JsonProperty boolean useTazIntraInfoMatrix,
        @JsonProperty double ptTravelTimeFactor,
        @JsonProperty double ptAccessFactor,
        @JsonProperty double ptEgressFactor,
        @JsonProperty double defaultBlockScore,
        @JsonProperty double averageDistancePtStop,
        @JsonProperty double beelineFactorPt,
        @JsonProperty double beelineFactorBike,
        @JsonProperty double beelineFactorFoot,
        @JsonProperty double beelineFactorMit

) {
}
