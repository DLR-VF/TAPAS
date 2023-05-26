package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.DataReader;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.implementation.NonClosingConnectionJdbcReader;
import de.dlr.ivf.api.io.implementation.ResultSetConverter;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.function.Supplier;

public class SimulationsJdbcDao implements SimulationsDao {

    private final DataSource simulationsTable;

    private final DataReader<ResultSet> dataReader;

    private final Converter<ResultSet, SimulationEntry> objectFactory;

    public SimulationsJdbcDao(Supplier<Connection> connectionSupplier, DataSource simulationsTable) {
        this.simulationsTable = simulationsTable;
        this.dataReader = new NonClosingConnectionJdbcReader(connectionSupplier);
        this.objectFactory = new ResultSetConverter<>(SimulationEntry.class, SimulationEntry::new);
    }

    @Override
    public Collection<SimulationEntry> load() {

        return dataReader.read(objectFactory, simulationsTable);
    }
}
