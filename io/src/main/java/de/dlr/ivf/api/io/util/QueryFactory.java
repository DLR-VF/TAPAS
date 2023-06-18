package de.dlr.ivf.api.io.util;

import de.dlr.ivf.api.io.configuration.model.DataSource;

import java.util.List;
import java.util.stream.Collectors;

public class QueryFactory {

    public static String newInsertQuery(List<String> columnNames, DataSource dataSource){
        //create column definition part: (col1, col2, ...)
        String sqlInsertColumnDefinition = columnNames.stream().collect(Collectors.joining(",","(",")"));
        //create parameterizable part: (?, ?, ...)
        String sqlParameterPart = columnNames.stream().map(c -> "?").collect(Collectors.joining(",","(",")"));

        return "INSERT INTO "+dataSource.getUri()+" "+sqlInsertColumnDefinition+" VALUES "+sqlParameterPart;
    }
}
