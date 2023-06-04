package de.dlr.ivf.api.io.writer;

import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.conversion.JavaToSqlTypeConverter;
import de.dlr.ivf.api.io.conversion.WriteableColumnToFieldMapping;
import de.dlr.ivf.api.io.writer.implementation.JdbcBatchWriter;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DataWriterFactory {

    public static DataWriter newJdbcWriter(DataSource dataSource, Supplier<Connection>  connectionSupplier, Class<?> objectType) {

        WriteableColumnToFieldMapping columnToFieldMapping = new WriteableColumnToFieldMapping(objectType);

        Map<String, Field> columnFieldMap = columnToFieldMapping.getTargetClassFieldMap();

        //since a prepared statement should be updated dynamically, the order of items does matter
        List<String> columnNames = new ArrayList<>(columnFieldMap.size());
        SortedMap<Integer, Method> methods = new TreeMap<>();

        Map<String, Field> fieldMap = columnFieldMap.values().stream().collect(Collectors.toMap(Field::getName, field-> field));

        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(objectType).getPropertyDescriptors();

            Map<String, Method> propertyNamesToGetMethods = Arrays.stream(propertyDescriptors)
                    .filter(propertyDescriptor -> propertyDescriptor.getReadMethod() != null)
                    .filter(propertyDescriptor -> fieldMap.containsKey(propertyDescriptor.getName()))
                    .collect(Collectors.toMap(
                            PropertyDescriptor::getName,
                            PropertyDescriptor::getReadMethod
                    ));

            AtomicInteger psParameterIndex = new AtomicInteger(1);
            columnFieldMap.forEach((k,v) -> {
                columnNames.add(k);
                methods.put(psParameterIndex.getAndIncrement(), propertyNamesToGetMethods.get(v.getName()));
            });
        } catch (IntrospectionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        //extract the getter methods

        //create column definition part: (col1, col2, ...)
        String sqlInsertColumnDefinition = columnNames.stream().collect(Collectors.joining(",","(",")"));
        //create parameterizable part: (?, ?, ...)
        String sqlParameterPart = columnNames.stream().map(c -> "?").collect(Collectors.joining(",","(",")"));

        String insertQuery = "INSERT INTO "+dataSource.getUri()+" "+sqlInsertColumnDefinition+" VALUES "+sqlParameterPart;

        Connection connection = connectionSupplier.get();

        PreparedStatement preparedStatement;
        try {
             preparedStatement = connection.prepareStatement(insertQuery);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return JdbcBatchWriter.builder()
                .batchSize(1000)
                .connection(connection)
                .preparedStatement(preparedStatement)
                .psIndexToMethodMap(methods)
                .typeConverter(new JavaToSqlTypeConverter())
                .build();
    }
}
