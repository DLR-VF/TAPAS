package de.dlr.ivf.tapas.model.choice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * The DiscreteDistributionFactory class provides methods for creating and manipulating discrete probability distributions.
 *
 * @param <T> the type of the discrete variables
 */
public class DiscreteDistributionFactory<T> {

    private final List<T> discreteVariables;

    public DiscreteDistributionFactory(Collection<T> discreteVariables){
        this.discreteVariables = new ArrayList<>(discreteVariables);
    }


    /**
     * Returns a new uniform normalized discrete distribution based on the current set of discrete variables.
     * The uniform normalized distribution assigns equal probability to each discrete variable,
     * ensuring that the sum of all probabilities is equal to 1.
     *
     * @return the new uniform normalized discrete distribution
     */
    public DiscreteDistribution<T> newUniformNormalizedDiscreteDistribution(){

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
            totalProbability += sourceProbability.getProbability();
        }

        for (DiscreteProbability<T> sourceProbability : sourceProbabilities) {
            double normalizedProbability = sourceProbability.getProbability() / totalProbability;
            DiscreteProbability<T> normalized = new DiscreteProbability<>(sourceProbability.getDiscreteVariable(), normalizedProbability);
            normalizedDistribution.addProbability(normalized);
        }

        return normalizedDistribution;
    }

    /**
     * Returns the collection of discrete variables.
     *
     * @return the collection of discrete variables
     */
    public Collection<T> getDiscreteVariables(){
        return discreteVariables;
    }

    public DiscreteDistribution<T> emptyDistribution(){
        return new DiscreteDistribution<>();
    }
}
