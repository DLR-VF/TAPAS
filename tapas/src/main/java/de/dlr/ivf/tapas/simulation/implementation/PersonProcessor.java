package de.dlr.ivf.tapas.simulation.implementation;

import de.dlr.ivf.tapas.model.plan.PlanningContext;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.Tour;
import de.dlr.ivf.tapas.simulation.Processor;

import java.util.Comparator;
import java.util.List;

public class PersonProcessor implements Processor<PlanningContext, TPS_PlanEnvironment> {

    @Override
    public TPS_PlanEnvironment process(PlanningContext planningContext) {

        List<TourContext> tourContexts = planningContext.tours()
                .stream()
                .sorted(Comparator.comparingInt(Tour::tourNumber))
                .map(TourContext::new)
                .toList();

        for(TourContext tourContext : tourContexts){
            tourContext.getTour().trips().forEach(trip -> );



        }

        return null;
    }
}
