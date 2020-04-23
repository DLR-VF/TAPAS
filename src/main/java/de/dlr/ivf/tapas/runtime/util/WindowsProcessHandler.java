package de.dlr.ivf.tapas.runtime.util;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * This class provides basic methods to find and kill processes on a Windows Operating System.
 *
 * @author mark_ma
 */
public class WindowsProcessHandler {

    /**
     * This method finds the system process by process id and name.
     *
     * @param pid  process id
     * @param name process name
     * @return true if process was found, false otherwise
     */
    public static boolean findProcess(int pid, String name) {
        try {
            String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "tasklist");
                Process proc = pb.start();

                InputStream inputstream = proc.getInputStream();
                InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
                BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
                String line;
                StringTokenizer st;
                int loop = -1;
                boolean start = false;

                while ((line = bufferedreader.readLine()) != null) {
                    if (start) {
                        st = new StringTokenizer(line, " ");
                        loop = st.countTokens() - 5;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < loop; i++) {
                            sb.append(st.nextToken() + " ");
                        }
                        sb.setLength(sb.length() - 1);
                        if (sb.toString().equals(name) && pid == Integer.parseInt(st.nextToken())) {
                            return true;
                        }
                    }
                    start = start || line.startsWith("=");
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder("ps", "-C java", "-o pid,comm");
                Process proc = pb.start();

                InputStream inputstream = proc.getInputStream();
                InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
                BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
                String line;
                StringTokenizer st;

                line = bufferedreader.readLine(); //first line is the headder

                while ((line = bufferedreader.readLine()) != null) {
                    st = new StringTokenizer(line, " ");
                    if (st.countTokens() == 2) {
                        if (pid == Integer.parseInt(st.nextToken()) && st.nextToken().equals(name)) {
                            return true;
                        }
                    }
                }

            }
        } catch (Exception e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, e);
        }

        return false;
    }

    /**
     * This method kills the process corresponding to the given pid
     *
     * @param pid process id
     * @return true if process was killed, false otherwise
     */
    public static int killProcess(int pid) {
        try {
            String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "TASKKILL /PID " + pid);
                Process proc = pb.start();
                try {
                    proc.waitFor();
                } catch (InterruptedException e) {
                    TPS_Logger.log(SeverenceLogLevel.ERROR, e);
                }
                byte[] bytes = new byte[proc.getInputStream().available()];
                proc.getInputStream().read(bytes);
                if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                    TPS_Logger.log(SeverenceLogLevel.WARN, new String(bytes));
                }
                return proc.exitValue();
            } else {
                ProcessBuilder pb = new ProcessBuilder("kill", Integer.toString(pid));
                Process proc = pb.start();
                try {
                    proc.waitFor();
                } catch (InterruptedException e) {
                    TPS_Logger.log(SeverenceLogLevel.ERROR, e);
                }
                byte[] bytes = new byte[proc.getInputStream().available()];
                proc.getInputStream().read(bytes);
                if (TPS_Logger.isLogging(SeverenceLogLevel.WARN)) {
                    TPS_Logger.log(SeverenceLogLevel.WARN, new String(bytes));
                }
                return proc.exitValue();
            }
        } catch (IOException e) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, e);
            return -1;
        }
    }
}
