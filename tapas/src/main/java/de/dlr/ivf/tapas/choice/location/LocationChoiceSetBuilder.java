package de.dlr.ivf.tapas.choice.location;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;
import de.dlr.ivf.tapas.util.distance.DistanceProvider;
import de.dlr.ivf.tapas.util.distance.providers.ModeMatrixDistanceProvider;
import de.dlr.ivf.tapas.util.traveltime.providers.TravelTimeCalculator;
import de.dlr.ivf.tapas.model.constants.Activities;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.location.TrafficAnalysisZones;
import de.dlr.ivf.tapas.model.plan.TourContext;
import de.dlr.ivf.tapas.model.scheme.Stay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Lazy
@Component
public class LocationChoiceSetBuilder implements LocationChoiceSetProvider<LocationChoiceContext>{

    private final TravelTimeCalculator travelTimeCalculator;
    private final TrafficAnalysisZones trafficAnalysisZones;
    private final Activities activities;
    private final TPS_Mode modeForDistance;
    private final int maxSystemSpeed;
    private final int maxChoiceSetSize;
    private final DistanceProvider<TPS_Mode> distanceProvider;
    private final int maxTriesLocationSelection;
    private final int minResultSetSize;
    private final int numTazRepresentatives;

    @Autowired
    public LocationChoiceSetBuilder(@Qualifier("tazTravelTimeCalculator") TravelTimeCalculator travelTimeCalculator,
                                    ModeMatrixDistanceProvider distanceCalculator,
                                    TrafficAnalysisZones trafficAnalysisZones,
                                    Activities activities,
                                    @Qualifier("modeForDistance") TPS_Mode modeForDistance,
                                    @Qualifier("maxSystemSpeed") int maxSystemSpeed,
                                    @Qualifier("numTazRepresentatives") int numTazRepresentatives,
                                    @Qualifier("maxTriesLocationSelection") int maxTriesLocationSelection){

        this.travelTimeCalculator = travelTimeCalculator;
        this.distanceProvider = distanceCalculator;
        this.trafficAnalysisZones = trafficAnalysisZones;
        this.activities = activities;
        this.modeForDistance = modeForDistance;
        this.maxSystemSpeed = maxSystemSpeed;
        this.maxChoiceSetSize = numTazRepresentatives * trafficAnalysisZones.getTrafficZones().size();
        this.maxTriesLocationSelection = maxTriesLocationSelection;
        this.minResultSetSize = 2 * numTazRepresentatives;
        this.numTazRepresentatives = numTazRepresentatives;
    }

    @Override
    public LocationChoiceSet buildSet(LocationChoiceContext context){

        Stay stay = context.stayToLocalize();
        TourContext tourContext = context.tourContext();

        TPS_ActivityConstant activityCode = activities.getActivity(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, stay.activity());
        TPS_TrafficAnalysisZone fromTaz = tourContext.getLocationForStay(context.comingFromStay()).getTrafficAnalysisZone();
        TPS_TrafficAnalysisZone toTaz = tourContext.getLocationForStay(context.rubberBandStay()).getTrafficAnalysisZone();


        int i = 1;
        int resultSetSize = 0;

        for(; i <= maxTriesLocationSelection + 1 && resultSetSize < minResultSetSize; i++){

            for(TPS_TrafficAnalysisZone taz : trafficAnalysisZones.getTrafficZones()){

                LocationChoiceSet resultSet = new LocationChoiceSet(maxChoiceSetSize);



                double totalTravelDurationToStay = tourContext.getCumulativeTravelDurationToStay(stay);
                double remainingTravelDurationFromStay = tourContext.getRemainingTravelDurationAfterStay(stay);
                double activityDistance = distanceProvider.getDistance(modeForDistance, taz, toTaz);
                activityDistance += distanceProvider.getDistance(modeForDistance, fromTaz, taz);

                double arrivalDistance = travelTimeCalculator.getTravelTime(modeForDistance, fromTaz, taz, stay.startTime());

            }
        }
        return null;
    }

    public void processTrafficAnalysisZone(TPS_TrafficAnalysisZone taz, TPS_ActivityConstant activity, Vehicle vehicle, LocationChoiceSet resultSet){
        if(skipTrafficAnalysisZone(taz, activity,vehicle)){
            return;
        }

        Collection<TPS_Location> locations = generateLocationRepresentatives(taz, activity);


    }

    public boolean skipTrafficAnalysisZone(TPS_TrafficAnalysisZone taz, TPS_ActivityConstant activity, Vehicle vehicle){

        return !taz.allowsActivity(activity) || // no activities of this type
                taz.getActivityWeightSum(activity) <= 0 ||
                vehicle.isRestricted() && taz.isRestricted();
    }

    public Collection<TPS_Location> generateLocationRepresentatives(TPS_TrafficAnalysisZone taz, TPS_ActivityConstant activity){
        return taz.selectActivityLocations(activity, numTazRepresentatives);
    }

    public boolean isDistanceConstrained(double systemSpeed, double comingFromDistance, double goingToDistance){
        return false;
    }

    public double calculateSumWeight(Collection<TPS_Location> locations){
        double sumWeight = 0;
        for (TPS_Location location : locations) {
            sumWeight += location.getData().getWeight();
        }

        return sumWeight;
    }
}
