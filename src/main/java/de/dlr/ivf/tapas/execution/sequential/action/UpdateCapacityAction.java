package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.loc.TPS_Location;

import java.util.function.Supplier;

public class UpdateCapacityAction implements TPS_PlanStateAction {

    private Supplier<TPS_Location> location_supplier;
    private int delta;


    public UpdateCapacityAction(Supplier<TPS_Location> location_supplier, int delta){
        this.location_supplier = location_supplier;
        this.delta = delta;
    }

    @Override
    public void run() {

        this.location_supplier.get().updateOccupancy(delta);
    }
}
