package de.dlr.ivf.tapas.daemon;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.daemon.configuration.DaemonConfiguration;
import de.dlr.ivf.tapas.daemon.monitors.ServerStateMonitor;
import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.configuration.EnvironmentConfiguration;
import de.dlr.ivf.tapas.environment.dao.DaoFactory;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.model.ServerState;
import de.dlr.ivf.tapas.util.IPInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import java.lang.System.Logger;

public class TapasDaemonLauncher {

    private static Logger logger = System.getLogger(ConnectionPool.class.getName());

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
            if (!Files.isRegularFile(configFile))
                throw new IllegalArgumentException("The provided argument is not a file. Provided arguments = "+ arguments);


            //read the configuration file with Jackson
            DaemonConfiguration configDto = new ObjectMapper().readValue(configFile.toFile(), DaemonConfiguration.class);

            EnvironmentConfiguration environment = configDto.getTapasEnvironment();

            ConnectionPool connectionPool = new ConnectionPool(environment.getConnectionDetails());


            ServersDao serversDao = DaoFactory.newJdbcServersDao(connectionPool, environment.getServerTable());
            SimulationsDao simulationsDao = DaoFactory.newJdbcSimulationsDao(connectionPool, environment.getSimulationsTable());

            TapasEnvironment tapasEnvironment = TapasEnvironment.builder()
                    .parametersDao(DaoFactory.newJdbcParametersDao(connectionPool, environment.getParameterTable()))
                    .serversDao(serversDao)
                    .simulationsDao(simulationsDao)
                    .build();

            ServerEntry serverEntry = ServerEntry.builder()
                    .serverIp(IPInfo.getEthernetInetAddress().getHostAddress())
                    .serverCores(Runtime.getRuntime().availableProcessors())
                    .serverName(IPInfo.getHostname())
                    .serverUsage(0)
                    .serverState(ServerState.STOP)
                    .build();

            ServerStateMonitor serverStateMonitor = new ServerStateMonitor(serversDao, serverEntry);
//
//            final BlockingQueue<SimulationEntry> simulationsToRun = new ArrayBlockingQueue<>(1);
//            SimulationRequestTask simulationRequest = SimulationRequestTask.builder()
//                    .simulationsDao(simulationsDao)
//                    .serverIp(serverEntry.getServerIp()).build();
//            ScheduledExecutorService simulationRequestMonitorService = Executors.newSingleThreadScheduledExecutor();
//
//            simulationRequestMonitorService.submit(simulationRequest);
//            simulationRequestMonitorService.scheduleAtFixedRate(simulationRequest,30,30,TimeUnit.SECONDS);
//
//            TapasManager serverManager = TapasManager.builder()
//                    .serverEntry(serverEntry)
//                    .serverStateMonitor(serverStateMonitor)
//                    .simulationRequest(simulationRequest)
//                    .build();

            //initialize the daemon first and then start the server state monitor
            TapasDaemon tapasDaemon = TapasDaemon.builder()
                    .tapasEnvironment(tapasEnvironment)
                    .serverStateMonitor(serverStateMonitor)
                    .serverEntry(serverEntry)
                    .build();

            ScheduledExecutorService serverStateMonitorService = Executors.newSingleThreadScheduledExecutor();
            serverStateMonitorService.submit(serverStateMonitor);
            serverStateMonitorService.scheduleAtFixedRate(serverStateMonitor, 1, 5, TimeUnit.SECONDS);

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
