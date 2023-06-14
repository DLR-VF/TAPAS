package de.dlr.ivf.api.io.connection.implementation;


import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.api.io.configuration.model.Login;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory extends BasePooledObjectFactory<Connection> {

    private final ConnectionDetails connectionDetails;

    public ConnectionFactory(ConnectionDetails connectionDetails){
        this.connectionDetails = connectionDetails;

    }
    @Override
    public Connection create() throws Exception {

        String url = connectionDetails.getUrl();
        Login login = connectionDetails.getLogin();

        try {
            return DriverManager.getConnection(url, login.getUser(), login.getPassword());

        } catch (SQLException e) {
            String message = """
                    Unable to establish JDBC connection to '%1$s'.
                    '%1$s' might not be accessible and/or supplied login credentials are wrong.
                    """
                    .formatted(url);
            System.err.println(message);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public void destroyObject(PooledObject<Connection> pooledConnection) throws Exception {
        super.destroyObject(pooledConnection);

        try {
            pooledConnection.getObject().close();
        }catch (SQLException e){
            System.err.println("Unable to close JDBC connection.");
        }
    }

    @Override
    public PooledObject<Connection> wrap(Connection connection) {
        return new DefaultPooledObject<>(connection);
    }
}
