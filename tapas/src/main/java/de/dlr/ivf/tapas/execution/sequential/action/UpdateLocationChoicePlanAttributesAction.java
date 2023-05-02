package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;
import de.dlr.ivf.tapas.model.TPS_AttributeReader;

public class UpdateLocationChoicePlanAttributesAction implements TPS_PlanStateAction{

    private final TPS_Plan plan;
    private final TPS_Person person;
    private final TPS_PlanningContext planning_context;
    private final TPS_Stay next_stay;

    public UpdateLocationChoicePlanAttributesAction(TPS_Plan plan, TPS_Person person, TPS_PlanningContext planning_context, TPS_Stay next_stay){

        this.plan = plan;
        this.person = person;
        this.planning_context = planning_context;
        this.next_stay = next_stay;
    }



    @Override
    public void run() {


        plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.PERSON_HAS_BIKE, planning_context.isBikeAvailable ? 1 : 0);
        plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.HOUSEHOLD_CARS, planning_context.carForThisPlan == null ? 0 : person.getHousehold().getNumberOfCars());
        plan.getAttributes().put(TPS_AttributeReader.TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS, next_stay.getActCode().getCode(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS));
    }
}
