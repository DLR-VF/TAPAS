/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.person;

import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.plan.TPS_AdaptedEpisode;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.scheme.TPS_Scheme;
import de.dlr.ivf.tapas.scheme.TPS_SchemePart;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

/**
 * A TPS_Worker proceeds all available households and searches plans for all household members. after a plan is
 * found the location occupancies are updated and the trips are stored. <br>
 * <br>
 * It is possible to create more workers. Then they are synchronised over all available households so no household is
 * proceeded twice.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_Worker implements Callable<Exception> {
    TPS_PreferenceParameters prefParams = new TPS_PreferenceParameters();
    /**
     * This is a list of single instances of the preference Model.
     * The list contains one instance per person. Creating new instances during the person read process results in too much needed memory,
     * Its allocated here, because the TPS_Worker is a single thread and I don't have to think about synchronization here.
     */
    List<TPS_Preference> preferenceModels = new ArrayList<>();
    //  The reference to the persistence manager
    private TPS_PersistenceManager PM = null;

    private boolean should_finish = false;

    private final String name;

    /**
     * Standard constructor.
     *
     * @param pm a reference to the persistence manager to be able to read some data
     */
    public TPS_Worker(TPS_PersistenceManager pm, String name) {
        this.PM = pm;
        this.prefParams.readParams();
        this.name = name;
    }

    /**
     * This method generates all permutations of a list of given elements with a fixed length
     * e.g. getAllLists({0;1},2}
     * returns
     * {[00];[01];[10];[11]}
     *
     * @param elements       the elements to permute
     * @param lengthOfVector the length of the output lists
     * @return a long list (elements^lengthOfList) containing all permutations
     */

    public static List<Integer[]> getAllLists(List<Integer> elements, int lengthOfVector) {
        List<Integer[]> allList = new LinkedList<>();
        if (lengthOfVector < 0) { //no permutations
            System.err.println(
                    "Du willst eine Permutation mit einer Länge von " + lengthOfVector + "! Das geht nicht!");
            return allList;
        }
        if (lengthOfVector == 0) { //no permutations
            return allList;
        } else if (lengthOfVector == 1) {        //lists of length 1 are just the list of elements
            for (Integer i : elements) {
                Integer[] val = new Integer[lengthOfVector];
                val[0] = i;
                allList.add(val);
            }
        } else {
            //the recursion--get all lists of length 3, length 2, all the way up to 1
            List<Integer[]> allSublists = getAllLists(elements, lengthOfVector - 1);
            //append the sublists to each element
            for (Integer i : elements) {
                //add the newly appended combination to the list
                for (Integer[] j : allSublists) {
                    Integer[] val = new Integer[lengthOfVector];
                    val[0] = i;
                    //copy the remainder elements
                    System.arraycopy(j, 0, val, 1, lengthOfVector - 1);
                    allList.add(val);
                }
            }
        }
        return allList;
    }

    /**
     * Function to assign a car to a given plan. The car can not be used during this period of time by someone else.
     *
     * @param plan
     */
    private void assignCarToPlan(TPS_Plan plan) {
        // pick cars used for this plan
        for (TPS_SchemePart schemePart : plan.getScheme()) {
            if (!schemePart.isHomePart()) { // are we leaving home?
                TPS_TourPart tourpart = (TPS_TourPart) schemePart;
                if (tourpart.getCar() != null) { // do we use a car?
                    TPS_AdaptedEpisode startEpisode = (plan.getAdaptedEpisode(tourpart.getFirstEpisode()));
                    TPS_AdaptedEpisode endEpisode = (plan.getAdaptedEpisode(tourpart.getLastEpisode()));
                    tourpart.getCar().pickCar(startEpisode.getStart(), endEpisode.getEnd(),
                            tourpart.getTourpartDistance(), plan.mustPayToll || tourpart.getCar().hasPaidToll); // pick
                    // car;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    public Exception call() {
        // run iterates over the households
        TPS_Household hh = null;
        try {
            while (!this.should_finish && (hh = PM.getNextHousehold()) != null) {
                if (TPS_Logger.isLogging(HierarchyLogLevel.HOUSEHOLD, SeverenceLogLevel.DEBUG)) {
                    TPS_Logger.log(HierarchyLogLevel.HOUSEHOLD, SeverenceLogLevel.DEBUG,
                            "Working on household: " + hh.toString());
                }
                processHousehold(hh);
                PM.returnHousehold(hh);
            }

            if(should_finish){
                TPS_Logger.log(getClass(),SeverenceLogLevel.INFO,name+" has finished remaining work, shutting down...");
            }
        } catch (Exception e) {
            TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverenceLogLevel.ERROR, e.getMessage(), e);
            return e;
        }
        return null;
    }

    /**
     * This method tries to find a suitable plan for the given person. First of all a scheme is selected. Then the
     * locations and travel times are selected. When the plan is complete then it is rated and when the rate is good
     * enough it is accepted. When it is not accepted new locations are searched for the same scheme. After a specific
     * number of unsuitable tries for this scheme the scheme is discharged and newly selected. If there is no suitable
     * plan after a specific number of tries found a warning is logged and the method stops. <br>
     * <br>
     * All plans during the execution are stored in the TPS_PlanEnvironment. This environment is then returned.
     *
     * @return all produced plans stored in the TPS_PlanEnvironment
     */
    private void createPersonPlans(TPS_PlanEnvironment pe, TPS_Car carDummy) {
        long time = System.currentTimeMillis();
        TPS_Household.Sorting sortAlgo = TPS_Household.Sorting.AGE;
        if (this.getParameters().isDefined(ParamString.HOUSEHOLD_MEMBERSORTING)) {
            sortAlgo = TPS_Household.Sorting.valueOf(
                    this.getParameters().getString(ParamString.HOUSEHOLD_MEMBERSORTING));
        }
        TPS_Person person = pe.getPerson();
        boolean isBikeAvailable = person.hasBike();
        boolean finished = false;

        int maxTriesScheme = this.getParameters().getIntValue(ParamValue.MAX_TRIES_SCHEME);
        // loop over different schemes
        for (int schemeIt = 0; !finished && schemeIt < maxTriesScheme; ++schemeIt) {
            // select a scheme randomly
            TPS_Scheme scheme = PM.getSchemesSet().findScheme(person);
            // use the scheme to build a plan and adapt the plan if needed
            TPS_Plan masterplan = new TPS_Plan(person, pe, scheme, this.PM);
            /* @see Manits entry 0002615
             *
             * This hack is necessary, because the current schemes have no university code (411). The hack method
             * changes the code to 411 within the plan's scheme copy.
             *
             * TODO Mantis 0002615: remove or modify this student hack for new schemes. If their exist separate schemes for students
             * remove this hack. If their can exist separate schemes you have to determine here if you have to hack the
             * scheme or not.
             */
            //if (person.isStudent()) {
            //	masterplan.hackStudents();
            //}
            // check different modes for the chosen locations
            boolean finishedPlanSearch = false;
            while (!finishedPlanSearch) {
                TPS_Plan modedPlan = new TPS_Plan(masterplan); // build a copy of the master plan
                TPS_PlanningContext pc = new TPS_PlanningContext(pe, carDummy, isBikeAvailable);
                modedPlan.selectLocationsAndModesAndTravelTimes(
                        pc); // mode choice for all trips and location choice for not fixed locations
                modedPlan.setTravelTimes();
                modedPlan.balanceStarts(); // the sum of travel times and its discrepancy from the travel times reported
                modedPlan.calcPlanFeasiblity();
                pe.addPlan(modedPlan);
                if (modedPlan.isPlanAccepted()) {
                    finished = true; //we found a good plan!
                }
                if (!pc.needsOtherModeAlternatives(modedPlan)) {
                    finishedPlanSearch = true;
                }
                //Abbruch bei nicht COST_Optimum
                if (!sortAlgo.equals(TPS_Household.Sorting.COST_OPTIMUM)) {
                    finishedPlanSearch = true;
                }
            }
            if (!finished) {
                pe.dismissScheme();
            }
        }
        //
        //System.out.println(pe.getPlans().size() + " plans generated; " + numberOfRejectedPlans + " plans rejected; " + numberOfFeasiblePlans + " plans feasible");
        boolean accepted = finished;
        if (TPS_Logger.isLogging(this.getClass(), SeverenceLogLevel.DEBUG)) {
            time = System.currentTimeMillis() - time;
            if (!accepted) {
                TPS_Logger.log(SeverenceLogLevel.DEBUG,
                        "No acceptable plan was found in " + pe.getNumberOfRejectedPlans() +
                                " rounds ( feasible Plans: " + pe.getNumberOfFeasiblePlans() + " )in " + time + "ms");
            } else {
                TPS_Logger.log(SeverenceLogLevel.DEBUG,
                        "Acceptable plan was found in round: " + pe.getNumberOfRejectedPlans() + "( feasible Plans: " +
                                pe.getNumberOfFeasiblePlans() + " ) in " + time + "ms");
            }
        }
    }

    public TPS_ParameterClass getParameters() {
        return this.PM.getParameters();
    }

    /**
     * Builds plans for all members of the given household
     *
     * @param hh The household to process
     */
    private void processHousehold(TPS_Household hh) {

        //create new instances of the preference models
        //if we found a household with more members than the current maximum
        while (hh.getNumberOfMembers() > preferenceModels.size()) {
            preferenceModels.add(new TPS_Preference());
        }

        //calculate the shopping motives or set the default values
        int number = 0;
        for (TPS_Person person : hh.getMembers(TPS_Household.Sorting.AGE)) {
            person.preferenceValues = this.preferenceModels.get(number);
            number++; //we advance to the next number
            if (this.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES)) {
                //calc the prefrence for shopping choice
                person.preferenceValues.computePreferences(prefParams, person);
            }
        }
        TPS_Household.Sorting sortAlgo = TPS_Household.Sorting.AGE;
        if (this.getParameters().isDefined(ParamString.HOUSEHOLD_MEMBERSORTING)) {
            sortAlgo = TPS_Household.Sorting.valueOf(
                    this.getParameters().getString(ParamString.HOUSEHOLD_MEMBERSORTING));
        }


        if (!sortAlgo.equals(TPS_Household.Sorting.COST_OPTIMUM)) {
            for (TPS_Person person : hh.getMembers(sortAlgo)) {


                if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG)) {
                    TPS_Logger.log(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG, "Working on person: " + person);
                }
                if (person.isChild()) {
                    if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG,
                                "Person is skipped because it is a child");
                    }
                    continue;
                }

                // check if age adaptation should occur
                if (this.getParameters().isTrue(ParamFlag.FLAG_REJUVENATE_RETIREE)) {
                    if (person.getAge() >= this.getParameters().getIntValue(ParamValue.REJUVENATE_AGE)) {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG)) {
                            TPS_Logger.log(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG,
                                    "Person's age gets adapted");
                        }
                        person.setAgeAdaption(true,
                                this.getParameters().getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS));
                    }
                }

                TPS_PlanEnvironment pe = new TPS_PlanEnvironment(person);
                createPersonPlans(pe, null);
                TPS_Plan plan = pe.getBestPlan();
                ((TPS_DB_IOManager)PM).addToAllPlans(plan);
                //PM.writePlan(plan);//todo remove later
                // reset the age adaption of the retirees
                person.setAgeAdaption(false, this.getParameters().getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS));
            }
        } else {
            // determine who may drive a car and how many cars exist
            int numCarsInHH = hh.getCarNumber();
            Vector<TPS_Person> competingCarDrivers = new Vector<>();
            for (TPS_Person person : hh.getMembers(sortAlgo)) {
                if (person.mayDriveACar()) {
                    competingCarDrivers.add(person);
                }
            }
            // we may have some persons who may drive a car, but not enough cars
            boolean carAssignmentNecessary = false;
            if (numCarsInHH < competingCarDrivers.size()) {
                // we may do here some more magic - determining whether the car is bound to a specific person, e.g.
                // by now we do not, we simply say that an assignment is necessary if more drivers than cars exist
                carAssignmentNecessary = true;
            }
            if (competingCarDrivers.size() <= 1) {
                carAssignmentNecessary = false;
                competingCarDrivers.clear();
            }
            // check car
            TPS_Car leastLimitedCar = null;
            if (hh.getCarNumber() > 0) {
                leastLimitedCar = hh.getCar(0); // TODO: get the car that poses the least limitations
            }
            // allocate plan storages
            HashMap<TPS_Person, TPS_PlanEnvironment> driverPlanEnvironments = new HashMap<>();
            HashMap<TPS_Person, TPS_Plan> bestPlans = new HashMap<>();
            // loop over the persons
            for (TPS_Person person : hh.getMembers(sortAlgo)) {
                if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG)) {
                    TPS_Logger.log(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG, "Working on person: " + person);
                }
                // skip children
                if (person.isChild()) {
                    if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG,
                                "Person is skipped because it is a child");
                    }
                    continue;
                }
                // check if age adaptation should be done
                if (this.getParameters().isTrue(ParamFlag.FLAG_REJUVENATE_RETIREE)) {
                    if (person.getAge() >= this.getParameters().getIntValue(ParamValue.REJUVENATE_AGE)) {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG)) {
                            TPS_Logger.log(HierarchyLogLevel.PERSON, SeverenceLogLevel.DEBUG,
                                    "Person's age gets adapted");
                        }
                        person.setAgeAdaption(true,
                                this.getParameters().getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS));
                    }
                }
                // check car
                TPS_Car carDummy = null;
                if (person.mayDriveACar()) {
                    carDummy = leastLimitedCar;
                }

                // build plan environment for the person
                TPS_PlanEnvironment pe = new TPS_PlanEnvironment(person);
                createPersonPlans(pe, carDummy);
                TPS_Plan bestPlan = pe.getBestPlan();
                if (!carAssignmentNecessary || !competingCarDrivers.contains(person) || !bestPlan.usesCar()) {
                    // the person has a plan without the need to interrogate on the car usage with other persons
                    bestPlans.put(person, bestPlan);
                } else {
                    driverPlanEnvironments.put(person, pe);
                }
                // reset the age adaption of the retirees
                person.setAgeAdaption(false, this.getParameters().getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS));
            }

            if (driverPlanEnvironments.size() == 1) {
                TPS_Person p = driverPlanEnvironments.keySet().iterator().next();
                bestPlans.put(p, driverPlanEnvironments.get(p).getBestPlan());
            } else if (driverPlanEnvironments.size() > 1) {
                /*
                 * ok we have a bunch of plan environments containing the competing drivers
                 * each person has at least two and at max three plans:
                 * 	one with a car (which is "the best" one!)
                 * 	one without fixed modes
                 * 	one with a bike (optional)
                 *
                 * Things to do:
                 *
                 * build the Matrix persons*plans
                 * fill it with a score
                 * find the best combination wtr to maxmimum number of cars
                 */
                Map<Integer, TPS_Person> scoreIndexMap = new HashMap<>();
                int maxPlanCount = 0;
                int index = 0;
                for (TPS_Person e : driverPlanEnvironments.keySet()) {
                    scoreIndexMap.put(index, e);
                    TPS_PlanEnvironment pe = driverPlanEnvironments.get(e);
                    maxPlanCount = Math.max(maxPlanCount, pe.getPlans().size());

                }
                double[][] scoreArray = new double[driverPlanEnvironments.size()][maxPlanCount];
                // calc permutations table
                for (TPS_Person e : driverPlanEnvironments.keySet()) {
                    //init score array with negative scores-> no plan here!
                    for (int i = 0; i < maxPlanCount; ++i) {
                        scoreArray[index][i] = -1;
                    }
                    TPS_PlanEnvironment pe = driverPlanEnvironments.get(e);
                    for (int i = 0; i < pe.getPlans().size(); ++i) {
                        TPS_Plan p = pe.getPlans().get(i);
                        if (p.usesCar()) {
                            scoreArray[index][i] = p.getAcceptanceProbability();
                        } else if (p.usesBike) {
                            scoreArray[index][i] = p.getAcceptanceProbability();
                        } else {
                            scoreArray[index][i] = p.getAcceptanceProbability();
                        }
                    }
                }

                //ok now make a sorted list with all permutations and its score
                List<Integer> e = new ArrayList<>();
                //possible values
                for (int i = 0; i < maxPlanCount; ++i) {
                    e.add(i);
                }

                List<Integer[]> permutationsl = getAllLists(e, driverPlanEnvironments.size());
                //hooray, now i have to find the best possible solution!
                double bestScore = Double.NEGATIVE_INFINITY, tmpScore, numOfCarPlans;
                Integer[] solution = new Integer[0];
                for (Integer[] vector : permutationsl) {
                    //see if the plans are filled:  non existing plans have a index of -1!
                    for (Integer integer : vector) {
                        if (integer < 0) {
                            continue; //TODO is this continue meant to be for the outer for loop?
                        }
                    }
                    //first sum up the scores to see if it is a candidate
                    tmpScore = 0;
                    for (int i = 0; i < vector.length; ++i) {
                        tmpScore += scoreArray[i][vector[i]];
                    }
                    if (tmpScore > bestScore) { //is the score better than the currently best one?
                        /*
                         * TODO: these checks do not look
                         * 	if a car is used twice at different times during the day in different plans!
                         * doing all these checks needs a lot of tricky methods and accurate choice modelling
                         * since all these "strange situations" occur only in very low frequencies,
                         * we just forbid those complicated plans
                         */
                        numOfCarPlans = 0;
                        for (Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
                            TPS_PlanEnvironment pe = driverPlanEnvironments.get(entry.getValue());
                            if (pe.getPlans().get(vector[entry.getKey()]).usesCar()) {
                                numOfCarPlans++;
                            }
                        }

                        //see if the set of plans is feasible
                        if (numOfCarPlans > numCarsInHH) { // too much cars used?
                            continue; //too many cars! -> next set of plans
                        }

                        //count number of restricted cars
                        int numRestrictedCars = 0;
                        for (TPS_Car car : hh.getAllCars()) {
                            if (car.isRestricted()) {
                                numRestrictedCars++;
                            }
                        }
                        //count num of restricted plans
                        int numRestrictedPlans = 0;
                        for (Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
                            TPS_PlanEnvironment pe = driverPlanEnvironments.get(entry.getValue());
                            if (pe.getPlans().get(vector[entry.getKey()]).entersRestrictedAreas()) {
                                numRestrictedPlans++;
                            }
                        }
                        if (numRestrictedCars > numRestrictedPlans) { //do we have sufficient unrestricted cars?
                            continue; //no!
                        }
                        //ok : set the cars
                        //first copy a list of cars so that we can safetly remove them when they are "used up"
                        List<TPS_Car> localCarList = new ArrayList<>(Arrays.asList(hh.getAllCars()));
                        //now put all the plans (with cars) to work on in a 2nd list
                        List<TPS_Plan> localPlanList = new ArrayList<>();
                        for (Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
                            TPS_PlanEnvironment pe = driverPlanEnvironments.get(entry.getValue());
                            TPS_Plan tmp = pe.getPlans().get(vector[entry.getKey()]);
                            if (tmp.usesCar()) {
                                localPlanList.add(tmp);
                            }
                        }
                        //safety check
                        if (localPlanList.size() > localCarList.size()) {
                            continue;
                        }

                        //put the restricted Plans to front
                        localPlanList.sort((arg0, arg1) -> arg0.entersRestrictedAreas() ? -1 : 1);

                        //now assign the cars
                        for (TPS_Plan actPlan : localPlanList) {
                            TPS_Car tmpCar = null;
                            int indexOfCar = 0;
                            //find an unrestricted car for a restrited plan. restricted plans come first!
                            if (actPlan.entersRestrictedAreas()) {
                                for (int i = 0; i < localCarList.size(); i++) {
                                    if (!localCarList.get(i).isRestricted()) { //not restricted?
                                        indexOfCar = i; //take it!
                                        break;
                                    }
                                }
                            }
                            tmpCar = localCarList.get(indexOfCar);
                            localCarList.remove(indexOfCar); //this car is used up!
                            for (TPS_SchemePart schemePart : actPlan.getScheme().getSchemeParts()) {
                                if (schemePart.isTourPart()) {
                                    ((TPS_TourPart) schemePart).setCar(tmpCar);
                                }
                            }
                        }
                        //DONE!
                        solution = vector;
                        bestScore = tmpScore;
                    }
                }

                if (solution.length > 0) {
                    for (Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
                        TPS_PlanEnvironment pe = driverPlanEnvironments.get(entry.getValue());
                        TPS_Plan tmp = pe.getPlans().get(solution[entry.getKey()]);
                        bestPlans.put(entry.getValue(), tmp);
                    }
                }
            }
            // write obtained results
            for (TPS_Person person : bestPlans.keySet()) {
                TPS_Plan plan = bestPlans.get(person);
                this.assignCarToPlan(plan); //TODO: isn't it too late here?
                ((TPS_DB_IOManager)PM).addToAllPlans(plan);
                //PM.writePlan(plan);//todo remove later
            }
        }
    }


    public void finish() {
        TPS_Logger.log(getClass(),SeverenceLogLevel.INFO,name+" finishing remaining work...");
        this.should_finish = true;
    }
}