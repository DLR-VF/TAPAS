package de.dlr.ivf.tapas.execution.sequential.statemachine.util;

import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateNoAction;
import de.dlr.ivf.tapas.model.person.TPS_Household;

public class StateMachineUtils {

    private static final TPS_PlanStateAction noAction = new TPS_PlanStateNoAction();
    private static final Object noEventData = new Object();

    public static TPS_PlanStateAction NoAction(){
        return noAction;
    }
    public static Object NoEventData(){ return noEventData; }

}
