package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.execution.sequential.io.TPS_WritableTrip;

public interface TPS_TripWriter {
    void writeTrip(TPS_WritableTrip trip) throws Exception;
    void finish();
}
