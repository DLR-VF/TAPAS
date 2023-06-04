package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
import de.dlr.ivf.api.io.reader.DataReader;
import de.dlr.ivf.api.io.reader.DataReaderFactory;
import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dto.ParameterEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.function.Supplier;

public class ParametersJdbcDao implements ParametersDao {

    private final DataSource paramTable;
    private final DataReader<ResultSet> dataReader;
    private final ResultSetConverter<ParameterEntry> objectFactory;

    public ParametersJdbcDao(Supplier<Connection> connectionSupplier, DataSource paramTable){
        this.paramTable = paramTable;
        this.dataReader = DataReaderFactory.newJdbcReader(connectionSupplier);
        this.objectFactory = new ResultSetConverter<>(new ColumnToFieldMapping<>(ParameterEntry.class), ParameterEntry::new);
    }

    @Override
    public Collection<ParameterEntry> load(int id) {
        return null;//dataReader.read(objectFactory,);
    }

    @Override
    public void insert(Collection<ParameterEntry> parameters) {

    }
}
