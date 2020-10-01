package de.dlr.ivf.tapas.plan.sequential.action;

import de.dlr.ivf.tapas.loc.TPS_Location;

public class TPS_PlanStateUpdateOccupancyAction implements TPS_PlanStateAction {

    private TPS_Location location;
    private int delta;


    public TPS_PlanStateUpdateOccupancyAction(TPS_Location location,  int delta){
        this.location = location;
        this.delta = delta;
    }

    @Override
    public void run() {
        this.location.updateOccupancy(delta);
    }
}
