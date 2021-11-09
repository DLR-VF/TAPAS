package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.util.CPUUsage;
import de.dlr.ivf.tapas.util.parameters.ParamString;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class SimulationServerUpdateTask implements Runnable{

    private final SimulationServerContext server_context;
    private final TPS_DB_Connector db_connector;
    private final String iAddr;
    private final CPUUsage cpu_usage;
    private final String server_table;

    public SimulationServerUpdateTask(SimulationServerContext server_context, TPS_DB_Connector db_connector){
        this.server_context = server_context;
        this.db_connector = db_connector;
        this.cpu_usage = new CPUUsage();
        this.server_table = db_connector.getParameters().getString(ParamString.DB_TABLE_SERVERS);
        this.iAddr = "inet '" + server_context.getIp().getHostAddress() + "'";
    }
    @Override
    public void run() {
        try {
            if(server_context.getRunningServer().isPresent())
                updateServerInDatabase();

        } catch (SQLException | IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void updateServerInDatabase() throws IOException, InterruptedException, SQLException {

        Connection connection = db_connector.getConnection(this);

        String query = "UPDATE " + this.server_table + " SET server_online = TRUE, server_usage = " +
                        cpu_usage.getCPUUsage() + ", server_ip = " + this.iAddr + " WHERE server_name = '" +
                        server_context.getHostname() + "'";

        int updated_row_count = connection.createStatement().executeUpdate(query);

        //if the server was not registered in the database anymore, reinsert it
        if(updated_row_count == 0){
            query = "INSERT INTO " + server_table + " (server_ip, server_online, server_cores, server_name) VALUES(" +
                    this.iAddr + ", TRUE, " + this.server_context.getCoreCount() + ", '" + server_context.getHostname() + "')";
            connection.createStatement().executeUpdate(query);
        }

    }
}
