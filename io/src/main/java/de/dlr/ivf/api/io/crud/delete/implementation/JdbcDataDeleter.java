package de.dlr.ivf.api.io.crud.delete.implementation;

import de.dlr.ivf.api.io.conversion.JavaToSqlTypeConverter;
import de.dlr.ivf.api.io.crud.delete.DataDeleter;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import lombok.Builder;

import java.sql.Connection;

@Builder
public class JdbcDataDeleter<T> implements DataDeleter<T> {
    private final Connection connection;
    private final String query;
    private final PreparedStatementParameterSetter<T> statementParameterSetter;
    private final JavaToSqlTypeConverter typeConverter;
    private final String idColumnName;
    @Override
    public void delete(T objectToDelete) {

    }
}
