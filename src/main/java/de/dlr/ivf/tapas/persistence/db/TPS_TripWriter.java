package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.plan.state.TPS_WritableTrip;

import java.io.IOException;

public interface TPS_TripWriter {
    void writeTrip(TPS_WritableTrip trip) throws IOException;
    void finish();
}
