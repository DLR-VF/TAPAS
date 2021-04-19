package de.dlr.ivf.tapas.scheme;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_AdaptedEpisode;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.*;

/**
 * This class represents a tour part. It is characterised by one trip at the beginning and at the end and all stays between
 * are out of home. The shortest tour consists of one trip (e.g. go with the dog). All stays in a tour are prioritised and
 * connected by their priorities.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_TourPart extends TPS_SchemePart{

    public boolean hasWorkActivity = false;
    public boolean hasEducationActivity = false;
    public boolean hasShoppingActivity = false;
    public boolean hasLeisureActivity = false;
    public boolean hasErrantActivity = false;
    /**
     * In this map are all complete travel durations for each stay in this tour part stored
     */
    private final Map<TPS_Stay, TravelDurations> travelDurationsMap;
    /**
     * Map with all connections of the stays to a previous and a next stay
     */
    private final Map<TPS_Stay, TPS_StayHierarchy> hierachyMap;
    /**
     * Automatically sorted set with all stays by their priority
     */
    private final SortedSet<TPS_Stay> prioritySet;
    /**
     * The car used with this plan
     */
    private TPS_Car car = null;
    // local variable to store the last used mode
    private TPS_ExtMode lastMode = null;
    /*
     * Variable to store the distances in this tour part updated by the function update
     */
    private double tourPartDistance;

    /**
     * This constructor builds a new tourpart with the given id. It instantiates all Collections which are used in this
     * instance.
     *
     * @param id
     */
    public TPS_TourPart(int id) {
        super(id);
        this.travelDurationsMap = new HashMap<>();
        this.hierachyMap = new HashMap<>();
        this.prioritySet = new TreeSet<>();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_SchemePart#addStay(de.dlr.ivf.tapas.plan.TPS_Stay)
     */
    @Override
    public boolean addStay(TPS_Stay stay, TPS_ParameterClass parameterClass) {
        boolean returnVal = super.addStay(stay, parameterClass);

		/*activity codes
		// 0: private matters
		// 1: work
		// 2: school
		// 3: shopping
		// 4: private matters
		// 5: free time
		// 6: other
		// 7: university
		*/
        int activityCodeTapas = stay.getActCode().getCode(TPS_ActivityCodeType.TAPAS);

        switch (activityCodeTapas) {
            case 1:
                this.hasWorkActivity = true;
                break;
            case 2:
                this.hasEducationActivity = true;
                break;
            case 3:
                this.hasShoppingActivity = true;
                break;
            case 4:
                this.hasErrantActivity = true;
                break;
            case 5:
                this.hasLeisureActivity = true;
                break;
            case 6:
                //nothing!
                break;
            case 7:
                this.hasEducationActivity = true;
                break;
        }

        if (returnVal) {
            int earliestStartOfStay = stay.getOriginalStart();
            int latestEndOfStay = (int) stay.getOriginalEnd();
            if (earliestStartOfStay < this.originalSchemePartStart) {
                this.originalSchemePartStart = earliestStartOfStay;
            }

            if (latestEndOfStay > this.originalSchemePartEnd) {
                this.originalSchemePartEnd = latestEndOfStay;
            }

            this.prioritySet.add(stay);

            TravelDurations td = new TravelDurations();
            //TODO: big bug? shouldnt this be the duration of the last trip
            //td.arrivalDuration = this.arrivalDurationSum;
            if (this.getPreviousTrip(stay) != null) td.arrivalDuration = this.getPreviousTrip(stay)
                                                                             .getOriginalDuration();
            else td.arrivalDuration = parameterClass.getIntValue(ParamValue.PT_MINIMUM_TT);
            td.departureDuration = parameterClass.getIntValue(ParamValue.PT_MINIMUM_TT);
            this.travelDurationsMap.put(stay, td);
        }

        return returnVal;
    }

    /**
     * This method adds a trip and increments the temporary complete arrival duration.
     *
     * @param trip trip to add
     * @return true if the trip was added, false otherwise
     */
    public boolean addTrip(TPS_Trip trip) {
        boolean returnVal = this.addEpisode(trip);
        if (returnVal) {

            //TODO: big bug? shouldnt this be the duration of the last trip
            //for (TravelDurations td : this.travelDurationsMap.values()) {
            //	td.departureDuration += trip.getOriginalDuration();
            //}
            TravelDurations td = this.getTravelDurations(this.getPreviousStay(trip));
            if (td != null) td.departureDuration = trip.getOriginalDuration();
        }
        return returnVal;
    }

    /**
     * gets the reference to a car attached to this plan environment
     *
     * @return the car
     */
    public TPS_Car getCar() {
        return this.car;
    }

    /**
     * sets an reference to the used car for this plan environment
     *
     * @param car the reference to an car of this household
     */
    public void setCar(TPS_Car car) {
        this.car = car;
        lastMode = TPS_ExtMode.simpleMIT;
        //this.car.pickCar(this.start, this.end);
    }

    /**
     * Returns the next trip to the episode provided
     *
     * @param episode
     * @return next trip to this episode
     */
    public TPS_Trip getNextTrip(TPS_Episode episode) {
        int index = this.episodes.indexOf(episode);
        for (++index; index < this.episodes.size(); index++) {
            if (this.episodes.get(index).isTrip()) {
                return (TPS_Trip) this.episodes.get(index);
            }
        }
        return null;
    }

    /**
     * Returns the sum of all original durations of all trip episodes of this tour part.
     *
     * @return sum of all original durations of all trips of this tour part
     */
    public double getOriginalTripDuration() {
        double sum = 0;
        for (TPS_Episode e : this.episodes) {
            if (e.isTrip()) {
                sum += e.getOriginalDuration();
            }
        }
        return sum;
    }


    public int getTotalTourPartDurationSeconds(){

        return getLastEpisode().getOriginalEnd() - getFirstEpisode().getOriginalStart();
    }

    /**
     * Returns the previous trip to the episode provided
     *
     * @param episode
     * @return previous trip to this episode
     */
    public TPS_Trip getPreviousTrip(TPS_Episode episode) {
        int index = this.episodes.indexOf(episode);
        for (--index; index >= 0; index--) {
            if (this.episodes.get(index).isTrip()) {
                return (TPS_Trip) this.episodes.get(index);
            }
        }
        return null;
    }

    /**
     * @return Iterator over all stays in this tour part in a priorised order
     */
    public Iterable<TPS_Stay> getPriorisedStayIterable() {
        return this.prioritySet;
    }

    /**
     * @param stay stay to get the hierarchy element
     * @return the previous and next stay to the given one
     */
    public TPS_StayHierarchy getStayHierarchy(TPS_Stay stay) {
        return this.hierachyMap.get(stay);
    }

    /**
     * Method to get the distance traveled in this tour part
     *
     * @return the distance traveled in meters
     */
    public double getTourpartDistance() {
        return this.tourPartDistance;
    }

    /**
     * @param key stay for which the travel durations are retrieved
     * @return travel durations corresponding to the given stay
     */
    public TravelDurations getTravelDurations(TPS_Stay key) {
        return travelDurationsMap.get(key);
    }

    /**
     * This method returns an iterator over all trips of this tour part.
     *
     * @return an instance of {@link TripIterator}
     */
    public Iterable<TPS_Trip> getTripIterator() {
        return new TripIterator();
    }

    /**
     * returns the mode, which was used for the last stay
     *
     * @return the mode
     */
    public TPS_ExtMode getUsedMode() {
        return this.lastMode;
    }

    /**
     * This method is quite simple, because all of the nasty work, like stay-merging has been done in {link TPS_DB_IO.readSchemeSet()}.
     * This method simply puts the hierarchical previous and next stay in a [@link TPS_StayHierarchy} structure.
     * If this stay is the first or the last of the scheme its precessor resp. successor is null
     */
    public void initHierarchy() {


        TPS_Stay prevStay = null, nextStay = null;
        TPS_Stay begin, end;
        SortedSet<TPS_Stay> done = new TreeSet<>();
        if (this.getFirstEpisode().isStay()) {
            begin = (TPS_Stay) this.getFirstEpisode();
        } else {
            begin = this.getPreviousStay(this.getFirstEpisode());
        }
        if (this.getLastEpisode().isStay()) {
            end = (TPS_Stay) this.getLastEpisode();
        } else {
            end = this.getNextStay(this.getLastEpisode());
        }

        for (TPS_Stay stay : this.prioritySet) { // first entry-> highest Prio
            prevStay = begin;
            nextStay = end;
            for (TPS_Stay tmp : done) {

                //higher Prio, older than prev and younger than stay?
                if (tmp.getOriginalStart() >= prevStay.getOriginalStart() &&
                        tmp.getOriginalStart() < stay.getOriginalStart() && tmp != nextStay) {
                    prevStay = tmp;
                }

                //higher Prio, younger than next and older than stay?
                if (tmp.getOriginalStart() <= nextStay.getOriginalStart() &&
                        tmp.getOriginalStart() > stay.getOriginalStart() && tmp != prevStay) {
                    nextStay = tmp;
                }

            }

            TPS_StayHierarchy hierachy = new TPS_StayHierarchy(prevStay, nextStay);
            this.hierachyMap.put(stay, hierachy);
            done.add(stay);
        }

        /**
         * The method is divided into two parts. The first part sets the priorities of all stays of the tour. The second part
         * uses the priorities to build the connection hierarchy of the stays.
         */

//		int priority = 0;
//		for (TPS_Stay stay : this.prioritySet) {
//			priority++;
//			stay.setPriority(priority);
//
//		}
//
//		// Generate the hierarchy using the priorities of the stays
//		TPS_Stay prev, next;
//		// System.out.println(this.getScheme()+"\n"+this);
//		for (TPS_Stay stay : this.getStayIterator()) {
//			// System.out.println(stay);
//			prev = stay;
//			do {
//				prev = this.getPreviousStay(prev);
//			} while (!prev.isAtHome() && prev.getPriority() > stay.getPriority());
//			next = stay;
//			do {
//				next = this.getNextStay(next);
//				// System.out.println(next);
//			} while (!next.isAtHome() && next.getPriority() > stay.getPriority());
//			TPS_StayHierarchy hierachy = new TPS_StayHierarchy(prev, next);
//			this.hierachyMap.put(stay, hierachy);
//		}
    }

    /**
     * @return the bikeUsed
     */
    public boolean isBikeUsed() {
        return lastMode != null && lastMode.isBikeUsed();
    }

    /**
     * checks if a car is attached to this plan environment
     *
     * @return true if a car is attached
     */
    public boolean isCarUsed() {
        return lastMode != null && lastMode.isCarUsed();
    }

    /**
     * Method to determine if this tourpart has to be done with a fixed mode, e.g. bike or car
     *
     * @return true if a fixed mode is used previously
     */
    public boolean isFixedModeUsed() {
        if (lastMode == null) {
            return false;
        } else {
            return lastMode.isFix();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_SchemePart#isHomePart()
     */
    @Override
    public boolean isHomePart() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_SchemePart#isTourPart()
     */
    @Override
    public boolean isTourPart() {
        return true;
    }

    /**
     * releases the used car, e.g. if the plan is not accepted
     *
     * @return success of this action
     */
    public void releaseCar() {
        this.car = null;
        this.lastMode = null;
    }

    /**
     * method to set the initial travel durations to their original values
     *
     * @param parameterClass parameter class reference
     */
    public void setInitialTravelDurations(TPS_ParameterClass parameterClass) {
        int minTime = (int) (parameterClass.getDoubleValue(ParamValue.MIN_DIST) / parameterClass.getDoubleValue(
                TPS_Mode.get(ModeType.WALK).getVelocity()));
        for (TPS_Stay stay : this.travelDurationsMap.keySet()) {
            int arrTime = minTime, depTime = minTime;
            if (this.getPreviousTrip(stay) != null) {
                arrTime = this.getPreviousTrip(stay).getOriginalDuration();
            }
            this.travelDurationsMap.get(stay).arrivalDuration = arrTime;

            if (this.getNextTrip(stay) != null) {
                depTime = this.getNextTrip(stay).getOriginalDuration();
            }
            this.travelDurationsMap.get(stay).departureDuration = depTime;
        }
    }

    /**
     * This method stores the last used departure mode from a stay
     *
     * @param mode The used departure mode
     * @param car  A car, if available.
     *             If the mode is not, "MIT" this information is discarded and the internal reference is still a null pointer.
     */
    public void setUsedMode(TPS_ExtMode mode, TPS_Car car) {
        if (mode == null) {
            this.lastMode = null;
            this.car = null;
        } else {
            if (mode.isCarUsed()) {
                if (car != null) {
                    this.lastMode = mode;
                    this.car = car;
                } else { //should never happen!
                    this.lastMode = null;
                    this.car = null;
                }
            } else {
                this.lastMode = mode;
                this.car = null;
            }
        }
    }

    public TPS_Stay getNextFixStay(TPS_Stay stay){

        return (TPS_Stay) this.getEpisodes()
                              .stream()
                              .filter(episode -> episode.isStay() && stay.getOriginalStart() < episode.getOriginalStart() && episode.getActCode().isFix())
                              .findFirst()
                              .orElseGet(() -> this.getLastEpisode());

    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_SchemePart#toString(java.lang.String)
     */
    @Override
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "TourPart [id=" + this.getId() + "]\n");
        for (TPS_Episode episode : this) {
            sb.append(episode.toString(prefix + " ") + "\n");
        }
        sb.append(prefix + " -PrioritSet [");
        for (TPS_Stay stay : this.prioritySet) {
            sb.append(stay.getId() + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("]\n");
        sb.append(prefix + " -TravelDurations [");
        for (TPS_Stay stay : this.travelDurationsMap.keySet()) {
            sb.append(stay.getId() + "->" + travelDurationsMap.get(stay));
        }
        sb.setLength(sb.length() - 1);
        sb.append("]\n");
        for (TPS_Stay stay : this.hierachyMap.keySet()) {
            sb.append(prefix + " -Hierachy connection: " + this.hierachyMap.get(stay).toString(stay) + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Method to update the travel distances for this plan up to this point
     */
    public void updateActualTravelDistances(TPS_Plan plan) {
        for (TPS_Stay stay : this.getPriorisedStayIterable()) {
            TPS_PlannedTrip arrivalTrip = null;
            TPS_PlannedTrip departureTrip = null;
            int minTime = (int) (plan.getPM().getParameters().getDoubleValue(ParamValue.MIN_DIST) /
                    plan.getPM().getParameters().getDoubleValue(TPS_Mode.get(ModeType.WALK).getVelocity()));
            //get arrival trip
            if (!this.isFirst(stay)) {
                arrivalTrip = plan.getPlannedTrip(this.getPreviousTrip(stay));
            }
            //get departure trip
            if (!this.isLast(stay)) {
                departureTrip = plan.getPlannedTrip(this.getNextTrip(stay));
            }
            if (arrivalTrip.getMode() != null) {
                this.travelDurationsMap.get(stay).arrivalDuration = Math.max(arrivalTrip.getDuration(), minTime);
            }
            if (departureTrip.getMode() != null) {
                this.travelDurationsMap.get(stay).departureDuration = Math.max(departureTrip.getDuration(), minTime);
            }
        }
    }

    /**
     * Method to update the travel durations for this plan up to this point
     *
     * @param plan
     */
    //fixme travel durations and travel distances should be switched and we need to take a deep look into the functionality
    public void updateActualTravelDurations(TPS_Plan plan) {
        this.tourPartDistance = 0;
        for (TPS_Trip trip : this.getTripIterator()) {

            TPS_AdaptedEpisode episode = plan.getAdaptedEpisode(trip);

            //get trip distance
            if (episode != null) {
                this.tourPartDistance += episode.getDistance();
            }
        }
    }


    /**
     * Structure to store the complete arrival and departure duration for one stay in the tour part.<br>
     * <br>
     * Example:<br>
     * episodes t s t s t s t s t<br>
     * durations 10 12 16 90 20 16 10 50 20<br>
     * <p>
     * the complete arrival duration for the second stay is 10 + 16 = 26<br>
     * and the complete departure duration is 20 + 10 + 20 = 50
     *
     * @author mark_ma
     */
    public class TravelDurations {
        /**
         * complete arrival duration
         */
        private double arrivalDuration;

        /**
         * complete departure duration
         */
        private double departureDuration;


        /**
         * @return the arrivalDuration
         */
        public double getArrivalDuration(TPS_ParameterClass parameterClass) {
            return Math.max(arrivalDuration, parameterClass.getDoubleValue(ParamValue.MIN_DIST) /
                    parameterClass.getDoubleValue(TPS_Mode.get(ModeType.WALK).getVelocity()));
        }

        /**
         * @return the departureDuration
         */
        public double getDepartureDuration(TPS_ParameterClass parameterClass) {
            return Math.max(departureDuration, parameterClass.getDoubleValue(ParamValue.MIN_DIST) /
                    parameterClass.getDoubleValue(TPS_Mode.get(ModeType.WALK).getVelocity()));
        }

        /**
         * @param arrivalDuration the arrivalDuration to set
         */
        public void setArrivalDuration(double arrivalDuration) {
            this.arrivalDuration = arrivalDuration;
        }

        /**
         * @param departureDuration the departureDuration to set
         */
        public void setDepartureDuration(double departureDuration) {
            this.departureDuration = departureDuration;
        }


        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "(" + arrivalDuration + ", " + departureDuration + ")";
        }
    }

    /**
     * This class provides a stay hierarchy in this tour part. They are sorted by their duration. Each stay has a link to a
     * stay which is the longest one of all stays which are earlier respectively later.
     *
     * @author mark_ma
     */
    public class TPS_StayHierarchy {

        /**
         * This stay indicates the longest stay of all later stays in this tour part or the first stay at home in the next
         * home part if there exist no later stay in the tour part.
         */
        private final TPS_Stay nextStay;

        /**
         * This stay indicates the longest stay of all previous stays in this tour part or the last stay at home in the
         * previous home part if no earlier stay exist in the tour part.
         */
        private final TPS_Stay prevStay;

        /**
         * Builds an instance and sets both references.
         *
         * @param prev
         * @param next
         */
        public TPS_StayHierarchy(TPS_Stay prev, TPS_Stay next) {
            this.nextStay = next;
            this.prevStay = prev;
        }

        /**
         * Returns the next longer stay in this tour part or the first stay at home in the next home part
         *
         * @return the next stay
         */
        public TPS_Stay getNextStay() {
            return nextStay;
        }

        /**
         * Returns the previous longer stay in this tour part or the last stay at home in the next home part
         *
         * @return the previous stay
         */
        public TPS_Stay getPrevStay() {
            return prevStay;
        }

        /**
         * Builds a String of this hierarchy element
         *
         * @param stay the corresponding stay to this hierarchy element
         * @return string of this hierarchy element
         */
        public String toString(TPS_Stay stay) {
            return prevStay.getId() + "<-" + stay.getId() + "->" + nextStay.getId();
        }
    }

    /**
     * This iterator allows to iterate over all trips of the scheme part by skipping the stays
     *
     * @author mark_ma goeh_da
     */
    public class TripIterator implements Iterator<TPS_Trip>, Iterable<TPS_Trip> {

        /**
         * Internal iterator over all episodes
         */
        private final Iterator<TPS_Episode> it;

        /**
         * next stay which will be returned
         */
        private TPS_Trip trip;

        /**
         * Initializes the next stay which will be returned and the internal iterator.
         */
        public TripIterator() {
            this.it = episodes.iterator();
            this.next();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return this.trip != null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<TPS_Trip> iterator() {
            return this;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#next()
         */
        public TPS_Trip next() {
            TPS_Trip temp = this.trip;
            this.trip = null;
            TPS_Episode e;
            while (this.it.hasNext() && this.trip == null) {
                e = it.next();
                if (e.isTrip()) {
                    this.trip = (TPS_Trip) e;
                }
            }
            return temp;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new RuntimeException("This method is not implemented");
        }

    }
}
