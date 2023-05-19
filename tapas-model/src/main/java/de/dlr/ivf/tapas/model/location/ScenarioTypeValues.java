package de.dlr.ivf.tapas.model.location;

import lombok.Builder;

/**
 * Stores all values which are used in the different simulation type:
 * parking (fee, has, type) and toll (fee, has, type)
 *
 * @author mark_ma
 */
@Builder
public class ScenarioTypeValues {
    /// Average speed inside the traffic analysis zone for motorised individual transport mode
    private double averageSpeedMIT;

    /// The travel time inside the traffic analysis zone for public transport mode
    private double averageSpeedPT = 1.0;

    /// The beeline factor for transforming beeline distances in net distances within this traffic analysis zone and only for car
    private double beelineFactorMIT;

    /// The costs for parking per hour in Euro
    private double feeParking;

    /// The costs for toll per entrance
    private double feeToll;

    /// The flag if parking fee is applicable
    private boolean hasParkingFee;

    /// The flag whether a toll shall be applied
    private boolean hasToll;

    /// The type of the parking fee
    private int typeParking;

    /// The type of the toll
    private int typeToll;

    /// The zone id for a public transport tariff system
    private int ptZone = 1;

    /// Whether intra MIT traffic is allowed
    private boolean intraMITTrafficAllowed = true;

    /// Whether intra PT traffic is allowed
    private boolean intraPTTrafficAllowed = true;

    private boolean isCarsharingServiceArea = false;

    /**
     * BUGFIX: Mantis Entry 4318 Default constructor, which fills the local
     * variables with reasonable values. If this TAZ is initialized, but not
     * filled with values, divisions by zero may happen, e.g.
     * averageTravelingSpeed is used
     */
    public ScenarioTypeValues() {
        // this.setAverageSpeedMIT(ParamValue.VELOCITY_CAR.getDoubleValue());
        // // minimum speed!
        // this.setAverageSpeedPT(ParamValue.VELOCITY_TRAIN.getDoubleValue());
        // // minimum speed!
        // this.setBeelineFactorMIT(ParamValue.BEELINE_FACTOR_MIT.getDoubleValue());
        // //default factor
        this.setFeeParking(0.0); // no parking fee
        this.setHasParkingFee(false);
        this.setTypeParking(0);
        this.setFeeToll(0.0); // no toll
        this.setHasToll(false);
        this.setTypeToll(0);
        this.setCarsharingServiceArea(false);
    }

    /**
     * Returns the average speed within the traffic analysis zone in m/s for
     * the motorised individual transport mode
     *
     * @return speed in m/s for MIT
     */
    public double getAverageSpeedMIT() {
        return averageSpeedMIT;
    }

    /**
     * Sets the average MIT speed in m/s
     *
     * @param averageSpeed The average MIT speed in m/s
     */
    public void setAverageSpeedMIT(double averageSpeed) {
        this.averageSpeedMIT = averageSpeed;
    }

    /**
     * Returns the average speed within the traffic analysis zone in m/s for
     * the public transport mode
     *
     * @return speed in m/s for PT
     */
    public double getAverageSpeedPT() {
        return averageSpeedPT;
    }

    /**
     * Sets the travel PT speed within the traffic analysis zone in m/s
     *
     * @param averageSpeed The average PT speed in m/s
     */
    public void setAverageSpeedPT(double averageSpeed) {
        this.averageSpeedPT = averageSpeed;
    }

    /**
     * Returns the beeline factor valid for trips within this traffic
     * analysis zone and for the car modes; used for converting beeline
     * distances into net distances
     *
     * @return beeline factor
     */
    public double getBeelineFactorMIT() {
        return beelineFactorMIT;
    }

    /**
     * Sets the beeline factor for converting beeline distances into net
     * distances for car trips within this traffic analysis zone
     *
     * @param beelineFactor The new beeline factor
     */
    public void setBeelineFactorMIT(double beelineFactor) {
        this.beelineFactorMIT = beelineFactor;
    }

    /**
     * Returns the parking fee per hour in €
     *
     * @return parking fee
     */
    public double getFeeParking() {
        return feeParking;
    }

    /**
     * Sets the parking fee in Euro per hour
     *
     * @param feeParking The parking fee for this TAZ
     */
    public void setFeeParking(double feeParking) {
        this.feeParking = feeParking;
    }

    /**
     * Returns the toll fee per entrance in €
     *
     * @return toll fee
     */
    public double getFeeToll() {
        return feeToll;
    }

