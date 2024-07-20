package de.dlr.ivf.tapas.choice.location;

import de.dlr.ivf.tapas.model.scheme.Stay;

public record LocationChoiceContext(Stay stayToLocalize, Stay comingFromStay, Stay rubberBandStay) {
}
