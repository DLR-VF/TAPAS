package de.dlr.ivf.api.io.crud.write.implementation;

import de.dlr.ivf.api.io.crud.write.DataWriter;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import lombok.Builder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Builder
public class SimpleJdbcWriter<S> implements DataWriter<S, Void> {

    private final Connection connection;
    private final String query;
    private final PreparedStatementParameterSetter<S> statementParameterSetter;

    @Override
    public Void write(S objectToWrite) {

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){

            statementParameterSetter.accept(preparedStatement, objectToWrite);
            preparedStatement.executeUpdate();

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
