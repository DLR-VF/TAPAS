package de.dlr.ivf.tapas.configuration.json.modechoice;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModeChoiceConfiguration(
        @JsonProperty ModesConfiguration modesConfiguration) {


}
