package de.dlr.ivf.api.io;

import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;

/**
 * An interface defining the contract to get a connection of type {@link T} to a specific {@link RemoteDataSource}.
 *
 * @param <T> type of connection
 *
 * @author Alain Schengen
 */
public interface ConnectionProvider<T> {

    T get(ConnectionDetails connector);
}
