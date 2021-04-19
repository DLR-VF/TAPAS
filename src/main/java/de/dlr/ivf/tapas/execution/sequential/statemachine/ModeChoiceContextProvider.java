package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.action.ActionProvider;
import de.dlr.ivf.tapas.execution.sequential.guard.Guard;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ModeChoiceContextProvider {

    public BiFunction<Supplier<Optional<TPS_Car>>,Supplier<Optional<Boolean>>, TPS_ModeChoiceContext> getModeChoiceContext = (householc_car, car_sharing_car) -> new TPS_ModeChoiceContext();



}
