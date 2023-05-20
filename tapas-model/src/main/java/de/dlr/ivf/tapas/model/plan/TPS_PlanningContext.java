/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.vehicle.CarController;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.scheme.TPS_Trip;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;

public class TPS_PlanningContext {
    // environment
    public TPS_PlanEnvironment pe;

    // plan
    public boolean influenceCarUsageInPlan;
    public Vehicle carForThisPlan;

    public CarController hhCar;
    public boolean influenceBikeUsageInPlan;
    public boolean isBikeAvailable;

    private boolean car_sharing_user;
    private TPS_Car car_sharing_car;

    public TPS_Trip previousTrip = null;
    public boolean fixLocationAtBase = false;

    public CarController getHhCar(){
        return this.hhCar;
    }


    //
    public TPS_PlanningContext(TPS_PlanEnvironment _pe, TPS_Car _car, boolean _isBikeAvailable) {
        pe = _pe;
        carForThisPlan = _car;
        influenceCarUsageInPlan = _car != null;
        influenceBikeUsageInPlan = false;
        isBikeAvailable = _isBikeAvailable;
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

    public Vehicle getHouseHoldCar() {
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
