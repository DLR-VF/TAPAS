package de.dlr.ivf.tapas.simulation.trafficgeneration;

import de.dlr.ivf.tapas.model.choice.DiscreteChoiceModel;
import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;
import de.dlr.ivf.tapas.model.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.scheme.Scheme;
import de.dlr.ivf.tapas.model.scheme.TPS_Scheme;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemeClass;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemeSet;
import org.springframework.beans.factory.annotation.Autowired;

public class SchemeProvider {

    private final TPS_SchemeSet schemeSet;
    private final DiscreteChoiceModel<Scheme> choiceModel;
    private final Map<TPS_PersonGroup, DiscreteDistribution<TPS_SchemeClass>>

    @Autowired
    public SchemeProvider(TPS_SchemeSet schemeSet, DiscreteChoiceModel<Scheme> choiceModel) {
        this.schemeSet = schemeSet;
        this.choiceModel = choiceModel;
    }

    public TPS_Scheme selectPlan(TPS_Person person){
        return schemeSet.findScheme(person);
    }
}
