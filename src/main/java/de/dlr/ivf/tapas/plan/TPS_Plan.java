/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.plan;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.constants.TPS_AgeClass.TPS_AgeCodeType;
import de.dlr.ivf.tapas.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.constants.TPS_Income;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.scheme.*;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.util.Timeline;
import de.dlr.ivf.tapas.util.parameters.*;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class for a TAPAS plan.
 * Contains trips, budgets, and many more stuff for plan related work
 *
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_Plan implements ExtendedWritable, Comparable<TPS_Plan> {
    /// The environment for this plan
    public TPS_PlanEnvironment pe;
    public List<TPS_Car> usedCars = new LinkedList<>();
    public boolean usesBike = false;
    public boolean mustPayToll = false;
    HashMap<TPS_Attribute, Integer> myAttributes = new HashMap<>();
    /// Fix (reused) locations
    Map<TPS_ActivityConstant, TPS_Location> fixLocations = new HashMap<>();
    /// The reference to the persistence manager
    private TPS_PersistenceManager PM = null;
    ///
    private double budgetAcceptanceProbability, timeAcceptanceProbability, acceptanceProbability;
    /// Adapted Episode, which is currently under work
    private TPS_AdaptedEpisode currentAdaptedEpisode;
    /// Map of located stays for stays during this plan
    private final Map<TPS_Stay, TPS_LocatedStay> locatedStays;
    /// A map of planned trips so far
    private final Map<TPS_Trip, TPS_PlannedTrip> plannedTrips;
    /// the actual selected scheme
    private final TPS_Scheme scheme;
    /// the feasibility flag
    private boolean feasible = false;
    /// variable for time adaptation analysis
    private int adapt = 0;
    /// variable for time adaptation differenceanalysis
    private int adaptAbsSum = 0;

    /**
     * Constructor for this class
     *
     * @param person   the person the scheme was selected for
     * @param pe
     * @param schemeIn the scheme selected for the person
     * @param pm       reference to the persistence manager for db-access
     */
    public TPS_Plan(TPS_Person person, TPS_PlanEnvironment pe, TPS_Scheme schemeIn, TPS_PersistenceManager pm) {
        this.pe = pe;
        this.PM = pm;

        this.locatedStays = new HashMap<>();
        this.plannedTrips = new TreeMap<>(new Comparator<TPS_Trip>() {
            public int compare(TPS_Trip trip1, TPS_Trip trip2) {
                return trip1.getOriginalStart() - trip2.getOriginalStart();
            }
        });

        this.scheme = schemeIn.clone();

        myAttributes.put(TPS_Attribute.HOUSEHOLD_INCOME_CLASS_CODE,
                TPS_Income.getCode(person.getHousehold().getIncome()));
        myAttributes.put(TPS_Attribute.PERSON_AGE, person.getAge());
        myAttributes.put(TPS_Attribute.PERSON_AGE_CLASS_CODE_PERSON_GROUP,
                person.getPersonGroup().getCode());
        myAttributes.put(TPS_Attribute.PERSON_AGE_CLASS_CODE_STBA, person.getAgeClass().getCode(TPS_AgeCodeType.STBA));
        int code;
        if (person.hasDrivingLicenseInformation()) {
            code = person.getDrivingLicenseInformation().getCode();
        } else {
            if (person.getAge() >= 18) {
                code = TPS_DrivingLicenseInformation.CAR.getCode();
            } else {
                code = TPS_DrivingLicenseInformation.NO_DRIVING_LICENSE.getCode();
            }
        }
        if (code == 0) { //BUGFIX: no driving license is coded in MID2008 as 2!
            code = 2;
        }
        myAttributes.put(TPS_Attribute.PERSON_DRIVING_LICENSE_CODE, code);
        myAttributes.put(TPS_Attribute.CURRENT_TAZ_SETTLEMENT_CODE_TAPAS,
                person.getHousehold().getLocation().getTrafficAnalysisZone().getBbrType()
                      .getCode(TPS_SettlementSystemType.TAPAS));
        myAttributes.put(TPS_Attribute.PERSON_AGE_CLASS_CODE_STBA, person.getAgeClass().getCode(TPS_AgeCodeType.STBA));
        myAttributes.put(TPS_Attribute.PERSON_HAS_BIKE, person.hasBike() ? 1 : 0);
        myAttributes.put(TPS_Attribute.HOUSEHOLD_CARS, person.getHousehold()
                                                             .getNumberOfCars()); // TODO: note that this is set once again in selectLocationsAndModesAndTravelTimes
        myAttributes.put(TPS_Attribute.PERSON_SEX_CLASS_CODE, person.getSex().getCode());

        if (this.scheme != null) {
            for (TPS_Episode e : this.scheme.getEpisodeIterator()) {
                if (e.isStay()) {
                    TPS_Stay stay = (TPS_Stay) e;
                    TPS_LocatedStay locatedStay = new TPS_LocatedStay(this, stay);
                    if (stay.isAtHome()) {
                        locatedStay.setLocation(person.getHousehold().getLocation());
                    }
                    this.locatedStays.put(stay, locatedStay);
                } else {
                    TPS_Trip trip = (TPS_Trip) e;
                    this.plannedTrips.put(trip, new TPS_PlannedTrip(this, trip, this.getPM().getParameters()));
                }
            }
        }
    }


    public TPS_Plan(TPS_Plan src) {
        this.pe = src.pe;
        this.PM = src.PM;
        src.usedCars.addAll(this.usedCars);

        this.locatedStays = new HashMap<>();
        this.plannedTrips = new TreeMap<>(Comparator.comparingInt(TPS_Episode::getOriginalStart));
        // same as(trip1, trip2) -> trip1.getOriginalStart() - trip2.getOriginalStart()

        this.scheme = src.scheme; // TODO: clarify: scheme is not changed after being adapted to the person (student hack or whatever) once
        for (TPS_Attribute attr : src.myAttributes.keySet()) {
            this.myAttributes.put(attr, src.myAttributes.get(attr));
        }
        for (TPS_Stay stay : src.locatedStays.keySet()) {
            TPS_LocatedStay locStay = new TPS_LocatedStay(this, stay);
            this.locatedStays.put(stay, locStay);
            if (src.isLocated(stay)) {
                locStay.setLocation(src.locatedStays.get(stay).getLocation());
            }
        }
        for (TPS_Episode e : this.scheme.getEpisodeIterator()) {
            if (e.isStay()) {
                continue;
            }
            TPS_Trip trip = (TPS_Trip) e;
            this.plannedTrips.put(trip, new TPS_PlannedTrip(this, trip, this.getPM().getParameters()));
        }
    }

    /**
     * Returns whether the given car may be used for this plan
     *
     * @param car The car to use
     * @return Whether the given car may be used
     */
    public boolean allowsCar(TPS_Car car) {
        double dist = 0;
        for (TPS_SchemePart schemePart : this.scheme) { //collect car specific distances
            if (schemePart.isTourPart()) {
                TPS_Car c = ((TPS_TourPart) schemePart).getCar();
                if (c != null) { //trip with car
                    dist += ((TPS_TourPart) schemePart).getTourpartDistance();
                }
            }
        }
        return car.getRangeLeft() > dist;
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
                    TPS_Logger.log(SeverenceLogLevel.WARN,
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
     * This method returns if the selected plan is feasible.
     * If any start of a stay/trip is after the end of the previous trip/stay it returns false.
     * If everythig is ok or there are no stays or trips it returns true.
     *
     * @return result if this plan is feasible
     */
    public void calcPlanFeasiblity() {
        feasible = true;
        Timeline tl = new Timeline();
        int start = 0, end = 0;
        Vector<TPS_Episode> sortedEpisodes = new Vector<>();

        //copy episodes from iterator to vector
        for (TPS_Episode e : this.scheme.getEpisodeIterator()) {
            sortedEpisodes.add(e);
        }

        //sort episodes according original start time
        sortedEpisodes.sort(Comparator.comparingInt(TPS_Episode::getOriginalStart));
        //insert episodes in the timeline
        double dist = 0;
        for (TPS_Episode e : sortedEpisodes) {
            TPS_AdaptedEpisode es = this.getAdaptedEpisode(e);
            if (es.isPlannedTrip()) dist += es.getDistance();
            //set minimum duration!
            if (es.getDuration() < this.PM.getParameters().getIntValue(ParamValue.SEC_TIME_SLOT)) es.setDuration(
                    this.PM.getParameters().getIntValue(ParamValue.SEC_TIME_SLOT));
            start = es.getStart();
            end = es.getDuration() + es.getStart();

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

        if (bookCar && //disable car booking for COST_OPTIMUM sorting
                this.PM.getParameters().isDefined(ParamString.HOUSEHOLD_MEMBERSORTING) &&
                this.PM.getParameters().getString(ParamString.HOUSEHOLD_MEMBERSORTING).equalsIgnoreCase(
                        TPS_Household.Sorting.COST_OPTIMUM.name())) {
            bookCar = false;
        }

        //now we check if the cars are used and if they have enough range to fullfill the plan
        if (bookCar) {
            Map<TPS_Car, Double> cars = new HashMap<>();
            for (TPS_SchemePart schemePart : this.scheme) { //collect car specific distances
                if (schemePart.isTourPart()) {
                    TPS_Car car = ((TPS_TourPart) schemePart).getCar();
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
            for (Entry<TPS_Car, Double> e : cars.entrySet()) {
                if (e.getKey().getRangeLeft() < e.getValue()) { // not enough?
                    feasible = false;
                    break;
                }
            }
        }
    }

    /**
     * compare Plans according to their acceptance probability
     */
    public int compareTo(TPS_Plan o) {
        return this.getAcceptanceProbability() > o.getAcceptanceProbability() ? 1 : -1;
    }

    /**
     * Method to create the output for log-file
     *
     * @return
     */
    public List<List<String>> createOutput() {
        List<List<String>> outList = new ArrayList<>();

        TPS_Stay nextStay, prevStay;
        TPS_PlannedTrip plannedTrip;

        int personID = pe.getPerson().getId();
        int schemeID = scheme.getId();
        int sex = pe.getPerson().getSex().ordinal();
        int mainActivity;
        double hHIncome = pe.getPerson().getHousehold().getIncome();
        int numberOfPersonsHH = pe.getPerson().getHousehold().getNumberOfMembers();

        for (TPS_TourPart tour : this.scheme.getTourPartIterator()) {
            //get highest priority trip
            if (tour.getPriorisedStayIterable().iterator().hasNext())
                mainActivity = tour.getPriorisedStayIterable().iterator().next().getActCode().getCode(
                        TPS_ActivityCodeType.ZBE);
            else mainActivity = -999;
            for (TPS_Trip trip : tour.getTripIterator()) {

                List<String> out = new ArrayList<>();

                plannedTrip = this.getPlannedTrip(trip);

                nextStay = plannedTrip.getTrip().getSchemePart().getNextStay(trip);
                prevStay = plannedTrip.getTrip().getSchemePart().getPreviousStay(trip);

                out.add(Integer.toString(personID)); // PersonID

                out.add(Integer.toString(schemeID)); // SchemeID

                out.add(Integer.toString(pe.getPerson().getPersonGroup().getCode())); // job
                out.add(Integer.toString(mainActivity)); // actCode

                // fahrten haben keine Aktivitätencodes! daher: Aktivitätencode der nächsten location
                int actCode = nextStay.getActCode().getCode(TPS_ActivityCodeType.ZBE);
                // TODO  Mantis 0002615
                if (actCode == 410 && pe.getPerson().isStudent()) actCode = 411;
                out.add(Integer.toString(actCode)); // actCode

                out.add(Integer.toString(pe.getPerson().getAgeClass().getCode(TPS_AgeCodeType.STBA))); // ageCat

                out.add(Integer.toString(sex)); // sex

                // TAZ departure
                out.add(Integer.toString(this.getLocatedStay(prevStay).getLocation().getTrafficAnalysisZone()
                                             .getTAZId()));

                // TAZ arrival
                out.add(Integer.toString(this.getLocatedStay(nextStay).getLocation().getTrafficAnalysisZone()
                                             .getTAZId()));

                // is next stay AtHome
                out.add(Integer.toString(nextStay.isAtHome() ? 1 : 0));

                // id of Mode of Trip
                out.add(Integer.toString(plannedTrip.getMode().getMCTCode()));

                // bee line Distance
                out.add(Integer.toString((int) plannedTrip.getDistanceBeeline()));

                // Distance
                out.add(Double.toString(Math.round(plannedTrip.getDistance())));

                // netDistance
                out.add(Double.toString(Math.round(plannedTrip.getDistanceEmptyNet())));

                // travel time
                out.add(Integer.toString(plannedTrip.getDuration()));

                // HouseHold Income
                out.add(Integer.toString(TPS_Income.getCode(hHIncome)));

                // Number of Persons Household
                out.add(Integer.toString(numberOfPersonsHH));

                // Coming Block ID

                if (this.getLocatedStay(prevStay).getLocation().hasBlock()) {
                    out.add(Integer.toString(this.getLocatedStay(prevStay).getLocation().getBlock().getId()));
                } else {
                    out.add("0");
                }

                // going Block ID
                if (this.getLocatedStay(nextStay).getLocation().hasBlock()) {
                    out.add(Integer.toString(this.getLocatedStay(nextStay).getLocation().getBlock().getId()));
                } else {
                    out.add("0");
                }

                // coming X Coordinate
                out.add(Double.toString(this.getLocatedStay(prevStay).getLocation().getCoordinate().getValue(0)));

                // coming Y Coordinate
                out.add(Double.toString(this.getLocatedStay(prevStay).getLocation().getCoordinate().getValue(1)));

                // going X Coordinate
                out.add(Double.toString(this.getLocatedStay(nextStay).getLocation().getCoordinate().getValue(0)));

                // going Y Coordinate
                out.add(Double.toString(this.getLocatedStay(nextStay).getLocation().getCoordinate().getValue(1)));

                // startTime
                // Die in myStart abgelegte Zeit ist in Sekunden nach mitternacht abgelegt;übersetzten in Minuten nach
                // Mitternacht

                int time = (int) (plannedTrip.getStart() * 1.66666666e-2);
                out.add(Double.toString(time));

                // FOBIRD departure
                out.add(Integer.toString(this.getLocatedStay(prevStay).getLocation().getTrafficAnalysisZone()
                                             .getBbrType().getCode(TPS_SettlementSystemType.FORDCP)));

                // FOBIRD going
                out.add(Integer.toString(this.getLocatedStay(nextStay).getLocation().getTrafficAnalysisZone()
                                             .getBbrType().getCode(TPS_SettlementSystemType.FORDCP)));

                // coming Has Toll
                out.add(Boolean.toString(this.getLocatedStay(prevStay).getLocation().getTrafficAnalysisZone()
                                             .hasToll(SimulationType.SCENARIO)));

                // going Has Toll
                out.add(Boolean.toString(this.getLocatedStay(nextStay).getLocation().getTrafficAnalysisZone()
                                             .hasToll(SimulationType.SCENARIO)));

                // locId
                int locId = Math.max(-1, this.getLocatedStay(nextStay).getLocation().getId());
                out.add(Integer.toString(locId));

                outList.add(out);
            }
        }

        return outList;
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
     * Return the persistence manager
     *
     * @return The persistence manager
     */
    public TPS_PersistenceManager getPM() {
        return PM;
    }

    /**
     * Return the parameter class reference (through the persistence manager)
     *
     * @return parameter class reference
     */
    public TPS_ParameterClass getParameters() {
        return this.PM.getParameters();
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
        return this.pe.isPlanAccepted(this) && this.isPlanFeasible();
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
     * Initiates the determination of locations and modes for stays and trips of the scheme
     */
    public void selectLocationsAndModesAndTravelTimes(TPS_PlanningContext pc) {
        if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(SeverenceLogLevel.DEBUG,
                    "Start select locations procedure for plan (number=" + pe.getNumberOfRejectedPlans() +
                            ") with scheme (id=" + this.scheme.getId() + ")");
        }
        long start = System.currentTimeMillis();

        // We need several loops to resolve the hierarchy of episodes.
        // No we can use the priority of the stays to solve all locations searches in one loop
        for (TPS_SchemePart schemePart : this.scheme) {
            if (schemePart.isHomePart()) {
                // Home Parts are already set
                if (TPS_Logger.isLogging(SeverenceLogLevel.FINE)) {
                    TPS_Logger.log(SeverenceLogLevel.FINE, "Skip home part (id=" + schemePart.getId() + ")");
                }
                continue;
            }

            TPS_TourPart tourpart = (TPS_TourPart) schemePart;
            if (TPS_Logger.isLogging(SeverenceLogLevel.FINE)) {
                TPS_Logger.log(SeverenceLogLevel.FINE,
                        "Start select location for each stay in tour part (id=" + tourpart.getId() + ")");
            }

            // check mobility options
            if (tourpart.isCarUsed()) {
                pc.carForThisPlan = tourpart.getCar();
            } else if (!pc.influenceCarUsageInPlan) {
                //check if a car could be used
                if (this.getPerson().mayDriveACar()) {
                    pc.carForThisPlan = TPS_Car.selectCar(this, tourpart);
                } else {
                    pc.carForThisPlan = null;
                }
            }

            if (tourpart.isBikeUsed()) { //was the bike used before?
                pc.isBikeAvailable = true;
            } else if (!pc.influenceBikeUsageInPlan) { // is the bike availability modded outside?
                pc.isBikeAvailable = this.getPerson().hasBike();
            }
            myAttributes.put(TPS_Attribute.PERSON_HAS_BIKE, pc.isBikeAvailable ? 1 : 0);

            if (pc.carForThisPlan == null) {
                myAttributes.put(TPS_Attribute.HOUSEHOLD_CARS, 0);
            } else {
                myAttributes.put(TPS_Attribute.HOUSEHOLD_CARS, this.getPerson().getHousehold().getNumberOfCars());
            }

            for (TPS_Stay stay : tourpart.getPriorisedStayIterable()) {
                myAttributes.put(TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
                        stay.getActCode().getCode(TPS_ActivityCodeType.TAPAS));

                pc.pe.getPerson().estimateAccessibilityPreference(stay,
                        this.PM.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES));

                TPS_LocatedStay currentLocatedStay = this.getLocatedStay(stay);
                if (!currentLocatedStay.isLocated()) {
                    setCurrentAdaptedEpisode(currentLocatedStay);
                    TPS_ActivityConstant currentActCode = stay.getActCode();
                    // Register locations for activities where the location will be used again.
                    // Flag for the case of a activity with unique location.
                    pc.fixLocationAtBase = currentActCode.isFix() && this.PM.getParameters().isTrue(
                            ParamFlag.FLAG_USE_FIXED_LOCS_ON_BASE) && !this.fixLocations.containsKey(currentActCode);

                    // when all tour parts are correctly instantiated the else case will never happen, because every
                    // tour part starts with a trip. In the current episode file there exist tour parts with no first
                    // trip (e.g. shopping in the same building where you live)
                    //TPS_Trip previousTrip = null;
                    if (!tourpart.isFirst(stay)) {
                        pc.previousTrip = tourpart.getPreviousTrip(stay);
                    } else {
                        pc.previousTrip = new TPS_Trip(-999, TPS_ActivityConstant.DUMMY, -999, 0, this.getParameters());
                    }

                    //First execution: fix locations will be set (ELSE branch)
                    //Further executions: fix locations are set already, take locations from map (IF branch)
                    //Non fix location, i.e. everything except home and work: also ELSE branch
                    //E.g.: Provides a work location, when ActCode is working in ELSE

                    if (currentActCode.isFix() && this.fixLocations.containsKey(currentActCode)) {
                        //now we check if the mode is fix and if we can reach the fix location with the fix mode!
                        //TODO: check only for restricted cars, but bike could also be fixed and no connection!
                        if (pc.carForThisPlan != null && // we have a car
                                pc.carForThisPlan.isRestricted() && //we have a restricted car
                                this.fixLocations.get(currentActCode).getTrafficAnalysisZone()
                                                 .isRestricted()) // we have a restricted car wanting to go to a restricted area! -> BAD!
                        {
                            currentLocatedStay.selectLocation(this, pc);
                            if (currentActCode.isFix()) {
                                this.fixLocations.put(currentActCode, this.getLocatedStay(stay).getLocation());
                            }
                        } else {
                            currentLocatedStay.setLocation(this.fixLocations.get(currentActCode));
                        }

                        if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE, SeverenceLogLevel.FINE)) {
                            TPS_Logger.log(HierarchyLogLevel.EPISODE, SeverenceLogLevel.FINE,
                                    "Set location from fix locations");
                        }
                    } else {
                        currentLocatedStay.selectLocation(this, pc);
                        if (currentActCode.isFix()) {
                            this.fixLocations.put(currentActCode, this.getLocatedStay(stay).getLocation());
                        }
                    }

                    if (currentLocatedStay.getLocation() == null) {
                        TPS_Logger.log(SeverenceLogLevel.ERROR, "No Location found!");
                    }
                    if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                        String s = "gewählte Location zu Stay: " + currentLocatedStay.getEpisode().getId() + ": " +
                                currentLocatedStay.getLocation().getId() + " in TAZ:" +
                                currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() + " in block: " +
                                (currentLocatedStay.getLocation().hasBlock() ? currentLocatedStay.getLocation()
                                                                                                 .getBlock()
                                                                                                 .getId() : -1) +
                                " via" + currentLocatedStay.getModeArr().getName() + "/" +
                                currentLocatedStay.getModeDep().getName();
                        TPS_Logger.log(SeverenceLogLevel.DEBUG, s);
                        TPS_Logger.log(SeverenceLogLevel.DEBUG,
                                "Selected location (id=" + currentLocatedStay.getLocation().getId() +
                                        ") for stay (id=" + currentLocatedStay.getEpisode().getId() + " in TAZ (id=" +
                                        currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() +
                                        ") in block (id= " +
                                        (currentLocatedStay.getLocation().hasBlock() ? currentLocatedStay.getLocation()
                                                                                                         .getBlock()
                                                                                                         .getId() : -1) +
                                        ") via modes " + currentLocatedStay.getModeArr().getName() + "/" +
                                        currentLocatedStay.getModeDep().getName());
                    }
                }
                // fetch previous and next stay
                TPS_Stay prevStay = tourpart.getStayHierarchy(stay).getPrevStay();
                TPS_Stay goingTo = tourpart.getStayHierarchy(stay).getNextStay();
                if (currentLocatedStay.getModeArr() == null || currentLocatedStay.getModeDep() == null) {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.FINE)) {
                        TPS_Logger.log(SeverenceLogLevel.FINE,
                                "Start select mode for each stay in tour part (id=" + tourpart.getId() + ")");
                    }
                    //do we have a fixed mode from the previous trip?
                    if (tourpart.isFixedModeUsed()) {
                        currentLocatedStay.setModeArr(tourpart.getUsedMode());
                        currentLocatedStay.setModeDep(tourpart.getUsedMode());
                    } else {
                        TPS_Location pLocGoingTo = this.getLocatedStay(goingTo).getLocation();
                        TPS_Car tmpCar = pc.carForThisPlan;
                        if (tmpCar != null && tmpCar.isRestricted() &&
                                (currentLocatedStay.getLocation().getTrafficAnalysisZone().isRestricted() ||
                                        pLocGoingTo.getTrafficAnalysisZone().isRestricted())) {
                            pc.carForThisPlan = null;
                        }
                        PM.getModeSet().selectMode(this, prevStay, currentLocatedStay, goingTo, pc);
                        pc.carForThisPlan = tmpCar;
                        //set the mode and car (if used)
                        tourpart.setUsedMode(currentLocatedStay.getModeDep(), pc.carForThisPlan);

                        //set some variables for the fixed modes
                        TPS_ExtMode em = tourpart.getUsedMode();
                        usesBike = em.isBikeUsed();
                        if (em.isCarUsed()) {
                            usedCars.add(pc.carForThisPlan);
                        }

                        //set variables for fixed modes:
                        pc.carForThisPlan = tourpart.getCar();
                        pc.isBikeAvailable = tourpart.isBikeUsed();
                        if (pc.carForThisPlan == null) {
                            myAttributes.put(TPS_Attribute.HOUSEHOLD_CARS, 0);
                        } else {
                            myAttributes.put(TPS_Attribute.HOUSEHOLD_CARS,
                                    this.getPerson().getHousehold().getNumberOfCars());
                        }
                        myAttributes.put(TPS_Attribute.PERSON_HAS_BIKE, pc.isBikeAvailable ? 1 : 0);
                    }
                }
                if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                    String s = "Chosen mode of Stay: " + currentLocatedStay.getEpisode().getId() + ": " +
                            currentLocatedStay.getModeArr() == null ? "NULL" :
                            currentLocatedStay.getModeArr().getName() + " in TAZ:" +
                                    currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() +
                                    " in block: " +
                                    (currentLocatedStay.getLocation().hasBlock() ? currentLocatedStay.getLocation()
                                                                                                     .getBlock()
                                                                                                     .getId() : -1) +
                                    " via" + currentLocatedStay.getModeArr().getName() + "/" +
                                    currentLocatedStay.getModeDep().getName();
                    TPS_Logger.log(SeverenceLogLevel.DEBUG, s);
                    TPS_Logger.log(SeverenceLogLevel.DEBUG,
                            "Selected mode (id=" + currentLocatedStay.getModeArr() == null ? "NULL" :
                                    currentLocatedStay.getModeArr().getName() + ") for stay (id=" +
                                            currentLocatedStay.getEpisode().getId() + " in TAZ (id=" +
                                            currentLocatedStay.getLocation().getTrafficAnalysisZone().getTAZId() +
                                            ") in block (id= " +
                                            (currentLocatedStay.getLocation().hasBlock() ? currentLocatedStay
                                                    .getLocation().getBlock().getId() : -1) + ") via modes " +
                                            currentLocatedStay.getModeArr().getName() + "/" +
                                            currentLocatedStay.getModeDep().getName());
                }

                //set travel time for the arriving mode
                if (!tourpart.isFirst(stay)) {
                    this.getPlannedTrip(tourpart.getPreviousTrip(stay)).setTravelTime(this.getLocatedStay(prevStay),
                            this.getLocatedStay(stay));
                }
                //set travel time for the departure mode
                if (!tourpart.isLast(stay)) {
                    this.getPlannedTrip(tourpart.getNextTrip(stay)).setTravelTime(this.getLocatedStay(stay),
                            this.getLocatedStay(goingTo));
                }
                //update the travel durations for this plan
                tourpart.updateActualTravelDurations(this);
                myAttributes.put(TPS_Attribute.CURRENT_TAZ_SETTLEMENT_CODE_TAPAS,
                        currentLocatedStay.getLocation().getTrafficAnalysisZone().getBbrType()
                                          .getCode(TPS_SettlementSystemType.TAPAS));
            }//end for tourpart
        }

        /* At this point everything should be ready, but:
         * locations within the same location group (malls etc.) should be accessed by foot.
         * You should not drive through a mall with an SUV, unless you play cruel video games.
         *
         * So check, if two adjacent locations are within the same group and adopt the mode accordingly.
         * This is done AFTER mode estimation, this might result in slightly wrong travel times within a mall.
         * TODO: Check if this inaccuracy is acceptable.
         */
        //tourpart.setUsedMode(null, null);

        TPS_LocatedStay prevLocatedStay = null;
        for (TPS_LocatedStay locatedStay : this.getLocatedStays()) {
            if (prevLocatedStay != null) { // not for the first stay, which starts at home
                if (locatedStay.isLocated() && prevLocatedStay.isLocated()) {
                    if (locatedStay.getLocation().isSameLocationGroup(prevLocatedStay.getLocation())) {
                        prevLocatedStay.setModeDep(TPS_ExtMode.simpleWalk);
                        locatedStay.setModeArr(TPS_ExtMode.simpleWalk);
                    }
                } else {
                    if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                        TPS_Logger.log(SeverenceLogLevel.WARN, "One location is null");
                    }
                }
            }
            prevLocatedStay = locatedStay;
        }

        //now we set the final travel times
        for (TPS_SchemePart schemePart : this.scheme) {
            if (schemePart.isTourPart()) {
                TPS_TourPart tourpart = (TPS_TourPart) schemePart;
                //update the travel durations for this plan
                tourpart.updateActualTravelDurations(this);
            }
        }

        TPS_Logger.log(SeverenceLogLevel.DEBUG,
                "Selected all locations in " + (System.currentTimeMillis() - start) + "ms");
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

    /**
     * Method to write this trip to an output-stream
     *
     * @param out
     * @throws IOException
     */
    public void writeOutputStream(Writer out) throws IOException {

        TPS_Stay nextStay, prevStay;
        TPS_PlannedTrip plannedTrip;

        int personID = pe.getPerson().getId();
        int schemeID = scheme.getId();
        int sex = pe.getPerson().getSex().ordinal();
        //int mainActivity;

        double hHIncome = pe.getPerson().getHousehold().getIncome();
        int numberOfPersonsHH = pe.getPerson().getHousehold().getNumberOfMembers();

        for (TPS_TourPart tour : this.scheme.getTourPartIterator()) {
            //get highest priority trip
            //if(tour.getPriorisedStayIterable().iterator().hasNext())
            //	mainActivity = tour.getPriorisedStayIterable().iterator().next().getActCode().getCode(TPS_ActivityCodeType.ZBE);
            //else
            //	mainActivity = -999;
            for (TPS_Trip trip : tour.getTripIterator()) {
                plannedTrip = this.getPlannedTrip(trip);

                nextStay = plannedTrip.getTrip().getSchemePart().getNextStay(trip);
                prevStay = plannedTrip.getTrip().getSchemePart().getPreviousStay(trip);

                out.write(Integer.toString(personID)); // PersonID
                out.write(", ");

                out.write(Integer.toString(schemeID)); // SchemeID
                out.write(", ");

                out.write(Integer.toString(pe.getPerson().getPersonGroup().getCode())); // job
                out.write(", ");

                //hauptaktivität
                //out.write(Integer.toString(mainActivity)); // actCode
                //out.write(", ");
                // aktuelle Priorität
                int actCode = nextStay.getActCode().getCode(TPS_ActivityCodeType.ZBE);
                // TODO  Mantis 0002615
                if (actCode == 410 && pe.getPerson().isStudent()) {
                    actCode = 411;
                }
                out.write(Integer.toString(actCode)); // actCode
                out.write(", ");

                out.write(Integer.toString(pe.getPerson().getAgeClass().getCode(TPS_AgeCodeType.STBA))); // ageCat
                out.write(", ");

                out.write(Integer.toString(sex)); // sex
                out.write(", ");

                // TAZ departure
                out.write(Integer.toString(this.getLocatedStay(prevStay).getLocation().getTrafficAnalysisZone()
                                               .getTAZId()));
                out.write(", ");

                // TAZ arrival
                out.write(Integer.toString(this.getLocatedStay(nextStay).getLocation().getTrafficAnalysisZone()
                                               .getTAZId()));
                out.write(", ");

                // is next stay AtHome
                out.write(Integer.toString(nextStay.isAtHome() ? 1 : 0));
                out.write(", ");

                // id of Mode of Trip
                out.write(Integer.toString(plannedTrip.getMode().getMCTCode()));
                out.write(", ");

                // bee line Distance
                out.write(Integer.toString((int) plannedTrip.getDistanceBeeline()));
                out.write(", ");


                out.write(Double.toString(Math.round(plannedTrip.getDistance())));
                out.write(", ");

                // netDistance

                out.write(Double.toString(Math.round(plannedTrip.getDistanceEmptyNet())));
                out.write(", ");

                // travel time
                out.write(Integer.toString(plannedTrip.getDuration()));
                out.write(", ");

                // HouseHold Income
                out.write(Integer.toString(TPS_Income.getCode(hHIncome)));
                out.write(", ");

                // Number of Persons Household
                out.write(Integer.toString(numberOfPersonsHH));
                out.write(", ");

                // Coming Block ID

                if (this.getLocatedStay(prevStay).getLocation().hasBlock()) {
                    out.write(Integer.toString(this.getLocatedStay(prevStay).getLocation().getBlock().getId()));
                } else {
                    out.write("0");
                }
                out.write(", ");

                // going Block ID
                if (this.getLocatedStay(nextStay).getLocation().hasBlock()) {
                    out.write(Integer.toString(this.getLocatedStay(nextStay).getLocation().getBlock().getId()));
                } else {
                    out.write("0");
                }
                out.write(", ");

                // coming X Coordinate
                out.write(Double.toString(this.getLocatedStay(prevStay).getLocation().getCoordinate().getValue(0)));
                out.write(", ");

                // coming Y Coordinate
                out.write(Double.toString(this.getLocatedStay(prevStay).getLocation().getCoordinate().getValue(1)));
                out.write(", ");

                // going X Coordinate
                out.write(Double.toString(this.getLocatedStay(nextStay).getLocation().getCoordinate().getValue(0)));
                out.write(", ");

                // going Y Coordinate
                out.write(Double.toString(this.getLocatedStay(nextStay).getLocation().getCoordinate().getValue(1)));
                out.write(", ");

                // startTime
                // Die in myStart abgelegte Zeit ist in Sekunden seit mitternacht abgelegt;übersetzten in Minuten nach
                // Mitternacht

                int time = (int) (plannedTrip.getStart() * 1.66666666e-2);
                out.write(Double.toString(time));
                out.write(", ");

                // FOBIRD departure
                out.write(Integer.toString(this.getLocatedStay(prevStay).getLocation().getTrafficAnalysisZone()
                                               .getBbrType().getCode(TPS_SettlementSystemType.FORDCP)));
                out.write(", ");

                // FOBIRD going
                out.write(Integer.toString(this.getLocatedStay(nextStay).getLocation().getTrafficAnalysisZone()
                                               .getBbrType().getCode(TPS_SettlementSystemType.FORDCP)));
                out.write(", ");

                // coming Has Toll
                out.write(Boolean.toString(this.getLocatedStay(prevStay).getLocation().getTrafficAnalysisZone()
                                               .hasToll(SimulationType.SCENARIO)));
                out.write(", ");

                // going Has Toll
                out.write(Boolean.toString(this.getLocatedStay(nextStay).getLocation().getTrafficAnalysisZone()
                                               .hasToll(SimulationType.SCENARIO)));
                out.write(", ");

                // locId
                int locId = Math.max(-1, this.getLocatedStay(nextStay).getLocation().getId());
                out.write(Integer.toString(locId));

                out.write("\n");
            }
        }
        out.flush();
    }

}
