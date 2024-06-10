package de.dlr.ivf.tapas.model.choice;

import java.util.Collection;


/**
 * The DiscreteDistributionFactory class provides methods for creating and manipulating discrete probability distributions.
 *
 * @param <T> the type of the discrete variables in the distributions
 */
public class DiscreteDistributionFactory<T> {

    /**
     * Returns a new uniform normalized discrete distribution based on the current set of discrete variables.
     * The uniform normalized distribution assigns equal probability to each discrete variable,
     * ensuring that the sum of all probabilities is equal to 1.
     *
     * @return the new uniform normalized discrete distribution
     */
    public DiscreteDistribution<T> newUniformNormalizedDiscreteDistribution(Collection<T> discreteVariables){

        DiscreteDistribution<T> distribution = new DiscreteDistribution<>();

        double uniformProbability = 1.0 / discreteVariables.size();

        for (T variable : discreteVariables) {
            DiscreteProbability<T> probability = new DiscreteProbability<>(variable, uniformProbability);
            distribution.addProbability(probability);
        }

        return distribution;
    }

    /**
     * Returns a new normalized discrete distribution based on the given source distribution.
     * The normalized distribution is formed by dividing each probability in the source distribution by the total probability,
     * ensuring that the sum of all probabilities in the normalized distribution is equal to 1.
     *
     * @param sourceDistribution the source discrete distribution
     * @return the new normalized discrete distribution
     */
    public DiscreteDistribution<T> newNormalizedDiscreteDistribution(DiscreteDistribution<T> sourceDistribution){

        DiscreteDistribution<T> normalizedDistribution = new DiscreteDistribution<>();
        Collection<DiscreteProbability<T>> sourceProbabilities = sourceDistribution.getProbabilities();
        double totalProbability = 0.0;

        for (DiscreteProbability<T> sourceProbability : sourceProbabilities) {
            totalProbability += sourceProbability.probability();
        }

        for (DiscreteProbability<T> sourceProbability : sourceProbabilities) {
            double normalizedProbability = sourceProbability.probability() / totalProbability;
            DiscreteProbability<T> normalized = new DiscreteProbability<>(sourceProbability.discreteVariable(), normalizedProbability);
            normalizedDistribution.addProbability(normalized);
        }

        return normalizedDistribution;
    }

    /**
     * Returns an empty discrete distribution.
     *
     * @return the empty discrete distribution
     */
    public DiscreteDistribution<T> emptyDistribution(){

        return new DiscreteDistribution<>();
    }
}
