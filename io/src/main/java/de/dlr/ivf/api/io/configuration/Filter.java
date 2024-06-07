package de.dlr.ivf.api.io.configuration;


import com.fasterxml.jackson.annotation.JsonProperty;

public record Filter (

    @JsonProperty
    String column,
    @JsonProperty
    String value
){}
