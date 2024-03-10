package de.dlr.ivf.tapas.choice.traveltime;

import de.dlr.ivf.tapas.choice.traveltime.records.TravelContext;

public interface TravelTimeCalculationVisitor {
    double visit(TravelContext travelContext, TravelTimeCalculationContext context);

    double visit(TravelContext travelContext, CarTravelTimeContext context);
}
