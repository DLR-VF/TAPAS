package de.dlr.ivf.api.io.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;

import java.sql.Connection;

public class ConnectionPool{

    private final HikariDataSource connectionPool;
    public ConnectionPool(ConnectionDetails connectionDetails){
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionDetails.getUrl());
        config.setUsername(connectionDetails.getLogin().getUser());
        config.setPassword(connectionDetails.getLogin().getPassword());
        config.setMaximumPoolSize(4);

        this.connectionPool = new HikariDataSource(config);
    }

    public Connection borrowObject(){
        try {
            Connection connection = connectionPool.getConnection();
            if(connection == null){
                throw new RuntimeException("Connection pool didnt return a connection");
            }
            return connection;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void returnObject(Connection connection){
        try {
            connection.close();
        } catch (Exception e) {
            System.err.println("Error returning the connection to the pool.");
        }
    }

    public void shutDown(){
        connectionPool.close();
    }
}
