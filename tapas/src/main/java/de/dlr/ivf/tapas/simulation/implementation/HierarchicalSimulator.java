package de.dlr.ivf.tapas.simulation.implementation;

import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.person.TPS_Preference;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemePart;
import de.dlr.ivf.tapas.model.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.model.vehicle.Vehicle;
import de.dlr.ivf.tapas.simulation.Simulator;

import java.util.*;

public class HierarchicalSimulator implements Simulator<TPS_Household, TPS_Household> {
    @Override
    public TPS_Household process(TPS_Household hh) {

        List<TPS_Preference> preferenceModels = new ArrayList<>(hh.getNumberOfMembers());
        for(int i = 0; i < hh.getNumberOfMembers(); i++){
            preferenceModels.add(new TPS_Preference());
        }


            //calculate the shopping motives or set the default values
            int number = 0;
            for (TPS_Person person : hh.getMembers(TPS_Household.Sorting.AGE)) {
                person.preferenceValues = preferenceModels.get(number);
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


                    if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG, "Working on person: " + person);
                    }
                    if (person.isChild()) {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
                            TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG,
                                    "Person is skipped because it is a child");
                        }
                        continue;
                    }

                    TPS_PlanEnvironment pe = new TPS_PlanEnvironment(person, getParameters());
                    createPersonPlans(pe, null);
                    TPS_Plan plan = pe.getBestPlan();
                    PM.writePlan(plan);

                }
            } else {
                // determine who may drive a car and how many cars exist
                int numCarsInHH = hh.getNumberOfCars();
                // check car
                TPS_Car leastLimitedCar = null;
                if (hh.getNumberOfCars() > 0) {
                    leastLimitedCar = hh.getLeastRestrictedCar() instanceof TPS_Car car ? car : null; // TODO: get the car that poses the least limitations
                }

                Vector<TPS_Person> competingCarDrivers = new Vector<>();
                int avLevel = PM.getParameters().getIntValue(ParamValue.AUTOMATIC_VEHICLE_LEVEL);
                int avMinAge = PM.getParameters().getIntValue(ParamValue.AUTOMATIC_VEHICLE_MIN_DRIVER_AGE);
                for (TPS_Person person : hh.getMembers(sortAlgo)) {
                    if (person.mayDriveACar(leastLimitedCar,avMinAge,avLevel)) {
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

                // allocate plan storages
                HashMap<TPS_Person, TPS_PlanEnvironment> driverPlanEnvironments = new HashMap<>();
                HashMap<TPS_Person, TPS_Plan> bestPlans = new HashMap<>();
                // loop over the persons
                for (TPS_Person person : hh.getMembers(sortAlgo)) {
                    if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
                        TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG, "Working on person: " + person);
                    }
                    // skip children
                    if (person.isChild()) {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
                            TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG,
                                    "Person is skipped because it is a child");
                        }
                        continue;
                    }

                    // check car
                    TPS_Car carDummy = null;
                    if (person.mayDriveACar(carDummy,avMinAge,avLevel)) {
                        carDummy = leastLimitedCar;
                    }

                    // build plan environment for the person
                    TPS_PlanEnvironment pe = new TPS_PlanEnvironment(person, PM.getParameters());
                    createPersonPlans(pe, carDummy);
                    TPS_Plan bestPlan = pe.getBestPlan();
                    if (!carAssignmentNecessary || !competingCarDrivers.contains(person) || !bestPlan.usesCar()) {
                        // the person has a plan without the need to interrogate on the car usage with other persons
                        bestPlans.put(person, bestPlan);
                    } else {
                        driverPlanEnvironments.put(person, pe);
                    }
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
                            for (Map.Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
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
                            for (Vehicle car : hh.getAllCars()) {
                                if (car.isRestricted()) {
                                    numRestrictedCars++;
                                }
                            }
                            //count num of restricted plans
                            int numRestrictedPlans = 0;
                            for (Map.Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
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
                            List<Vehicle> localCarList = new ArrayList<>(hh.getAllCars());
                            //now put all the plans (with cars) to work on in a 2nd list
                            List<TPS_Plan> localPlanList = new ArrayList<>();
                            for (Map.Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
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
                                Vehicle tmpCar = null;
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
                        for (Map.Entry<Integer, TPS_Person> entry : scoreIndexMap.entrySet()) {
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
                    PM.writePlan(plan);
                }
            }
        }

}
