package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.conversion.JavaToSqlTypeConverter;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
import de.dlr.ivf.api.io.util.PreparedStatementContext;
import de.dlr.ivf.api.io.util.PreparedStatementContextFactory;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import de.dlr.ivf.tapas.environment.dao.implementation.ParametersJdbcDao;
import de.dlr.ivf.tapas.environment.dao.implementation.ServersJdbcDao;
import de.dlr.ivf.tapas.environment.dao.implementation.SimulationsJdbcDao;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;

public class DaoFactory {

    public static SimulationsDao newJdbcSimulationsDao(ConnectionPool connectionPool, DataSource simulationsTable){
        PreparedStatementContext context = PreparedStatementContextFactory.newPreparedStatementContext(SimulationEntry.class);
        PreparedStatementContext extendedContext = PreparedStatementContextFactory.newExtendedPreparedStatementContext(SimulationEntry.class);

        Converter<Object, Object> javaToSqlConverter = new JavaToSqlTypeConverter();
        return SimulationsJdbcDao.builder()
                .preparedStatementContext(context)
                .insertParameterSetter(new PreparedStatementParameterSetter<>(context.getIndexedInvocableMethods(), javaToSqlConverter))
                .updateParameterSetter(new PreparedStatementParameterSetter<>(extendedContext.getIndexedInvocableMethods(), javaToSqlConverter))
                .connectionPool(connectionPool)
                .inputConverter(new ResultSetConverter<>(new ColumnToFieldMapping<>(SimulationEntry.class), SimulationEntry::new))
                .simulationsTable(simulationsTable)
                .build();
    }

    public static ServersDao newJdbcServersDao(ConnectionPool connectionPool, DataSource serversTable){

        return new ServersJdbcDao(connectionPool, serversTable);

    }

    public static ParametersDao newJdbcParametersDao(ConnectionPool connectionSupplier, DataSource parametersTable){
        return new ParametersJdbcDao(connectionSupplier, parametersTable);
    }
}
