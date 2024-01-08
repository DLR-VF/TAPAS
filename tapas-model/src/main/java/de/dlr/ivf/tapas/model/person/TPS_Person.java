/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.person;

import de.dlr.ivf.tapas.model.constants.TPS_AgeClass;
import de.dlr.ivf.tapas.model.constants.TPS_AgeClass.TPS_AgeCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.model.constants.TPS_Sex;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;
import de.dlr.ivf.tapas.model.vehicle.TPS_Car;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.TPS_FastMath;
import de.dlr.ivf.tapas.model.person.TPS_PreferenceParameters.*;
import de.dlr.ivf.tapas.model.person.TPS_Preference.PreferenceSet;
import lombok.Builder;
import lombok.Setter;

import java.util.Map;

/**
 * This class represents a person.
 */
@Builder
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_Person implements ExtendedWritable {
    private final int status;

    /**
     * group code as integer, only used if ParamFlag.FLAG_USE_GROUP_COLUMN_FOR_PERSON_GROUPS is true
     */
    private final int group;
    public ShoppingPreferenceAccessibility currentAccessibilityPreference = ShoppingPreferenceAccessibility.Sonstige;
    public ShoppingPreferenceSupply currentSupplyPreference = ShoppingPreferenceSupply.Sonstige;
    /**
     * This is the local reference to the preference model. It must be set before we touch it and the instance must be thread save (either one per thread or synchronized).
     * I decided for one instance per thread. Thus the reference must be set in the TPS_Worker-thread.
     */
    public TPS_Preference preferenceValues;
    private final double errorTerm;
    /// id of the person
    private final int id;
    /// sex of the person
    private final TPS_Sex sex;
    /// Flag if person holds a long term ticket (season / month...)for the public transport
    private final boolean abo;
    /// age of the person
    private final int age;
    /// class of the age of the person
    private final TPS_AgeClass ageClass;
    /// monthly financial budget for travel expenditures in Euro, includes car usage and pt usage but no fix budget
    private final double budget;
    /// flag if the person is prone to be a car pooler
    private final boolean carPooler;
    /// information if person holds a driving license and when applicable the type of licence
    private final TPS_DrivingLicenseInformation drivingLicenseInformation;
    /// the household the person belongs to
    private TPS_Household household;
    /// flag if the person is prone to be a teleworker
    //private boolean teleworker;
    /// flag if the person is working
    private final double working;
    /// flag if this person has a bike
    private final boolean hasBike;
    /// Id to the fixed work location
    private final int workLocationID;
    private final int educationLevel;

    private final TPS_PersonGroup personGroup;

    private final boolean isChild;

    private final boolean isPupil;

    //todo this does not belong here since this depends on the household context and should be handled outside
    //todo remove in the future
    @Setter
    private double driverScore;

    /**
     * returns the status field of the person like (1=child under 6, 2=pupil, 3=student, 4=retiree,
     * 5=other non working person, 6=working full-time, 7=working part-time, 8= trainee,
     * 9=non working
     *
     * @return status field of the person
     */
    public int getStatus() {
        return status;
    }


    public void estimateAccessibilityPreference(TPS_Stay stay, boolean use_shopping_motives) {

        if (use_shopping_motives && stay.hasDetailedShoppingInformation()) { //Ritas Einkaufsmodell!

            //choose the preference according to the model
            ShoppingClass s = stay.getTypeOfShoppingGoods(); //returns NULL for no shopping or no detailed information
            if (s != null) {
                currentAccessibilityPreference = selectPreferenceAccessibility(s);
                currentSupplyPreference = selectPreferenceSupply(s);
            } else {
                currentAccessibilityPreference = ShoppingPreferenceAccessibility.Sonstige;
                currentSupplyPreference = ShoppingPreferenceSupply.Sonstige;
            }
        } else {
            currentAccessibilityPreference = ShoppingPreferenceAccessibility.Sonstige;
            currentSupplyPreference = ShoppingPreferenceSupply.Sonstige;
        }
        // default
        stay.locationChoiceMotive = currentAccessibilityPreference;
        stay.locationChoiceMotiveSupply = currentSupplyPreference;
    }

    /**
     * Returns the age of the person
     *
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * Returns the age category of the person
     *
     * @return the age category
     */
    public TPS_AgeClass getAgeClass() {
        return this.ageClass;
    }

    /**
     * Returns the monthly variable budget for travel expenditures in Euro
     *
     * @return the budget
     */
    public double getBudget() {
        return budget;
    }

    /**
     * Returns information about driving license availability of the person
     *
     * @return driving license status
     */
    public TPS_DrivingLicenseInformation getDrivingLicenseInformation() {
        return drivingLicenseInformation;
    }


    /**
     * Coding:
     * 0 = none
     * 1 = school
     * 2 = college/Abitur
     * 3 = professional training/ Ausbildung
     * 4 = university
     * 5 = other';
     *
     * @return the education level of this person
     */
    public int getEducationLevel() {
        return educationLevel;
    }

    /**
     * Returns the household the person belongs to
     *
     * @return the household
     */
    public TPS_Household getHousehold() {
        return household;
    }

    /**
     * Sets the household of the person
     *
     * @param household
     */
    public void setHousehold(TPS_Household household) {
        this.household = household;
    }

    /**
     * Returns the id of the person
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the person group of the person, indication work status, sex, age and car ownership
     *
     * @return the person group
     */
    public TPS_PersonGroup getPersonGroup() {
        return this.personGroup;
    }

    /**
     * Returns the person group code if ParamFlag.FLAG_USE_GROUP_COLUMN_FOR_PERSON_GROUPS is true
     * Use this attribute and function with care! Only used if the flag is true and it only returns the group code as an integer.
     * It does not return a person group instance (of {@link TPS_PersonGroup}.
     * @return person group code as integer
     */
    public int getGroup() {
        return group;
    }

    /**
     * Returns the sex of the person
     *
     * @return the sex
     */
    public TPS_Sex getSex() {
        return sex;
    }

    /**
     * @return the workLocationID
     */
    public int getWorkLocationID() {
        return workLocationID;
    }

    /**
     * Returns the work amount
     *
     * @return The work amount
     */
    public double getWorkingAmount() {
        return working;
    }

    /**
     * Returns whether the person holds an season ticket for public transport
     *
     * @return true if an season ticket is present, false else
     */
    public boolean hasAbo() {
        return abo;
    }

    /**
     * @return Returns true if this person has a usable bike.
     */
    public boolean hasBike() {
        return hasBike;
    }

    /**
     * Returns whether the person holds a driving license
     *
     * @return true if the person holds a license; false else
     */
    private boolean hasDrivingLicense() {
        return drivingLicenseInformation == TPS_DrivingLicenseInformation.CAR;
    }

    /**
     * Returns whether the information about the driving license of the person is available
     *
     * @return true if the information is available; false else
     */
    public boolean hasDrivingLicenseInformation() {
        if (getDrivingLicenseInformation() != null)
            return this.getDrivingLicenseInformation().hasDrivingLicenseInformation();
        return false;
    }

    /**
     * Returns whether the person is prone to car pooling
     *
     * @return true if applicable; false else
     */
    public boolean isCarPooler() {
        return carPooler;
    }

    /**
     * Returns whether the person is a child and is too young to go to school according to age
     *
     * @return true if child; false else
     */
    public boolean isChild() {
        return this.isChild;
    }

    /**
     * Returns whether the person is a pupil and goes to school
     *
     * @return true if child; false else
     */
    public boolean isPupil() {
        return this.isPupil;
    }

    /**
     * Returns whether the person is a university student
     *
     * @return true if the person is a university student; false else
     */
    public boolean isStudent() {
        return this.getPersonGroup().isStudent();
    }

    /**
     * Returns whether the person is working
     *
     * @return if working; false else
     */
    public boolean isWorking() {
        return working > 0.25;
    }

    /**
     * Returns whether the person may drive a car
     * <p>
     * Combines driving information if available and age to determine whether the person may drive a car.
     *
     * @return true if the person may drive a car; false else
     */
    public boolean mayDriveACar(TPS_Car car, int automaticVehicleMinDriverAge, int automaticVehicleLevel) {
        boolean mayDriveCar = false;


        mayDriveCar |= (hasDrivingLicenseInformation() && hasDrivingLicense()) ;
        mayDriveCar |= (!hasDrivingLicenseInformation() && getAge() >= 18);
        if(car !=null && !mayDriveCar){
            mayDriveCar |= this.getAge() >= automaticVehicleMinDriverAge &&
                    car.getAutomationLevel() >=automaticVehicleLevel;
        }
        return mayDriveCar;
    }

    /**
     * This is a hard coded linear regression model to see if the person is the primary driver of a car.
     *
     * @return a probability of being the primary driver
     */
    public double primaryDriver() {
        return driverScore;
    }

    private ShoppingPreferenceAccessibility selectPreferenceAccessibility(ShoppingClass s) {
        ShoppingPreferenceAccessibility accessibilityPreference = ShoppingPreferenceAccessibility.Sonstige; //default value!
        double[] utilities = new double[ShoppingPreferenceAccessibility.valueArray.length];
        int index = 0;
        double sum = 0;
        boolean oneIsSet = false;
        if (s.equals(ShoppingClass.UEL)) {
            index = 0;
        }
        Map<ShoppingPreferenceAccessibility, PreferenceSet> accessForShoppingClass = preferenceValues.getPreferencesAccess()
                .get(s);
        PreferenceSet prefSet;
        //calc utilities with a new random uncertantiy (normal distributed) for every choice
        for (ShoppingPreferenceAccessibility e : ShoppingPreferenceAccessibility.valueArray) {
            prefSet = accessForShoppingClass.get(e);
            if (prefSet.isUsed) {
                utilities[index] = TPS_FastMath.exp(prefSet.utility + //utility
                        Randomizer.randomGaussianDistribution(() -> Randomizer.randomGaussian(), 0, prefSet.sigma)
                        //uncertainty
                        + this.errorTerm); //errorterm
                sum += utilities[index];
                oneIsSet = true;
            } else {
                utilities[index] = 0;
            }
            index++;
        }

        if (!oneIsSet) //nothing to estimate!
            return accessibilityPreference; //return the default

        //normalize to get probabilities
        for (index = 0; index < utilities.length; ++index) {
            utilities[index] /= sum;
        }


        //draw the choice
        double rand = Randomizer.random();
        sum = 0;
        index = 0;
        for (ShoppingPreferenceAccessibility e : ShoppingPreferenceAccessibility.valueArray) {
            prefSet = accessForShoppingClass.get(e);
            if (prefSet.isUsed) {
                sum += utilities[index];
                index++;
                if (sum >= rand) {
                    accessibilityPreference = e;
                    break; //finish!
                }
            } else {//skip this index
                index++;
            }
        }
        return accessibilityPreference;
    }

    private ShoppingPreferenceSupply selectPreferenceSupply(ShoppingClass s) {
        ShoppingPreferenceSupply supplyPreference = ShoppingPreferenceSupply.Sonstige; //default value!
        double[] utilities = new double[ShoppingPreferenceSupply.valueArray.length];
        int index = 0;
        double sum = 0;
        boolean oneIsSet = false;
        Map<ShoppingPreferenceSupply, PreferenceSet> supplyForShoppingClass = preferenceValues.getPreferencesSupply()
                .get(s);
        PreferenceSet prefSet;

        //calc utilities with a new random uncertainty (normal distributed) for every choice
        for (ShoppingPreferenceSupply e : ShoppingPreferenceSupply.valueArray) {
            prefSet = supplyForShoppingClass.get(e);
            if (prefSet.isUsed) {
                utilities[index] = TPS_FastMath.exp(prefSet.utility + //utility
                        Randomizer.randomGaussianDistribution(() -> Randomizer.randomGaussian(), 0, prefSet.sigma)
                        //uncertainty
                        + this.errorTerm); //errorterm
                sum += utilities[index];
                oneIsSet = true;

            } else {
                utilities[index] = 0;
            }
            index++;
        }

        if (!oneIsSet) //nothing to estimate!
            return supplyPreference; //return the default


        //normalize to get probabilities
        for (index = 0; index < utilities.length; ++index) {
            utilities[index] /= sum;
        }


        //draw the choice
        double rand = Randomizer.random();
        sum = 0;
        index = 0;
        for (ShoppingPreferenceSupply e : ShoppingPreferenceSupply.valueArray) {
            prefSet = supplyForShoppingClass.get(e);
            if (prefSet.isUsed) {
                sum += utilities[index];
                index++;
                if (sum >= rand) {
                    supplyPreference = e;
                    break; //finish!
                }
            } else {//skip this index
                index++;
            }
        }
        return supplyPreference;
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
        return prefix + this.getClass().getSimpleName() + " [id=" + id + ", age=" + age + " in class=" +
                this.getAgeClass().getCode(TPS_AgeCodeType.STBA) + ", sex=" + sex.name() + ", status=" + status +"]";
    }

}
