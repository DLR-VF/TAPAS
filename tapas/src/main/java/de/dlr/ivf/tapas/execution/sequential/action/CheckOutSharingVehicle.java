package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.mode.SharingDelegator;

public class CheckOutSharingVehicle<T> implements TPS_PlanStateAction {

    private final SharingDelegator<T> sharing_delegator;
    private final T requested_vehicle;
    private final int start_id;

    public CheckOutSharingVehicle(SharingDelegator<T> sharing_delegator, int start_id, T requested_vehicle){
        this.sharing_delegator = sharing_delegator;
        this.start_id = start_id;
        this.requested_vehicle = requested_vehicle;
    }


    @Override
    public void run() {
        sharing_delegator.checkOut(start_id,requested_vehicle);
    }
}
