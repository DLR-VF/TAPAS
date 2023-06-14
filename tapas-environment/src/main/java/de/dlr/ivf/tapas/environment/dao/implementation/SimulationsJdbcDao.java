package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.crud.read.DataReader;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.crud.read.DataReaderFactory;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
import de.dlr.ivf.api.io.crud.update.DataUpdater;
import de.dlr.ivf.api.io.crud.update.DataUpdaterFactory;
import de.dlr.ivf.api.io.crud.write.DataWriter;
import de.dlr.ivf.api.io.crud.write.DataWriterFactory;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;

public class SimulationsJdbcDao implements SimulationsDao {

    private final DataSource simulationsTable;
    private final Converter<ResultSet, SimulationEntry> objectFactory;
    private final ConnectionPool connectionPool;

    public SimulationsJdbcDao(ConnectionPool connectionPool, DataSource simulationsTable) {
        this.simulationsTable = simulationsTable;
        this.connectionPool = connectionPool;
        this.objectFactory = new ResultSetConverter<>(new ColumnToFieldMapping<>(SimulationEntry.class), SimulationEntry::new);
    }

    @Override
    public Collection<SimulationEntry> load() {
        Connection connection = connectionPool.borrowObject();

        DataReader<ResultSet> dataReader = DataReaderFactory.newJdbcReader(connection);
        var simulations = dataReader.read(objectFactory, simulationsTable);
        connectionPool.returnObject(connection);

        return simulations;
    }

    @Override
    public int save(SimulationEntry simulationEntry) {

        Connection connection = connectionPool.borrowObject();
        DataWriter<SimulationEntry, Integer> writer =
                DataWriterFactory.newIdReturningSimpleJdbcWriter(simulationsTable,connection, SimulationEntry.class);

        int simulationId = writer.write(simulationEntry);
        connectionPool.returnObject(connection);

        return simulationId;
    }

    @Override
    public void update(int simId, SimulationEntry simulation) {
        Connection connection = connectionPool.borrowObject();

        DataUpdater<SimulationEntry> updater =
                DataUpdaterFactory.newSimpleJdbcUpdater(simulationsTable, connection, SimulationEntry.class);
        updater.update(simId, simulation);
        connectionPool.returnObject(connection);
    }

    @Override
    public void remove(int simId) {
        Connection connection = connectionPool.borrowObject();
        //try(Prep)
    }
}
