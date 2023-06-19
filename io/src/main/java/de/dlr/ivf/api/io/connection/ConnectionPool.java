package de.dlr.ivf.api.io.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.api.io.connection.implementation.ConnectionFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.sql.Connection;

public class ConnectionPool{

   // private final ObjectPool<Connection> connectionPool;
    private final HikariDataSource connectionPool;
    public ConnectionPool(ConnectionDetails connectionDetails){
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionDetails.getUrl());
        config.setUsername(connectionDetails.getLogin().getUser());
        config.setPassword(connectionDetails.getLogin().getPassword());
        config.setMaximumPoolSize(4);

        this.connectionPool = new HikariDataSource(config);


        //todo think about making this parameterizable
//        poolConfig.setMaxTotal(5);
//        poolConfig.setBlockWhenExhausted(true);
//        //poolConfig.setMaxWaitMillis(10000);
//        poolConfig.setTestOnBorrow(true);
//        poolConfig.setTestOnCreate(true);
//        poolConfig.setTestOnReturn(true);
//        poolConfig.setMinIdle(2);



        //this.connectionPool = new GenericObjectPool<>(new ConnectionFactory(connectionDetails), poolConfig);

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
