package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.choice.LocationContext;
import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

public class SelectLocationAction implements TPS_PlanStateAction{

    private final TourContext tour_context;
    private final LocationContext location_context;
    private final PlanContext plan_context;

    public SelectLocationAction(TourContext tour_context, LocationContext location_context, PlanContext plan_context){

        this.tour_context = tour_context;
        this.location_context = location_context;
        this.plan_context = plan_context;
    }

    @Override
    public void run() {

        TPS_Plan plan = plan_context.getPlan();
        TPS_Stay next_stay = tour_context.getNextStay();
        TPS_LocatedStay next_located_stay = plan.getLocatedStay(next_stay);

        location_context.getFromFixLocations(next_stay.getActCode())
                        .ifPresentOrElse(
                                location -> { location_context.setNextLocation(location);
                                              next_located_stay.setLocation(location);
                                            },
                                () -> next_located_stay.selectLocation(plan,plan.getPlanningContext(), () -> tour_context.getCurrentStay(), tour_context.getNextHomeStay())
                        );
        if(next_stay.getActCode().isFix() || next_stay.isAtHome()){
            location_context.addToFixLocations(next_stay.getActCode(),next_located_stay.getLocation());
        }

    }
}
