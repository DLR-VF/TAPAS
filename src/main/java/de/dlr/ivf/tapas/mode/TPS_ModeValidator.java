package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.execution.sequential.context.ModeContext;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;

import java.util.Optional;
import java.util.function.Predicate;

public class TPS_ModeValidator {

    private final TazBasedCarSharingDelegator car_sharing_delegator;
    //todo emergency mode / taz based fallback
    public TPS_ModeValidator(TazBasedCarSharingDelegator car_sharing_mediator){

        this.car_sharing_delegator = car_sharing_mediator;
    }

    public void validateMode(TPS_PlanningContext planning_context, ModeContext mode_context, TPS_Location start_location, TPS_Location end_location, TPS_PlannedTrip planned_trip, Predicate<TPS_Car> car_filter){

        TPS_ExtMode next_mode = mode_context.getNextMode();

        switch (next_mode.primary.getModeType()){
            case MIT: validateHouseHoldCarWithChosenMode(planning_context.getHouseHoldCar(), next_mode); break;
            case BIKE: validateBikeWithChosenMode(planning_context.isBikeAvailable, next_mode); break;
            case CAR_SHARING: validateCarSharing(planning_context, next_mode,start_location,end_location, car_filter); break;
        }

        //now set the mode manually to the trip
        planned_trip.setMode(next_mode);
    }

    private void validateCarSharing(TPS_PlanningContext planning_context, TPS_ExtMode chosen_mode, TPS_Location start_location, TPS_Location end_location, Predicate<TPS_Car> car_filter) {

        Optional<TPS_Car> car_sharing_car = this.car_sharing_delegator.request(start_location.getTAZId(), end_location.getTAZId(), car_filter);

        car_sharing_car.ifPresentOrElse(
                car -> planning_context.setCarSharingCar(car),
                () -> chosen_mode.primary = TPS_Mode.get(TPS_Mode.ModeType.PT)
        );
    }

    private void validateBikeWithChosenMode(boolean isBikeAvailable, TPS_ExtMode chosen_mode) {
        if(!isBikeAvailable)
            chosen_mode.primary = TPS_Mode.get(TPS_Mode.ModeType.PT);
    }

    private void validateHouseHoldCarWithChosenMode(TPS_Car household_car, TPS_ExtMode chosen_mode) {

        if(household_car == null)
            chosen_mode.primary = TPS_Mode.get(TPS_Mode.ModeType.PT);
    }
}
