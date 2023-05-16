package de.dlr.ivf.tapas.execution.sequential.context;


import de.dlr.ivf.tapas.execution.sequential.choice.LocationContext;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.CarFleetManager;

import java.util.*;
import java.util.stream.Collectors;

public class PlanContext implements ContextUpdateable {

    private final Deque<TPS_TourPart> tour_parts;

    private TPS_Episode current_episode;
    private TPS_Episode last_episode;

    private TPS_Episode next_episode;
    private TPS_Location home_location;

    private TourContext current_tour_context;

    private LocationContext location_context;
    private CarFleetManager carFleetManager;

    private final TPS_Plan plan;

    private int absolute_time_deviation = 0;

    public CarFleetManager getCarFleetManager() {
        return carFleetManager;
    }

    public PlanContext(TPS_Plan plan, CarFleetManager carFleetManager, LocationContext location_context) {

        this.plan = plan;

        this.location_context = location_context;

        this.home_location = location_context.getHomeLocation();

        this.carFleetManager = carFleetManager;

        List<TPS_SchemePart> scheme_parts = plan.getScheme().getSchemeParts();

        this.current_episode = scheme_parts.get(0).getFirstEpisode();

        this.last_episode = scheme_parts.get(scheme_parts.size()-1).getFirstEpisode();

        this.tour_parts = scheme_parts.stream()
                                      .filter(TPS_SchemePart::isTourPart)
                                      .sorted(Comparator.comparingDouble(TPS_SchemePart::getOriginalSchemePartStart))
                                      .filter(scheme_part -> scheme_part.getEpisodes().size() > 1)
                                      .map(TPS_TourPart.class::cast)
                                      .collect(Collectors.toCollection(ArrayDeque::new));

        updateContext();
    }

    public Optional<TourContext> getTourContext() {

            return Optional.ofNullable(current_tour_context);
    }

    public void updateTimeDeviation(int delta_time){
        this.absolute_time_deviation += delta_time;
    }


    public TPS_Plan getPlan() {
        return this.plan;
    }

    public LocationContext getLocationContext() {

        return this.location_context;
    }

    private TourContext getNextTourContext(){
        TPS_TourPart tour_part = tour_parts.poll();

        if(tour_part == null)
            return null;

        TPS_HomePart current_home_part = plan.getHomePartPriorToTourPart(tour_part);
        TPS_HomePart next_home_part = plan.getHomePartAfterTourPart(tour_part);

        return new TourContext(tour_part, (TPS_Stay) current_home_part.getFirstEpisode(),location_context, (TPS_Stay) next_home_part.getFirstEpisode());
    }

    public int getTimeDeviation() {
        return this.absolute_time_deviation;
    }

    @Override
    public void updateContext() {

        TPS_TourPart tour_part = tour_parts.poll();

        if(tour_part == null) {
            this.current_tour_context = null;
        }else{
            TPS_HomePart current_home_part = plan.getHomePartPriorToTourPart(tour_part);
            TPS_HomePart next_home_part = plan.getHomePartAfterTourPart(tour_part);

            this.current_tour_context = new TourContext(tour_part, (TPS_Stay) current_home_part.getFirstEpisode(),location_context, (TPS_Stay) next_home_part.getFirstEpisode());
        }
    }
}
