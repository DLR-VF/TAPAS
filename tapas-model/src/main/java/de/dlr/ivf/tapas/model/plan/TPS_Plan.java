/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.plan;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.model.constants.TPS_AgeClass.TPS_AgeCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.constants.TPS_Income;
import de.dlr.ivf.tapas.model.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;

import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;
import de.dlr.ivf.tapas.parameter.CURRENCY;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Class for a TAPAS plan.
 * Contains trips, budgets, and many more stuff for plan related work
 *
 * @author cyga_ri
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_Plan implements Comparable<TPS_Plan> {
    /// Flag for DEBUG-Modus
    private static final boolean DEBUG = false;
    /// The environment for this plan
    public TPS_PlanEnvironment pe;
    public List<Vehicle> usedCars = new LinkedList<>();
    public boolean usesBike = false;
    public boolean mustPayToll = false;
    private final Map<TPS_Attribute, Integer> myAttributes;
    /// Fix (reused) locations
    Map<TPS_ActivityConstant, TPS_Location> fixLocations = new HashMap<>();
    /// The reference to the persistence manager
    ///
    private double budgetAcceptanceProbability, timeAcceptanceProbability, acceptanceProbability;
    /// Adapted Episode, which is currently under work
    private TPS_AdaptedEpisode currentAdaptedEpisode;
    /// Map of located stays for stays during this plan
    private final Map<TPS_Stay, TPS_LocatedStay> locatedStays;
    /// A map of planned trips so far
    private final Map<TPS_Trip, TPS_PlannedTrip> plannedTrips;
    /// the actual selected scheme
    private TPS_Scheme scheme;
    /// the feasibility flag
    private boolean feasible = false;
    /// variable for time adaptation analysis
    private int adapt = 0;
    /// variable for time adaptation differenceanalysis
    private int adaptAbsSum = 0;
    private TPS_PlanningContext pc;

    /**
     * Constructor for this class
     *
     * @param scheme the scheme selected for the person
     */
    public TPS_Plan(TPS_Location homeLocation, TPS_PlanEnvironment pe, TPS_Scheme scheme, Map<TPS_Attribute, Integer> planAttributes) {
        this.pe = pe;

        this.scheme = scheme;
        this.myAttributes = planAttributes;

        this.locatedStays = new HashMap<>();
        this.plannedTrips = new TreeMap<>(Comparator.comparingInt(TPS_Episode::getOriginalStart));

        for (TPS_Episode e : scheme.getEpisodeIterator()) {
            if (e.isStay()) {
                TPS_Stay stay = (TPS_Stay) e;
                TPS_LocatedStay locatedStay = new TPS_LocatedStay(this, stay);
                if (stay.isAtHome()) {
                    locatedStay.setLocation(homeLocation);
                }
                this.locatedStays.put(stay, locatedStay);
            } else {
                TPS_Trip trip = (TPS_Trip) e;
                this.plannedTrips.put(trip, new TPS_PlannedTrip(this, trip));
            }
        }

    }

    /**
     * Function tries to modify the scheme in such way, that the travel times and duration of the stays do not vary too
     * much from the initial times reported. This is done by varying the timing (duration / start / end) of the episodes
     * within the varianze reported for similar schemes
     *
     * @return 0 > error (no adoption), !0 > OK
     */
    public int balanceStarts() {

        int absAdaptation = 0;

        TPS_Episode reference = null, act;
        TPS_AdaptedEpisode actAdapted = null;
        for (TPS_SchemePart schemePart : this.scheme) {
            if (schemePart.isTourPart()) {
                TPS_TourPart tourpart = (TPS_TourPart) schemePart;
                act = tourpart.getPriorisedStayIterable().iterator().next();
                if (reference == null || ((TPS_Stay) reference).compareTo(((TPS_Stay) act)) < 0) {
                    reference = act;
                }
            }
        }
        if (reference != null) {
            //adopt all previous
            act = reference;
            actAdapted = this.getAdaptedEpisode(act);
            do {
                //get estimated start
                int endOfLastEpisode = actAdapted.getStart();
                //go one back
                act = act.getSchemePart().getPreviousEpisode(act);
                if (act != null) {
                    actAdapted = this.getAdaptedEpisode(act);
                    //get duration
                    int duration = actAdapted.getDuration();
                    //statistics
                    this.adapt += actAdapted.getEnd() - endOfLastEpisode;
                    absAdaptation += Math.abs(actAdapted.getEnd() - endOfLastEpisode);
                    this.adaptAbsSum += absAdaptation;
                    //set new start
                    actAdapted.setStart(endOfLastEpisode - duration);
                }
            } while (act != null);

            //adopt allsucceding
            act = reference;
            actAdapted = this.getAdaptedEpisode(act);
            do {
                //get estimated start
                int startOfNextEpisode = actAdapted.getEnd();
                //go one further
                act = act.getSchemePart().getNextEpisode(act);
                if (act != null) {
                    actAdapted = this.getAdaptedEpisode(act);
                    //get duration
                    double start = actAdapted.getStart();
                    //statistics
                    this.adapt += start - startOfNextEpisode;
                    absAdaptation += Math.abs(start - startOfNextEpisode);

                    this.adaptAbsSum += absAdaptation;
                    //set new start
                    actAdapted.setStart(startOfNextEpisode);
                }
            } while (act != null);
        }
        int lastStart = Integer.MIN_VALUE, start;
        TPS_PlannedTrip pt;
        for (TPS_TourPart tp : this.scheme.getTourPartIterator()) {
            for (TPS_Trip t : tp.getTripIterator()) {
                pt = this.getPlannedTrip(t);
                start = pt.getStart();
                if (start <= lastStart) {
                    TPS_Logger.log(SeverityLogLevel.WARN,
                            "Same or earlier start of episode detected! last:" + lastStart + " act:" + start);
                    start = lastStart + 1;
                    pt.setStart(start);
                }
                lastStart = start;
            }
        }

        return absAdaptation;
    }

    /**
     * compare Plans according to their acceptance probability
     */
    public int compareTo(TPS_Plan o) {
        return this.getAcceptanceProbability() > o.getAcceptanceProbability() ? 1 : -1;
    }

    /**
     * Method to look if a trip of this plan enters a restricted area
     *
     * @return true if it enters a restricted area
     */
    public boolean entersRestrictedAreas() {
        for (TPS_LocatedStay stay : locatedStays.values()) {
            if (stay.isLocated() && stay.getLocation().getTrafficAnalysisZone().isRestricted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the general acceptance probability based on the deviation of the travel time compared to the original travel time.
     *
     * @return A value between 0 and 1 describing the acceptance
     */
    public double getAcceptanceProbability() {
        return acceptanceProbability;
    }

    /**
     * Sets the general acceptance probability based on the deviation of the travel time compared to the original travel time.
     *
     * @param acceptanceProbability A value between 0 and 1 describing the acceptance
     */
    public void setAcceptanceProbability(double acceptanceProbability) {
        this.acceptanceProbability = acceptanceProbability;
    }

    /**
     * gets the adapted episode for the given key
     *
     * @param key key for the episode to look for
     * @return null if key is not in the planed trips or located stays
     */
    public TPS_AdaptedEpisode getAdaptedEpisode(TPS_Episode key) {
        if (key.isStay()) {
            return locatedStays.get(key);
        } else {
            return plannedTrips.get(key);
        }
    }

    /**
     * Getter for  a specific attribute value. Throws a runtime exception, if the value is not set!
     *
     * @param att The attribute to look for
     * @return the value or RuntimeException
     */
    public int getAttributeValue(TPS_Attribute att) {
        if (this.myAttributes.containsKey(att)) {
            return this.myAttributes.get(att);
        } else {
            throw new RuntimeException("Attribute " + att.toString() + " not set!");
        }
    }

    /**
     * Getter for the attribute map
     *
     * @return
     */
    public Map<TPS_Attribute, Integer> getAttributes() {
        return this.myAttributes;
    }

    /**
     * Gets the budget acceptance probability based on the deviation of the travel costs compared to the available budget.
     *
     * @return A value between 0 and 1 describing the acceptance
     */
    public double getBudgetAcceptanceProbability() {
        return budgetAcceptanceProbability;
    }

    /**
     * Sets the budget acceptance probability based on the deviation of the travel costs compared to the available budget.
     *
     * @param budgetAcceptanceProbability A value between 0 and 1 describing the acceptance
     */
    public void setBudgetAcceptanceProbability(double budgetAcceptanceProbability) {
        this.budgetAcceptanceProbability = budgetAcceptanceProbability;
    }

    /**
     * Gets the acceptance probability based on the deviation of the travel costs compared to the available budget and the time deviation compared to the time deviation in the scheme class of the plan.
     *
     * @return A value between 0 and 1 describing the acceptance
     */
    public double getCombinedAcceptanceProbability() {
        return timeAcceptanceProbability * budgetAcceptanceProbability;
    }

    /**
     * gets the current episode, which is adapted
     *
     * @return
     */
    public TPS_AdaptedEpisode getCurrentAdaptedEpisode() {
        return currentAdaptedEpisode;
    }

    /**
     * sets the currently adapted episode
     *
     * @param currentAdaptedEpisode
     */
    public void setCurrentAdaptedEpisode(TPS_AdaptedEpisode currentAdaptedEpisode) {
        this.currentAdaptedEpisode = currentAdaptedEpisode;
    }

    /**
     * Gets the previous Stay with respect tho the Hierarchy of the actual schemepart
     *
     * @param adaptedEpisode
     * @return
     */
    public TPS_LocatedStay getHierarchizedPreviousStay(TPS_AdaptedEpisode adaptedEpisode) {
        TPS_TourPart tp = (TPS_TourPart) adaptedEpisode.getEpisode().getSchemePart();
        return this.getLocatedStay(tp.getStayHierarchy((TPS_Stay) adaptedEpisode.getEpisode()).getPrevStay());
    }

    /**
     * gets the located stay for the given key
     *
     * @param key
     * @return null if key is not found in locatedStays or the stay otherwise
     */
    public TPS_LocatedStay getLocatedStay(TPS_Stay key) {
        return locatedStays.get(key);
    }

    /**
     * Getter for a Collection of the located stays for this plan.
     *
     * @return The Collection of TPS_LocatedStay
     */
    public Collection<TPS_LocatedStay> getLocatedStays() {
        return locatedStays.values();
    }


    /**
     * gets the person for this plan from the plan environment
     *
     * @return
     */
    public TPS_Person getPerson() {
        return this.pe.getPerson();
    }

    /**
     * gets the local plan environment
     *
     * @return
     */
    public TPS_PlanEnvironment getPlanEnvironment() {
        return pe;
    }

    /**
     * gets the planed trip for the given key
     *
     * @param key key for the trip to look for
     * @return null if key is not in the planed trips or the trip elsewise
     */
    public TPS_PlannedTrip getPlannedTrip(TPS_Trip key) {
        return plannedTrips.get(key);
    }

    /**
     * Getter for a Collection of the planned trips for this plan.
     *
     * @return The Collection of TPS_PlannedTrip
     */
    public Collection<TPS_PlannedTrip> getPlannedTrips() {
        return plannedTrips.values();
    }

    /**
     * gets the previous stay for the actual adapted Episode
     *
     * @param adaptedEpisode
     * @return
     */
    public TPS_LocatedStay getPreviousLocatedStay(TPS_AdaptedEpisode adaptedEpisode) {
        return this.getLocatedStay(
                adaptedEpisode.getEpisode().getSchemePart().getPreviousStay(adaptedEpisode.getEpisode()));
    }

    /**
     * gets the actual scheme for this plan
     *
     * @return
     */
    public TPS_Scheme getScheme() {
        return scheme;
    }

    /**
     * This method returns the absolute sum of time adaptations to make this plan feasible
     *
     * @return the time adaptation in seconds
     */
    public int getSumOfTimeAdaptation() {
        return this.adaptAbsSum;
    }

    /**
     * Gets the budget acceptance probability based on the deviation of the travel time deviation compared to the time deviation in the scheme class of the plan.
     *
     * @return A value between 0 and 1 describing the acceptance
     */
    public double getTimeAcceptanceProbability() {
        return timeAcceptanceProbability;
    }

    /**
     * Sets the budget acceptance probability based on the deviation of the travel time deviation compared to the time deviation in the scheme class of the plan.
     *
     * @param timeAcceptanceProbability A value between 0 and 1 describing the acceptance
     */
    public void setTimeAcceptanceProbability(double timeAcceptanceProbability) {
        this.timeAcceptanceProbability = timeAcceptanceProbability;
    }

    /**
     * This method returns the time adaptation to make this plan feasible
     *
     * @return the time adaptation in seconds
     */
    public int getTimeAdaptation() {
        return this.adapt;
    }

    /**
     * Calculates the financial expenditures for the modal use in the scheme in Euro
     *
     * @return sum of the travel costs
     */
    public double getTravelCosts() {
        double sumFinancialCosts = 0.0;
        for (TPS_PlannedTrip pt : this.plannedTrips.values()) {
            sumFinancialCosts += pt.getCosts(CURRENCY.EUR);
        }
        return sumFinancialCosts;
    }

    /**
     * Calculates the aggregate travel time of the scheme resulting from the chosen modes and locations.
     *
     * @return sum of travel time of the scheme
     */
    public double getTravelDuration() {
        double sumTravelTime = 0.0;
        for (TPS_PlannedTrip pt : this.plannedTrips.values()) {
            sumTravelTime += pt.getDuration();
        }
        return sumTravelTime;
    }

    /**
     * Change all 410 codes for students to 411
     */
    public void hackStudents() {
        for (TPS_SchemePart part : this.scheme) {
            for (TPS_Stay stay : part.getStayIterator()) {
                if (stay.getActCode().hasAttribute(TPS_ActivityConstantAttribute.SCHOOL)) {
                    stay.setActCode(TPS_ActivityConstant.getActivityCodeByTypeAndCode(TPS_ActivityCodeType.ZBE, 411));
                }
            }
        }
    }

    /**
     * Determines whether a stay has been located
     *
     * @param stay stay to check
     * @return true if a location has been chosen; false else
     */
    public boolean isLocated(TPS_Stay stay) {
        return this.getLocatedStay(stay).isLocated();
    }

    /**
     * Determines whether the travel times resulting from the chosen modes and locations for the plan exceed too much
     * the travel times initially scheduled for the scheme. The acceptance limit is defined in the configuration.
     *
     * @return true, if the plan with its travel times is not too far off the designated travel times; false if the
     * travel times exceed the maximum difference defined in the configuration
     */
    public boolean isPlanAccepted() {
        return this.pe.isPlanAccepted(this);
    }

    /**
     * This method returns if the selected plan is feasible.
     * If any start of a stay/trip is after the end of the previous trip/stay it returns false.
     * If everythig is ok or there are no stays or trips it returns true.
     *
     * @return result if this plan is feasible
     */
    public boolean isPlanFeasible() {
        return feasible;
    }

    /**
     * sets the feasibility of this plan by expert analysis
     *
     * @param feasible the feasibility for this plan
     */
    public void setPlanFeasible(boolean feasible) {
        this.feasible = feasible;
    }

    /**
     * Removes a given Attribute. If it does not exist nothing happens.
     *
     * @param att The Attribute to remove
     */
    public void removeAttribute(TPS_Attribute att) {
        this.myAttributes.remove(att);
    }

    /**
     * Resets a scheme to its initial state; e.g. clears modes and locations selected
     */
    public void reset() {
        TPS_Stay stay;
        TPS_Trip trip;
        TPS_LocatedStay locStay = null;
        this.usesBike = false;
        this.usedCars.clear();

        for (TPS_TourPart tp : this.scheme.getTourPartIterator()) {
            if (tp.isCarUsed()) tp.releaseCar();
        }
        for (TPS_Episode e : this.scheme.getEpisodeIterator()) {
            if (e.isStay()) {
                stay = (TPS_Stay) e;
                locStay = this.getLocatedStay(stay);
                locStay.init();
                if (stay.isAtHome()) {
                    locStay.setLocation(this.getPerson().getHousehold().getLocation());
                }
            } else {
                trip = (TPS_Trip) e;
                this.plannedTrips.get(trip).setMode(null);
                this.plannedTrips.get(trip).setDuration(trip.getOriginalDuration());
            }
        }
    }



    /**
     * Setter for a specific attribute of this plan
     *
     * @param att the Attribute from TPS_Attribute
     * @param val the value
     */
    public void setAttributeValue(TPS_Attribute att, int val) {
        this.myAttributes.put(att, val);
    }

    /**
     * Calls for each trip of the plan the routine setTravelTime von {@link TPS_PlannedTrip}.
     */
    public void setTravelTimes() {
        // If the first Episode is a trip, we cannot say how far it is and how
        // much time was needed

        TPS_Trip trip = null;
        TPS_Stay from = null;
        TPS_Stay to = null;

        for (TPS_SchemePart sp : this.scheme) {
            if (sp.isTourPart()) {
                for (TPS_Episode episode : sp) {
                    /*
                     * Es wird erwartet das an Stelle it ein Trip und an Stelle itComingFrom = it - 1 und an Stelle
                     * itGoingTo = it + 1 ein Stay ist. Es kann abgesichert werden, dass nur trips verarbeitet werden,
                     * bei denen das der Fall ist. Im Input-File ist das aber nicht immer so. Was passiert mit diesen
                     * Trips, für die das nicht der Fall ist?
                     */
                    if (episode.isTrip()) {
                        trip = (TPS_Trip) episode;
                        from = sp.getPreviousStay(episode);
                        to = sp.getNextStay(episode);

                        // TPS_Location locFrom =
                        // this.getLocatedStay(from).getLocation();
                        // TPS_Location locTo =
                        // this.getLocatedStay(to).getLocation();

                        // if (locFrom != null && locTo != null)
                        // LOG.warn("\t\t\t\t '--> Aufruf trip.setTravelTime it: "
                        // + (it + 1) + " from stay: "
                        // + from.getMyNumber() + " loc: " + locFrom.getMyId() +
                        // " in Bez: "
                        // + from.getPLocation().getBez92() + " (" +
                        // from.getPLocation().getRw() + ", "
                        // + from.getPLocation().getHw() + "); to stay: " +
                        // to.getMyNumber() + " loc: "
                        // + locTo.getMyId() + " in Bez: " +
                        // to.getPLocation().getBez92() + " ("
                        // + to.getPLocation().getRw() + ", " +
                        // to.getPLocation().getHw() + ")");
                        // else
                        // LOG.debug("\t\t\t\t '--> Aufruf trip.setTravelTime it: "
                        // + (it + 1) + " from stay: "
                        // + from.getMyNumber() + " (null, null) to stay: " +
                        // to.getMyNumber() + " (null, null)");

                        this.getPlannedTrip(trip).setTravelTime(this.getLocatedStay(from), this.getLocatedStay(to));
                    }
                }
            }
        }
    }

    /**
     * overrides toString to use local toString function
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /**
     * puts a prefix to the toString functionality of this specially formated output.
     * PREFIX is currently disabled!
     */
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName() + "\n");
        sb.append(this.scheme.toString(" ") + "\n");
        for (TPS_Episode e : this.scheme.getEpisodeIterator()) {
            Object ew = null;
            if (e.isStay()) {
                ew = this.getLocatedStay((TPS_Stay) e);
            } else if (e.isTrip()) {
                ew = this.getPlannedTrip((TPS_Trip) e);
            }
            sb.append(" " + e.getId() + " -> " + ew.toString() + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Returns whether a car is used within this plan
     *
     * @return Whether a car is used
     */
    public boolean usesCar() {
        return this.usedCars.size() > 0;
    }

    public void setPlanningContext(TPS_PlanningContext pc){
        this.pc = pc;
    }

    public TPS_PlanningContext getPlanningContext(){
        return this.pc;
    }
    public Map<TPS_ActivityConstant, TPS_Location> getFixLocations(){
        return this.fixLocations;
    }

    public Optional<TPS_TourPart> getNextTourPart(TPS_TourPart tour_part){

        //what is the position of the current tour part in our scheme parts
        int index = this.scheme.getSchemeParts().indexOf(tour_part);

        //check if there actually is one more tour part after the current one
        if(index + 2 < this.scheme.getSchemeParts().size())
            return Optional.of((TPS_TourPart) this.scheme.getSchemeParts().get(index + 2));
        else
            return Optional.empty();



    }
    /*
        returns the next home part stay after this tour part
     */
    public TPS_Stay getNextHomeStay(TPS_TourPart tour_part){
        int tour_part_index = this.scheme.getSchemeParts().indexOf(tour_part);
        TPS_SchemePart sp = null;

        Optional<TPS_SchemePart> optional_home_part = IntStream.range(tour_part_index,this.scheme.getSchemeParts().size())
                                                               .mapToObj(i -> this.scheme.getSchemeParts().get(i))
                                                               .filter(TPS_SchemePart::isHomePart)
                                                               .findFirst();
        if(optional_home_part.isPresent()){
            sp = optional_home_part.get();
        }else { //this case should not happen but if it does we simply return the last home stay
            for (int i = this.scheme.getSchemeParts().size() - 1; i >= 0; i--) {
                if (this.scheme.getSchemeParts().get(i).isHomePart()) {
                    sp = this.scheme.getSchemeParts().get(i);
                    break;
                }
            }
        }
        return (TPS_Stay) sp.getFirstEpisode();
    }


    public TPS_HomePart getHomePartPriorToTourPart(TPS_TourPart tour_part){

        int tour_part_index = this.getScheme().getSchemeParts().indexOf(tour_part);
        return (TPS_HomePart) this.getScheme().getSchemeParts().get(tour_part_index-1);
    }

    public TPS_HomePart getHomePartAfterTourPart(TPS_TourPart tour_part){

        int tour_part_index = this.getScheme().getSchemeParts().indexOf(tour_part);
        return (TPS_HomePart) this.getScheme().getSchemeParts().get(tour_part_index+1);
    }


}
