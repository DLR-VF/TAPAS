package de.dlr.ivf.api.io.crud.update.implementation;

import de.dlr.ivf.api.io.conversion.JavaToSqlTypeConverter;
import de.dlr.ivf.api.io.crud.update.DataUpdater;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import lombok.Builder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Builder
public class SimpleJdbcUpdater<T> implements DataUpdater<T> {

    private final Connection connection;
    private final String query;
    private final PreparedStatementParameterSetter<T> statementParameterSetter;
    private final JavaToSqlTypeConverter typeConverter;
    private final String idColumnName;

    @Override
    public void update(T objectToUpdate) {

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){

            //set parameters
            statementParameterSetter.set(preparedStatement, objectToUpdate);

            //now set the value for the where clause
            preparedStatement.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
