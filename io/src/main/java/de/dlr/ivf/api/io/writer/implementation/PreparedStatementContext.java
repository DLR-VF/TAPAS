package de.dlr.ivf.api.io.writer.implementation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.SortedMap;

@AllArgsConstructor
@Getter
public class PreparedStatementContext {

    private final String query;
    private final SortedMap<Integer, Method> methods;
    private final String[] idColumns;
}
