package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.DataReader;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.implementation.NonClosingConnectionJdbcReader;
import de.dlr.ivf.api.io.implementation.ResultSetConverter;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.function.Supplier;

public class ServersJdbcDao implements ServersDao {

    private final DataSource simulationsTable;

    private final DataReader<ResultSet> dataReader;

    private final Converter<ResultSet, ServerEntry> objectFactory;

    public ServersJdbcDao(Supplier<Connection> connectionSupplier, DataSource simulationsTable){
        this.simulationsTable = simulationsTable;
        this.dataReader = new NonClosingConnectionJdbcReader(connectionSupplier);
        this.objectFactory = new ResultSetConverter<>(ServerEntry.class, ServerEntry::new);
    }
    @Override
    public Collection<ServerEntry> load() {
        return dataReader.read(objectFactory, simulationsTable);
    }

    @Override
    public void removeServer(ServerEntry serverEntry) {
        System.out.println("not implemented yet");
    }

    @Override
    public void save(ServerEntry serverEntry) {

    }

    @Override
    public void update(ServerEntry serverEntry) {

    }
}
