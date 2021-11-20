package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.util.TPS_AttributeReader;

/**
 * Updates mode choice tree parameters
 */
public class UpdateModeChoicePlanAttributesAction implements TPS_PlanStateAction {
    private final TPS_Plan plan;
    private final TPS_LocatedStay next_located_stay;

    /**
     * @param plan the plan of a person
     * @param next_located_stay the next located stay
     */
    public UpdateModeChoicePlanAttributesAction(TPS_Plan plan, TPS_LocatedStay next_located_stay) {

        this.plan = plan;
        this.next_located_stay = next_located_stay;
    }

    @Override
    public void run() {

        plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.CURRENT_TAZ_SETTLEMENT_CODE_TAPAS,
                next_located_stay.getLocation().getTrafficAnalysisZone().getBbrType()
                        .getCode(TPS_SettlementSystem.TPS_SettlementSystemType.TAPAS));

        //todo add distance category from actual location to dest location

    }
}
