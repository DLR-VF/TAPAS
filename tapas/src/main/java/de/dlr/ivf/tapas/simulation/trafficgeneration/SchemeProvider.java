package de.dlr.ivf.tapas.simulation.trafficgeneration;

import de.dlr.ivf.tapas.model.choice.DiscreteChoiceModel;
import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;
import de.dlr.ivf.tapas.model.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.model.scheme.*;

import java.util.Map;

/**
 * The SchemeProvider class is responsible for selecting a scheme based on a given TPS_PersonGroup.
 *
 * It uses a map of TPS_PersonGroup to DiscreteDistribution<SchemeClass> to store the probability distribution
 * for scheme classes associated with each TPS_PersonGroup.
 *
 * The schemeClassChoiceModel is used to select a scheme class based on the probability distribution.
 *
 * Finally, the schemeChoiceModel is used to select a scheme from the selected scheme class's scheme distribution.
 */
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

    /**
     * Selects a scheme based on a given TPS_PersonGroup.
     *
     * @param personGroup the person group for which to select a scheme
     * @return the selected scheme
     */
    public Scheme selectScheme(TPS_PersonGroup personGroup){

        DiscreteDistribution<SchemeClass> schemeClassDistribution = schemeClassDistributions.get(personGroup);

        SchemeClass schemeClass = schemeClassChoiceModel.makeChoice(schemeClassDistribution);

        return schemeChoiceModel.makeChoice(schemeClass.schemeDistribution());
    }
}
