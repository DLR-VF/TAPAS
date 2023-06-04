package de.dlr.ivf.api.io.conversion;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class ColumnToFieldMapping<T> {

    private final Map<String, Field> targetClassFieldMap;
    private final Class<T> targetClass;

    public ColumnToFieldMapping(Class<T> targetClass){
        this.targetClass = targetClass;
        this.targetClassFieldMap = extractColumnFields(targetClass);
    }

    private Map<String, Field> extractColumnFields(Class<?> dtoClass) {
        return Arrays.stream(dtoClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(Column.class) != null)
                .collect(Collectors.toMap(
                        field -> field.getAnnotation(Column.class).value(),
                        field -> field
                ));
    }
}
