package de.dlr.ivf.api.io.implementation;

import de.dlr.ivf.api.io.ColumnMappingConverter;
import de.dlr.ivf.api.io.SqlArrayUtils;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This converter converts a {@link ResultSet} to an object {@link T} using reflection. {@link de.dlr.ivf.api.io.Column}
 * annotated fields will be directly mapped to the column name in the {@link ResultSet}.
 *
 * @param <T> type of object after conversion
 *
 * @author Alain Schengen
 */

public class ResultSetConverter<T> extends ColumnMappingConverter<ResultSet, T> {

    private final String className;

    public ResultSetConverter(Class<T> targetClass, Supplier<T> objectFactory) {
        super(targetClass, objectFactory);
        this.className = targetClass.getName();
    }

    @Override
    public T convert(@NonNull ResultSet objectToConvert) {

        T convertedObject = getObjectFactory().get();

        for(Map.Entry<String, Field> fieldEntry : getTargetClassFieldMap().entrySet()){

            String columnName = fieldEntry.getKey();
            Field field = fieldEntry.getValue();
            field.setAccessible(true);

            try {

                Object valueToSet = objectToConvert.getObject(columnName);
                if (valueToSet instanceof Array array) {
                    if (array.getArray() == null) {
                        field.set(convertedObject, new Object[0]);
                    } else {
                        var intArray = SqlArrayUtils.extractIntArray(array);
                        if(intArray != null) {
                            field.set(convertedObject, intArray);
                        }else{
                            var doubleArray = SqlArrayUtils.extractDoubleArray(array);
                            if(doubleArray != null){
                                field.set(convertedObject, doubleArray);
                            }
                        }
                    }

                } else { //todo extract this to super class
                    if (field.getType().isEnum()) {
                        if(valueToSet instanceof String stringValue){ //try to map the enum from String
                            Method valueOf = field.getType().getMethod("valueOf", String.class);
                            Object enumConstant = valueOf.invoke(null, stringValue);
                            field.set(convertedObject, enumConstant);
                        }else{ //try mapping it to an int
                            if(valueToSet instanceof Integer intValue) {
                                Method ordinal = field.getType().getMethod("ordinal", Integer.class);
                                Object enumConstant = ordinal.invoke(null, intValue);
                                field.set(convertedObject, enumConstant);
                            }
                        }
                    } else {
                        field.set(convertedObject, valueToSet);
                    }
                }
            }catch (SQLException e){
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("Can't access field: "+field.getName() + " in class: "+ className, e);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException("Can't access method.", e);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("Unable to invoke method.", e);
            }
        }
        return convertedObject;
    }
}
