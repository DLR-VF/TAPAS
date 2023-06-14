package de.dlr.ivf.tapas.environment.strategy;

import java.io.File;

public interface InsertStrategy<T> {
    void insert(T objectToInsert);
}
