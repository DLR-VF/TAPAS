package de.dlr.ivf.tapas.configuration.json.locationchoice;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LocationChoiceConfiguration(
        @JsonProperty int numTazRepresentatives,
        @JsonProperty String modeForDistance,
        @JsonProperty int maxTriesLocationSelection
) {
}
