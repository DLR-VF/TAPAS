package de.dlr.ivf.tapas.model.choice;

import java.util.Random;

/**
 * The DiscreteChoiceModel class represents a model for making discrete choices based on a given distribution.
 *
 * @param <T> the type of the discrete variable
 */
public class DiscreteChoiceModel<T> {

    private final Random randomNumberGenerator;

    /**
     * Initializes a new instance of the DiscreteChoiceModel class with the given seed.
     *
     * @param seed the seed for the random number generator
     */
    public DiscreteChoiceModel(int seed){
        this.randomNumberGenerator = new Random(seed);
    }

    /**
     * This method makes a choice based on a given discrete distribution. The distribution must be normalized.
     *
     * @param distribution the discrete distribution from which to make a choice
     *
     * @return the chosen discrete variable
     * @throws IllegalArgumentException if the distribution probabilities are invalid
     */
    public T makeChoice(DiscreteDistribution<T> distribution){

        return makeChoice(randomNumberGenerator.nextDouble(), distribution);
    }

    /**
     * This method makes a choice based on a given random number and a discrete distribution.
     *
     * @param randomNumber the random number used to make the choice
     * @param distribution the discrete distribution from which to make a choice
     *
     * @return the chosen discrete variable
     * @throws IllegalArgumentException if the distribution probabilities are invalid
     */
    public T makeChoice(double randomNumber, DiscreteDistribution<T> distribution){

        double cumulativeProbability = 0.0;

        if(randomNumber < 0 || randomNumber > 1){
            throw new IllegalArgumentException("Probability must be between 0 and 1");
        }

        for (DiscreteProbability<T> probability : distribution.getProbabilities()) {
            cumulativeProbability += probability.probability();
            if (randomNumber <= cumulativeProbability) {
                return probability.discreteVariable();
            }
        }
        throw new IllegalArgumentException("Invalid distribution probabilities");
    }
}
