package de.dlr.ivf.tapas.plan.state.guard;
@FunctionalInterface
public interface TPS_PlanStateGuardStrategy {
    <T> TPS_PlanStateGuard adjust(T value);
}
