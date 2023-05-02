package de.dlr.ivf.api.io;

import de.dlr.ivf.api.converter.Converter;
import lombok.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class ColumnMappingConverter<S,T> implements Converter<S,T> {

    private final Map<String, Field> targetClassFieldMap;
    private final Supplier<T> objectFactory;

    public ColumnMappingConverter(Class<T> targetClass, Supplier<T> objectFactory){

        this.objectFactory = objectFactory;

        //extract all fields from the dto class that are annotated with @Column.
        this.targetClassFieldMap = extractColumnFields(targetClass);
        setFieldAccessibility();
        checkForDefaultConstructor(targetClass);
    }

    @Override
    public abstract T convert(@NonNull S dto);

    private void setFieldAccessibility(){
        targetClassFieldMap.values()
                .forEach(field -> field.setAccessible(true));
    }

    private void checkForDefaultConstructor(Class<T> dtoClass) {
        Constructor<?>[] constructors= dtoClass.getConstructors();

        Arrays.stream(constructors)
                .filter(constructor -> constructor.getParameterCount() == 0)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("The provided class does not contain an empty constructor"));
    }

    private Map<String, Field> extractColumnFields(Class<T> dtoClass) {
        return Arrays.stream(dtoClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(Column.class) != null)
                .collect(Collectors.toMap(
                        field -> field.getAnnotation(Column.class).value(),
                        field -> field
                ));
    }

    protected Map<String, Field> getTargetClassFieldMap(){
        return this.targetClassFieldMap;
    }

    protected Supplier<T> getObjectFactory(){
        return this.objectFactory;
    }
}
