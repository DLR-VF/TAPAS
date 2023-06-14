package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
import de.dlr.ivf.api.io.crud.read.DataReader;
import de.dlr.ivf.api.io.crud.read.DataReaderFactory;
import de.dlr.ivf.api.io.crud.write.DataWriter;
import de.dlr.ivf.api.io.crud.write.DataWriterFactory;
import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dto.ParameterEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;

public class ParametersJdbcDao implements ParametersDao {

    private final DataSource paramTable;
    private final ResultSetConverter<ParameterEntry> objectFactory;
    private final ConnectionPool connectionSupplier;

    public ParametersJdbcDao(ConnectionPool connectionSupplier, DataSource paramTable){
        this.paramTable = paramTable;
        this.connectionSupplier = connectionSupplier;
        this.objectFactory = new ResultSetConverter<>(new ColumnToFieldMapping<>(ParameterEntry.class), ParameterEntry::new);
    }

    @Override
    public Collection<ParameterEntry> load(int id) {

        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
        //var params = reader.read(objectFactory,paramTable,new Filter())
        return null;//dataReader.read(objectFactory,);
    }

    @Override
    public void insert(Collection<ParameterEntry> parameters) {
        Connection connection = connectionSupplier.borrowObject();
        DataWriter<ParameterEntry, Void> writer = DataWriterFactory.newJdbcBatchWriter(paramTable, connection, ParameterEntry.class);
        writer.write(parameters);
        try {
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }finally {
            connectionSupplier.returnObject(connection);
        }
    }
}
