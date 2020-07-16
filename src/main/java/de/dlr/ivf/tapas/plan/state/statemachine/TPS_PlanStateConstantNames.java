package de.dlr.ivf.tapas.plan.state.statemachine;

public enum TPS_PlanStateConstantNames {
    EXECUTION_READY("execution ready"),
    EXECUTION_DONE("execution done successfully"),
    PLANNING_TRIP("planning trip"),
    AT_HOME("at home"),
    ON_MOVE("on move"),
    ON_ACTIVITY("on activity");

    private String s;
    TPS_PlanStateConstantNames(String s) {
        this.s = s;
    }

    public String getName(){
        return this.s;
    }
}
