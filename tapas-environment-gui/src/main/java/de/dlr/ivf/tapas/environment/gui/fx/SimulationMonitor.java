package de.dlr.ivf.tapas.environment.gui.fx;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.*;
import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.TapasEnvironment.TapasEnvironmentBuilder;
import de.dlr.ivf.tapas.environment.configuration.EnvironmentConfiguration;
import de.dlr.ivf.tapas.environment.dao.DaoFactory;
import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.gui.fx.model.ModelFactory;
import de.dlr.ivf.tapas.environment.gui.fx.model.ServersModel;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;
import de.dlr.ivf.tapas.environment.gui.fx.view.ViewHandler;
import de.dlr.ivf.tapas.environment.gui.fx.view.controllers.ServerEntryController;
import de.dlr.ivf.tapas.environment.gui.fx.view.controllers.SimulationEntryController;
import de.dlr.ivf.tapas.environment.gui.fx.view.controllers.SimulationMonitorController;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.ViewModelFactory;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.ServersViewModel;
import de.dlr.ivf.tapas.environment.gui.fx.viewmodel.implementation.SimulationsViewModel;
import de.dlr.ivf.tapas.environment.gui.services.SimulationDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.ServerDataUpdateService;
import de.dlr.ivf.tapas.environment.gui.services.SimulationInsertionService;
import javafx.application.Application;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SimulationMonitor extends Application {
    private final Collection<ScheduledService<?>> scheduledServices = new ArrayList<>();
    private final Collection<Service<?>> regularServices = new ArrayList<>();

    private JdbcConnectionManager connectionPool;

    private TapasEnvironment tapasEnvironment;

    @Override
    public void init(){

        //get args
        Parameters params = getParameters();
        List<String> paraList = params.getRaw();

        validateOneArgument(paraList);

        Path configFile = Paths.get(paraList.get(0));
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");

        //read config and set up connection pool
        EnvironmentConfiguration configDto = readConfigFile(configFile);
        this.connectionPool = new JdbcConnectionManager(JdbcConnectionProvider.newJdbcConnectionProvider());

        TapasEnvironmentBuilder tapasEnvironmentBuilder = TapasEnvironment.builder();
        ConnectionDetails connectionDetails = configDto.getConnectionDetails();

        //setup simulation entries data access object
        SimulationsDao simDao = DaoFactory.newJdbcSimulationsDao(
                newConnectionSupplier(connectionDetails, connectionPool, SimulationDataUpdateService.class),
                configDto.getSimulationsTable());
        tapasEnvironmentBuilder.simulationsDao(simDao);

        //setup server entries data access object
        ServersDao serversDao = DaoFactory.newJdbcServersDao(
                newConnectionSupplier(connectionDetails, connectionPool, ServerDataUpdateService.class),
                configDto.getServerTable());
        tapasEnvironmentBuilder.serversDao(serversDao);

        this.tapasEnvironment = tapasEnvironmentBuilder.build();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            ModelFactory modelFactory = new ModelFactory();
            ViewModelFactory viewModelFactory = new ViewModelFactory(modelFactory);
            ViewHandler viewHandler = new ViewHandler(primaryStage, viewModelFactory);
            viewHandler.start();

            //init simulations overview panel
            SimulationsModel simulationsModel = modelFactory.getSimulationEntryModel(tapasEnvironment.getSimulationsDao());

            //  scheduled service to reload simulation entries from the database
            SimulationDataUpdateService simulationDataUpdateService = new SimulationDataUpdateService(simulationsModel::getSimulations);
            simulationDataUpdateService.setPeriod(Duration.seconds(1));
            scheduledServices.add(simulationDataUpdateService);

            SimulationInsertionService insertionService = new SimulationInsertionService(tapasEnvironment.getSimulationsDao(), tapasEnvironment.getParametersDao());
            SimulationsViewModel simViewModel = viewModelFactory.getSimulationEntryViewModel(simulationDataUpdateService, tapasEnvironment.getSimulationsDao(), insertionService);
            regularServices.add(insertionService);

            //init servers overview panel
            ServersModel serversModel = modelFactory.getServerEntryModel(tapasEnvironment.getServersDao());

            //  scheduled service to reload server entries from the database
            ServerDataUpdateService serverDataUpdateService = new ServerDataUpdateService(serversModel::getServerData);
            serverDataUpdateService.setPeriod(Duration.seconds(1));
            scheduledServices.add(serverDataUpdateService);

            ServersViewModel serversViewModel = viewModelFactory.getServerEntryViewModel(serverDataUpdateService, tapasEnvironment.getServersDao());

            //start all scheduled services
            scheduledServices.forEach(ScheduledService::start);

            //init the controller
            FXMLLoader loader = new FXMLLoader(SimulationMonitorController.class.getResource("/view/SimulationMonitor.fxml"));

            loader.setControllerFactory(controllerClass -> {
                if (controllerClass == SimulationMonitorController.class) {
                    return new SimulationMonitorController();
                } else if (controllerClass == SimulationEntryController.class) {
                    return new SimulationEntryController(simViewModel);
                } else if (controllerClass == ServerEntryController.class) {
                    return new ServerEntryController(serversViewModel);
                }
                return null;
            });

            //set up layout
            Pane root = loader.load();

            Scene scene = new Scene(root);

            URL cssFile = SimulationMonitor.class.getResource("/css/SimulationMonitor.css");
            if (cssFile != null) {
                scene.getStylesheets().add(cssFile.toExternalForm());
            }

            primaryStage.setScene(scene);

            primaryStage.setTitle("SimulationMonitor");

            //primaryStage.setOnCloseRequest(event -> Platform.exit());

            //ScenicView.show(scene);

            primaryStage.show();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Override
    public void stop() throws Exception{

        System.out.println("Stopping scheduled services");
        scheduledServices.forEach(ScheduledService::cancel);

        System.out.println("Shutting down the connection pool");
        connectionPool.shutDown();

        System.exit(0);
    }

    private EnvironmentConfiguration readConfigFile(Path configFile) {
        try {
            //read the configuration file with Jackson
            return new ObjectMapper().readValue(configFile.toFile(), EnvironmentConfiguration.class);
        } catch (DatabindException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("The supplied file does not map to an EnvironmentConfiguration.", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void validateOneArgument(List<String> paraList) {

        if (paraList.size() != 1) {
            String arguments = paraList.stream()
                    .collect(Collectors.joining(",", "[", "]"));

            String message = "SimulationControl needs a single configuration file as input argument. Provided arguments = " + arguments;
            System.err.println(message);
            throw new IllegalArgumentException(message);
        }
    }

    private Supplier<Connection> newConnectionSupplier(ConnectionDetails connectionDetails, JdbcConnectionManager connectionPool, Class<?> requestingClass){
        ConnectionRequest connectionRequest = new ConnectionRequest(requestingClass, connectionDetails);
        return  () -> connectionPool.getConnection(connectionRequest);
    }
}
