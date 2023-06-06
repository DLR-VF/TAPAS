package de.dlr.ivf.api.io.writer.implementation;

import de.dlr.ivf.api.io.writer.DataWriter;
import lombok.Builder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;

@Builder
public class SimpleJdbcWriter<S> implements DataWriter<S, Void> {

    private final Supplier<Connection> connection;
    private final String query;
    private final PreparedStatementParameterSetter<S> statementParameterSetter;

    @Override
    public Void write(S objectToWrite) {
        try (Connection con = connection.get();
             PreparedStatement preparedStatement = con.prepareStatement(query)){

            statementParameterSetter.accept(preparedStatement, objectToWrite);
            preparedStatement.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }
}
