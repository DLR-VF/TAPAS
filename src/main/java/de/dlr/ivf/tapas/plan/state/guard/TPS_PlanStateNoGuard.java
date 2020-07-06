package de.dlr.ivf.tapas.plan.state.guard;

public class TPS_PlanStateNoGuard implements TPS_PlanStateGuard {

    @Override
    public boolean test(Object o) {
        return false;
    }
}
