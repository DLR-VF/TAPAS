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
import java.util.function.Supplier;

@Builder
public class SimpleJdbcWriter<S> implements DataWriter<S, Void> {

    private final Supplier<Connection> connection;
    private final Converter<Object, Object> typeConverter;
    private final String query;
    /**
     * item order is important here. The result of each method invocation must map to a prepared statement parameter.
     */
    private final SortedMap<Integer, Method> psIndexToMethodMap;

    @Override
    public Void write(S objectToWrite) {
        try (Connection con = connection.get();
             PreparedStatement preparedStatement = con.prepareStatement(query)){

            //todo extract this into a injectable writing strategy
            for(Map.Entry<Integer, Method> entry : psIndexToMethodMap.entrySet()){

                Method m = entry.getValue();

                Object paramToSet = m.invoke(objectToWrite);
                if(paramToSet != null){
                    paramToSet = typeConverter.convert(paramToSet);
                }
                preparedStatement.setObject(entry.getKey(), paramToSet);
            }
            preparedStatement.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }
}
