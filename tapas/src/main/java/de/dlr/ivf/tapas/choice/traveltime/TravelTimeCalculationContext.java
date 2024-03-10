package de.dlr.ivf.tapas.choice.traveltime;

public interface TravelTimeCalculationContext {
    double accept(TravelTimeCalculationVisitor visitor);
}
