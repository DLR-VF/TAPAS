package de.dlr.ivf.tapas.execution.sequential.statemachine;

public enum TPS_PlanStateConstantNames {
    INITIALIZING("initializing states"),
    EXECUTION_READY("execution ready"),
    EXECUTION_DONE("execution done successfully"),
    PLANNING_TRIP("planning trip"),
    AT_HOME("at home"),
    FINISHING_DAY("day finished"),
    ON_TRIP("on move"),
    ON_ACTIVITY("on activity");

    private String s;
    TPS_PlanStateConstantNames(String s) {
        this.s = s;
    }

    public String getName(){
        return this.s;
    }
}
