package de.dlr.ivf.tapas.environment.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import de.dlr.ivf.api.io.configuration.ConnectionDetails;
import de.dlr.ivf.api.io.configuration.DataSource;
import lombok.*;

@ToString
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class EnvironmentConfiguration {


    @JsonProperty
    @JsonSetter(nulls = Nulls.SET)
    private final ConnectionDetails connectionDetails;

    @JsonProperty
    private final DataSource simulationsTable;

    @JsonProperty
    private final DataSource serverTable;

    @JsonProperty
    private final DataSource parameterTable;
}
