package de.dlr.ivf.tapas.util.traveltime.records;

import de.dlr.ivf.tapas.model.location.Locatable;

public record TravelContext(Locatable start, Locatable destination, int startTime) { }
