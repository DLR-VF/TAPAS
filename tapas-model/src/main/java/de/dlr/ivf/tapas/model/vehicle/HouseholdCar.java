package de.dlr.ivf.tapas.model.vehicle;


/**
 * this is a decorator for a {@link Vehicle} that implement different cost strategies.
 */
public class HouseholdCar implements Vehicle {

    private final TPS_Car car;


    public HouseholdCar(TPS_Car car){

        this.car = car;
    }

    @Override
    public double costPerKilometer() {
        return car.costPerKilometer();
    }

    @Override
    public double variableCostPerKilometer() {
        return car.variableCostPerKilometer();
    }

    @Override
    public int id() {
        return this.car.id();
    }

    @Override
    public FuelType fuelType() {
        return this.car.getFuelType();
    }

    @Override
    public boolean isRestricted() {
        return car.isRestricted();
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
