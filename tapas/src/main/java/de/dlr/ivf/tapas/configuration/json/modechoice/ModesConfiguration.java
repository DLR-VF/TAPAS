package de.dlr.ivf.tapas.configuration.json.modechoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;

import java.util.Collection;

public record ModesConfiguration(
        @JsonProperty DataSource modes,
        @JsonProperty Collection<SimpleModeParameters> modeParameters,
        @JsonProperty int carSharingCheckOutPenalty,
        @JsonProperty int carSharingAccessAddon
        ) {
}
