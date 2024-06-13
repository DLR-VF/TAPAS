package de.dlr.ivf.tapas;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.tapas.configuration.json.TapasConfig;
import de.dlr.ivf.tapas.configuration.spring.*;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.simulation.SimulationRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.lang.System.Logger.Level;

@ComponentScan(basePackages={"de.dlr.ivf.tapas.configuration.json"})
public class TapasLauncher{
    private static final System.Logger logger = System.getLogger(TapasLauncher.class.getName());

    public static void main(String[] args) {

        if (args.length != 1) {

            String arguments = Arrays.stream(args)
                    .collect(Collectors.joining(",", "[", "]"));

            throw new IllegalArgumentException("TAPAS needs a single configuration file as input argument. Provided arguments = " + arguments);
        }

        Path configFile = Paths.get(args[0]);
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");


        try {
            //read the configuration file with Jackson
            logger.log(Level.INFO, "Loading configuration file: " + configFile);
            TapasConfig tapasConfig = new ObjectMapper()
                    //.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(configFile.toFile(), TapasConfig.class);
            logger.log(Level.INFO, "Loaded configuration file: " + configFile);

            //set up the Spring application context
            logger.log(Level.INFO, "Initializing TAPAS application context...");
            AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
            applicationContext.registerBean(TapasConfig.class, () -> tapasConfig);

            applicationContext.register(
                    TapasFactory.class,
                    RegionBeanFactory.class,
                    HouseholdBeanFactory.class,
                    LocationBeanFactory.class,
                    TrafficAnalysisZoneBeanFactory.class,
                    CarBeanFactory.class,
                    TrafficGenerationBeanFactory.class,
                    LocationChoiceModelBeanFactory.class,
                    //ModeChoiceModelBeanFactory.class,
                    TripPriorityRunnerBeanFactory.class,
                    SchemeBeanFactory.class,
                    MatrixBeanFactory.class
            );

            applicationContext.refresh();
            applicationContext.start();


            //run the simulation
            String simulationRunnerName = tapasConfig.getSimulationRunnerToUse();

            SimulationRunner runner = applicationContext.getBean(simulationRunnerName, SimulationRunner.class);
            //SchemeProvider schemeProvider = applicationContext.getBean("schemeProvider", SchemeProvider.class);
            Map<String, MatrixMap> matrixMaps  = applicationContext.getBean("matrixMaps", Map.class);
            logger.log(Level.INFO, "Launching simulation...");
            Thread t = new Thread(runner, "SimulationThread");
            t.start();

        } catch (DatabindException e) {
            throw new IllegalArgumentException("The supplied file does not map to TapasConfig.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("The supplied file is not accessible.", e);
        }
    }
}
