package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.execution.sequential.io.TPS_WritableTrip;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

public class TripPersistenceAction implements TPS_PlanStateAction {

    private TPS_TripWriter writer;
    private PlanContext plan_context;
    private TourContext tour_context;
    private TPS_Trip associated_trip;
    private TPS_PersistenceManager pm;

    public TripPersistenceAction(TPS_TripWriter writer, PlanContext plan_context, TourContext tour_context, TPS_PersistenceManager pm){
        this.writer = writer;
        this.plan_context = plan_context;
        this.tour_context = tour_context;
        this.associated_trip = tour_context.getNextTrip();
        this.pm = pm;
    }

    @Override
    public void run() {
        try {
            writer.writeTrip(new TPS_WritableTrip(this.plan_context,this.tour_context, this.pm));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
