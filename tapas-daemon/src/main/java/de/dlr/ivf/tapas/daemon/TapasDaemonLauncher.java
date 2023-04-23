package de.dlr.ivf.tapas.daemon;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.tapas.daemon.configuration.DaemonConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TapasDaemonLauncher {

    public static void main(String[] args) {

        if(args.length != 1){

            String arguments = Arrays.stream(args)
                    .collect(Collectors.joining(",","[","]"));

            throw new IllegalArgumentException("The TAPAS-Daemon needs a single configuration file as input argument. Provided arguments = "+arguments);
        }

        Path configFile = Paths.get(args[0]);
        if(!Files.isRegularFile(configFile))
            throw new IllegalArgumentException("The provided argument is not a file.");


        try {

            //read the configuration file
            DaemonConfiguration configDto = new ObjectMapper().readValue(configFile.toFile(), DaemonConfiguration.class);

            //initialize the connection Provider
           // ConnectionProvider connectionProvider = initConnectionProvider(configDto.getReferences());

            System.out.println("");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
