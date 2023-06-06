package de.dlr.ivf.api.io.writer.implementation;

import de.dlr.ivf.api.io.writer.DataWriter;
import lombok.Builder;

import java.sql.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Builder
public class IdReturningSimpleJdbcWriter<S> implements DataWriter<S,Integer> {

    private final Supplier<Connection> connection;
    private final String query;
    private final BiConsumer<PreparedStatement, S> psParameterSetter;
    private final String[] idColumn;

    @Override
    public Integer write(S objectToWrite) {
        try (Connection con = connection.get();
            PreparedStatement preparedStatement = con.prepareStatement(query, idColumn)){

            psParameterSetter.accept(preparedStatement,objectToWrite);

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
}
