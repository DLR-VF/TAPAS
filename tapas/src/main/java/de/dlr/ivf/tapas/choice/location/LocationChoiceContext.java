package de.dlr.ivf.tapas.choice.location;

import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.Stay;

public record LocationChoiceContext(TourContext tourContext,
                                    Stay stayToLocalize, 
                                    Stay comingFromStay, 
                                    Stay rubberBandStay) {
}
