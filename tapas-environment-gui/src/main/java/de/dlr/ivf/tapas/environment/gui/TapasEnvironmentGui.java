package de.dlr.ivf.tapas.environment.gui;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.DataReader;
import de.dlr.ivf.api.io.DataReaderFactory;
import de.dlr.ivf.api.io.JdbcConnectionProvider;
import de.dlr.ivf.api.io.implementation.ResultSetConverter;
import de.dlr.ivf.tapas.environment.configuration.EnvironmentConfiguration;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.gui.fx.view.controllers.SimulationMonitorController;
import de.dlr.ivf.tapas.environment.gui.tasks.ServerDataUpdateTask;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TapasEnvironmentGui extends Application {

    private SimulationMonitorController controller;
    private final Collection<Connection> backGroundTaskConnections = new ArrayList<>();

    public static void main(String[] args) {

        Application.launch(args);
    }

    private void addConnection(Connection connection){
        this.backGroundTaskConnections.add(connection);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parameters params = getParameters();
        List<String> paraList = params.getRaw();

        if (paraList.size() != 1) {
            String arguments = paraList.stream()
                    .collect(Collectors.joining(",", "[", "]"));

            throw new IllegalArgumentException("SimulationControl needs a single configuration file as input argument. Provided arguments = " + arguments);
        }

        Path configFile = Paths.get(paraList.get(0));
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");
        EnvironmentConfiguration configDto;
        try {
            //read the configuration file with Jackson
            configDto = new ObjectMapper().readValue(configFile.toFile(), EnvironmentConfiguration.class);

            // Constructs the client
            //SimulationControl control = new SimulationControl(() -> JdbcConnectionProvider.newJdbcConnectionProvider().get(configDto.getConnectionDetails()));
        } catch (DatabindException e) {
            throw new IllegalArgumentException("The supplied file does not map to an EnvironmentConfiguration.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //init the controller

        Connection serverUpdateTaskConnection = JdbcConnectionProvider.newJdbcConnectionProvider().get(configDto.getConnectionDetails());
        addConnection(serverUpdateTaskConnection);
        DataReader<ResultSet> reader = DataReaderFactory.newOpenConnectionJdbcReader(() -> serverUpdateTaskConnection);
        Supplier<Collection<ServerEntry>> serverEntrySupplier = () -> reader.read(new ResultSetConverter<>(ServerEntry.class,ServerEntry::new),configDto.getServerTable());
        ServerDataUpdateTask serverDataUpdateTask = new ServerDataUpdateTask(serverEntrySupplier);
        serverDataUpdateTask.setPeriod(Duration.seconds(1));
        serverDataUpdateTask.start();


        //this.controller = new SimulationMonitorController(serverDataUpdateTask);
        FXMLLoader loader = new FXMLLoader(SimulationMonitorController.class.getResource("/view/SimulationMonitor.fxml"));


        loader.setController(controller);

        Pane root = loader.load();

        Scene scene = new Scene(root);

        scene.getStylesheets().add(TapasEnvironmentGui.class.getResource("/css/SimulationMonitor.css").toExternalForm());

        primaryStage.setScene(scene);

        primaryStage.setTitle("SimulationMonitorOld");

        //primaryStage.setOnCloseRequest(event -> Platform.exit());

        //ScenicView.show(scene);

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception{
        System.out.println("Closing "+backGroundTaskConnections.size()+" background task connections");
        backGroundTaskConnections.forEach(connection ->{
            try {
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        });
        System.exit(0);
    }
}
