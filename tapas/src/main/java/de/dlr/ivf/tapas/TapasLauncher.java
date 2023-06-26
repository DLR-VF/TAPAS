package de.dlr.ivf.tapas;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TapasLauncher{

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
            TapasConfig configDto = new ObjectMapper().readValue(configFile.toFile(), TapasConfig.class);

            TPS_ParameterClass parameterClass = new TPS_ParameterClass();
            parameterClass.loadRuntimeParameters(Paths.get(configDto.getRunTimeFile()).toFile());

            TapasInitializer initializer = new TapasInitializer(parameterClass,
                    new ConnectionPool(configDto.getConnectionDetails()));

            Tapas tapas = initializer.init();

        } catch (DatabindException e) {
            throw new IllegalArgumentException("The supplied file does not map to TapasConfig.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
