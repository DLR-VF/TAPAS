package de.dlr.ivf.tapas.configuration.json.region.matrix;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public record MatrixMapConfiguration(
        @JsonProperty String name,
        @JsonProperty Collection<MatrixMapEntry> matrices

) {}