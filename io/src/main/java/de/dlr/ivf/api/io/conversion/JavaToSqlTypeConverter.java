package de.dlr.ivf.api.io.conversion;

import de.dlr.ivf.api.converter.Converter;

import lombok.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


//todo implement caching
public class JavaToSqlTypeConverter implements Converter<Object, Object> {
    @Override
    public Object convert(@NonNull Object dto) {

        Class<?> dtoClass = dto.getClass();
        if(dtoClass.isEnum()){
            try {
                Method nameMethod = dto.getClass().getMethod("name");
                return nameMethod.invoke(dto);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }

        return dto;
    }
}
