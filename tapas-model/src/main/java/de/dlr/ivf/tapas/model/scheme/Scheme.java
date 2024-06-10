package de.dlr.ivf.tapas.model.scheme;

import java.util.Collection;

public record Scheme(
        int id,
        int schemeClassId,
        Collection<Tour> tours
) {
}
