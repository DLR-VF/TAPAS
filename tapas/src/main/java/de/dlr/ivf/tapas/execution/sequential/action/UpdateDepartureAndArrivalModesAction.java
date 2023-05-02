package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.model.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;

public class UpdateDepartureAndArrivalModesAction implements TPS_PlanStateAction {

    private final TPS_LocatedStay current_located_stay;
    private final TPS_LocatedStay next_located_stay;
    private final TPS_ExtMode next_mode;

    public UpdateDepartureAndArrivalModesAction(TPS_LocatedStay current_located_stay, TPS_LocatedStay next_located_stay, TPS_ExtMode next_mode) {
        this.current_located_stay = current_located_stay;
        this.next_located_stay = next_located_stay;
        this.next_mode = next_mode;
    }

    @Override
    public void run() {

        this.current_located_stay.setModeDep(next_mode);
        this.next_located_stay.setModeArr(next_mode);

    }
}
