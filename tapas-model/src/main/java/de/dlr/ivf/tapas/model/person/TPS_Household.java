/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.person;

import de.dlr.ivf.tapas.model.constants.TPS_Sex;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.vehicle.CarFleetManager;
import de.dlr.ivf.tapas.model.vehicle.Cars;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import lombok.Builder;
import lombok.Singular;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a household.
 *
 * @author mark_ma
 */
@Builder
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_Household implements ExtendedWritable {
    /// household id
    private final int id;

    /// household income
    private double income;

    /// household type
    private int type;

    /// household location
    private final TPS_Location location;

    /// household member list
    @Singular
    private final Collection<TPS_Person> members;

    /// number of cars in household
    private final Cars cars;

    private boolean leastRestrictedCarInitialized;

    private TPS_Car leastRestrictedCar;


    private final CarFleetManager carFleetManager ;

    /**
     * Returns all cars in the household
     *
     * @return an array of cars, null if no cars are present
     */
    public Collection<TPS_Car> getAllCars() {
        return this.cars.getCars();
    }

    /**
     * Returns the number of cars available in the household for the time period specified
     *
     * @param start time period start
     * @param end   time period end
     * @return an array of available cars, null if no cars are available
     */
    public List<TPS_Car> getAvailableCars(double start, double end) {
        return this.getAvailableCars((int) (start + 0.5), (int) (end + 0.5)); //incl. rounding
    }


    /**
     * Returns the number of cars in the household
     *
     * @return number of cars
     */
    public int getNumberOfCars() {
        return cars.getCars().size();
    }

    /**
     * Returns the id of the household
     *
     * @return household id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the household income
     *
     * @return household income
     */
    public double getIncome() {
        return income;
    }

    /**
     * Returns the least restricted car for this household or null, if no car is available.
     *
     * @return the car
     */
    public TPS_Car getLeastRestrictedCar() {
        //initialize on the first call
        if (!this.leastRestrictedCarInitialized) {
            this.leastRestrictedCarInitialized = true;
            this.leastRestrictedCar = this.cars.getCars().stream()
                    .min(Comparator.comparing(TPS_Car::isRestricted))
                    .orElse(null);
        }


        return this.leastRestrictedCar;
    }

    /**
     * Returns the location of the household
     *
     * @return household location
     */
    public TPS_Location getLocation() {
        return location;
    }

    /**
     * Returns all members of a household sorted by age
     *
     * @return all household members
     */
    public Collection<TPS_Person> getMembers(Sorting e) {

        return switch (e) {
            case AGE -> this.members.stream()
                    .sorted(Comparator
                            .comparingInt(TPS_Person::getAge)
                            .reversed())
                    .collect(Collectors.toList());
            case RANDOM -> this.members.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(ArrayList::new),
                            memberList -> {
                                Collections.shuffle(memberList);
                                return memberList;
                            }));
            case PRIMARY_DRIVER -> this.members.stream()
                    .sorted(Comparator.comparingDouble(TPS_Person::primaryDriver).reversed())
                    .collect(Collectors.toList());
            default -> this.members;
        };
    }


    public CarFleetManager getCarFleetManager(){
        return this.carFleetManager;
    }

    public int getNumCarDrivers() {
        int sum = 0;
        for (TPS_Person p : members) {
            if (p.mayDriveACar(null,0,0)) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumChildren() {
        int sum = 0;
        for (TPS_Person p : members) {
            if (p.getAge() < 18) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumGrownups() {
        int sum = 0;
        for (TPS_Person p : members) {
            if (p.getAge() >= 18) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumHalfEmployed() {
        int sum = 0;
        for (TPS_Person p : members) {
            if (p.getWorkingAmount() <= .5) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumMalePersons() {
        int sum = 0;
        for (TPS_Person p : members) {
            if (p.getSex() == TPS_Sex.MALE) {
                ++sum;
            }
            if (p.getSex() == TPS_Sex.NON_RELEVANT && Math.random() >= .5) {
                ++sum;
            }
        }
        return sum;
    }

    /**
     * Returns the household equivalence income by the new OECD scale
     * The equivalence income is computed by the total income of the household divided by the sum of the person
     * weights
     * I.e. the first adult gets a person weight of 1,
     * each additional person in the household who is 14 years or older gets a weight of 0.5,
     * each child under 14 has a weight of 0.3.
     *
     * @return household equivalence income
     */
    public double getHouseholdEquivalenceIncome() {
        // first sum all person weights of members with >=14 years of the household
        // remember the first adult has weight 1, each additional adult (>=14) has weight 0.5
        double personWeightSum = 1 + (this.members.stream().filter(p -> p.getAge() >= 14).count() - 1) * 0.5;
        personWeightSum += 0.3 * this.members.stream().filter(p -> p.getAge() < 14).count();
        return this.getIncome() / personWeightSum;
    }

    /**
     * Returns the number of members in this household
     *
     * @return
     */
    public int getNumberOfMembers() {
        return members.size();
    }

    /**
     * Returns the type of the household
     *
     * @return household type
     */
    public int getType() {
        return type;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getId();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.util.tapas.ivf.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        return prefix + this.getClass().getSimpleName() + "[id=" + this.getId() + ", type=" + this.getType() +
                ", members=" + this.members.size() + ", cars=" + this.getNumberOfCars() + ", income=" +
                this.getIncome() + "]";
    }


    public enum Sorting {NONE, RANDOM, AGE, PRIMARY_DRIVER, COST_OPTIMUM}
}
