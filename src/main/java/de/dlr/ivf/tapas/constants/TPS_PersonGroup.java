/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.constants;

import de.dlr.ivf.tapas.constants.TPS_AgeClass.TPS_AgeCodeType;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.person.TPS_Person;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents the person groups of TAPAS. Each person group contains of one sex, one car constant and some age
 * classes. The age classes have a finer granularity than the person groups, so a person group have to store more than one
 * age class. There also exist a fits*( method to check whether a person fits to the person group constant.
 */
public class TPS_PersonGroup implements Comparable<TPS_PersonGroup> {

    private static final HashMap<Integer, TPS_PersonGroup> PERSON_GROUP_MAP = new HashMap<>();

    /**
     * List of all age classes the person group constant contains
     */
    private List<TPS_AgeClass> ageClasses;
    private int minAge;
    private int maxAge;
    /**
     * number of cars in the household
     */
    private TPS_CarCode carCode;
    /**
     * states if there are children in the household
     */
    private TPS_HasChildCode hasChildCode;
    private TPS_PersonType persType;
    /**
     * Sex constant; 1= male, 2 = female, 0 = not relevant
     */
    private TPS_Sex sex;
    /**
     * code obtained from the db
     */
    private int code;
    private String description;

    public TPS_PersonGroup(int code, String description, int ageClass, int minAge, int maxAge, int cars, int sex, String personType, String hasChild) {
        this.code = code;
        this.description = description;
        this.setAgeClasses(TPS_AgeClass.getConstants(TPS_AgeCodeType.PersGroup, ageClass));
//        this.setMinAge(this.getAgeClasses().stream().mapToInt(ac->ac.getMin()).min().orElse(-1));
//        this.setMaxAge(this.getAgeClasses().stream().mapToInt(ac->ac.getMax()).max().orElse(-1));
        this.setMinAge(minAge);
        this.setMaxAge(maxAge);
        this.setSex(TPS_Sex.getEnum(sex));
        this.setCarCode(TPS_CarCode.getEnum(cars));
        this.setPersonType(TPS_PersonType.valueOf(personType));
        this.setHasChildCode(TPS_HasChildCode.valueOf(hasChild));
    }

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Empties the global static person group map
     */
    public static void clearPersonGroupMap() {
        PERSON_GROUP_MAP.clear();
    }

    /**
     * Returns a collection/view of the values of the PERSON_GROUP_MAP, i.e. all stored person group constants
     * Note: changes in the map are reflected in the collection and vice-versa
     *
     * @return a collection/view of the values of the PERSON_GROUP_MAP
     */
    public static Collection<TPS_PersonGroup> getConstants() {
        return PERSON_GROUP_MAP.values();
    }

    /**
     * @param code person group code from the db
     * @return person group object for a given TPS_PersonGroupType and code (int)
     */
    public static TPS_PersonGroup getPersonGroupByCode(int code) {
        return PERSON_GROUP_MAP.get(code);
    }

    /**
     * @param person of who the group is determined
     * @return person group object for a given person
     */
    public static TPS_PersonGroup getPersonGroupOfPerson(TPS_Person person) {
        for (TPS_PersonGroup tpg : PERSON_GROUP_MAP.values()) {
            if (tpg.fits(person)){
                return tpg;
            }
        }
        if (TPS_Logger.isLogging(TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.ERROR)) {
            TPS_Logger.log(
                    TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.ERROR,
                    "Person does not match any person group: " + person.toString());
        }
        return null;
    }

    /**
     * Add to static collection of all person groups constants.
     * The objects are accessed by their ids from the db.
     */
    public void addPersonGroupToMap() {
        PERSON_GROUP_MAP.put(code, this);
    }

    public int compareTo(TPS_PersonGroup persGroup) {
        return this.getCode() - persGroup.getCode();
    }

    /**
     * This method checks the following attributes: <br>
     * a) {@link TPS_AgeClass}: Checks if the person's age class is included in the person group <br>
     * b) {@link TPS_Sex}: checks the sex of person and group <br>
     * c) {@link TPS_CarCode}: checks whether the amount of cars in the person's household corresponds with the constant in car
     * d) {@link TPS_HasChildCode} checks whether the number of children in the person's household corresponds to the HasChildCode
     * constant in the person group constant.
     *
     * @param person which is checked against the person group
     * @return true if the person attributes fit with the person group constant
     */
    public boolean fits(TPS_Person person) {
        return this.getMinAge()<=person.getAge() &&
                this.getMaxAge() >= person.getAge() &&
                this.getSex().fits(person.getSex()) &&
                this.getCarCode().fits(person.getHousehold().getNumberOfCars()) &&
                this.getHasChildCode().fits(person.getHousehold().getNumChildren()) &&
                (this.getPersonType().equals(TPS_PersonType.getPersonTypeByStatus(person.getStatus()))||
                        //if a person is over 64 we do not care about the persontype, all are non working (retirees)
                        person.getAge()>=65);
    }

    /**
     * Returns the age categories; see {@link TPS_AgeClass}
     *
     * @return return all age classes
     */
    private List<TPS_AgeClass> getAgeClasses() {
        return ageClasses;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sets age classes.
     *
     * @param ageClasses list of of age classes objects
     */
    private void setAgeClasses(List<TPS_AgeClass> ageClasses) {
        this.ageClasses = ageClasses;
    }

    /**
     * Returns the categories for car availability in the household; see {@link TPS_CarCode}
     *
     * @return cars
     */
    private TPS_CarCode getCarCode() {
        return carCode;
    }

    /**
     * Sets CarCode.
     *
     * @param carCode set carCode
     */
    private void setCarCode(TPS_CarCode carCode) {
        this.carCode = carCode;
    }


    public int getCode() {
        return code;
    }

    /**
     * Returns the type of the person; see {@link TPS_PersonType}
     *
     * @return person type
     */
    public TPS_PersonType getPersonType() {
        return persType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets person type.
     *
     * @param persType new person type for the person type field
     */
    private void setPersonType(TPS_PersonType persType) {
        this.persType = persType;
    }


    private void setHasChildCode(TPS_HasChildCode hasChildCode) {
        this.hasChildCode = hasChildCode;
    }

    private TPS_HasChildCode getHasChildCode() {
        return this.hasChildCode;
    }

    /**
     * Returns the sex of the person; see {@link TPS_Sex}
     *
     * @return sex
     */
    public TPS_Sex getSex() {
        return sex;
    }

    /**
     * Sets sex.
     *
     * @param sex new sex code
     */
    private void setSex(TPS_Sex sex) {
        this.sex = sex;
    }

    /**
     * States if the person is a child, that means the person is before school-age!
     *
     * @return true if the constant represents a child, false otherwise
     */
    public boolean isChild() {
        return TPS_PersonType.CHILD.equals(this.getPersonType());
    }

    /**
     * States if the person is a pupil, that means the person goes to school
     *
     * @return true if the constant represents a pupil, false otherwise
     */
    public boolean isPupil() {
        return TPS_PersonType.PUPIL.equals(this.getPersonType());
    }

    /**
     * States if the person is a student (university)
     *
     * @return true if the constant represents a student, false otherwise
     */
    public boolean isStudent() {
        return TPS_PersonType.STUDENT.equals(this.getPersonType());
    }

    public String toString() {
        return this.getClass().getSimpleName() + "." + this.getCode() + "[code=" + this.getCode() + "]";
    }

    public String toString(boolean extended) {
        if (extended) return super.toString();
        return toString();
    }

    /**
     * General person types as enum constants.
     */
    public enum TPS_PersonType {
        ADULT, NON_WORKING_ADULT, WORKING_ADULT, CHILD, PUPIL, RETIREE, STUDENT, TRAINEE;

        public static TPS_PersonType getPersonTypeByStatus(int status) {
            switch (status) {
                case 1:
                    return CHILD;
                case 2:
                    return PUPIL;
                case 3:
                    return STUDENT;
                case 4:
                    return NON_WORKING_ADULT; //actually it could be RETIREE TODO
                case 5:
                    return NON_WORKING_ADULT;
                case 6:
                    return WORKING_ADULT;
                case 7:
                    return WORKING_ADULT;
                case 8:
                    return TRAINEE;
                case 9:
                    return NON_WORKING_ADULT;
            }
            return null;
        }
    }
}
