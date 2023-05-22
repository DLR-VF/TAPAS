package de.dlr.ivf.tapas.runtime.sumoDaemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SumoMatrixImporterConfig {
    @JsonProperty
    private final ConnectionDetails connectionDetails;

    @JsonProperty
    private DataSource tazTable;

    @JsonProperty
    private DataSource matricesTable;
}
