package de.dlr.ivf.tapas.choice.location;

import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.location.WeightedLocation;
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
    private final double incFactor;

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
        this.incFactor = 1.0 / maxTriesLocationSelection;
    }

    /**
     * Builds a location choice set based on the given context.
     *
     * @param context the location choice context
     * @return the location choice set
     */
    @Override
    public LocationChoiceSet buildSet(LocationChoiceContext context){

        Stay stay = context.stayToLocalize();
        TourContext tourContext = context.tourContext();

        TPS_ActivityConstant activityCode = activities.getActivity(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, stay.activity());
        TPS_Location fromLoc = tourContext.getLocationForStay(context.comingFromStay());
        TPS_Location toLoc = tourContext.getLocationForStay(context.rubberBandStay());
        TPS_TrafficAnalysisZone fromTaz = fromLoc.getTrafficAnalysisZone();
        TPS_TrafficAnalysisZone toTaz = toLoc.getTrafficAnalysisZone();
        Vehicle boundVehicle = tourContext.getBoundVehicle();


        int i = switch(activityCode.getCode(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS)){
            case 1 -> maxTriesLocationSelection;
            default -> 1;
        };

        int resultSetSize = 0;

        LocationChoiceSet resultSet = new LocationChoiceSet(maxChoiceSetSize);

        for(; i <= maxTriesLocationSelection + 1 && resultSetSize < minResultSetSize; i++){

            double systemSpeed = maxSystemSpeed * incFactor * i;

            for(TPS_TrafficAnalysisZone taz : trafficAnalysisZones.getTrafficZones()){

                double totalTravelDurationToStay = tourContext.getCumulativeTravelDurationToStay(stay);
                double remainingTravelDurationFromStay = tourContext.getRemainingTravelDurationAfterStay(stay);
                double activityDistance = distanceProvider.getDistance(modeForDistance, taz, toTaz);
                activityDistance += distanceProvider.getDistance(modeForDistance, fromTaz, taz);

                if(skipTrafficAnalysisZone(taz, activityCode, boundVehicle)
                        || isDistanceConstrained(systemSpeed, totalTravelDurationToStay, remainingTravelDurationFromStay, activityDistance)
                ){
                    continue;
                }

                Collection<TPS_Location> locations = generateLocationRepresentatives(taz, activityCode);

                double sumWeight = calculateSumWeight(locations);
                // now weight the total weight of the cell by the normalized weight of this location
                // in the set of representatives
                double tazWeight = taz.getActivityWeightSum(activityCode);

                for (TPS_Location loc : locations) {
                    resultSet.addWeightedLocation(new WeightedLocation(loc, tazWeight * loc.getData().getWeight() / sumWeight));
                    resultSetSize++;
                }
            }
        }
        return resultSet;
    }


    /**
     * Determines whether to skip a specific Traffic Analysis Zone based on the given parameters.
     *
     * @param taz      the Traffic Analysis Zone to check
     * @param activity the activity constant representing the type of activity
     * @param vehicle  the vehicle to check for restrictions
     * @return true if the Traffic Analysis Zone should be skipped, that is, in case a Traffic Analysis Zone does not
     * contain a location for the specified activity or the weight for that activity is less than 0 or the specified
     * vehicle is not allowed to enter the zone.
     */
    public boolean skipTrafficAnalysisZone(TPS_TrafficAnalysisZone taz, TPS_ActivityConstant activity, Vehicle vehicle){

        return !taz.allowsActivity(activity) || // no activities of this type
                taz.getActivityWeightSum(activity) <= 0 ||
                vehicle.isRestricted() && taz.isRestricted();
    }

    /**
     * Generate location representatives for a given Traffic Analysis Zone and activity constant.
     *
     * @param taz      the Traffic Analysis Zone for which to generate location representatives
     * @param activity the activity constant representing the type of activity
     * @return a collection of TPS_Location objects representing the location representatives for the given Traffic Analysis Zone and activity
     */
    public Collection<TPS_Location> generateLocationRepresentatives(TPS_TrafficAnalysisZone taz, TPS_ActivityConstant activity){
        return taz.selectActivityLocations(activity, numTazRepresentatives);
    }

    /**
     * Determines whether the distance between two points is greater than a given threshold based on the system speed.
     *
     * @param systemSpeed          the system speed
     * @param comingFromTravelTime the travel time from the starting point to the current point
     * @param goingToTravelTime    the travel time from the current point to the destination point
     * @param distanceThreshold    the distance threshold
     * @return true if the distance between the points is greater than the threshold, false otherwise
     */
    public boolean isDistanceConstrained(double systemSpeed, double comingFromTravelTime, double goingToTravelTime, double distanceThreshold){
        return comingFromTravelTime * systemSpeed + goingToTravelTime * systemSpeed > distanceThreshold;
    }

    public double calculateSumWeight(Collection<TPS_Location> locations){
        double sumWeight = 0;
        for (TPS_Location location : locations) {
            sumWeight += location.getData().getWeight();
        }

        return sumWeight;
    }
}
