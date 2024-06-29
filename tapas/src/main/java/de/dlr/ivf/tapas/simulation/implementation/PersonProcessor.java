package de.dlr.ivf.tapas.simulation.implementation;
import java.lang.System.Logger;
import de.dlr.ivf.tapas.initializers.TourContextFactory;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.plan.Plan;
import de.dlr.ivf.tapas.model.plan.PlanningContext;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.PlannedTour;
import de.dlr.ivf.tapas.model.scheme.Tour;
import de.dlr.ivf.tapas.simulation.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * The PersonProcessor class is responsible for processing a PlanningContext and producing a Plan with planned tours.
 */
@Lazy
@Component
public class PersonProcessor implements Processor<PlanningContext, Plan> {
    private final Logger logger = System.getLogger(PersonProcessor.class.getName());

    private final TourContextFactory tourContextFactory;
    private final HierarchicalTourProcessor tourProcessor;
    private final int homeActivityId;

    @Autowired
    public PersonProcessor(TourContextFactory tourContextFactory, HierarchicalTourProcessor tourProcessor,
                           @Qualifier("homeActivityId") int homeActivityId) {
        this.tourContextFactory = tourContextFactory;
        this.tourProcessor = tourProcessor;
        this.homeActivityId = homeActivityId;
    }

    /**
     * Processes a PlanningContext and produces a Plan with planned tours.
     *
     * @param planningContext The PlanningContext containing information about the person, home location, and tours.
     * @return The generated Plan with planned tours.
     */
    @Override
    public Plan process(PlanningContext planningContext) {

        logger.log(System.Logger.Level.DEBUG, "Processing Person {0}: ", planningContext.person().getId());

        Map<Integer, TPS_Location> preKnownLocations = Map.of(homeActivityId, planningContext.homeLocation());


        List<TourContext> tourContexts = planningContext.tours()
                .stream()
                .sorted(Comparator.comparingInt(Tour::tourNumber))
                .map(tour -> tourContextFactory.newTourContext(tour, preKnownLocations))
                .toList();

        Plan plan = new Plan();

        for(TourContext tourContext : tourContexts){
            PlannedTour plannedTour = tourProcessor.process(tourContext);

            int tourNumber = tourContext.getTour().tourNumber();
            plan.addPlannedTour(tourNumber, plannedTour);
            plan.addTourContext(tourNumber, tourContext);
        }

        return plan;
    }
}
