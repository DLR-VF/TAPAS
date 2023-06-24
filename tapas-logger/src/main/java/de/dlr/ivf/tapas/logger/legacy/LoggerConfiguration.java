package de.dlr.ivf.tapas.logger.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

@Getter
public class LoggerConfiguration {

    @JsonProperty
    String hierarchyLogLevelMask;

    @JsonProperty
    String severanceLogLevelMask;

    @JsonProperty
    String loggingFolder;

    @JsonProperty
    Map<String,String> logLevels;

}
