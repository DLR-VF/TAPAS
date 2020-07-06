package de.dlr.ivf.tapas.plan.state.states;

public enum TPS_PlanStateConstantNames {
    EXECUTION_READY("execution ready"),
    EXECUTION_DONE("execution done successfully");

    private String s;
    TPS_PlanStateConstantNames(String s) {
        this.s = s;
    }

    public String getName(){
        return this.s;
    }
}
