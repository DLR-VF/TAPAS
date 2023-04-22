package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.guard.Guard;
import de.dlr.ivf.tapas.scheme.TPS_Episode;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class AdaptGuardAction implements TPS_PlanStateAction {

    private final Guard guard;
    private final TPS_Episode next_episode;
    private final BiFunction<TPS_Episode, Supplier<Integer>, Integer> guard_adaption_function;
    private Supplier<Integer> delta_time_supplier;

    public AdaptGuardAction(Guard guard, BiFunction<TPS_Episode, Supplier<Integer>, Integer> guard_adaption_function, TPS_Episode next_episode, Supplier<Integer> delta_time_supplier ) {
        this.guard = guard;
        this.delta_time_supplier = delta_time_supplier;
        this.next_episode = next_episode;
        this.guard_adaption_function = guard_adaption_function;
    }

    @Override
    public void run() {
        this.guard.setValueToTest(guard_adaption_function.apply(next_episode,delta_time_supplier));
    }
}
