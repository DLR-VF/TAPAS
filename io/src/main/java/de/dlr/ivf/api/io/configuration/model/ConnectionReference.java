package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ConnectionReference {
    @JsonProperty("urlReference")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String urlReference;

    @JsonProperty("loginReference")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String loginReference;
}
