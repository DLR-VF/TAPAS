package de.dlr.ivf.tapas.model.choice;

import java.util.ArrayList;
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

        return newNormalizedDiscreteDistribution(sourceDistribution.getProbabilities());
    }

    /**
     * Returns a new normalized discrete distribution based on the given source probabilities.
     * The normalized distribution is formed by dividing each probability in the source probabilities by the total probability,
     * ensuring that the sum of all probabilities in the normalized distribution is equal to 1.
     *
     * @param sourceProbabilities the source collection of discrete probabilities
     * @return the new normalized discrete distribution
     */
    public DiscreteDistribution<T> newNormalizedDiscreteDistribution(Collection<DiscreteProbability<T>> sourceProbabilities){

        Collection<DiscreteProbability<T>> normalizedProbabilities = calculateNormalizedProbabilities(sourceProbabilities);

        DiscreteDistribution<T> normalizedDistribution = new DiscreteDistribution<>();

        normalizedProbabilities.forEach(normalizedDistribution::addProbability);

        return normalizedDistribution;
    }

    /**
     * Calculates the normalized probabilities of a collection of discrete probabilities.
     *
     * @param sourceProbabilities the source collection of discrete probabilities
     *
     * @return the collection of normalized discrete probabilities
     */
    private Collection<DiscreteProbability<T>> calculateNormalizedProbabilities(Collection<DiscreteProbability<T>> sourceProbabilities) {

        Collection<DiscreteProbability<T>> normalizedProbabilities = new ArrayList<>();

        double totalProbability = 0.0;

        for (DiscreteProbability<T> sourceProbability : sourceProbabilities) {
            totalProbability += sourceProbability.probability();
        }

        for (DiscreteProbability<T> sourceProbability : sourceProbabilities) {
            double normalizedProbability = Math.clamp(sourceProbability.probability() / totalProbability, 0.0, 1.0);
            DiscreteProbability<T> normalized = new DiscreteProbability<>(sourceProbability.discreteVariable(), normalizedProbability);
            normalizedProbabilities.add(normalized);
        }

        return normalizedProbabilities;
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
