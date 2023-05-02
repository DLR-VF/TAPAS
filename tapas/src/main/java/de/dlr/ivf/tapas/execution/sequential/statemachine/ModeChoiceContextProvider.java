package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.model.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.model.person.TPS_Car;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ModeChoiceContextProvider {

    public BiFunction<Supplier<Optional<TPS_Car>>,Supplier<Optional<Boolean>>, TPS_ModeChoiceContext> getModeChoiceContext = (householc_car, car_sharing_car) -> new TPS_ModeChoiceContext();



}
