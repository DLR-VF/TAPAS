/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.constants;

import de.dlr.ivf.tapas.constants.TPS_AgeClass.TPS_AgeCodeType;
import de.dlr.ivf.tapas.person.TPS_Person;

import java.util.Collection;
import java.util.EnumMap;
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
    /**
     * number of cars in the household
     */
    private TPS_CarCode carCode;
    private TPS_PersonType persType;
    /**
     * Sex constant; 1= male, 2 = female, 0 = not relevant
     */
    private TPS_Sex sex;
    /**
     * id obtained from the db
     */
    private final int id;
    /**
     * mapping of the {@link TPS_PersonGroupType} to the internal representation
     */
    private final EnumMap<TPS_PersonGroupType, TPS_InternalConstant<TPS_PersonGroupType>> map;

    public TPS_PersonGroup(int id, String[] attributes) {
        if ((attributes.length - 4) % 3 != 0) {
            throw new RuntimeException("Person need n*3 + 4 attributes n*(name, code, type) + (code_ageclass, " +
                    "code_sex, code_cars, persType): " + attributes.length);
        }

        this.map = new EnumMap<>(TPS_PersonGroupType.class);
        this.id = id;
        TPS_InternalConstant<TPS_PersonGroupType> tic;
        for (int i = 0; i < attributes.length - 4; i += 3) {
            tic = new TPS_InternalConstant<>(attributes[i], Integer.parseInt(attributes[i + 1]),
                    TPS_PersonGroupType.valueOf(attributes[i + 2]));
            this.map.put(tic.getType(), tic);
        }

        for (TPS_PersonGroupType type : TPS_PersonGroupType.values()) {
            if (!this.map.containsKey(type)) {
                throw new RuntimeException(
                        "Person group code for " + this.getId() + " for type " + type.name() + " not " + "defined");
            }
        }
        this.setAgeClasses(TPS_AgeClass
                .getConstants(TPS_AgeCodeType.PersGroup, Integer.parseInt(attributes[attributes.length - 4])));
        this.setSex(TPS_Sex.getEnum(Integer.parseInt(attributes[attributes.length - 3])));
        this.setCarCode(TPS_CarCode.getEnum(Integer.parseInt(attributes[attributes.length - 2])));
        this.setPersType(TPS_PersonType.valueOf(attributes[attributes.length - 1]));
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
     * @param type like TAPAS, VISEVA or VISEVA_R, see TPS_PersonGroupType
     * @param code person group code from the db
     * @return person group object for a given TPS_PersonGroupType and code (int)
     */
    public static TPS_PersonGroup getPersonGroupByTypeAndCode(TPS_PersonGroupType type, int code) {
        for (TPS_PersonGroup tpg : PERSON_GROUP_MAP.values()) {
            if (tpg.getCode(type) == code) {
                return tpg;
            }
        }
        return null;
    }

    /**
     * Add to static collection of all person groups constants.
     * The objects are accessed by their ids from the db.
     */
    public void addPersonGroupToMap() {
        PERSON_GROUP_MAP.put(id, this);
    }

    public int compareTo(TPS_PersonGroup persGroup) {
        return this.getCode() - persGroup.getCode();
    }

    /**
     * This method checks the following attributes: <br>
     * a) {@link TPS_AgeClass}: Checks if the person's age class is included in the person group <br>
     * b) {@link TPS_Sex}: checks the sex of person and group <br>
     * c) {@link TPS_CarCode}: checks whether the amount of cars in the person's household corresponds with the constant in car
     * constant in the person group constant.
     *
     * @param person which is checked against the person group
     * @return true if the person attributes fit with the person group constant
     */
    public boolean fits(TPS_Person person) {
        return this.getAgeClasses().contains(person.getAgeClass()) && this.getSex() == person.getSex() &&
                this.getCarCode().fits(person.getHousehold().getCarNumber());
    }

    /**
     * Returns the age categories; see {@link TPS_AgeClass}
     *
     * @return return all age classes
     */
    private List<TPS_AgeClass> getAgeClasses() {
        return ageClasses;
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

    public int getCode(TPS_PersonGroupType type) {
        return this.map.get(type).getCode();
    }

    public int getCode() {
        return this.getCode(TPS_PersonGroupType.TAPAS);
    }

    public int getId() {
        return id;
    }

    /**
     * Returns the type of the person; see {@link TPS_PersonType}
     *
     * @return person type
     */
    public TPS_PersonType getPersType() {
        return persType;
    }

    /**
     * Sets person type.
     *
     * @param persType new person type for the person type field
     */
    private void setPersType(TPS_PersonType persType) {
        this.persType = persType;
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
        return TPS_PersonType.CHILD.equals(this.getPersType());
    }

    /**
     * States if the person is a pupil, that means the person goes to school
     *
     * @return true if the constant represents a pupil, false otherwise
     */
    public boolean isPupil() {
        return TPS_PersonType.PUPIL.equals(this.getPersType());
    }

    /**
     * States if the person is a retiree
     *
     * @return true if the constant represents a retiree, false otherwise
     */
    public boolean isRetiree() {
        return TPS_PersonType.RETIREE.equals(this.getPersType());
    }

    /**
     * States if the person is a student (university)
     *
     * @return true if the constant represents a student, false otherwise
     */
    public boolean isStudent() {
        return TPS_PersonType.STUDENT.equals(this.getPersType());
    }

    public String toString() {
        return this.getClass().getSimpleName() + "." + this.getId() + "[id=" + this.getId() + "]";
    }

    public String toString(boolean extended) {
        if (extended) return super.toString();
        return toString();
    }

    /**
     * General person types as enum constants.
     */
    public enum TPS_PersonType {
        ADULT, CHILD, PUPIL, RETIREE, STUDENT, TRAINEE
    }

    /**
     * This enum provides all person code types for this application and further analysis tools like VISEVA.
     */
    public enum TPS_PersonGroupType {
        /**
         * Person code type for this application
         */
        TAPAS,
        /**
         * Person code type for VISEVA
         */
        VISEVA,
        /**
         * Person code type for VISEVA with a special differentiation of the retirees
         */
        VISEVA_R
    }
}
