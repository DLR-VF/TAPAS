package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.reader.DataReader;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.reader.DataReaderFactory;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
import de.dlr.ivf.api.io.writer.DataWriter;
import de.dlr.ivf.api.io.writer.DataWriterFactory;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.function.Supplier;

public class SimulationsJdbcDao implements SimulationsDao {

    private final DataSource simulationsTable;

    private final DataReader<ResultSet> dataReader;

    private final DataWriter<SimulationEntry,Integer> dataWriter;

    private final Converter<ResultSet, SimulationEntry> objectFactory;

    public SimulationsJdbcDao(Supplier<Connection> connectionSupplier, DataSource simulationsTable) {
        this.simulationsTable = simulationsTable;
        this.dataReader = DataReaderFactory.newOpenConnectionJdbcReader(connectionSupplier);
        this.dataWriter = DataWriterFactory.newIdReturningSimpleJdbcWriter(simulationsTable,connectionSupplier, SimulationEntry.class);
        this.objectFactory = new ResultSetConverter<>(new ColumnToFieldMapping<>(SimulationEntry.class), SimulationEntry::new);
    }

    @Override
    public Collection<SimulationEntry> load() {

        return dataReader.read(objectFactory, simulationsTable);
    }

    @Override
    public int save(SimulationEntry simulationEntry) {
        return dataWriter.write(simulationEntry);
    }

    @Override
    public void update(int simId, SimulationEntry simulation) {

    }
}
