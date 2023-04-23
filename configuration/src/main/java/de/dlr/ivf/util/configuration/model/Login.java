package de.dlr.ivf.util.configuration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Login {

    @JsonProperty
    private final String user;

    @JsonProperty
    private final String password;
}
