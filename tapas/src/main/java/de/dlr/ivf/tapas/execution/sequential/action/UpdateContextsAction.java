package de.dlr.ivf.tapas.execution.sequential.action;
import de.dlr.ivf.tapas.execution.sequential.context.ContextUpdateable;

import java.util.List;

public class UpdateContextsAction implements TPS_PlanStateAction{
    private final List<ContextUpdateable> updateable_contexts;

    public UpdateContextsAction(List<ContextUpdateable> updateable_contexts) {

        this.updateable_contexts = updateable_contexts;
    }

    @Override
    public void run() {

        updateable_contexts.forEach(ContextUpdateable::updateContext);
    }
}
