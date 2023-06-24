package de.dlr.ivf.api.io.conversion;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.util.SqlArrayUtils;
import de.dlr.ivf.api.io.annotation.Column;
import lombok.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This converter converts a {@link ResultSet} to an object {@link T} using reflection. {@link Column}
 * annotated fields will be directly mapped to the column name in the {@link ResultSet}.
 *
 * @param <T> type of object after conversion
 *
 * @author Alain Schengen
 */

public class ResultSetConverter<T> implements Converter<ResultSet, T> {

    private final Supplier<T> objectFactory;
    private final ColumnToFieldMapping<T> columnMapping;

    public ResultSetConverter(ColumnToFieldMapping<T> columnMapping, Supplier<T> objectFactory) {
        this.columnMapping = columnMapping;
        this.objectFactory = objectFactory;
        setFieldAccessibility();
        checkForDefaultConstructor(columnMapping.getTargetClass());
    }

    @Override
    public T convert(@NonNull ResultSet objectToConvert) {

        T convertedObject = this.objectFactory.get();

        for(Map.Entry<String, Field> fieldEntry : columnMapping.getAllFieldMappings().entrySet()){

            String columnName = fieldEntry.getKey();
            Field field = fieldEntry.getValue();

            try {

                Object valueToSet = objectToConvert.getObject(columnName);
                //todo extract this into a SqlToJavaConverter
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

                } else {
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
                throw new RuntimeException("Can't access field: "+field.getName() + " in class: "+ columnMapping.getTargetClass(), e);
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

    private void setFieldAccessibility(){
        this.columnMapping.getAllFieldMappings().values()
                .forEach(field -> field.setAccessible(true));
    }

    private void checkForDefaultConstructor(Class<?> dtoClass) {
        Constructor<?>[] constructors = dtoClass.getConstructors();

        Arrays.stream(constructors)
                .filter(constructor -> constructor.getParameterCount() == 0)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("The provided class does not contain an empty constructor"));
    }
}
