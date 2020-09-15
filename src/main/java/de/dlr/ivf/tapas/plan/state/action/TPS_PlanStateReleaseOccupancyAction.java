package de.dlr.ivf.tapas.plan.state.action;

import de.dlr.ivf.tapas.loc.TPS_Location;

public class TPS_PlanStateReleaseOccupancyAction implements TPS_PlanStateAction {

    private TPS_Location location;


    public TPS_PlanStateReleaseOccupancyAction(TPS_Location location){
        this.location = location;
    }

    @Override
    public void run() {
        this.location.updateOccupancy(-1);
    }
}
