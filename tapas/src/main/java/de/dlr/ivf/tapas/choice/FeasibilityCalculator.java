package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.model.Timeline;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.plan.TPS_AdaptedEpisode;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.scheme.TPS_Episode;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemePart;
import de.dlr.ivf.tapas.model.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FeasibilityCalculator {

    private final int timeSecSlot;
    private final boolean hhSortingCostOptimum;

    public FeasibilityCalculator(TPS_ParameterClass parameterClass){
        this.timeSecSlot = parameterClass.getIntValue(ParamValue.SEC_TIME_SLOT);
        this.hhSortingCostOptimum = parameterClass.isDefined(ParamString.HOUSEHOLD_MEMBERSORTING) &&
                parameterClass.getString(ParamString.HOUSEHOLD_MEMBERSORTING).equalsIgnoreCase(
                        TPS_Household.Sorting.COST_OPTIMUM.name());

    }

    public boolean calcPlanFeasibility(TPS_Plan plan){
        boolean feasible = true;
        Timeline tl = new Timeline();
        int start = 0, end = 0;

        //sort episodes according original start time, get the adapted episode and filter by planned trips
        Collection<TPS_AdaptedEpisode> sortedEpisodes = StreamSupport.stream(plan.getScheme().getEpisodeIterator().spliterator(), false)
                .sorted(Comparator.comparingInt(TPS_Episode::getOriginalStart))
                .map(plan::getAdaptedEpisode)
                .filter(TPS_AdaptedEpisode::isPlannedTrip)
                .collect(Collectors.toCollection(ArrayList::new));

        //insert episodes in the timeline
        double dist = 0;
        for (TPS_AdaptedEpisode e : sortedEpisodes) {

            dist += e.getDistance();
            //set minimum duration!
            if (e.getDuration() < this.timeSecSlot)
                e.setDuration(this.timeSecSlot);

            start = e.getStart();

            end = e.getDuration() + e.getStart();

            //if(start<0||end<0){ //quick bugfix for negative start times
            //	feasible = false;
            //	break;
            //}
            if (!tl.add(start, end)) {
                feasible = false;
                break;
            }
        }

        boolean bookCar = feasible;

        if (bookCar && hhSortingCostOptimum) {
            bookCar = false;
        }

        //now we check if the cars are used and if they have enough range to fullfill the plan
        if (bookCar) {
            Map<Vehicle, Double> cars = new HashMap<>();
            for (TPS_SchemePart schemePart : plan.getScheme()) { //collect car specific distances
                if (schemePart.isTourPart()) {
                    Vehicle car = ((TPS_TourPart) schemePart).getCar();
                    if (car != null) { //trip with car
                        dist = 0;
                        if (cars.containsKey(car)) {
                            dist = cars.get(car);
                        }
                        cars.put(car, dist + ((TPS_TourPart) schemePart).getTourpartDistance());
                    }
                }
            }
            //now check every used car, if it has enough range left
            for (Map.Entry<Vehicle, Double> e : cars.entrySet()) {
                if (e.getKey().getRangeLeft() < e.getValue()) { // not enough?
                    feasible = false;
                    break;
                }
            }
        }
        return feasible;
    }
}
