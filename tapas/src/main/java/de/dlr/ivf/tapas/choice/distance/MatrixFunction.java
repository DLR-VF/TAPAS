package de.dlr.ivf.tapas.choice.distance;

import de.dlr.ivf.tapas.model.location.Locatable;

public interface MatrixFunction {

    double apply(Locatable start, Locatable end);
}
