package de.dlr.ivf.api.io.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FilterableDataSource(
        @JsonProperty String uri,
        @JsonProperty Filter filterBy
){
}
