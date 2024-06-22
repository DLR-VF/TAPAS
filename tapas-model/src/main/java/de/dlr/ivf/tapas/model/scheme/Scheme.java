package de.dlr.ivf.tapas.model.scheme;

import java.util.Set;

public record Scheme(
        int id,
        int schemeClassId,
        Set<Tour> tours
) {
}
