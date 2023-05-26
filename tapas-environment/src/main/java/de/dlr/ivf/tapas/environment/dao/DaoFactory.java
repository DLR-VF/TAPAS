package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.tapas.environment.dao.implementation.ServersJdbcDao;
import de.dlr.ivf.tapas.environment.dao.implementation.SimulationsJdbcDao;

import java.sql.Connection;
import java.util.function.Supplier;

public class DaoFactory {

    public static SimulationsDao newJdbcSimulationsDao(Supplier<Connection> connectionSupplier, DataSource simulationsTable){

        return new SimulationsJdbcDao(connectionSupplier, simulationsTable);

    }

    public static ServersDao newJdbcServersDao(Supplier<Connection> connectionSupplier, DataSource serversTable){

        return new ServersJdbcDao(connectionSupplier, serversTable);

    }
}
