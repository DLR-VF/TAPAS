package de.dlr.ivf.tapas.model.mode;

import java.util.Optional;
import java.util.function.Predicate;

public interface SharingDelegator<T> {
    Optional<T> request(int start_id, int end_id, Predicate<T> filter);
    void checkIn(int end_id, T used_vehicle);
    void checkOut(int start_id, T requested_vehicle);
}
