/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.person;

import de.dlr.ivf.tapas.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.constants.TPS_Sex;
import de.dlr.ivf.tapas.person.TPS_PreferenceParameters.ChoiceParams;

import java.util.HashMap;
import java.util.Map;

/**
 * This class holdts the specific preference parameters for this person.
 *
 * @author hein_mh
 */
public class TPS_Preference {


    Map<TPS_PreferenceParameters.ShoppingClass, Map<TPS_PreferenceParameters.ShoppingPreferenceSupply, PreferenceSet>> preferencesSupply = new HashMap<>();

    Map<TPS_PreferenceParameters.ShoppingClass, Map<TPS_PreferenceParameters.ShoppingPreferenceAccessibility, PreferenceSet>> preferencesAccess = new HashMap<>();

    /**
     * standard constructor, which needs a reference to the person it is attached to
     */
    public TPS_Preference() {

        for (TPS_PreferenceParameters.ShoppingClass s : TPS_PreferenceParameters.ShoppingClass.values()) {
            //Supply based model

            // init the preference structures
            Map<TPS_PreferenceParameters.ShoppingPreferenceSupply, PreferenceSet> tmpPref = new HashMap<>();
            PreferenceSet tmpSet;
            this.preferencesSupply.put(s, tmpPref);
            for (TPS_PreferenceParameters.ShoppingPreferenceSupply e : TPS_PreferenceParameters.ShoppingPreferenceSupply.valueArray) {
                tmpSet = new PreferenceSet();
                tmpPref.put(e, tmpSet);
            }
            //set "Sonstige" to default
            tmpSet = tmpPref.get(TPS_PreferenceParameters.ShoppingPreferenceSupply.Sonstige);
            //default utility= 1
            tmpSet.utility = 1;
            // init the isUsed flag
            tmpSet.isUsed = true;


            //preference based model

            // init the preference structures
            Map<TPS_PreferenceParameters.ShoppingPreferenceAccessibility, PreferenceSet> tmpPrefAcc = new HashMap<>();
            this.preferencesAccess.put(s, tmpPrefAcc);
            for (TPS_PreferenceParameters.ShoppingPreferenceAccessibility e : TPS_PreferenceParameters.ShoppingPreferenceAccessibility.valueArray) {
                tmpSet = new PreferenceSet();
                tmpPrefAcc.put(e, tmpSet);
            }
            //set "Sonstige" to default
            tmpSet = tmpPrefAcc.get(TPS_PreferenceParameters.ShoppingPreferenceAccessibility.Sonstige);
            //default utility= 1
            tmpSet.utility = 1;
            // init the isUsed flag
            tmpSet.isUsed = true;
            tmpSet.sigma = 0;

        }


    }

    private double calcUtility(ChoiceParams param, TPS_Person person) {
        TPS_Household myHH = person.getHousehold();
        double utility = param.constant;  //choice constant
        if (person.getSex() == TPS_Sex.FEMALE) {
            utility += param.female;
        }
        if (person.getAge() >= 65) {
            utility += param.age65;
        } else if (person.getAge() >= 45) {
            utility += param.age45;
        } else if (person.getAge() >= 25) {
            utility += param.age25;
        } else if (person.getAge() >= 18) {
            utility += param.age18;
        }


        if (person.getEducationLevel() >= 3 && person.getEducationLevel() <= 4) {
            utility += param.abi;
        } //FIXME TPS_PersonGroup is no TPS_PersonType
        if (person.getPersonGroup().equals(TPS_PersonGroup.TPS_PersonType.RETIREE)) {
            utility += param.retired;
        }
        if (person.isWorking()) {
            utility += param.working;
        }
        //count adults, children, cars and bikes
        double children = 0;
        double adults = 0;
        double cars = myHH.getNumberOfCars();
        double bikes = 0;
        for (TPS_Person p : myHH.getMembers(TPS_Household.Sorting.NONE)) {
            if (p.getAge() >= 18) {
                adults += 1.0;
            } else {
                children += 1.0;
            }
            if (p.hasBike()) {
                bikes += 1.0;
            }
        }
        utility += adults * param.numAdults;
        utility += children > 0 ? param.hasChildren : 0;
        utility += cars > 0 ? param.hasCars : 0;
        utility += bikes * param.numBikes;

        if (myHH.getIncome() > 4000.0) {
            utility += param.income4000;
        } else if (myHH.getIncome() > 3200.0) {
            utility += param.income3200;
        } else if (myHH.getIncome() > 2000.0) {
            utility += param.income2000;
        } else if (myHH.getIncome() > 1100.0) {
            utility += param.income1100;
        } else if (myHH.getIncome() > 500.0) {
            utility += param.income1100;
        } else {
            utility += param.income500;
        }
        return utility;
    }

    /**
     * Method to precompute the preference values
     *
     * @param parameters
     */
    public void computePreferences(TPS_PreferenceParameters parameters, TPS_Person person) {

        PreferenceSet tmpSet;
        for (TPS_PreferenceParameters.ShoppingClass s : TPS_PreferenceParameters.ShoppingClass.values()) {
            //first the supply modell
            for (TPS_PreferenceParameters.ShoppingPreferenceSupply e : TPS_PreferenceParameters.ShoppingPreferenceSupply
                    .values()) {
                ChoiceParams param = parameters.paramsSupply.get(s).get(e);
                tmpSet = this.preferencesSupply.get(s).get(e);
                tmpSet.isUsed = param.isUsed;
                if (tmpSet.isUsed) {
                    //store the sigma
                    tmpSet.sigma = param.sigma;
                    //compute and store the utility
                    tmpSet.utility = this.calcUtility(param, person);

                }
            }
            //now the accessibility
            for (TPS_PreferenceParameters.ShoppingPreferenceAccessibility e : TPS_PreferenceParameters.ShoppingPreferenceAccessibility
                    .values()) {
                ChoiceParams param = parameters.paramsAccess.get(s).get(e);
                tmpSet = this.preferencesAccess.get(s).get(e);
                tmpSet.isUsed = param.isUsed;
                if (tmpSet.isUsed) {
                    //store the sigma
                    tmpSet.sigma = param.sigma;
                    //compute and store the utility
                    tmpSet.utility = this.calcUtility(param, person);

                }
            }
        }
    }

    class PreferenceSet {
        double utility = 0;
        double sigma = 0;
        boolean isUsed = false;
    }
}
