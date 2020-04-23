package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.util.DaemonControlProperties;
import de.dlr.ivf.tapas.runtime.util.DaemonControlProperties.DaemonControlPropKey;
import de.dlr.ivf.tapas.runtime.util.IPInfo;
import de.dlr.ivf.tapas.util.TPS_Argument;
import de.dlr.ivf.tapas.util.TPS_Argument.TPS_ArgumentType;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author mark_ma
 */
public final class SimulationDaemon {

    private final static String sysoutPrefix = "-- Daemon -- ";
    /**
     * Date format for the exception file appendix
     */
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss");
    private static Process p;
    private static SimulationDaemon daemon = null;
    private static TPS_DB_Connector con = null;
    private static final List<Process> processes = new ArrayList<>(); //for later use when the Daemon will be able to start multiple servers
    /**
     * Parameter array with all parameters for this main method.<br>
     * PARAMETERS[0] = tapas network directory path
     */
    private static final TPS_ArgumentType<?>[] PARAMETERS = {new TPS_ArgumentType<>("tapas network directory path",
            File.class)};
    private String hostname;
    private final String[] arguments;
    private Timer timer;

    /**
     * This main method starts the {@link SimulationServer} and catches each exception which is thrown and stores it in a
     * file with the current timestamp. After 2.5 seconds the simulation server is restarted.
     *
     * @param args
     */
    private SimulationDaemon(String[] args) {
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        try {
            this.hostname = IPInfo.getHostname();
            File propFile = new File("daemon.properties");
            DaemonControlProperties prop = new DaemonControlProperties(propFile);
            String runtimeConf = prop.get(DaemonControlPropKey.LOGIN_CONFIG);
            if (runtimeConf == null || runtimeConf.length() == 0) {
                runtimeConf = "runtime.csv";
                prop.set(DaemonControlPropKey.LOGIN_CONFIG, runtimeConf);
                prop.updateFile();
            }
            Object[] parameters = TPS_Argument.checkArguments(args, PARAMETERS);

            File tapasNetworkDirectory = ((File) parameters[0]).getParentFile();

            File runtimeFile = new File(new File(tapasNetworkDirectory, parameterClass.SIM_DIR), runtimeConf);
            parameterClass.loadRuntimeParameters(runtimeFile);
            parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tapasNetworkDirectory.getPath());

            con = new TPS_DB_Connector(parameterClass);
            con.executeUpdate("UPDATE " + parameterClass.getString(ParamString.DB_TABLE_SERVERS) +
                    " SET remote_boot = true WHERE server_name = '" + hostname + "'", this);

            this.timer = new Timer();
            ServerProcessChecker spc = new ServerProcessChecker();
            timer.schedule(spc, 10, 750);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.arguments = args;
        Runtime.getRuntime().addShutdownHook(new ShutDownProcedure());
    }

    public static void main(String[] args) {

        try {
            //Set -Ddebug=true in the VM-options to enable debugging mode
            String debugString = System.getProperty("debug");

            if (debugString != null && debugString.equalsIgnoreCase("true")) SimulationServer.main(args);
            else {
                RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

                List<String> command = new ArrayList<>();
                command.add("java");
                command.addAll(runtime.getInputArguments());
                command.add("-cp");
                command.add(System.getProperty("java.class.path", "."));
                command.add(SimulationServer.class.getName());
                command.addAll(Arrays.asList(args));

                p = new ProcessBuilder(command).inheritIO().redirectErrorStream(true).start();
                processes.add(p);
            }
        } catch (Exception e) {
            try {
                File f = new File(
                        "SimulationDaemonException_" + SDF.format(new Date(System.currentTimeMillis()) + ".log"));
                if (!f.exists()) f.createNewFile();
                PrintWriter pw = new PrintWriter(f);
                pw.flush();
                pw.close();
                Thread.sleep(2500);
                e.printStackTrace();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        if (daemon == null) daemon = new SimulationDaemon(args);
    }


    public void startServer() {

        if (!p.isAlive()) {
            System.out.println(sysoutPrefix + "Launching Server...");
            Runnable r = () -> main(arguments);
            Thread t = new Thread(r);
            t.start();
        } else System.out.println(sysoutPrefix + "Previous Process is still running...");

    }

    /**
     * Works as a shutdown hook and will block the cleanup procedure until its (if any) {@link SimulationServer} is shut down
     *
     * @author sche_ai
     */
    private class ShutDownProcedure extends Thread {

        @Override
        public void run() {

            try {
                con.executeUpdate("UPDATE " + con.getParameters().getString(ParamString.DB_TABLE_SERVERS) +
                                " SET remote_boot = false WHERE server_name = '" + hostname + "'",
                        this); //shutting down the daemon means no remote server start up
                if (p.isAlive()) {
                    System.out.println(sysoutPrefix + "Waiting for Server to shut down...");
                    con.executeUpdate("UPDATE " + con.getParameters().getString(ParamString.DB_TABLE_PROCESSES) +
                                    " SET shutdown = true WHERE host = '" + hostname + "' AND end_time IS NULL",
                            this);   //set the shutdown flag on the server process
                }
                while (p.isAlive()) Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerProcessChecker extends TimerTask {

        @Override
        public void run() {

            try (ResultSet rs = con.executeQuery(
                    "SELECT * FROM " + con.getParameters().getString(ParamString.DB_TABLE_SERVERS) +
                            " WHERE server_name = '" + hostname + "'", this)) {
                if (rs.next()) {
                    boolean startup = rs.getBoolean("server_boot_flag");
                    if (startup) startServer();
                }
            } catch (Exception e) {
                //nothing to do
            }
        }
    }
}
