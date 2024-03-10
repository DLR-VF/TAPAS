package de.dlr.ivf.tapas.model.choice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DiscreteProbability<T> {

    private final T discreteVariable;
    private final double probability;
}
