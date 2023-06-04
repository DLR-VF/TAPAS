package de.dlr.ivf.api.io;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class JdbcConnectionManager {

    private final Map<ConnectionRequest, Connection> connections;

    private final ConnectionProvider<Connection> connectionProvider;

    public JdbcConnectionManager(ConnectionProvider<Connection> connectionProvider){
        this.connections = new HashMap<>();
        this.connectionProvider = connectionProvider;
    }

    //todo override hashcode and equals method in ConnectionRequest.
    public Connection getConnection(ConnectionRequest connectionRequest) {

        Connection connection = connections.get(connectionRequest);

        try {
            if (connection == null || connection.isClosed()) {
                connection = connectionProvider.get(connectionRequest.getConnectionDetails());
                connections.put(connectionRequest, connection);
            }
        }catch (SQLException e){
            String message = "An error occurred during connection request";
            System.err.println(message);
            e.printStackTrace();
            throw new RuntimeException(message, e);
        }

        return connection;
    }

    public void shutDown() {
        for(Map.Entry<ConnectionRequest, Connection> entry : connections.entrySet()){

            System.out.println("Closing connection for: "+entry.getKey().getRequestingClass().getSimpleName());
            try {
                entry.getValue().close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
