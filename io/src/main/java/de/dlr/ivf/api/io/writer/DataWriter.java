package de.dlr.ivf.api.io.writer;

import java.util.Collection;

public interface DataWriter<S> {

    void write(S objectToWrite);

    default void write(Collection<S> objectsToWrite){
        objectsToWrite.forEach(this::write);
    }

    void close();
}
