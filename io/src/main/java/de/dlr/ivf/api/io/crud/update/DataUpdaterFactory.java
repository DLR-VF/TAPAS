package de.dlr.ivf.api.io.crud.update;

import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.conversion.JavaToSqlTypeConverter;
import de.dlr.ivf.api.io.crud.update.implementation.SimpleJdbcUpdater;
import de.dlr.ivf.api.io.util.PreparedStatementContext;
import de.dlr.ivf.api.io.util.PreparedStatementContextFactory;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

public class DataUpdaterFactory {

    public static <T> DataUpdater<T> newSimpleJdbcUpdater(DataSource dataSource, Connection connection, Class<T> objectType){

        var typeConverter = new JavaToSqlTypeConverter();
        PreparedStatementContext psContext = PreparedStatementContextFactory.newExtendedPreparedStatementContext(objectType);

        return SimpleJdbcUpdater.<T>builder()
                .statementParameterSetter(new PreparedStatementParameterSetter<>(psContext.getIndexedInvocableMethods(), typeConverter))
                .connection(connection)
                .typeConverter(typeConverter)
                .query(generateUpdateQuery(dataSource,psContext))
                .build();
    }


    private static String generateUpdateQuery(DataSource dataSource, PreparedStatementContext psContext){

        List<String> columns = psContext.getUpdatableColumnNames();

        if(columns != null){

            //build first part of query: UPDATE someTable SET colName1 = ?, colName2 = ?,...
            String query = "UPDATE "+dataSource.getUri()+" SET "+
                    columns.stream()
                            .map(colName -> colName+" = ?")
                            .collect(Collectors.joining(", "));

            //add where clause
            List<String> filterColumnNames = psContext.getFilterColumns();

            String whereClause = filterColumnNames.stream().map(columnName -> columnName + " = ?").collect(Collectors.joining(" AND "));

            return "".equals(whereClause) ? query : query+" WHERE "+whereClause;
        }
        return null;
    }
}
