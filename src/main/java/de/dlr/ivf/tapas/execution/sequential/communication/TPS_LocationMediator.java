package de.dlr.ivf.tapas.execution.sequential.communication;

import de.dlr.ivf.tapas.loc.TPS_Location;

public class TPS_LocationMediator implements TPS_Mediator {
    private TPS_Location location;

    public TPS_LocationMediator(TPS_Location location){
        this.location = location;

    }
    @Override
    public TPS_Location request() {
        return null;
    }

    @Override
    public void offer() {

    }
}
