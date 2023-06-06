package de.dlr.ivf.api.io.writer;

import java.util.Collection;
import java.util.stream.Collectors;

public interface DataWriter<S, T> {

    T write(S objectToWrite);

    default Collection<T> write(Collection<S> objectsToWrite){
        return objectsToWrite.stream()
                .map(this::write)
                .collect(Collectors.toList());
    }
}
