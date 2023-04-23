package de.dlr.ivf.tapas.environment.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.configuration.model.UrlLoginFileReference;
import lombok.*;

@ToString
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class EnvironmentConfiguration {


    @JsonProperty
    @JsonSetter(nulls = Nulls.SET)
    private final UrlLoginFileReference references;

    @JsonProperty
    private final DataSource simulationsTable;

    @JsonProperty
    private final DataSource serverTable;
}
