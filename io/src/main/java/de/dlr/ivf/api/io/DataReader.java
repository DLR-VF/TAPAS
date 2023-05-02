package de.dlr.ivf.api.io;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.configuration.model.DataSource;

import java.util.Collection;

public interface DataReader<S> {

    <T> Collection<T> read(Converter<S,T> objectFactory, DataSource dataSource);
}
