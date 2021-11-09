package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class SimulationServerRegistrationManager implements ShutDownable {

    private final String iAddr;
    private final SimulationServerContext server_context;
    private final TPS_DB_Connector dbConnector;
    private ScheduledExecutorService executor;

    public SimulationServerRegistrationManager(SimulationServerContext server_context, TPS_DB_Connector dbConnector) {

        this.server_context = server_context;
        this.dbConnector = dbConnector;

        this.iAddr = "inet '" + server_context.getIp().getHostAddress() + "'";


    }

    public ShutDownable start(){

        handleServerProcessConflicts();

        registerServerProcess();

        registerServerIntoDatabase();

        //set up the periodic update task
        if(this.executor == null) {
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.scheduleAtFixedRate(new SimulationServerUpdateTask(this.server_context, this.dbConnector),1,3, TimeUnit.SECONDS);
        }

        return this;
    }

    private void handleServerProcessConflicts() {

        TPS_ParameterClass parameters = dbConnector.getParameters();

        //check if there is a SimulationServer zombie process currently running
        String process_table = parameters.getString(ParamString.DB_TABLE_PROCESSES);
        String query = "SELECT * FROM " + process_table + " WHERE host = '" +
                server_context.getHostname() + "' AND end_time IS NULL";

        List<String> unfinished_process_identifiers = new ArrayList<>();

        try{
            Connection connection = dbConnector.getConnection(this);
            ResultSet rs = connection.createStatement().executeQuery(query);

            while(rs.next())
                unfinished_process_identifiers.add(rs.getString("identifier"));

            rs.close();

            if(unfinished_process_identifiers.size() > 0){
                String process_array = unfinished_process_identifiers.stream()
                                                                     .collect(Collectors.joining("','","('","')"));
                TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.WARN,
                        "The following process identifiers have not properly finished or are still running: \n"+process_array);
                TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO,
                        "Cleaning up broken processes.");

                unfinished_process_identifiers.stream()
                                              .map(identifier -> identifier.split("@")[0])
                                              .forEach(this::killProcess);

                query = "UPDATE "+process_table+" SET end_time = '"+ LocalDateTime.now() +
                        "' WHERE identifier IN "+process_array+" AND end_time IS NULL";
                int update_count = connection.createStatement().executeUpdate(query);

                TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO,
                        "Cleaned up "+update_count+" out of "+unfinished_process_identifiers.size()+" processes");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    private void killProcess(String pid){
        try {
            if (SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec("taskkill /F /PID " + pid);
            else Runtime.getRuntime().exec("kill -9 " + pid);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void registerServerProcess() {
        TPS_ParameterClass parameters = dbConnector.getParameters();
        String process_table = parameters.getString(ParamString.DB_TABLE_PROCESSES);

        String process_identifier = ManagementFactory.getRuntimeMXBean().getName();
        int process_id = Integer.parseInt(process_identifier.split("@")[0]); // e.g. PID@HOSTNAME -> PID

        final String query = "INSERT INTO " + process_table + " (identifier, server_ip, p_id, start_time, host, sim_key, sha512)" +
                       " VALUES ('" + process_identifier + "', " + this.iAddr + ", " + process_id + ", '" + LocalDateTime.now() +
                       "', '" + server_context.getHostname() + "', 'IDLE', '')";

        try{
            Connection connection = dbConnector.getConnection(this);
            int a = connection.createStatement().executeUpdate(query);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO,
                    "Inserted "+a+" server process.");

        }catch (SQLException e){
            e.printStackTrace();
        }
    }


    private void registerServerIntoDatabase(){
        TPS_ParameterClass parameters = dbConnector.getParameters();

        String server_table = parameters.getString(ParamString.DB_TABLE_SERVERS);
        String server_name = server_context.getHostname();

        String query = "SELECT * FROM " + server_table + " WHERE server_name = '" + server_name + "'";

        try{
            Connection connection = dbConnector.getConnection(this);
            ResultSet server_result = connection.createStatement().executeQuery(query);

            //check if server is already known
            if (server_result.next()) {
                query = "UPDATE " + server_table + " SET server_online = TRUE WHERE server_name = '" + server_name + "'";
                server_result.close();
            }else {
                query = "INSERT INTO " + server_table + " (server_ip, server_online, server_cores, server_name) VALUES(" +
                        this.iAddr + ", TRUE, " + this.server_context.getCoreCount() + ", '" + server_name + "')";
            }

            connection.createStatement().executeUpdate(query);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {

    }
}
