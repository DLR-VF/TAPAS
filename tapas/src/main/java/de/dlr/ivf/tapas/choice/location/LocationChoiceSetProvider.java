package de.dlr.ivf.tapas.choice.location;

public interface LocationChoiceSetProvider<T> {
    LocationChoiceSet buildSet(T context);
}
