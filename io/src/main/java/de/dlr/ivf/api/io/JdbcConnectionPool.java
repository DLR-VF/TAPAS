package de.dlr.ivf.api.io;

import lombok.Builder;
import lombok.Singular;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Builder
public class JdbcConnectionPool {

    @Singular
    private final Map<ConnectionRequest, Connection> connections;

    private final JdbcConnectionProvider connectionProvider;

    //todo override hashcode and equals method in ConnectionRequest.
    public Connection getConnection(ConnectionRequest connectionRequest) throws SQLException {

        Connection connection = connections.get(connectionRequest);

        if(connection == null || connection.isClosed()){
            connection = connectionProvider.get(connectionRequest.getConnectionDetails());
            connections.put(connectionRequest, connection);
        }

        return connection;
    }
}
