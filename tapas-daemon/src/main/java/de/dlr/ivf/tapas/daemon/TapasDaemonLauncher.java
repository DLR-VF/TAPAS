package de.dlr.ivf.tapas.daemon;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.daemon.configuration.DaemonConfiguration;
import de.dlr.ivf.tapas.daemon.services.StateRequestFactory;
import de.dlr.ivf.tapas.daemon.services.implementation.ServerStateRequest;
import de.dlr.ivf.tapas.daemon.services.implementation.SimulationRequestService;
import de.dlr.ivf.tapas.daemon.services.StateMonitor;
import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.configuration.EnvironmentConfiguration;
import de.dlr.ivf.tapas.environment.dao.DaoFactory;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.model.ServerState;
import de.dlr.ivf.tapas.util.IPInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;


public class TapasDaemonLauncher {

    private static final Logger logger = System.getLogger(TapasDaemonLauncher.class.getName());

    /**
     * Flow:
     * 1 - check that the input is a single file and validly maps to a DaemonConfiguration or exit with an exception.
     * 2 - set up the daemon background tasks like polling for new simulations and updating the server status
     *
     * @param args takes a single Json-file that maps to {@link DaemonConfiguration} file
     */
    public static void main(String[] args) {

        try {

            String arguments = Arrays.stream(args)
                    .collect(Collectors.joining(",", "[", "]"));

            if (args.length != 1) {
                throw new IllegalArgumentException("The TAPAS-Daemon needs a single configuration file as input argument. Provided arguments = " + arguments);
            }

            Path configFile = Paths.get(args[0]);
            if (!Files.isRegularFile(configFile)) {
                throw new IllegalArgumentException("The provided argument is not a file. Provided arguments = " + arguments);
            }

            logger.log(Level.INFO, "Reading configuration.");
            DaemonConfiguration configDto = new ObjectMapper().readValue(configFile.toFile(), DaemonConfiguration.class);

            EnvironmentConfiguration environment = configDto.getTapasEnvironment();

            logger.log(Level.INFO, "Building TAPAS environment.");
            ConnectionPool connectionPool = new ConnectionPool(environment.getConnectionDetails());

            ServersDao serversDao = DaoFactory.newJdbcServersDao(connectionPool, environment.getServerTable());
            SimulationsDao simulationsDao = DaoFactory.newJdbcSimulationsDao(connectionPool, environment.getSimulationsTable());

            TapasEnvironment tapasEnvironment = TapasEnvironment.builder()
                    .parametersDao(DaoFactory.newJdbcParametersDao(connectionPool, environment.getParameterTable()))
                    .serversDao(serversDao)
                    .simulationsDao(simulationsDao)
                    .build();

            logger.log(Level.INFO, "Starting TAPAS daemon.");
            String serverIdentifier = IPInfo.getHostname();

            Duration pollingRate = Duration.ofSeconds(5);

            StateMonitor<ServerState> serverStateMonitor =
                    new StateMonitor<>(
                            new ServerStateRequest(tapasEnvironment, serverIdentifier),
                            "ServerStateMonitor",
                            pollingRate,
                            ServerState.RUN);

            SimulationRequestService simulationRequestService =
                    new SimulationRequestService(tapasEnvironment, serverIdentifier,pollingRate);

            TapasServer tapasServer = TapasServer.builder()
                    .connectionPool(connectionPool)
                    .stateRequestFactory(new StateRequestFactory(tapasEnvironment))
                    .pollingRate(pollingRate)
                    .simulationRequestService(simulationRequestService)
                    .build();

            TapasDaemon tapasDaemon = TapasDaemon.builder()
                    .stateMonitor(serverStateMonitor)
                    .tapasServer(tapasServer)
                    .build();

            Thread daemonThread = new Thread(tapasDaemon);
            daemonThread.start();


        } catch (DatabindException e) {
            e.printStackTrace(); //throw new IllegalArgumentException("The supplied file does not map to a DaemonConfiguration.", e);
        } catch (IOException e) {
            e.printStackTrace();//throw new RuntimeException(e);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }
}
