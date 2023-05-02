package de.dlr.ivf.tapas.model.mode;

import java.util.Optional;
import java.util.function.Predicate;

public interface SharingMediator<T> {
    Optional<T> request(Predicate<T> filter);
    void release(T requested_vehicle);
    void checkIn(T used_vehicle);
    void checkOut(T requested_vehicle);
}
