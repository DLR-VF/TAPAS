package de.dlr.ivf.tapas.model.vehicle;

/**
 * A class representing the absence of a vehicle.
 *
 * <p>
 * This class implements the {@link Vehicle} interface and provides the behavior specific to a vehicle that does not exist.
 * </p>
 *
 * @see Vehicle
 */
public class NoVehicle implements Vehicle{
    @Override
    public boolean isRestricted() {
        return false;
    }
}
