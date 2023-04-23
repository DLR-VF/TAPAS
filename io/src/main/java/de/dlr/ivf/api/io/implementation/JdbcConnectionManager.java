package de.dlr.ivf.api.io.implementation;

import de.dlr.ivf.api.io.ConnectionManager;
import de.dlr.ivf.api.io.ConnectionProvider;
import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;


/**
 * A light JDBC connection provider that only returns a connection to a {@link RemoteDataSource} that has
 * previously been registered.
 *
 * @author Alain Schengen
 */
public class JdbcConnectionManager implements ConnectionManager<Connection> {

    /**
     * Contains all {@link RemoteDataSource} registered in the manager.
     */
    private final Set<RemoteDataSource> remoteDataSources;

    /**
     * The actual {@link Connection}-factory that is being delegated to.
     */
    private final ConnectionProvider<Connection> connectionProvider;

    public JdbcConnectionManager(ConnectionProvider<Connection> connectionProvider){
        this.remoteDataSources = new HashSet<>();
        this.connectionProvider = connectionProvider;
    }

    /**
     * This method first checks whether the {@link RemoteDataSource} has been registered with this {@link ConnectionManager}.
     * If not, an {@link IllegalArgumentException} is thrown otherwise the call will be delegated to the injected {@link ConnectionProvider}
     *
     * @param remoteDataSource the {@link RemoteDataSource} to get a {@link Connection} to.
     * @return a {@link Connection} to access the {@link RemoteDataSource}.
     */
    @Override
    public Connection get(RemoteDataSource remoteDataSource) {


        RemoteDataSource dataSource = remoteDataSources.stream()
                .filter(remoteDataSource::equals)
                .findAny()
                .orElseThrow(
                        () -> new IllegalArgumentException("Datasource '%1$s' has not been registered to connection provider"
                                .formatted(remoteDataSource.getUri())
                ));

        return connectionProvider.get(dataSource);
    }

    public void addDataSource(RemoteDataSource dataSource) {

        if (remoteDataSources.contains(dataSource))
            throw new IllegalArgumentException("Remote datasource already added");
        else
            this.remoteDataSources.add(dataSource);
    }
}
