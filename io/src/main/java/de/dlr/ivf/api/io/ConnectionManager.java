package de.dlr.ivf.api.io;

import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;
import de.dlr.ivf.api.io.implementation.BaseJdbcConnectionProvider;
import de.dlr.ivf.api.io.implementation.JdbcConnectionManager;

import java.sql.Connection;

/**
 * A {@link ConnectionManager} allows managing remote data sources and as a {@link ConnectionProvider} should be
 * used as a gatekeeper for establishing new connections.
 * Note: The interface provides static factory methods which might be exported to a separate module and hence
 * might be removed in the future.
 *
 * @param <T> type of connection
 *
 * @author Alain Schengen
 */
public interface ConnectionManager<T> extends ConnectionProvider<T> {

    /**
     * Register a {@link RemoteDataSource} with this {@link ConnectionManager}.
     *
     * @param dataSource to register with the {@link ConnectionManager}.
     */
    void addDataSource(RemoteDataSource dataSource);


    /**
     * Factory method for a {@link JdbcConnectionManager} using a {@link BaseJdbcConnectionProvider} connection factory.
     *
     * @return
     */
    static ConnectionManager<Connection> newJdbcConnectionManager(){
        return new JdbcConnectionManager(new BaseJdbcConnectionProvider());
    }
}

