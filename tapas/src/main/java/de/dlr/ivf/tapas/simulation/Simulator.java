package de.dlr.ivf.tapas.simulation;

public interface Simulator<S,T> {

    T process(S entity);
}
