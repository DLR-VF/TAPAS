package de.dlr.ivf.tapas.model.distribution;/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class TPS_DiscreteDistributionTest {

    ArrayList<Character> standardTestSingletons = new ArrayList<>();
    TPS_DiscreteDistribution<Character> standardTestDistribution;

    HashMap<Character, Double> weirdTestMap = new HashMap<>();
    TPS_DiscreteDistribution<Character> weirdTestDistribution;

    @Test
    void draw_DistributionWithZerosRandomValue00_IndexThree() {
        assertEquals(3, weirdTestDistribution.draw(0.0));
    }

    @Test
    void draw_DistributionWithZerosRandomValue01_IndexThree() {
        assertEquals(3, weirdTestDistribution.draw(0.1));
    }

    @Test
    void draw_DistributionWithZerosRandomValue02_IndexThree() {
        assertEquals(3, weirdTestDistribution.draw(0.2));
    }

    @Test
    void draw_DistributionWithZerosRandomValue03_IndexFour() {
        assertEquals(4, weirdTestDistribution.draw(0.3));
    }

    @Test
    void draw_DistributionWithZerosRandomValue06_IndexFour() {
        assertEquals(4, weirdTestDistribution.draw(0.6));
    }

    @Test
    void draw_DistributionWithZerosRandomValue07_IndexSeven() {
        assertEquals(7, weirdTestDistribution.draw(0.7));
    }

    @Test
    void draw_DistributionWithZerosRandomValue09_IndexSeven() {
        assertEquals(7, weirdTestDistribution.draw(0.9));
    }

    @Test
    void draw_DistributionWithZerosRandomValue100_IndexSeven() {
        assertEquals(7, weirdTestDistribution.draw(10));
    }

    @Test
    void draw_DistributionWithZerosRandomValue10_IndexSeven() {
        assertEquals(7, weirdTestDistribution.draw(1.0));
    }

    @Test
    void draw_RandomValueEqualsOneThird_ZerothIndex() {
        assertEquals(0, standardTestDistribution.draw(1 / 3.0));
    }

    @Test
    void draw_RandomValueEqualsOne_LastIndex() {
        assertEquals(2, standardTestDistribution.draw(1));
    }

    @Test
    void draw_RandomValueEqualsTwoThird_FirstIndex() {
        assertEquals(1, standardTestDistribution.draw(2 / 3.0));
    }

    @Test
    void draw_RandomValueEqualsZeroPointFour_FirstIndex() {
        assertEquals(1, standardTestDistribution.draw(0.4));
    }

    @Test
    void draw_RandomValueEqualsZeroPointSeven_LastIndex() {
        assertEquals(2, standardTestDistribution.draw(0.7));
    }

    @Test
    void draw_RandomValueEqualsZero_ZerothIndex() {
        assertEquals(0, standardTestDistribution.draw(0.0));
    }

    @Test
    void draw_RandomValueGreaterThanOne_LastIndex() {
        assertEquals(2, standardTestDistribution.draw(10));
    }

    @Test
    void draw_RandomValueLessThanZero_ZerothIndex() {
        assertEquals(0, standardTestDistribution.draw(-4));
    }

    @BeforeEach
    void init() {
        standardTestSingletons.add('a');
        standardTestSingletons.add('b');
        standardTestSingletons.add('c');
        standardTestDistribution = new TPS_DiscreteDistribution<>(standardTestSingletons);

        weirdTestMap.put('a', 0.0);
        weirdTestMap.put('b', 0.0);
        weirdTestMap.put('c', 0.0);
        weirdTestMap.put('d', 0.2);
        weirdTestMap.put('e', 0.4);
        weirdTestMap.put('f', 0.0);
        weirdTestMap.put('g', 0.0);
        weirdTestMap.put('h', 0.4);
        weirdTestMap.put('i', 0.0);
        weirdTestMap.put('j', 0.0);
        weirdTestDistribution = new TPS_DiscreteDistribution<>(weirdTestMap);
    }

    @Test
    void normalize_NotNormalizedDistributionAndDoNotNormalizeValues_True() {
        standardTestDistribution.setValues(3.0);
        assertTrue(standardTestDistribution.normalize(false));
        assertNotEquals(1, standardTestDistribution.sum());
        assertEquals(1, standardTestDistribution.cumulativeValues[standardTestDistribution.size() - 1]);
        assertArrayEquals(new double[]{1 / 3.0, 2 / 3.0, 1}, standardTestDistribution.cumulativeValues);
        assertArrayEquals(new double[]{3.0, 3.0, 3.0}, standardTestDistribution.values);
    }

    @Test
    void normalize_NotNormalizedDistribution_True() {
        standardTestDistribution.setValues(3.0);
        assertTrue(standardTestDistribution.normalize());
        assertEquals(1, standardTestDistribution.sum());
        assertEquals(1, standardTestDistribution.cumulativeValues[standardTestDistribution.size() - 1]);
        assertArrayEquals(new double[]{1 / 3.0, 2 / 3.0, 1}, standardTestDistribution.cumulativeValues);
        assertArrayEquals(new double[]{1 / 3.0, 1 / 3.0, 1 / 3.0}, standardTestDistribution.values);
    }

    @Test
    void normalize_ZeroDistributionAndDoNotNormalizeValues_False() {
        standardTestDistribution.setValues(0.0);
        assertFalse(standardTestDistribution.normalize(false));
    }

    @Test
    void normalize_ZeroDistribution_False() {
        standardTestDistribution.setValues(0.0);
        assertFalse(standardTestDistribution.normalize());
    }

    @Test
    void setSingletons_TestEqualValues_False() {
        standardTestSingletons.set(1, 'd');
        assertNotEquals(standardTestSingletons, standardTestDistribution.getSingletons());
    }

    @Test
    void setSingletons_TestEqualValues_True() {
        assertEquals(standardTestSingletons, standardTestDistribution.getSingletons());
    }

    @Test
    void setSingletons_TestSameObject_False() {
        assertNotSame(this.standardTestSingletons, this.standardTestDistribution.getSingletons());
    }

    @Test
    void sum_InfinityValues_ThrowRuntimeException() {
        standardTestDistribution.setValues(Double.NEGATIVE_INFINITY);
        assertThrows(RuntimeException.class, () -> standardTestDistribution.sum());
    }

    @Test
    void sum_NaNValues_ThrowRuntimeException() {
        standardTestDistribution.setValues(Double.NaN);
        assertThrows(RuntimeException.class, () -> standardTestDistribution.sum());
    }

    @Test
    void sum_NormalValues_ThrowRuntimeException() {
        assertDoesNotThrow(() -> standardTestDistribution.sum());
    }

    @Test
    void sum_PositiveInfinityValues_ThrowRuntimeException() {
        standardTestDistribution.setValues(Double.POSITIVE_INFINITY);
        assertThrows(RuntimeException.class, () -> standardTestDistribution.sum());
    }

}
