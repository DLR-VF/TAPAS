package de.dlr.ivf.tapas.execution.sequential.statemachine.util;

import de.dlr.ivf.tapas.person.TPS_Household;
import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateAction;
import de.dlr.ivf.tapas.execution.sequential.action.TPS_PlanStateNoAction;

public class StateMachineUtils {

    private static final TPS_PlanStateAction noAction = new TPS_PlanStateNoAction();
    private static final Object noEventData = new Object();
    private static final TPS_Household emptyHousehold = new TPS_Household(-1);

    public static TPS_PlanStateAction NoAction(){
        return noAction;
    }
    public static Object NoEventData(){ return noEventData; }

    public static TPS_Household EmptyHouseHold(){
        return emptyHousehold;
    }
}
