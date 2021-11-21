/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.TapasLogin;
import de.dlr.ivf.tapas.util.TPS_Argument.TPS_ArgumentType;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public final class SimulationDaemon implements Runnable{

    private final TapasSimulationProvider simulation_provider;
    private final TPS_DB_Connector db_connector;
    private SimulationServer simulation_server;
    private List<ShutDownable> shutdownable_services = new ArrayList<>();


    public SimulationDaemon(TPS_DB_Connector dbConnector, TapasSimulationProvider simulation_provider) {
        this.db_connector = dbConnector;
        this.simulation_provider= simulation_provider;
    }

    public static void main(String[] args) {

        if(!ArgumentInputHandler.validate(args)){
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.APPLICATION, TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "The provided input arguments are invalid. Make sure that solely a single existing file is passed.");
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.APPLICATION, TPS_LoggingInterface.SeverenceLogLevel.INFO,
                    "Closing the application...");
            return;
        }

        try {

            TPS_ParameterClass parameters = ArgumentInputHandler.readParameters(args).orElseThrow(
                    () -> new IllegalArgumentException("The provided input file is not a valid parameter file."));

            TapasLogin tapas_login = TapasLogin.fromParameterClass(parameters).orElseThrow(
                    () -> new IllegalArgumentException("The provided parameters file does not contain login credentials to the database."));

            TPS_DB_Connector dbConnector = TPS_DB_Connector.fromTapasLoginCredentials(tapas_login, parameters).orElseThrow(
                    () -> new RuntimeException("Cannot establish connection to database: "+tapas_login.getDatabase()+" on host: "+tapas_login.getHost()+" for user: "+tapas_login.getUser()));


            String simulations_table = parameters.getString(ParamString.DB_TABLE_SIMULATIONS);
            String simulation_parameter_table = parameters.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS);

            TapasSimulationProvider simulation_provider = new TapasSimulationProvider(dbConnector, simulations_table, simulation_parameter_table);

            SimulationDaemon daemon = new SimulationDaemon(dbConnector, simulation_provider);


            Thread t = new Thread(daemon);
            t.start();

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    @Override
    public void run() {
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.APPLICATION, TPS_LoggingInterface.SeverenceLogLevel.INFO,"Starting TAPAS-Daemon...");
        Runtime.getRuntime().addShutdownHook(new TapasShutDownProcedure(shutdownable_services));

        SimulationServerContext server_context = SimulationServerContext.newLocalServerContext();
        ShutDownable server_manager = new SimulationServerRegistrationManager(server_context, db_connector).start();

        shutdownable_services.add(server_manager);


        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.APPLICATION, TPS_LoggingInterface.SeverenceLogLevel.INFO,"Waiting for simulation to start...");

        while(true) {
            Optional<TPS_Simulation> tapas_simulation = simulation_provider.requestSimulation();

            try {
                if (tapas_simulation.isPresent()) {

                    TPS_Simulation simulation = tapas_simulation.get();
                    TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.APPLICATION, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Starting simulation: " + simulation.getSimulationKey());


                    this.simulation_server = new SimulationServer(simulation, db_connector);
                    shutdownable_services.add(this.simulation_server);
                    server_context.setRunningServer(this.simulation_server);


                    Thread t = new Thread(this.simulation_server);
                    t.start();
                    t.join();

                    this.simulation_server = null;
                }
                Thread.sleep(5000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
