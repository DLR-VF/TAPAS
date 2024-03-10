package de.dlr.ivf.tapas.model.choice;

import java.util.ArrayList;
import java.util.Collection;

public class DiscreteDistribution<T> {

    private final Collection<DiscreteProbability<T>> discreteProbabilities;

    public DiscreteDistribution(){
        this.discreteProbabilities = new ArrayList<>();
    }

    public Collection<DiscreteProbability<T>> getProbabilities(){
        return discreteProbabilities;
    }

    public void addProbability(DiscreteProbability<T> probability) {
        discreteProbabilities.add(probability);
    }
}
