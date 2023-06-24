package de.dlr.ivf.api.io.crud.write;

import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.conversion.JavaToSqlTypeConverter;
import de.dlr.ivf.api.io.crud.write.implementation.JdbcBatchWriter;
import de.dlr.ivf.api.io.crud.write.implementation.SimpleJdbcWriter;
import de.dlr.ivf.api.io.util.PreparedStatementContext;
import de.dlr.ivf.api.io.util.PreparedStatementContextFactory;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import de.dlr.ivf.api.io.util.QueryFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataWriterFactory {

    public static <S> DataWriter<S,Void> newJdbcBatchWriter(DataSource dataSource, Connection  connection, Class<S> objectType) {

        PreparedStatementContext psContext = PreparedStatementContextFactory.newPreparedStatementContext(objectType);

        PreparedStatement preparedStatement;
        try {
             preparedStatement = connection.prepareStatement(QueryFactory.newInsertQuery(psContext.getUpdatableColumnNames(),dataSource));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return JdbcBatchWriter.<S>builder()
                .batchSize(1000)
                .connection(connection)
                .preparedStatement(preparedStatement)
                .statementParameterSetter(new PreparedStatementParameterSetter<>(psContext.getIndexedInvocableMethods(), new JavaToSqlTypeConverter()))
                .build();
    }

    public static <S> DataWriter<S,Void> newJdbcWriter(DataSource dataSource, Connection  connection, Class<S> objectType){

        PreparedStatementContext psContext = PreparedStatementContextFactory.newPreparedStatementContext(objectType);

        return SimpleJdbcWriter.<S>builder()
                .connection(connection)
                .statementParameterSetter(new PreparedStatementParameterSetter<>(psContext.getIndexedInvocableMethods(), new JavaToSqlTypeConverter()))
                .query(QueryFactory.newInsertQuery(psContext.getUpdatableColumnNames(), dataSource))
                .build();
    }
}
