package de.dlr.ivf.tapas.simulation.trafficgeneration;

import de.dlr.ivf.tapas.model.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemeSet;
import de.dlr.ivf.tapas.simulation.TrafficGeneration;
import org.springframework.beans.factory.annotation.Autowired;

public class PersonGroupTrafficGeneration implements TrafficGeneration<TPS_PersonGroup> {

    @Autowired
    private final TPS_SchemeSet schemeSet;

    public PersonGroupTrafficGeneration(TPS_SchemeSet schemeSet) {
        this.schemeSet = schemeSet;
    }


    @Override
    public TPS_Plan selectPlan(TPS_PersonGroup context) {
        return new TPS_Plan(null, null, schemeSet.selectScheme(context), null);
    }
}
