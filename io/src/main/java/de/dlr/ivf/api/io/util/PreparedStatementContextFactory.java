package de.dlr.ivf.api.io.util;

import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PreparedStatementContextFactory {

    public static <S> PreparedStatementContext newPreparedStatementContext(Class<S> objectType){

        ColumnToFieldMapping<S> columnToFieldMapping = new ColumnToFieldMapping<>(objectType);

        Map<String, Field> fieldMap = columnToFieldMapping.getAllFieldMappings()
                .values()
                .stream()
                .collect(Collectors.toMap(Field::getName, field -> field));

        Map<String, Method> propertyNamesToGetMethods = generateFieldToGetMethodsMap(objectType, fieldMap);

        Map<String, Field> writableColumnFieldMap = columnToFieldMapping.getNonIgnorableFieldMappings();

        //since a prepared statement should be updated dynamically, the order of items does matter
        List<String> columnNames = new ArrayList<>(writableColumnFieldMap.size());
        SortedMap<Integer, Method> methods = new TreeMap<>();
        AtomicInteger psParameterIndex = new AtomicInteger(1);
        writableColumnFieldMap.forEach((k,v) -> {
            columnNames.add(k);
            methods.put(psParameterIndex.getAndIncrement(), propertyNamesToGetMethods.get(v.getName()));
        });


        var ignorableFieldMappings = columnToFieldMapping.getIgnorableFieldMappings();
        List<String> keys = ignorableFieldMappings == null ? Collections.emptyList() : new ArrayList<>(ignorableFieldMappings.keySet());

        return new PreparedStatementContext(columnNames, methods, keys, propertyNamesToGetMethods);
    }

    public static <S> PreparedStatementContext newExtendedPreparedStatementContext(Class<S> objectType){

        PreparedStatementContext context = newPreparedStatementContext(objectType);

        SortedMap<Integer, Method> indexedInvocableMethods = context.getIndexedInvocableMethods();

        AtomicInteger highestIndex = new AtomicInteger(indexedInvocableMethods.lastKey());

        context.getAutogeneratedColumns().forEach(key -> indexedInvocableMethods.put(highestIndex.incrementAndGet(), context.getPropertyNamesToGetMethods().get(key)));

        return context;
    }

    private static <T> Map<String, Method> generateFieldToGetMethodsMap(Class<T> objectType, Map<String, Field> fieldMap){
        PropertyDescriptor[] propertyDescriptors;
        try {
            propertyDescriptors = Introspector.getBeanInfo(objectType).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return Arrays.stream(propertyDescriptors)
                .filter(propertyDescriptor -> propertyDescriptor.getReadMethod() != null)
                .filter(propertyDescriptor -> fieldMap.containsKey(propertyDescriptor.getName()))
                .collect(Collectors.toMap(
                        PropertyDescriptor::getName,
                        PropertyDescriptor::getReadMethod
                ));
    }
}