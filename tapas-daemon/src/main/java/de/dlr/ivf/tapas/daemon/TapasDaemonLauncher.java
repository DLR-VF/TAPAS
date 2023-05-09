package de.dlr.ivf.tapas.daemon;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.tapas.daemon.configuration.DaemonConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TapasDaemonLauncher {

    /**
     * Flow:
     * 1 - check that the input is a single file and validly maps to a DaemonConfiguration or exit with an exception.
     * 2 - set up the daemon background tasks like polling for new simulations and updating the server status
     *
     * @param args takes a single Json-file that maps to {@link DaemonConfiguration} file
     */
    public static void main(String[] args) {

        if (args.length != 1) {

            String arguments = Arrays.stream(args)
                    .collect(Collectors.joining(",", "[", "]"));

            throw new IllegalArgumentException("The TAPAS-Daemon needs a single configuration file as input argument. Provided arguments = " + arguments);
        }

        Path configFile = Paths.get(args[0]);
        if (!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");


        try {
            //read the configuration file with Jackson
            DaemonConfiguration configDto = new ObjectMapper().readValue(configFile.toFile(), DaemonConfiguration.class);

        } catch (DatabindException e) {
            throw new IllegalArgumentException("The supplied file does not map to a DaemonConfiguration.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //initialize the connection Provider
//        ConnectionManager<Connection> connectionManager = ConnectionManager.newJdbcConnectionManager();
//        connectionManager.a
//
//    }
//
//        private Path composeLogDirPath(){
//        String dirSeparator = System.getProperty("file.separator");
//        String dirName = getString(ParamString.FILE_WORKING_DIRECTORY);
//
//        if (!dirName.endsWith(dirSeparator)) {
//            dirName = dirName + dirSeparator;
//        }
//
//        dirName = dirName + LOG_DIR + getString(ParamString.RUN_IDENTIFIER);

        //return Paths.get("./");
    }
}
