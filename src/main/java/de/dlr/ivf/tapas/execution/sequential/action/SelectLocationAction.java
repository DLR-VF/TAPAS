package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.choice.LocationContext;
import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

/**
 * This action selects a location.
 */
public class SelectLocationAction implements TPS_PlanStateAction{

    private final TourContext tour_context;
    private final LocationContext location_context;
    private final PlanContext plan_context;

    /**
     *
     * @param tour_context the current tour context of a person
     * @param location_context the current location context of a person
     * @param plan_context the plan context of a person
     */
    public SelectLocationAction(TourContext tour_context, LocationContext location_context, PlanContext plan_context){

        this.tour_context = tour_context;
        this.location_context = location_context;
        this.plan_context = plan_context;
    }

    /**
     * This selects the next location to visit. If the next stay is fix, it will be revisited.
     */
    @Override
    public void run() {

        TPS_Plan plan = plan_context.getPlan();
        TPS_Stay next_stay = tour_context.getNextStay();
        TPS_LocatedStay next_located_stay = plan.getLocatedStay(next_stay);

        if(next_stay.isAtHome()){ //we do know where we live hopefully
            updateContextAndStayLocation(location_context.getHomeLocation(), next_located_stay);
        }else {
            location_context.getFromFixLocations(next_stay.getActCode())
                    .ifPresentOrElse(
                            //revisit the fix location
                            location -> updateContextAndStayLocation(location,next_located_stay),
                            //select a new location
                            () -> {
                                next_located_stay.selectLocation(plan, plan.getPlanningContext(), tour_context::getCurrentStay, tour_context::getLastStay);
                                location_context.setNextLocation(next_located_stay.getLocation());
                            }
                    );
        }

        if(next_stay.getActCode().isFix()){
            location_context.addToFixLocations(next_stay.getActCode(),next_located_stay.getLocation());
        }

        location_context.setNextLocation(next_located_stay.getLocation());

    }

    private void updateContextAndStayLocation(TPS_Location next_location, TPS_LocatedStay located_stay) {

        location_context.setNextLocation(next_location);
        located_stay.setLocation(next_location);
    }
}
