package de.dlr.ivf.api.io.crud.write.implementation;

import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import de.dlr.ivf.api.io.crud.write.DataWriter;
import lombok.Builder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


//todo implement locking mechanism when closing
@Builder
public class JdbcBatchWriter<S> implements DataWriter<S, Void>, AutoCloseable {

    private final Connection connection;
    private final int batchSize;
    private final PreparedStatementParameterSetter<S> statementParameterSetter;
    private final PreparedStatement preparedStatement;

    @Builder.Default
    private int count = 0;

    @Override
    public Void write(S objectToWrite) {
        try {
            statementParameterSetter.set(preparedStatement, objectToWrite);
            preparedStatement.addBatch();

            if(++count % batchSize == 0){
                preparedStatement.executeBatch();
            }

        }catch (SQLException e){
            e.printStackTrace();
            throw  new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void close() {
        try {
            preparedStatement.executeBatch();
            connection.setAutoCommit(true);
        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
