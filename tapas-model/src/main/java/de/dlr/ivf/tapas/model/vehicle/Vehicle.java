package de.dlr.ivf.tapas.model.vehicle;

import de.dlr.ivf.tapas.model.parameter.SimulationType;

public interface Vehicle {

    double costPerKilometer();

    double variableCostPerKilometer();

    int id();

    FuelType fuelType();

    boolean isRestricted();
}
