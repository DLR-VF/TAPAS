package de.dlr.ivf.tapas.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RandomizerTest {

    @Test
    void randomGumbelDistributionAlphaTest() {

        assertThrows(IllegalArgumentException.class, () -> Randomizer.randomGumbelDistribution(() -> Randomizer.random(), 1,-1));
        assertThrows(IllegalArgumentException.class, () -> Randomizer.randomGumbelDistribution(() -> Randomizer.random(), 1,0));
    }


    //should we be using an accepted margin of error due to floating point precisions?
    @Test
    void randomGumbelDistributionResultValidation(){

        double mu = 1;
        double alpha = 1;
        double rand = 0.5;
        assertEquals(Randomizer.randomGumbelDistribution(() -> rand, mu, alpha), mu - alpha* (Math.log(-Math.log(rand))));

    }

    @Test
    void randomGaussianDistributionValidStd(){
        assertThrows(IllegalArgumentException.class, () -> Randomizer.randomGaussianDistribution(() -> Randomizer.randomGaussian(), 1,-1));
    }

    @Test
    void randomGaussianDistributionResultValidation(){
        double rand = 0.5;
        double std = 1;
        double mean = 2;
        assertEquals(Randomizer.randomGaussianDistribution(() -> rand, mean, std), rand * std + mean);
    }
}