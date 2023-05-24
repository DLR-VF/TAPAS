package de.dlr.ivf.tapas.environment.dao;

import java.util.Collection;

public interface ParametersDao {

    //todo figure out what exactly should be returned and inserted
    Collection<?> load();

    void insert(Collection<?> parameters);


}
