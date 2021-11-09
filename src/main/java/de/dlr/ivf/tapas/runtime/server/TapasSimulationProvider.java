package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class TapasSimulationProvider {

    private final TPS_DB_Connector db_connector;
    private final String simulations_table;
    private final String simulation_parameters_table;

    public TapasSimulationProvider(TPS_DB_Connector dbConnector, String sim_table, String sim_param_table) {
        this.db_connector = dbConnector;
        this.simulations_table = sim_table;
        this.simulation_parameters_table = sim_param_table;
    }

    public Optional<TPS_Simulation> requestSimulation(){

        TPS_Simulation simulation = null;

        try {
            Connection connection = db_connector.getConnection(this);

            connection.setAutoCommit(false);

            String lock = "LOCK TABLE " + simulations_table + " IN ACCESS EXCLUSIVE MODE;";

            connection.createStatement().execute(lock);

            String available_simulations = "SELECT * FROM " + simulations_table +
                    " WHERE sim_finished = false" +
                    " AND sim_ready = true" +
                    " AND sim_started = true" +
                    " AND (simulation_server IS NULL OR simulation_server = '')" +
                    " ORDER BY timestamp_insert LIMIT 1";

            ResultSet available_simulation = connection.createStatement().executeQuery(available_simulations);

            if (available_simulation.next()) {

                String sim_key = available_simulation.getString("sim_key");

                //
                String read_param_query =  "SELECT * FROM " + simulation_parameters_table +
                        " WHERE sim_key = '" + sim_key + "'";

                ResultSet sim_param_result = connection.createStatement().executeQuery(read_param_query);

                TPS_ParameterClass simulation_parameters = new TPS_ParameterClass();
                simulation_parameters.readRuntimeParametersFromDB(sim_param_result);

                //dedicate the server to the simulation
                String update_host_query = "UPDATE " + simulations_table + " SET simulation_server = '" + db_connector.getHostName() + "' WHERE sim_key = '" + sim_key + "'";
                connection.createStatement().execute(update_host_query);

                //set up the simulation
                simulation = new TPS_Simulation(sim_key, simulation_parameters);
            }

            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            //nothing to do...
            e.printStackTrace();
        }

        return Optional.ofNullable(simulation);
    }
}
