package de.dlr.ivf.tapas.plan.state.communication;

import de.dlr.ivf.tapas.loc.TPS_Location;

public class TPS_LocationMediator implements TPS_Mediator {
    private TPS_Location location;

    public TPS_LocationMediator(TPS_Location location){
        this.location = location;

    }
    @Override
    public void request(double score) {

    }

    @Override
    public void offer() {

    }
}
