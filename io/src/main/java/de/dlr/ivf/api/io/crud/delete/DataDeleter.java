package de.dlr.ivf.api.io.crud.delete;

import java.util.Collection;

public interface DataDeleter<T> {
    void delete(T objectToDelete);

    default void deleteAll(Collection<T> objectsToDelete){
        objectsToDelete.forEach(this::delete);
    }
}
