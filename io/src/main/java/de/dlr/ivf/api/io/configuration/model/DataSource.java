package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Optional;

@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DataSource {

    @JsonProperty
    private final String uri;

    public String getUri() {
        return uri;
    }
}
