package de.dlr.ivf.tapas.choice.traveltime.implementation;

import de.dlr.ivf.tapas.choice.traveltime.TravelTimeCalculationContext;
import de.dlr.ivf.tapas.choice.traveltime.TravelTimeCalculationVisitor;
import de.dlr.ivf.tapas.choice.traveltime.records.TravelContext;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;

public class MitTravelTimeContext implements TravelTimeCalculationContext {

//    private final TravelContext travelContext;
//    private final TPS_Car car;

    @Override
    public double accept(TravelTimeCalculationVisitor visitor) {
        return -1;
    }
}
