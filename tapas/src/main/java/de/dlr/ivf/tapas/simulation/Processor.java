package de.dlr.ivf.tapas.simulation;

public interface Processor<S,T> {

    T process(S entity);
}
