package de.dlr.ivf.tapas.plan.state.action;

import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.TPS_SequentialWorker;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

public class TPS_PlanStatePersistenceAction implements TPS_PlanStateAction {

    private TPS_SequentialWorker worker;
    private TPS_Plan plan;
    private TPS_TourPart tp;
    private TPS_Trip trip;

    public TPS_PlanStatePersistenceAction(TPS_SequentialWorker worker, TPS_Plan plan, TPS_TourPart tp, TPS_Trip trip){
        this.worker = worker;
        this.plan = plan;
        this.tp = tp;
        this.trip = trip;
    }

    @Override
    public void run() {
        worker.addTripToOutput(this.plan,this.tp, this.trip);
    }
}
