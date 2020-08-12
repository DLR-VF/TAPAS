package de.dlr.ivf.tapas.plan;

import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.plan.state.action.TPS_PlanStateNoAction;

public class StateMachineUtils {

    private static final TPS_PlanStateAction noAction = new TPS_PlanStateNoAction();
    private static final Object noEventData = new Object();

    public static TPS_PlanStateAction NoAction(){
        return noAction;
    }
    public static Object NoEventData(){ return noEventData; }
}
