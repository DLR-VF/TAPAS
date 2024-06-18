package de.dlr.ivf.tapas.simulation.implementation;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.PlannedTour;
import de.dlr.ivf.tapas.model.scheme.Trip;
import de.dlr.ivf.tapas.simulation.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Lazy
@Component
public class HierarchicalTourProcessor implements Processor<TourContext, PlannedTour> {

    private final Comparator<Trip> tripPriorityComparator;

    @Autowired
    public HierarchicalTourProcessor(Comparator<Trip> tripPriorityComparator) {
        this.tripPriorityComparator = tripPriorityComparator;
    }

    @Override
    public PlannedTour process(TourContext context) {

        List<Trip> orderedTrips = context.getTour().trips().stream().sorted(tripPriorityComparator).toList();

        for(Trip trip : orderedTrips) {

            TPS_Location startLocation = context.getLocationForStay(trip.startStay());
            TPS_Location endLocation = context.getLocationForStay(trip.endStay());





        }


        return null;
    }
}
