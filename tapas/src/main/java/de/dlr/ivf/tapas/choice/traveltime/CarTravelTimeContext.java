package de.dlr.ivf.tapas.choice.traveltime;

public class CarTravelTimeContext implements TravelTimeCalculationContext{


    @Override
    public double accept(TravelTimeCalculationVisitor visitor) {
        return 0;
    }
}
