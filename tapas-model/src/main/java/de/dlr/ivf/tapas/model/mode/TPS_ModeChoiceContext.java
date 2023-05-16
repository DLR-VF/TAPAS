/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.mode;


import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;

public class TPS_ModeChoiceContext {

    public TPS_Stay fromStay, toStay; // !!! fromStay not always set
    //public TPS_LocatedStay fromStayLocated, toStayLocated;
    public TPS_Location fromStayLocation, toStayLocation;
    public double duration;
    public boolean isBikeAvailable;
    public TPS_Car carForThisPlan;
    public int startTime;
    public TPS_Mode combinedMode = null;
    private TPS_Car car_sharing_car;

    private TPS_Mode current_mode = null;

    public TPS_ModeChoiceContext() {}

    public void setCar(TPS_Car car){
        this.carForThisPlan = car;
    }

    public void setBikeAvailability(boolean is_bike_available){
        this.isBikeAvailable = is_bike_available;
    }

    public void setCurrentMode(TPS_Mode mode){
        this.current_mode = mode;
    }

    public TPS_Car getCarSharingCar(){
        return this.car_sharing_car;
    }

    public void setCarSharingCar(TPS_Car car){

        this.car_sharing_car = car;
    }

}
