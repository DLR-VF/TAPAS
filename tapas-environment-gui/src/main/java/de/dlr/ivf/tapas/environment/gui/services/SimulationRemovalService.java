package de.dlr.ivf.tapas.environment.gui.services;

import com.opencsv.exceptions.CsvException;
import de.dlr.ivf.tapas.environment.EnvironmentHelpers;
import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.ParameterEntry;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.model.SimulationState;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

public class SimulationRemovalService extends Service<Void> {

    private final SimulationsDao simulationsDao;
    private final ParametersDao parametersDao;

    public SimulationRemovalService(SimulationsDao simulationsDao, ParametersDao parametersDao){
        this.simulationsDao = simulationsDao;
        this.parametersDao = parametersDao;
    }
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected void failed() {
                super.failed();
                reset();
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                reset();
            }

            @Override
            protected Void call() throws Exception {

                try {
                    //insert the simulation first and get the id
                    String simKey = EnvironmentHelpers.generateSimulationKey();

                    SimulationEntry simulation = SimulationEntry.builder()
                            .simDescription("some description")
                            .simInsertedTime(Timestamp.valueOf(LocalDateTime.now()))
                            .simKey(simKey)
                            .simProgress(0)
                            .build();

                    Thread.sleep(2000);

                    int simId = simulationsDao.save(simulation);

                    //todo this is bold... think of something better
                    if(simId < 1){
                        throw new IllegalArgumentException("Seems that returned simulation id of the inserted simulation is less than 1. simId: "+simId);
                    }

                    Thread.sleep(2000);
                    simulation.setId(simId);

                    //now insert parameters
                    //Map<String, String> parameters = readParameters(simFile, simKey);
                    Collection<ParameterEntry> parameterEntries = parameters.entrySet()
                            .stream()
                            .map(entry -> ParameterEntry.builder()
                                    .simId(simId)
                                    .simKey(simKey)
                                    .paramKey(entry.getKey())
                                    .paramValue(entry.getValue())
                                    .build())
                            .toList();

                    Thread.sleep(2000);
                    parametersDao.insert(parameterEntries);

                    Thread.sleep(2000);
                    //now update the simulation state
                    simulation.setSimState(SimulationState.READY);
                    simulationsDao.update(simId, simulation);
                    Thread.sleep(2000);

                } catch (IOException | CsvException | IllegalArgumentException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                return null;
            }
        };
    }
}
