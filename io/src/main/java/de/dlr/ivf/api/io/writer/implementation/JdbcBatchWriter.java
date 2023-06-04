package de.dlr.ivf.api.io.writer.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.writer.DataWriter;
import lombok.Builder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedMap;


//todo implement locking mechanism when closing
@Builder
public class JdbcBatchWriter implements DataWriter {

    private final Connection connection;
    private final int batchSize;
    private final Converter<Object, Object> typeConverter;

    /**
     * item order is important here. The result of each method invocation must map to a prepared statement parameter.
     */
    private final SortedMap<Integer, Method> psIndexToMethodMap;
    private final PreparedStatement preparedStatement;

    @Builder.Default
    private int count = 0;

    @Override
    public void write(Object objectToWrite) {
        try {

            for(Map.Entry<Integer, Method> entry : psIndexToMethodMap.entrySet()){

                Method m = entry.getValue();

                Object paramToSet = m.invoke(objectToWrite);
                if(paramToSet != null){
                    paramToSet = typeConverter.convert(paramToSet);
                }
                preparedStatement.setObject(entry.getKey(), paramToSet);
            }
            preparedStatement.addBatch();

            if(++count % batchSize == 0){
                preparedStatement.executeBatch();
            }
        }catch (SQLException e){
            e.printStackTrace();
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {

            preparedStatement.executeBatch();
            connection.close();

        }catch (SQLException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
