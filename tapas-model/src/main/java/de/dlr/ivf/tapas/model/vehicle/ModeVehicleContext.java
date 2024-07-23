package de.dlr.ivf.tapas.model.vehicle;

import de.dlr.ivf.tapas.model.mode.TPS_Mode;

public record ModeVehicleContext(
        TPS_Mode mode,
        Vehicle vehicle
) {
    public boolean usesBoundMode(){
        return mode.isFix();
    }

    public boolean usesRestrictedVehicle(){
        return vehicle.isRestricted();
    }
}
