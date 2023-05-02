package de.dlr.ivf.tapas.execution.sequential.communication;

import de.dlr.ivf.tapas.model.person.TPS_Car;
import de.dlr.ivf.tapas.model.person.TPS_Person;

import java.util.Optional;

public interface CarRequestHandler {

    Optional<TPS_Car> requestCar(TPS_Person requesting_person);
}
