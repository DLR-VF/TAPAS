/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.server;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;



public class SimulationServer implements Runnable, ShutDownable {

    private final TPS_Simulation simulation;
    private final TPS_DB_Connector db_connector;
    private TPS_Simulator simulator;
    private TPS_DB_IOManager pm;

    public SimulationServer(TPS_Simulation simulation, TPS_DB_Connector db_connector) {
        this.simulation = simulation;
        this.db_connector = db_connector;
    }


    public void run() {

        TPS_ParameterClass simulation_parameters = simulation.getParameters();

        int worker_count = 1;

        if(simulation_parameters.isDefined(ParamValue.WORKER_COUNT))
            worker_count = simulation_parameters.getIntValue(ParamValue.WORKER_COUNT);

        //init the pm
        this.pm = new TPS_DB_IOManager(db_connector);

        if(simulation_parameters.isDefined(ParamFlag.FLAG_SEQUENTIAL_EXECUTION) &&
                simulation_parameters.isTrue(ParamFlag.FLAG_SEQUENTIAL_EXECUTION)){

            this.simulator = new SequentialSimulator(pm, db_connector, simulation);
        }else {
            this.simulator = new HierarchicalSimulator(pm);
        }

        //the simulator will block
        this.simulator.run(worker_count);
    }



    @Override
    public void shutdown() {

    }

    //~ Inner Classes -----------------------------------------------------------------------------
//    private class RunWhenShuttingDown extends Thread {
//        private final TPS_DB_Connector dbConnector;
//        private final SimulationServer server;
//
//        public RunWhenShuttingDown(TPS_DB_Connector dbConnector, SimulationServer server) {
//            this.dbConnector = dbConnector;
//            this.server = server;
//        }
//
//        public void run() {
//
//
//
//            //update server process in the server_processes table. This will trigger cleanup procedures inside the DB
//            String query = "UPDATE " + server.getParameters().getString(
//                    ParamString.DB_TABLE_PROCESSES) + " SET end_time = '" + LocalDateTime.now().toString() +
//                    "', tapas_exit_ok = " + !server.isManuallyShutDown() + " WHERE host = '" + server.getHostname() +
//                    "' AND end_time IS NULL";
//
//            int rowcount = dbConnector.executeUpdate(query, this);
//            System.out.println(sysoutPrefix + "Updated " + rowcount + " rows from the server processes table.");
//            System.out.println(sysoutPrefix + "Shutdown finished!");
//
//            query = "UPDATE " + server.getParameters().getString(ParamString.DB_TABLE_SERVERS) + " SET server_online = false WHERE server_name = '" + server.getHostname() + "'";
//            dbConnector.execute(query, this);
//        }
//    }

//

}
