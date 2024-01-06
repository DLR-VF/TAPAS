package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.scheme.TPS_Scheme;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemeSet;

public class SchemeSelector {

    private final TPS_SchemeSet schemeSet;

    public SchemeSelector(TPS_SchemeSet schemeSet){
        this.schemeSet = schemeSet;
    }

    public TPS_Scheme selectPlan(TPS_Person person){
        return schemeSet.findScheme(person);
    }
}
