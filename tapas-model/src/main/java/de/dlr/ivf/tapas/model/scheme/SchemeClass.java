package de.dlr.ivf.tapas.model.scheme;

import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;

public record SchemeClass(
    int id,
    double mean,
    double deviation,
    DiscreteDistribution<Scheme> schemeDistribution
){}
