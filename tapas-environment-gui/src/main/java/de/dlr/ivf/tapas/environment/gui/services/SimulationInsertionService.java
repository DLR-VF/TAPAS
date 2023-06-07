package de.dlr.ivf.tapas.environment.gui.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import de.dlr.ivf.tapas.environment.EnvironmentHelpers;
import de.dlr.ivf.tapas.environment.dao.ParametersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.ParameterEntry;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class SimulationInsertionService extends Service<Void> {

    @Setter
    private File simFile;
    private final SimulationsDao simulationsDao;
    private final ParametersDao parametersDao;
    public SimulationInsertionService(SimulationsDao simulationsDao, ParametersDao parametersDao){
        this.simulationsDao = simulationsDao;
        this.parametersDao = parametersDao;
    }
    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
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

                    int simId = simulationsDao.save(simulation);

                    //todo this is bold... think of something better
                    if(simId < 1){
                        throw new IllegalArgumentException("Seems that returned simulation id of the inserted simulation is less than 1. simId: "+simId);
                    }

                    simulation.setId(simId);

                    //now insert parameters
                    Map<String, String> parameters = readParameters(simFile, simKey);
                    Collection<ParameterEntry> parameterEntries = parameters.entrySet()
                            .stream()
                            .map(entry -> ParameterEntry.builder()
                                    .simId(simId)
                                    .simKey(simKey)
                                    .paramKey(entry.getKey())
                                    .paramValue(entry.getValue())
                                    .build())
                            .toList();

                    parametersDao.insert(parameterEntries);

                    simulationsDao.update(simId, simulation);

                } catch (IOException | CsvException | IllegalArgumentException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

//
//        String projectName = parameters.getOrDefault("PROJECT_NAME", "");
//        String query = String.format("INSERT INTO simulations (sim_key, sim_file, sim_description) VALUES('%s', '%s', '%s')",
//                sim_key, filename, projectName);
                return null;
            }
        };
    }

    /**
     *  This is mostly a port from the old simulation control
     */
    private Map<String, String> readParameters(File parameterFile, String simKey) throws IOException, CsvException {

        Stack<File> parameterFiles = new Stack<>();
        parameterFiles.push(parameterFile);

        HashMap<String, String> parameters = new HashMap<>();

        String key;
        String value;
        String parent;

        while (!parameterFiles.empty()) {
            File file = parameterFiles.pop();

            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withIgnoreQuotations(true)
                    .build();

            CSVReader reader = new CSVReaderBuilder(new FileReader(file))
                    .withSkipLines(1)
                    .withCSVParser(parser)
                    .build();

            List<String[]> lines = reader.readAll();
            reader.close();

            for (String[] line : lines) {

                if (line.length <= 2) {
                    continue;
                }

                key = line[0];
                value = line[1];

                if (key.equals("FILE_PARENT_PROPERTIES") || key.equals("FILE_FILE_PROPERTIES") || key.equals(
                        "FILE_LOGGING_PROPERTIES") || key.equals("FILE_PARAMETER_PROPERTIES") || key.equals(
                        "FILE_DATABASE_PROPERTIES")) {
                    parent = file.getParent();
                    while (value.startsWith("./")) {
                        value = value.substring(2);
                        parent = new File(parent).getParent();
                    }
                    parameterFiles.push(new File(parent, value));
                } else { // this does not overwrites old values!
                    if (!parameters.containsKey(key)) {
                        parameters.put(key, value);
                    }
                }
            }
        }
        //fix the SUMO-dir by appending the simulation run!
        String paramVal = parameters.get("SUMO_DESTINATION_FOLDER");
        paramVal += "_" + simKey;
        parameters.put("SUMO_DESTINATION_FOLDER", paramVal);

        return parameters;
    }
}
