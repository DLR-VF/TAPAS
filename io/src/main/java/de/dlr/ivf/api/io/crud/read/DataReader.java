package de.dlr.ivf.api.io.crud.read;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface DataReader<S> {

    <T> Collection<T> read(Converter<S,T> objectFactory, DataSource dataSource, Collection<Filter> filters);

    default <T> Collection<T> read(Converter<S,T> objectFactory, DataSource dataSource, Filter filter){
        return read(objectFactory, dataSource, List.of(filter));
    }

    default <T> Collection<T> read(Converter<S,T> objectFactory, DataSource dataSource){
        return read(objectFactory, dataSource, Collections.emptyList());
    }
}
