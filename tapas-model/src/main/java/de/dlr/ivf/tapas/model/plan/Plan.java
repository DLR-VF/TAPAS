package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.scheme.PlannedTour;

import java.util.HashMap;
import java.util.Map;

public class Plan {

    private final Map<Integer, TourContext> tourContexts;
    private final Map<Integer, PlannedTour> plannedTours;

    public Plan(){
        this.tourContexts = new HashMap<>();
        this.plannedTours = new HashMap<>();
    }

    public void addPlannedTour(int tourNumber, PlannedTour plannedTour){
        plannedTours.put(tourNumber, plannedTour);
    }

    public void addTourContext(int tourNumber, TourContext tourContext){
        tourContexts.put(tourNumber, tourContext);
    }

}
