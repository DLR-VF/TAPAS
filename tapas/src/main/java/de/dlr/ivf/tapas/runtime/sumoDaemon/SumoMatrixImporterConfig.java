package de.dlr.ivf.tapas.runtime.sumoDaemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SumoMatrixImporterConfig {

    @JsonProperty
    private RemoteDataSource tazTable;

    @JsonProperty
    private RemoteDataSource matricesTable;
}
