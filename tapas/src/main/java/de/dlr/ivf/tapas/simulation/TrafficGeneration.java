package de.dlr.ivf.tapas.simulation;

import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import org.springframework.stereotype.Component;

@Component
public interface TrafficGeneration<T> {
    TPS_Plan selectPlan(T context);
}
