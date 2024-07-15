package de.dlr.ivf.tapas.configuration.json.region.matrix;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;

import java.util.Collection;
import java.util.Map;

public record MatrixConfiguration(
        @JsonProperty DataSource matricesSource,
        @JsonProperty Map<String, Map<String, String>> modeMatrixMapMappings,
        @JsonProperty Collection<MatrixMapConfiguration> matrixMaps,
        @JsonProperty String beelineDistanceMatrixName
) {}
