package de.dlr.ivf.tapas.choice.traveltime.records;

import de.dlr.ivf.tapas.model.location.Locatable;

public record TravelContext(Locatable start, Locatable destination, int startTime) { }
