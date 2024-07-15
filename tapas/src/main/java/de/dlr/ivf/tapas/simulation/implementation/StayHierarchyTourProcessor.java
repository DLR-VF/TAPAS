package de.dlr.ivf.tapas.simulation.implementation;

import de.dlr.ivf.tapas.model.constants.Activities;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.plan.StayHierarchies;
import de.dlr.ivf.tapas.model.plan.StayHierarchy;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.PlannedTour;
import de.dlr.ivf.tapas.model.scheme.Stay;
import de.dlr.ivf.tapas.model.scheme.Trip;
import de.dlr.ivf.tapas.simulation.Processor;
import de.dlr.ivf.tapas.simulation.choice.location.LocationChoiceContext;
import de.dlr.ivf.tapas.simulation.choice.location.LocationChoiceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Lazy
@Component
public class StayHierarchyTourProcessor implements Processor<TourContext, PlannedTour> {

    private final Comparator<Trip> tripPriorityComparator;
    private final StayHierarchies stayHierarchies;
    private final Activities activities;
    //private final LocationChoiceModel<LocationChoiceContext> locationChoiceModel;

    @Autowired
    public StayHierarchyTourProcessor(Comparator<Trip> tripPriorityComparator, StayHierarchies stayHierarchies, Activities activities) {
        this.tripPriorityComparator = tripPriorityComparator;
        this.stayHierarchies = stayHierarchies;
        this.activities = activities;
    }

    @Override
    public PlannedTour process(TourContext context) {

        List<Trip> orderedTrips = context.getTour().trips().stream().sorted(tripPriorityComparator).toList();

        StayHierarchy stayHierarchy = stayHierarchies.getStayHierarchy(context.getTour().id());

        for(Stay stay : stayHierarchy.getPrioritizedStays()){



        }

        return null;
    }
}
