package de.dlr.ivf.api.io.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DataSource {

    @JsonProperty
    private final String uri;

    public String getUri() {
        return uri;
    }
}
