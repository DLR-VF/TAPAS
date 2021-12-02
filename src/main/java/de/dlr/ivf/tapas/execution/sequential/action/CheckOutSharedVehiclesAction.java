package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.communication.SharingDelegator;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.execution.sequential.context.ModeContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

public class CheckOutSharedVehiclesAction implements TPS_PlanStateAction {
    private final TPS_HouseholdCarMediator household_car_provider;
    private final TPS_PlanningContext planning_context;
    private final TourContext tour_context;
    private final SharingDelegator<TPS_Car> car_sharing_delegator;

    public CheckOutSharedVehiclesAction(TPS_HouseholdCarMediator householdCarProvider, TPS_PlanningContext pc, TourContext tour_context, SharingDelegator<TPS_Car> car_sharing_delegator) {

        this.household_car_provider = householdCarProvider;
        this.planning_context = pc;
        this.tour_context = tour_context;
        this.car_sharing_delegator = car_sharing_delegator;
    }

    @Override
    public void run() {

        TPS_Stay current_stay = tour_context.getCurrentStay();
        ModeContext mode_context = tour_context.getModeContext();
        TPS_ExtMode next_chosen_mode = mode_context.getNextMode();

        if(household_car_provider != null && current_stay.isAtHome()) {

            if (next_chosen_mode.primary == TPS_Mode.get(TPS_Mode.ModeType.MIT))
                household_car_provider.checkOut(planning_context.getHouseHoldCar());
            else {
                if(planning_context.getHouseHoldCar() != null) {
                    household_car_provider.release(planning_context.getHouseHoldCar());
                    planning_context.setHouseHoldCar(null);
                }
            }
        }

        if(next_chosen_mode.primary == TPS_Mode.get(TPS_Mode.ModeType.CAR_SHARING)){
             TPS_Location current_location = tour_context.getCurrentLocation();
             car_sharing_delegator.checkOut(current_location.getTAZId(), planning_context.getCarSharingCar());
        }

    }
}
