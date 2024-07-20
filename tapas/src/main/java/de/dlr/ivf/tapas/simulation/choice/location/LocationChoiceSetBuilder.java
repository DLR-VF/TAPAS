package de.dlr.ivf.tapas.simulation.choice.location;

import de.dlr.ivf.tapas.choice.distance.providers.ModeMatrixDistanceProvider;
import de.dlr.ivf.tapas.choice.traveltime.providers.TravelTimeCalculator;
import de.dlr.ivf.tapas.model.constants.Activities;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.location.TrafficAnalysisZones;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.Stay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Lazy
@Component
public class LocationChoiceSetBuilder {

    private final TravelTimeCalculator travelTimeCalculator;
    private final TrafficAnalysisZones trafficAnalysisZones;
    private final Activities activities;
    private final Modes modes;
    private final int maxSystemSpeed;

    @Autowired
    public LocationChoiceSetBuilder(@Qualifier("tazTravelTimeCalculator") TravelTimeCalculator travelTimeCalculator,
                                    ModeMatrixDistanceProvider distanceCalculator,
                                    TrafficAnalysisZones trafficAnalysisZones,
                                    Activities activities,
                                    Modes modes,
                                    @Qualifier("maxSystemSpeed") int maxSystemSpeed) {
        this.travelTimeCalculator = travelTimeCalculator;
        this.trafficAnalysisZones = trafficAnalysisZones;
        this.activities = activities;
        this.modes = modes;
        this.maxSystemSpeed = maxSystemSpeed;
    }

    public Collection<TPS_Location> buildSet(TourContext tourContext, Stay stay, TPS_TrafficAnalysisZone fromTaz, TPS_TrafficAnalysisZone toTaz){

        TPS_ActivityConstant activityCode = activities.getActivity(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, stay.activity());

        for(TPS_TrafficAnalysisZone taz : trafficAnalysisZones.getTrafficZones()){


            if (!taz.allowsActivity(activityCode)) {
                // no activities of this type
                continue;
            }

            double weight = taz.getActivityWeightSum(activityCode);
            if (weight <= 0) {
                // do not process "zero weight"-locations
                continue;
            }
            double totalTravelDurationToStay = tourContext.getCumulativeTravelDurationToStay(stay);
            double remainingTravelDurationFromStay = tourContext.getRemainingTravelDurationAfterStay(stay);
            double activityDistance = 0;

            double arrivalDistance = travelTimeCalculator.getTravelTime(modes.getModeByName("WALK"), fromTaz, taz, stay.startTime());

        }
        return null;
    }
}
