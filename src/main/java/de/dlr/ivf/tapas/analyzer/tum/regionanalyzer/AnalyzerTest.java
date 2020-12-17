/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategory;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.RegionCode;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.TripIntention;

import java.util.ArrayList;
import java.util.EnumSet;
//import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Job;

@SuppressWarnings("rawtypes")
public class AnalyzerTest {

    public static void main(String[] args) {

        // testFiltered();
        // testSingleSplit();
        // testEnums();
        // testEnumSets();
        // testHashCodes();
        testCategoryCombination();
    }

    public static void testCategoryCombination() {
        // System.out.println(Mode.class.getName());
        ArrayList<CategoryCombination> cats = CategoryCombination.listAllCombinations(Categories.Mode,
                Categories.DistanceCategory, Categories.PersonGroup, Categories.TripIntention);
        // System.out.println(cats);
        System.out.println(cats.size());

        System.out.println(Categories.isValid(DistanceCategory.CAT_1));
        System.out.println(Categories.isValid(de.dlr.ivf.tapas.analyzer.inputfileconverter.Job.JOB_1));

    }

    public static void testEnumSets() {
        EnumSet<Categories> es1 = EnumSet.of(Categories.RegionCode, Categories.Mode);
        EnumSet<Categories> es2 = EnumSet.of(Categories.Mode, Categories.RegionCode);

        System.out.println(es1 == es2);
        System.out.println(es1.equals(es2));
        System.out.println(es1.hashCode() == es2.hashCode());
    }

    // public static void testHashCodes() {
    // CategoryCombination cc = new CategoryCombination(Job.JOB_1,
    // RegionCode.REGION_0);
    // CategoryCombination cc2 = new CategoryCombination(RegionCode.REGION_0,
    // Job.JOB_1);
    //
    // CategoryCombination cc3 = new CategoryCombination(Job.JOB_10,
    // RegionCode.REGION_0);
    // HashMap<CategoryCombination, Integer> hm = new
    // HashMap<CategoryCombination, Integer>();
    //
    // hm.put(cc, 1);
    // hm.put(cc2, 2);
    // hm.put(cc3, 3);
    //
    // System.out.println(hm);
    // System.out.println("contains 1 " + hm.containsKey(cc));
    // System.out.println("contains 2 " + hm.containsKey(cc2));
    // System.out.println("contains 3 " + hm.cont
    //
    // }

    public static void testEnums() {

        Enum enum1 = RegionCode.REGION_0;
        System.out.println(enum1 == RegionCode.REGION_0);
        Enum enum3 = RegionCode.REGION_0;
        System.out.println(enum1 == enum3);

        Enum enum2 = TripIntention.TRIP_31;
        System.out.println(enum1 == enum2);
    }

}
