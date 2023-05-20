package de.dlr.ivf.tapas.model.vehicle;

/**
 * This is a decorator for a {@link Vehicle} that implements a different cost strategy.
 */
public class CompanyCar implements Vehicle {

    private final Vehicle car;

    public CompanyCar(Vehicle car){
        this.car = car;
    }
    @Override
    public double costPerKilometer() {
        return 0;
    }

    @Override
    public double variableCostPerKilometer() {
        return 0;
    }

    @Override
    public int id() {
        return this.car.id();
    }

    @Override
    public FuelType fuelType() {
        return this.car.fuelType();
    }

    @Override
    public boolean isRestricted() {
        return this.car.isRestricted();
    }

    @Override
    public int getAutomationLevel() {
        return car.getAutomationLevel();
    }

    @Override
    public double getRangeLeft() {
        return car.getRangeLeft();
    }
}
