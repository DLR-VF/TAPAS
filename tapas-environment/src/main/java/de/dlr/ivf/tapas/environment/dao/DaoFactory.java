package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.environment.dao.implementation.ParametersJdbcDao;
import de.dlr.ivf.tapas.environment.dao.implementation.ServersJdbcDao;
import de.dlr.ivf.tapas.environment.dao.implementation.SimulationsJdbcDao;

import java.sql.Connection;
import java.util.function.Supplier;

public class DaoFactory {

    public static SimulationsDao newJdbcSimulationsDao(ConnectionPool connectionSupplier, DataSource simulationsTable){

        return new SimulationsJdbcDao(connectionSupplier, simulationsTable);

    }

    public static ServersDao newJdbcServersDao(ConnectionPool connectionPool, DataSource serversTable){

        return new ServersJdbcDao(connectionPool, serversTable);

    }

    public static ParametersDao newJdbcParametersDao(ConnectionPool connectionSupplier, DataSource parametersTable){
        return new ParametersJdbcDao(connectionSupplier, parametersTable);
    }
}
