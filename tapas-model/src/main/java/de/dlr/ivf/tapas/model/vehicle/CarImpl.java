package de.dlr.ivf.tapas.model.vehicle;

import de.dlr.ivf.tapas.model.mode.ModeUtils;
import lombok.Builder;
import lombok.Getter;

@Builder
public class CarImpl implements TPS_Car{

    /**N
     * this fuel type
     */
    private FuelTypeName type;
    /**
     * this car size
     */
    private final int kbaNo;

    private long entry_time = 0;

    /**
     * this car id
     * -- GETTER --
     *  GEts the id of this car
     *
     * @return the car id

     */
    @Getter
    private int id;
    /**
     * Enum for the engine emissions class
     * -- GETTER --
     *
     * @return the engineClass

     */
    @Getter
    private EmissionClass emissionClass;
    /**
     * Flag if this car is restricted in access of certain areas
     */

    private final boolean restricted;
    /**
     * Level of automation for this car
     * -- GETTER --
     *
     * @return the automation

     */
    @Getter
    private final int automationLevel;

    /**
     * fix costs, eg. insurance, tax, parking-zone in Euro
     * -- GETTER --
     *  Method to get the fixed costs of this car
     *
     * @return the actual fix costs

     */
    @Getter
    private double fixCosts;
    private double costPerKilometer;
    private double variableCostPerKilometer;

    /**
     * -- GETTER --
     *  Method to get the fuel type of this car
     *
     * @return the reference of the fueltype, which is a singelton
     */
    @Getter
    private final FuelType fuelType;
    private double rangeLeft;

    @Override
    public double getRangeLeft() {
        return this.rangeLeft;
    }


    /**
     * This method converts the internal kba number to a size
     *
     * @return the car size
     */
    public CarSize getCarSize() {
        return ModeUtils.getCarSize(this.kbaNo);
    }

    /**
     * Method to get the cost per kilometre, depending on fueltype
     */
    @Override
    public double costPerKilometer() {

        return this.costPerKilometer;
    }

    public FuelTypeName getFuelTypeName(){
        return this.fuelType.getFuelType();
    }

    /**
     * Method to get the kba-number of this car
     *
     * @return the kba number
     */
    public int getKBANumber() {
        return this.kbaNo;
    }

    /**
     * Method to get the cost per kilometre, depending on fueltype
     */
    @Override
    public double variableCostPerKilometer() {
        return this.fuelType.getVariableCostPerKm() * ModeUtils.getKBAVariableCostPerKilometerFactor(this.kbaNo);
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public FuelType fuelType() {
        return this.fuelType;
    }

    /**
     * @return the restricted
     */
    public boolean isRestricted() {
        return restricted;
    }

}
