package de.dlr.ivf.tapas.environment.dao;

import de.dlr.ivf.tapas.environment.dto.ParameterEntry;

import java.util.Collection;

public interface ParametersDao {

    //todo figure out what exactly should be returned and inserted
    Collection<ParameterEntry> load(int id);

    void insert(Collection<ParameterEntry> parameters);


}
