/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.person;

import de.dlr.ivf.tapas.constants.TPS_AgeClass;
import de.dlr.ivf.tapas.constants.TPS_AgeClass.TPS_AgeCodeType;
import de.dlr.ivf.tapas.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.constants.TPS_Sex;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.person.TPS_Preference.PreferenceSet;
import de.dlr.ivf.tapas.person.TPS_PreferenceParameters.ShoppingClass;
import de.dlr.ivf.tapas.person.TPS_PreferenceParameters.ShoppingPreferenceAccessibility;
import de.dlr.ivf.tapas.person.TPS_PreferenceParameters.ShoppingPreferenceSupply;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.TPS_FastMath;

import java.util.Map;

/**
 * This class represents a person.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_Person implements ExtendedWritable {
    public ShoppingPreferenceAccessibility currentAccessibilityPreference = ShoppingPreferenceAccessibility.Sonstige;
    public ShoppingPreferenceSupply currentSupplyPreference = ShoppingPreferenceSupply.Sonstige;
    /**
     * This is the local reference to the preference model. It must be set before we touch it and the instance must be thread save (either one per thread or synchronized).
     * I decided for one instance per thread. Thus the reference must be set in the TPS_Worker-thread.
     */
    public TPS_Preference preferenceValues;
    double errorTerm = 0;
    /// id of the person
    private final int id;
    /// the person group
    private TPS_PersonGroup persGroup;
    /// sex of the person
    private final TPS_Sex sex;
    /// Flag if person holds a long term ticket (season / month...)for the public transport
    private final boolean abo;
    /// age of the person
    private int age;
    /// flag if the age has been adapted (artificial rejuvenation of retirees)
    private boolean ageAdaption;
    /// class of the age of the person
    private TPS_AgeClass ageClass;
    /// monthly financial budget for travel expenditures in Euro, includes car usage and pt usage but no fix budget
    private final double budget;
    /// flag if the person is prone to be a car pooler
    private boolean carPooler;
    /// information if person holds a driving license and when applicable the type of licence
    private TPS_DrivingLicenseInformation drivingLicenseInformation;
    /// the household the person belongs to
    private TPS_Household household;
    /// flag if the person is prone to be a teleworker
    private boolean teleworker;
    /// flag if the person is working
    private final double working;
    /// flag if this person has a bike
    private final boolean hasBike;
    /// Id to the fixed work location
    private int workLocationID = -1;
    private double driverScore = -1;
    private int educationLevel = 0;

    /**
     * This constructor just calls the init() method.
     *
     * @param id                   The id of this person
     * @param sex                  The sex of this person
     * @param personGroup          The person group this person matches
     * @param age                  The age of this person
     * @param abo                  Whether this person has a public transport abo
     * @param hasBike              Whether this person has a bike
     * @param budget               This person's budget
     * @param isWorking            Whether this person is working
     * @param isTeleworker         Whether this person is a teleworker
     * @param workLocationID       The id of this person's work location (negative if not valid)
     * @param eduactionLevel
     * @param use_shopping_motives
     */
    public TPS_Person(int id, TPS_Sex sex, TPS_PersonGroup personGroup, int age, boolean abo, boolean hasBike, double budget, double isWorking, boolean isTeleworker, int workLocationID, int eduactionLevel, boolean use_shopping_motives) {
        this.persGroup = personGroup;
        this.id = id;
        this.sex = sex;
        this.age = age;
        this.ageClass = TPS_AgeClass.getAgeClass(this.getAge());
        this.abo = abo;
        this.hasBike = hasBike;
        this.budget = budget;
        this.carPooler = false;
        this.teleworker = false;
        this.setHousehold(null);
        this.setDrivingLicenseInformation(TPS_DrivingLicenseInformation.UNKNOWN);
        this.working = isWorking;
        this.teleworker = isTeleworker;
        this.workLocationID = workLocationID;
        this.educationLevel = eduactionLevel;
        if (use_shopping_motives) {
            this.errorTerm = Randomizer.randomGumbelDistribution(() -> Randomizer.random(), 0,
                    1); // constant for one person
        }

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
     * Sets the driving license information of the person
     *
     * @param drivingLicenseInformation
     */
    public void setDrivingLicenseInformation(TPS_DrivingLicenseInformation drivingLicenseInformation) {
        this.drivingLicenseInformation = drivingLicenseInformation;
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
    public TPS_PersonGroup getPersGroup() {
        return persGroup;
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
     * Returns whether the age of the person was adapted to account for younger behavior of the elderly
     *
     * @return true if the age was adapted; false else
     */
    public boolean isAgeAdapted() {
        return ageAdaption;
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
     * Sets whether the person is prone to car pooling
     *
     * @param carPooler
     */
    public void setCarPooler(boolean carPooler) {
        this.carPooler = carPooler;
    }

    /**
     * Returns whether the person is a child and is too young to go to school according to age
     *
     * @return true if child; false else
     */
    public boolean isChild() {
        return this.getPersGroup().isChild();
    }

    /**
     * Returns whether the person is a pupil and goes to school
     *
     * @return true if child; false else
     */
    public boolean isPupil() {
        return this.getPersGroup().isPupil();
    }

    /**
     * Returns whether the person is a university student
     *
     * @return true if the person is a university student; false else
     */
    public boolean isStudent() {
        return this.getPersGroup().isStudent();
    }

    /**
     * Returns whether the person is prone to teleworking
     *
     * @return true if teleworking; false else
     */
    public boolean isTeleworker() {
        return teleworker;
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
    public boolean mayDriveACar() {
        return (hasDrivingLicenseInformation() && hasDrivingLicense()) ||
                (!hasDrivingLicenseInformation() && getAge() >= 18);
    }

    /**
     * This is a hard coded linear regression model to see if the person is the primary driver of a car.
     *
     * @return a probability of being the primary driver
     */
    public double primaryDriver() {
        if (driverScore < -0.1) {
            int members = 0;
            //int numberOfAdhults=0;
            int numberOfChilds = 0;
            int numberOfWorkingMembers = 0;
            int numberOfMembersWithDrivingLicense = 0;
            int ageYoungestAdhult = 1000; //I hope no one becomes older, ever!
            int ageYoungestChild = 1000;
            int numberOfCars = this.getHousehold().getCarNumber();
            //shortcut:
            if (numberOfCars == 0 || !this.hasDrivingLicense()) {
                driverScore = 0;
                return 0;
            }

            driverScore = -25.6959310091439; //constant
            driverScore += this.age * -0.104309375757471;
            driverScore += Math.log(this.age) * 3.53025970082386;
            driverScore += this.sex == TPS_Sex.MALE ? -1.34768311274 : 0;
            driverScore += this.sex == TPS_Sex.MALE ? this.age * 0.0449898677627131 : 0;
            driverScore += this.working > 0 ? 1.02298146009324 : 0;
            driverScore += this.hasDrivingLicense() ? 10.9458075841947 : 0;

            //sum up all statistics above
            for (TPS_Person p : this.getHousehold().getMembers(TPS_Household.Sorting.NONE)) {
                members++;
                if (p.working > 0) numberOfWorkingMembers++;
                if (p.hasDrivingLicense()) numberOfMembersWithDrivingLicense++;
                if (p.age >= 18) {
                    //numberOfAdhults++;
                    ageYoungestAdhult = Math.min(ageYoungestAdhult, p.age);
                } else {
                    numberOfChilds++;
                    ageYoungestChild = Math.min(ageYoungestChild, p.age);
                }
            }

            if (numberOfChilds == 0) { //keine Kinder
                if (members == 1) { //ein Erwachsener
                    if (ageYoungestAdhult < 30) {
                        driverScore += 0.355021416722072;
                    } else if (ageYoungestAdhult < 60) {
                        driverScore += 0.171869345495256;
                    } else {
                        driverScore += 1.22484387968388;
                    }
                } else if (members == 2) { //zwei erwachsene
                    if (ageYoungestAdhult < 30) {
                        driverScore += -0.225129329476169;
                    } else if (ageYoungestAdhult < 60) {
                        driverScore += -0.526338766819736;
                    } else {
                        driverScore += 0.00349663057568653;
                    }
                } else { // mehr als 3
                    driverScore += -0.383355892620622;
                }
            } else {                                        //Haushalte mit Kindern
                if (ageYoungestChild < 6) {
                    driverScore += -0.422373712001074;
                } else if (ageYoungestChild < 14) {
                    driverScore += -0.457447530364251;
                } else {
                    driverScore += -0.362948716707795;
                }
            }

            driverScore += numberOfWorkingMembers * -0.358492255297056;
            driverScore += numberOfMembersWithDrivingLicense * 1.20018551149737;
            driverScore += numberOfCars * -1.69765399821086;
            if (numberOfMembersWithDrivingLicense > 0) //avoid div by zero! //TODO is this Integer division correct?
                driverScore += (numberOfCars / numberOfMembersWithDrivingLicense) * 10.6624425307085;


            driverScore = 1.0 / (1.0 + Math.exp(-driverScore));

            //finally cap it (for safety)!
            driverScore = Math.max(0.0, Math.min(1.0, driverScore)); //cap to 0-1
        }


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
        Map<ShoppingPreferenceAccessibility, PreferenceSet> accessForShoppingClass = preferenceValues.preferencesAccess
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
        double[] utilities = new double[TPS_PreferenceParameters.ShoppingPreferenceSupply.valueArray.length];
        int index = 0;
        double sum = 0;
        boolean oneIsSet = false;
        Map<TPS_PreferenceParameters.ShoppingPreferenceSupply, PreferenceSet> supplyForShoppingClass = preferenceValues.preferencesSupply
                .get(s);
        PreferenceSet prefSet;

        //calc utilities with a new random uncertantiy (normal distributed) for every choice
        for (TPS_PreferenceParameters.ShoppingPreferenceSupply e : TPS_PreferenceParameters.ShoppingPreferenceSupply.valueArray) {
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
        for (TPS_PreferenceParameters.ShoppingPreferenceSupply e : TPS_PreferenceParameters.ShoppingPreferenceSupply.valueArray) {
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

    /**
     * This method sets the person's age to a new value. this is used in scenario versions. The prediction is that people are
     * more mobile in higher ages in future. So in these scenarios they get younger. This leads to a new selection of the
     * person group that these values are consistent
     *
     * @param ageAdaption            flag if an age adaption should be performed
     * @param rejuvenate_by_nb_years
     */
    public void setAgeAdaption(boolean ageAdaption, int rejuvenate_by_nb_years) {
        if (this.ageAdaption != ageAdaption) {
            if (this.ageAdaption && !ageAdaption) {
                this.age = this.age + rejuvenate_by_nb_years;
            } else if (!this.ageAdaption && ageAdaption) {
                this.age = this.age - rejuvenate_by_nb_years;
            }
            this.ageClass = TPS_AgeClass.getAgeClass(this.age);
            if (!persGroup.fits(this)) {
                // is this correct??
                // Yes I think so, if there are mistakes in the age classes then here is the point to start with debugging
                for (TPS_PersonGroup tpg : TPS_PersonGroup.getConstants()) {
                    if (tpg.fits(this)) {
                        this.persGroup = tpg;
                    }
                }
            }
        }
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
        return prefix + this.getClass().getSimpleName() + " [id=" + id + ", persGroup=" + persGroup.getCode() +
                ", age=" + age + " in class=" + this.getAgeClass().getCode(TPS_AgeCodeType.STBA) + ", sex=" +
                sex.name() + "]";
    }

}
