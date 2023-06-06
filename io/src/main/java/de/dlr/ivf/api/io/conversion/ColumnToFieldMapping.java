package de.dlr.ivf.api.io.conversion;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Getter
public class ColumnToFieldMapping<T> {

    private final Class<T> targetClass;
    private final Map<String, Field> ignorableFieldMappings;
    private final Map<String, Field> nonIgnorableFieldMappings;
    private final Map<String, Field> allFieldMappings;

    public ColumnToFieldMapping(Class<T> targetClass){
        this.targetClass = targetClass;
        Map<Boolean, Map<String, Field>> columnAnnotatedFields = extractColumnFields(targetClass);

        this.allFieldMappings = new HashMap<>();

        this.ignorableFieldMappings = columnAnnotatedFields.get(true);
        if(ignorableFieldMappings != null){
            allFieldMappings.putAll(ignorableFieldMappings);
        }

        this.nonIgnorableFieldMappings = columnAnnotatedFields.get(false);
        if(nonIgnorableFieldMappings != null){
            allFieldMappings.putAll(nonIgnorableFieldMappings);
        }
    }

    private Map<Boolean, Map<String, Field>> extractColumnFields(Class<?> dtoClass) {
        return Arrays.stream(dtoClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(Column.class) != null)
                .collect(groupingBy(field -> field.getAnnotation(Column.class).ignoreWrite(),
                        Collectors.toMap(
                                field -> field.getAnnotation(Column.class).value(),
                                field -> field
                        )));
    }
}
