/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.function.DoubleSupplier;

/**
 * This class creates random values.
 * The generator uses the Apache Commons Math MersenneTwister implementation.
 *
 * @author mark_ma
 */
public class Randomizer {

    /**
     * Static random value is set if it is defined in a parameter file
     */
    private static final double RANDOM_VALUE;


    private static final boolean FLAG_INFLUENCE_RANDOM_NUMBER = false;

    /**
     * the random generator
     */
    private static final MersenneTwister generator;

    static {
        generator = new MersenneTwister();//new Random(RANDOM_SEED_NUMBER);
        RANDOM_VALUE = generator.nextDouble();
    }

    /**
     * This method checks if the random value is fix. If it is fix a default random value is returned, otherwise a real
     * random value;
     *
     * @return random value
     */
    public static synchronized double random() {
//		if (ParamFlag.FLAG_INFLUENCE_RANDOM_NUMBER.isTrue()) {
        if (FLAG_INFLUENCE_RANDOM_NUMBER) {
            return RANDOM_VALUE; //return the fair dice roll! *OMG*
        }
        return generator.nextDouble();
    }

    public static synchronized double randomGaussian() {
        return generator.nextGaussian();
    }


    /**
     * This method returns a gaussian (aka "normal")  distributed random value with a given mean and an given standard deviation
     *
     * @param mean   the desired mean
     * @param stdDev the desired std deviation
     * @return
     */

    public static synchronized double randomGaussianDistribution(DoubleSupplier gen, double mean, double stdDev) {
        if (stdDev < 0) {
            throw new IllegalArgumentException("The standard deviation cannot be < 0");
        }

        return gen.getAsDouble() * stdDev + mean;//generator.nextGaussian()*stdDev + mean;
    }

    /**
     * This method returns a gumbel (aka "Extreme value distribution")  distributed random value with a given mu = location and alpha = scale
     *
     * @param mu    the desired location
     * @param alpha the desired scale
     * @return
     */

    public static synchronized double randomGumbelDistribution(DoubleSupplier gen, double mu, double alpha) {
        if (alpha <= 0) {
            throw new IllegalArgumentException("The scaling parameter alpha must be > 0");
        }

        double rand = gen.getAsDouble();

        if (rand < Double.MIN_NORMAL) rand = Double.MIN_NORMAL;// avoid log(0)!

        return mu - alpha * (Math.log(-Math.log(rand)));
    }
}
