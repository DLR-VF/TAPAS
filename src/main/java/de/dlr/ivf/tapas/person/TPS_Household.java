package de.dlr.ivf.tapas.person;

import de.dlr.ivf.tapas.constants.TPS_Sex;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.util.ExtendedWritable;

import java.util.*;

/**
 * This class represents a household.
 *
 * @author mark_ma
 */
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
    private final SortedMap<Integer, TPS_Person> members;

    /// number of cars in household
    private TPS_Car[] cars;

    private boolean leastRestrictedCarInitialized = false;

    private TPS_Car leastRestrictedCar = null;

    public TPS_HouseholdCarMediator getCarMediator() {
        return car_mediator;
    }

    public void setCarMediator(TPS_HouseholdCarMediator car_mediator) {
        this.car_mediator = car_mediator;
    }

    private TPS_HouseholdCarMediator car_mediator = null;


    /**
     * Constructor
     * TODO: remove, this won't work - it is used by HouseholdSet only, which is not used
     *
     * @param id The id of the household
     */
    public TPS_Household(int id) {
        this.id = id;
        this.location = null;
        this.members = new TreeMap<>();
    }


    /**
     * Constructor
     *
     * @param id     The id of the household
     * @param income The income of the household
     * @param type   The type of the household
     * @param loc    The location of the household
     * @param cars   The cars that this household has
     */
    public TPS_Household(int id, int income, int type, TPS_Location loc, TPS_Car[] cars) {
        this.id = id;
        this.location = loc;
        this.income = income;
        this.type = type;
        if (cars != null) this.cars = cars;
        else this.cars = new TPS_Car[0];
        this.members = new TreeMap<>();
    }


    /**
     * Adds this person to the household
     *
     * @param p person to be added
     */
    public void addMember(TPS_Person p) {
        p.setHousehold(this);
        members.put(p.getId(), p);
    }

    /**
     * Returns all cars in the household
     *
     * @return an array of cars, null if no cars are present
     */
    public TPS_Car[] getAllCars() {
        return this.cars;
    }

    /**
     * Returns the number of cars available in the household for the time period specified
     *
     * @param start time period start
     * @param end   time period end
     * @return an array of available cars, null if no cars are available
     */
    public TPS_Car[] getAvailableCars(double start, double end) {
        return this.getAvailableCars((int) (start + 0.5), (int) (end + 0.5)); //incl. rounding
    }


    public void initializeCarMediator(TPS_Household household, List<TPS_Plan> household_plans){
        if(this.car_mediator == null)
            this.car_mediator = new TPS_HouseholdCarMediator(household);
    }

    /**
     * Returns the number of cars available in the household for the time period specified
     *
     * @param start time period start
     * @param end   time period end
     * @return an array of available cars, null if no cars are available
     */
    public TPS_Car[] getAvailableCars(int start, int end) {
        int availableCars = 0;
        if (this.cars == null) return null;
        for (TPS_Car car : this.cars) {
            if (car.isAvailable(start, end)) {
                availableCars++;
            }
        }
        if (availableCars == 0) return null;
        TPS_Car[] available = new TPS_Car[availableCars];
        for (int i = 0, j = 0; i < this.cars.length && j < availableCars; ++i) {
            if (this.cars[i].isAvailable(start, end)) {
                available[j++] = this.cars[i];
            }
        }
        return available;
    }

    /**
     * Retrieves the car on the specified index or null otherwise
     *
     * @param index the index of the car
     * @return the car
     */
    public TPS_Car getCar(int index) {
        if (this.cars == null) return null;
        if (index <= this.cars.length) {
            return this.cars[index];
        } else {
            return null;
        }

    }

    /**
     * Returns the number of cars in the household
     *
     * @return number of cars
     */
    public int getCarNumber() {
        if (cars != null) {
            return cars.length;
        } else {
            return 0;
        }
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
            this.leastRestrictedCar = null;
            this.leastRestrictedCarInitialized = true;
            if (this.cars != null && this.cars.length > 0) {
                this.leastRestrictedCar = this.cars[0]; //take the first candidate
                //check for restriction
                for (TPS_Car car : this.cars) {
                    if (!car.isRestricted() && this.leastRestrictedCar.isRestricted()) {
                        this.leastRestrictedCar = car;
                    }
                }
            }
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

        List<TPS_Person> agesortedPersons = new ArrayList<>(members.values());
        //TODO why is sorted by age? It isn't, right?

        switch (e) {
            case NONE:
            case COST_OPTIMUM:
                break;
            case AGE:
                //sort that list by age descending!
                agesortedPersons.sort((arg0, arg1) -> {
                    return arg1.getAge() - arg0.getAge(); //age
                });
                break;

            case RANDOM:
                //sort that list by random!
                agesortedPersons.sort((arg0, arg1) -> {
                    return Math.random() < 0.5 ? -1 : 1; //random
                });
                break;
            case PRIMARY_DRIVER:
                //sort that list by age descending!
                agesortedPersons.sort((arg0, arg1) -> arg0.primaryDriver() > arg1.primaryDriver() ? -1 : 1);
                break;

        }

        return agesortedPersons;
    }

    public int getNumCarDrivers() {
        int sum = 0;
        for (TPS_Person p : members.values()) {
            if (p.mayDriveACar()) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumChildren() {
        int sum = 0;
        for (TPS_Person p : members.values()) {
            if (p.getAge() < 18) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumGrownups() {
        int sum = 0;
        for (TPS_Person p : members.values()) {
            if (p.getAge() >= 18) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumHalfEmployed() {
        int sum = 0;
        for (TPS_Person p : members.values()) {
            if (p.getWorkingAmount() <= .5) {
                ++sum;
            }
        }
        return sum;
    }

    public int getNumMalePersons() {
        int sum = 0;
        for (TPS_Person p : members.values()) {
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
     * @see de.dlr.ivf.tapas.util.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        return prefix + this.getClass().getSimpleName() + "[id=" + this.getId() + ", type=" + this.getType() +
                ", members=" + this.members.size() + ", cars=" + this.getCarNumber() + ", income=" + this.getIncome() +
                "]";
    }


    public enum Sorting {NONE, RANDOM, AGE, PRIMARY_DRIVER, COST_OPTIMUM}
}
