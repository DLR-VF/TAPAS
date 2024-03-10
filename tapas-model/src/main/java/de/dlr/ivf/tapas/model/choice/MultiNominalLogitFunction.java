package de.dlr.ivf.tapas.model.choice;


public interface MultiNominalLogitFunction<T> {

    DiscreteDistribution<T> apply(DiscreteDistribution<T> distribution);
}
