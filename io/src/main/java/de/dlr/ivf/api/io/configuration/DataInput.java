package de.dlr.ivf.api.io.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DataInput {


    //todo integrate null values
    @JsonProperty
    private final DataSource dataSource;
    @JsonProperty
    private final Collection<Filter> filter;
    @JsonProperty
    private final ConnectionDetails connectionDetails;
    @JsonProperty
    private final ConnectionReference connectionReference;
}
