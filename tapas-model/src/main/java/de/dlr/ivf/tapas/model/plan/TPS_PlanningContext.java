/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.TPS_AttributeReader;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.vehicle.CarController;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.scheme.TPS_Trip;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class TPS_PlanningContext {
    private final TPS_Household household;
    // environment
    public TPS_PlanEnvironment pe;

    // plan
    public boolean influenceCarUsageInPlan;
    public TPS_Car carForThisPlan;

    public CarController hhCar;
    public boolean influenceBikeUsageInPlan;
    public boolean isBikeAvailable;

    private boolean car_sharing_user;
    private TPS_Car car_sharing_car;

    public TPS_Trip previousTrip = null;
    public boolean fixLocationAtBase = false;

    private final Map<TPS_AttributeReader.TPS_Attribute, Integer> planningAttributes;
    @Getter
    private final TPS_Person person;

    public CarController getHhCar(){
        return this.hhCar;
    }


    //
    public TPS_PlanningContext(TPS_PlanEnvironment _pe, TPS_Car _car, boolean _isBikeAvailable, TPS_Person person, TPS_Household household) {
        pe = _pe;
        carForThisPlan = _car;
        influenceCarUsageInPlan = _car != null;
        influenceBikeUsageInPlan = false;
        isBikeAvailable = _isBikeAvailable;
        planningAttributes = new HashMap<>();
        this.person = person;
        this.household = household;
    }

    public void addAttribute(TPS_AttributeReader.TPS_Attribute attribute, int value){
        planningAttributes.put(attribute, value);
    }

    public int numHouseholdCars(){
        return household.getNumberOfCars();
    }

    public void addAttributes(Map<TPS_AttributeReader.TPS_Attribute, Integer> attributes){
        planningAttributes.putAll(attributes);
    }


    public boolean needsOtherModeAlternatives(TPS_Plan plan) {

        if (plan.usesCar()) {            //check if we need to rerun this plan with no car
            influenceCarUsageInPlan = true;
            carForThisPlan = null; //forbid car for next run
        } else if (plan.usesBike) {            //check if we need to rerun this plan with no car and no bike
            influenceBikeUsageInPlan = true;
            isBikeAvailable = false; //forbid bike for next run
        } else { //found a plan with no fixed modes!
            return false;
        }
        return true;
    }

    public void setHhCar(CarController car){
        this.hhCar = car;
    }

    public TPS_Car getHouseHoldCar() {
        return this.carForThisPlan;
    }

    public boolean isHouseHoldCarAvailable(){
        return carForThisPlan != null;
    }

    public void setCarSharingCar(TPS_Car car) {
        this.car_sharing_car = car;
    }

    public void setCarPooler(boolean carPooler) {
        this.car_sharing_user = carPooler;
    }

    public boolean isCarSharingUser() {
        return this.car_sharing_user;
    }

    public TPS_Car getCarSharingCar() {
        return this.car_sharing_car;
    }

    public void setHouseHoldCar(TPS_Car household_car) {

        this.carForThisPlan = household_car;

    }
}
