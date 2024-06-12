package de.dlr.ivf.tapas.configuration.json.region.matrix;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dlr.ivf.api.io.configuration.DataSource;

import java.util.Collection;

public record MatrixConfiguration(
        @JsonProperty DataSource matricesSource,
        @JsonProperty Collection<MatrixMapConfiguration> matrixMaps
) {}
