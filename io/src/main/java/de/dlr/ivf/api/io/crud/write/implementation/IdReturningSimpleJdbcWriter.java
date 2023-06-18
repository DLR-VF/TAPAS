package de.dlr.ivf.api.io.crud.write.implementation;

import de.dlr.ivf.api.io.crud.write.DataWriter;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import lombok.Builder;

import java.sql.*;

@Builder
public class IdReturningSimpleJdbcWriter<S> implements DataWriter<S,Integer> {

    private final Connection connection;
    private final String query;
    private final PreparedStatementParameterSetter<S> psParameterSetter;
    private final String[] idColumn;

    @Override
    public Integer write(S objectToWrite) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(query, idColumn)){

            psParameterSetter.set(preparedStatement,objectToWrite);

            preparedStatement.executeUpdate();

            try(ResultSet generatedSimulationId = preparedStatement.getGeneratedKeys()){
                if(generatedSimulationId.next()){
                    return generatedSimulationId.getInt(1);
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        //nothing to do here since connections are injected from outside, and they should be handled from the outside
    }
}
