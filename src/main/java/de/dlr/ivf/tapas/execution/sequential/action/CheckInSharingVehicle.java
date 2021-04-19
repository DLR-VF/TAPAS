package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.communication.SharingDelegator;

public class CheckInSharingVehicle<T> implements TPS_PlanStateAction{
    private final SharingDelegator<T> sharing_delegator;
    private final T used_vehicle;
    private final int start_id;

    public CheckInSharingVehicle(SharingDelegator<T> sharing_delegator, int start_id, T used_vehicle){
        this.sharing_delegator = sharing_delegator;
        this.start_id = start_id;
        this.used_vehicle = used_vehicle;
    }


    @Override
    public void run() {
        sharing_delegator.checkIn(start_id,used_vehicle);
    }
}
