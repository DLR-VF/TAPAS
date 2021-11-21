package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class ArgumentInputHandler {

    public static boolean validate(String[] args){
        return args.length == 1 && Files.exists(Paths.get(args[0])) && !Files.isDirectory(Paths.get(args[0]));
    }

    public static Optional<TPS_ParameterClass> readParameters(String[] args){

        return TPS_ParameterClass.of(Paths.get(args[0]).toFile());
    }
}
