package de.dlr.ivf.tapas.mode.cost;

import de.dlr.ivf.tapas.mode.cost.implementation.BerlinTaxiCost;
import de.dlr.ivf.tapas.mode.cost.implementation.DistanceCostPerKm;
import de.dlr.ivf.tapas.mode.cost.implementation.FixCost;

public sealed interface ModeDistanceCostFunction permits BerlinTaxiCost, DistanceCostPerKm, FixCost {

    double computeCost(double distance);
}
