package de.dlr.ivf.api.io.implementation;

import de.dlr.ivf.api.io.ColumnMappingConverter;
import lombok.NonNull;

import java.lang.reflect.Field;
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
    public T convert(@NonNull ResultSet dto) {

        T convertedObject = getObjectFactory().get();

        for(Map.Entry<String, Field> fieldEntry : getTargetClassFieldMap().entrySet()){

            String columnName = fieldEntry.getKey();
            Field field = fieldEntry.getValue();

            try {

                Object valueToSet = dto.getObject(columnName);
                if (valueToSet instanceof Array array) {
                    if (array.getArray() == null) {
                        field.set(dto, new Object[0]);
                    } else {
                        field.set(dto, array.getArray());
                    }

                } else {
                    field.set(dto, dto.getObject(columnName));
                }
            }catch (SQLException e){
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Can't access field: "+field.getName() + " in class: "+ className, e);
            }
        }
        return convertedObject;
    }
}
