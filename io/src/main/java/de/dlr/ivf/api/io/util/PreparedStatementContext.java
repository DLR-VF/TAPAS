package de.dlr.ivf.api.io.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.SortedMap;

@AllArgsConstructor
@Getter
public class PreparedStatementContext {

    private final List<String> updatableColumnNames;
    private final SortedMap<Integer, Method> indexedInvocableMethods;
    private final List<String> filterColumns;
}
