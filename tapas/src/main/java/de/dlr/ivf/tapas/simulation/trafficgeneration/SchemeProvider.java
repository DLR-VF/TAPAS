package de.dlr.ivf.tapas.simulation.trafficgeneration;

import de.dlr.ivf.tapas.model.choice.DiscreteChoiceModel;
import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;
import de.dlr.ivf.tapas.model.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.model.scheme.*;

import java.util.Map;

public class SchemeProvider {

    private final Map<TPS_PersonGroup, DiscreteDistribution<SchemeClass>> schemeClassDistributions;
    private final DiscreteChoiceModel<SchemeClass> schemeClassChoiceModel;
    private final DiscreteChoiceModel<Scheme> schemeChoiceModel;

    public SchemeProvider(Map<TPS_PersonGroup, DiscreteDistribution<SchemeClass>> schemeClassDistributions,
                          DiscreteChoiceModel<SchemeClass> schemeClassChoiceModel,
                          DiscreteChoiceModel<Scheme> schemeChoiceModel) {
        this.schemeClassDistributions = schemeClassDistributions;
        this.schemeClassChoiceModel = schemeClassChoiceModel;
        this.schemeChoiceModel = schemeChoiceModel;
    }

    public Scheme selectScheme(TPS_PersonGroup personGroup){

        DiscreteDistribution<SchemeClass> schemeClassDistribution = schemeClassDistributions.get(personGroup);

        SchemeClass schemeClass = schemeClassChoiceModel.makeChoice(schemeClassDistribution);

        return schemeChoiceModel.makeChoice(schemeClass.schemeDistribution());
    }
}
