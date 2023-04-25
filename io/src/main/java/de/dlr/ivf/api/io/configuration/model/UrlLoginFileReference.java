package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class UrlLoginFileReference {

    @JsonProperty
    private final String urlFile;

    @JsonProperty
    private final String loginFile;
}