    /**
     * Sets the toll fee per entrance
     *
     * @param feeToll The toll for this TAZ
     */
    public void setFeeToll(double feeToll) {
        this.feeToll = feeToll;
    }

    /**
     * This Method returns the fare zone for the given TAZ- cell
     *
     * @return The id of the PT fare zone this TAZ is assigned to
     */
    public int getPtZone() {
        return ptZone;
    }

    /**
     * This method sets the PT zone for the PT fare
     *
     * @param ptZone The id of the PT fare zone
     */
    public void setPtZone(int ptZone) {
        this.ptZone = ptZone;
    }

    /**
     * Returns the parking type / category
     *
     * @return the category of parking fee
     */
    public int getTypeParking() {
        return typeParking;
    }

    /**
     * Sets the type of parking fee applicable for the TAZ
     *
     * @param typeParking The parking fee type to set
     */
    public void setTypeParking(int typeParking) {
        this.typeParking = typeParking;
    }

    /**
     * Returns the category / type of the toll
     *
     * @return toll type
     */
    public int getTypeToll() {
        return typeToll;
    }

    /**
     * Sets the type of toll fee applicable for the tvz
     *
     * @param typeToll The toll type to set
     */
    public void setTypeToll(int typeToll) {
        this.typeToll = typeToll;
    }

    /**
     * Flag if a parking fee is charged in the traffic analysis zone
     *
     * @return true if parking fee is applicable, false else
     */
    public boolean hasParkingFee() {
        return hasParkingFee;
    }

    /**
     * Flag if a toll fee is charged in the traffic analysis zone
     *
     * @return true if toll fee is applicable, false else
     */
    public boolean hasToll() {
        return hasToll;
    }

    /**
     * @return the isCarsharingServiceArea
     */
    public boolean isCarsharingServiceArea() {
        return isCarsharingServiceArea;
    }

    /**
     * @param isCarsharingServiceArea the isCarsharingServiceArea to set
     */
    public void setCarsharingServiceArea(boolean isCarsharingServiceArea) {
        this.isCarsharingServiceArea = isCarsharingServiceArea;
    }

    /**
     * This method returns true if intra cell traffic is allowed for MIT
     *
     * @return Whether intra MIT traffic is allowed
     */
    public boolean isIntraMITTrafficAllowed() {
        return intraMITTrafficAllowed;
    }

    /**
     * This method sets the intra cell traffic for MIT
     *
     * @param intraMITTrafficAllowed true if traffic within this cell is allowed
     */
    public void setIntraMITTrafficAllowed(boolean intraMITTrafficAllowed) {
        this.intraMITTrafficAllowed = intraMITTrafficAllowed;
    }

    /**
     * This method returns true if intra cell traffic is allowed for PT
     *
     * @return Whether intra PT traffic is allowed
     */
    public boolean isIntraPTTrafficAllowed() {
        return intraPTTrafficAllowed;
    }

    /**
     * This method sets the intra cell traffic for PT
     *
     * @param intraPTTrafficAllowed true if traffic within this cell is allowed
     */
    public void setIntraPTTrafficAllowed(boolean intraPTTrafficAllowed) {
        this.intraPTTrafficAllowed = intraPTTrafficAllowed;
    }

    /**
     * Sets the flag indicating, if a parking fee is charged in the TAZ
     *
     * @param hasParkingFee Whether a parking fee is charged in this TAZ
     */
    public void setHasParkingFee(boolean hasParkingFee) {
        this.hasParkingFee = hasParkingFee;
    }

    /**
     * Sets the flag indicating, if a toll fee is charged in the TAZ
     *
     * @param hasToll Whether a toll is charged in this TAZ
     */
    public void setHasToll(boolean hasToll) {
        this.hasToll = hasToll;
    }

    /**
     * Returns this object's string representation
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "[averageSpeed=" + averageSpeedMIT + ", beelineFactor=" + beelineFactorMIT + ", feeParking=" +
                feeParking + ", feeToll=" + feeToll + ", hasParkingFee=" + hasParkingFee + ", hasToll=" + hasToll +
                ", travelTime=" + averageSpeedPT + ", typeParking=" + typeParking + ", typeToll=" + typeToll +
                ", PTZone=" + ptZone + ", IntraPT=" + this.intraPTTrafficAllowed + ", IntraMIT=" +
                this.intraMITTrafficAllowed + "]";
    }

}
