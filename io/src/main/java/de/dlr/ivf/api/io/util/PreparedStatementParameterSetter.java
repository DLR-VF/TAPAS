package de.dlr.ivf.api.io.util;

import de.dlr.ivf.api.converter.Converter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiConsumer;

public class PreparedStatementParameterSetter<T> implements BiConsumer<PreparedStatement, T> {

    /**
     * item order is important here. The result of each method invocation must map to a prepared statement parameter.
     */
    private final SortedMap<Integer, Method> psIndexToMethodMap;

    private final Converter<Object, Object> typeConverter;

    public PreparedStatementParameterSetter(SortedMap<Integer, Method> psIndexToMethodMap, Converter<Object, Object> typeConverter){
        this.psIndexToMethodMap = psIndexToMethodMap;
        this.typeConverter = typeConverter;
    }

    @Override
    public void accept(PreparedStatement preparedStatement, T objectToWrite) {
        try{
            for(Map.Entry<Integer, Method> entry : psIndexToMethodMap.entrySet()){

                Method m = entry.getValue();

                Object paramToSet = m.invoke(objectToWrite);

                if(paramToSet != null){
                    paramToSet = typeConverter.convert(paramToSet);
                }
                preparedStatement.setObject(entry.getKey(), paramToSet);
            }
        } catch (IllegalAccessException | InvocationTargetException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
