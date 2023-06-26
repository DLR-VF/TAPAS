package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.crud.read.DataReaderFactory;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;

public class ServersJdbcDao implements ServersDao {

    private final DataSource simulationsTable;

    private final Converter<ResultSet, ServerEntry> objectFactory;
    private final ConnectionPool connectionPool;

    public ServersJdbcDao(ConnectionPool connectionPool, DataSource simulationsTable){
        this.simulationsTable = simulationsTable;
        this.connectionPool = connectionPool;
        this.objectFactory = new ResultSetConverter<>(new ColumnToFieldMapping<>(ServerEntry.class), ServerEntry::new);
    }
    @Override
    public Collection<ServerEntry> load() {
        Connection connection = connectionPool.borrowObject();

        var dataReader = DataReaderFactory.newJdbcReader(connection);
        var serverEntries = dataReader.read(objectFactory, simulationsTable);
        connectionPool.returnObject(connection);

        return serverEntries;
    }

    @Override
    public void removeServers(Collection<ServerEntry> serverEntries) {
        System.out.println("not implemented yet");
    }

    @Override
    public void insert(ServerEntry serverEntry) {

    }

    @Override
    public void update(ServerEntry serverEntry) {
        Collection<ServerEntry> serverEntries = this.load();


    }
}
