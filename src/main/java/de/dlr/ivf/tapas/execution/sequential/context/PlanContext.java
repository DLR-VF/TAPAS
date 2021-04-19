package de.dlr.ivf.tapas.execution.sequential.context;

import de.dlr.ivf.tapas.execution.sequential.choice.LocationContext;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.*;
import java.util.*;
import java.util.stream.Collectors;

public class PlanContext {

    private final Deque<TPS_TourPart> tour_parts;

    private TPS_Episode current_episode;
    private TPS_Episode last_episode;

    private TPS_Episode next_episode;
    private TPS_Location home_location;

    private TourContext current_tour_context;

    private LocationContext location_context;

    private final TPS_Plan plan;

    private int absolute_time_deviation = 0;

    public TPS_HouseholdCarMediator getHouseholdCarProvider() {
        return household_car_provider;
    }

    private TPS_HouseholdCarMediator household_car_provider;

    public PlanContext(TPS_Plan plan, TPS_HouseholdCarMediator household_car_provider, LocationContext location_context) {

        this.plan = plan;

        this.location_context = location_context;

        this.home_location = location_context.getHomeLocation();

        this.household_car_provider = household_car_provider;

        List<TPS_SchemePart> scheme_parts = plan.getScheme().getSchemeParts();

        this.current_episode = scheme_parts.get(0).getFirstEpisode();

        this.last_episode = scheme_parts.get(scheme_parts.size()-1).getFirstEpisode();

        this.tour_parts = scheme_parts.stream()
                                      .filter(TPS_SchemePart::isTourPart)
                                      .sorted(Comparator.comparingDouble(TPS_SchemePart::getOriginalSchemePartStart))
                                      .map(TPS_TourPart.class::cast)
                                      .collect(Collectors.toCollection(ArrayDeque::new));
    }

    public Optional<TourContext> getTourContext() {

        if(current_tour_context == null || current_tour_context.isFinished()) {
            current_tour_context = getNextTourContext();
            return Optional.ofNullable(current_tour_context);
        }else
            return Optional.of(current_tour_context);
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

        return new TourContext(tour_part, (TPS_Stay) current_home_part.getFirstEpisode(),null, (TPS_Stay) next_home_part.getFirstEpisode());
    }

    public Integer getTimeDeviation() {
        return this.absolute_time_deviation;
    }
}
