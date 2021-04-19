package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.execution.sequential.io.TPS_WritableTrip;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

public class TripPersistenceAction implements TPS_PlanStateAction {

    private TPS_TripWriter writer;
    private TPS_Plan plan;
    private TPS_TourPart tp;
    private TPS_Trip associated_trip;

    public TripPersistenceAction(TPS_TripWriter writer, PlanContext plan_context, TourContext tour_context){
        this.writer = writer;
        this.plan = plan_context.getPlan();
        this.tp = tour_context.getTourPart();
        this.associated_trip = associated_trip;
    }

    @Override
    public void run() {
        try {
            writer.writeTrip(new TPS_WritableTrip(this.plan,this.tp, this.associated_trip));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
