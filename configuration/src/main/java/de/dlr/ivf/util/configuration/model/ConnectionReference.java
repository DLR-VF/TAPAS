package de.dlr.ivf.util.configuration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ConnectionReference (
    @JsonProperty("urlReference")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String urlReference,

    @JsonProperty("loginReference")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String loginReference){}
