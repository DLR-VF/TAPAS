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
import de.dlr.ivf.tapas.runtime.server.SimTimeProvider;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

/**
 * In its current form, this action checks in used household and used car sharing cars.
 */
public class CheckInSharedVehiclesAction implements TPS_PlanStateAction{
    private final TPS_HouseholdCarMediator household_car_provider;
    private final TPS_PlanningContext planning_context;
    private final TourContext tour_context;
    private final SharingDelegator<TPS_Car> car_sharing_delegator;
    private final SimTimeProvider sim_time_provider;

    /**
     *
     * @param householdCarProvider the managing household based car instance
     * @param pc the planning context
     * @param tour_context the tour context
     * @param car_sharing_delegator the managing car sharing instance
     */
    public CheckInSharedVehiclesAction(TPS_HouseholdCarMediator householdCarProvider, TPS_PlanningContext pc, TourContext tour_context, SharingDelegator<TPS_Car> car_sharing_delegator, SimTimeProvider sim_time_provider){
        this.household_car_provider = householdCarProvider;
        this.planning_context = pc;
        this.tour_context = tour_context;
        this.car_sharing_delegator = car_sharing_delegator;
        this.sim_time_provider = sim_time_provider;
    }

    /**
     * If the car belongs to the household it will be made available for every other person that belongs to the household.
     * If the car is a car sharing car it will be checked in to the operating service at the destination traffic analysis zone
     */
    @Override
    public void run() {

        TPS_Stay arriving_stay = tour_context.getNextStay();
        ModeContext mode_context = tour_context.getModeContext();
        TPS_ExtMode arriving_mode = mode_context.getNextMode();

        //if(household_car_provider != null && arriving_stay.isAtHome()) {

        if (arriving_mode.primary == TPS_Mode.get(TPS_Mode.ModeType.MIT) && arriving_stay.isAtHome()) {
            household_car_provider.checkIn(planning_context.getHouseHoldCar());
            planning_context.setHouseHoldCar(null);
            return;
        }
        //}


        if(arriving_mode.primary == TPS_Mode.get(TPS_Mode.ModeType.CAR_SHARING)){
            TPS_Location arriving_location = tour_context.getNextLocation();
            car_sharing_delegator.checkIn(arriving_location.getTAZId(), planning_context.getCarSharingCar());
            planning_context.setCarSharingCar(null);
        }
    }
}
