/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.util;

import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * This class can be used to determine the CPU usage of a Linux or a Windows System. For Linux the /proc/stat file and
 * for Windows the WMICprocess is used.
 *
 * @author mark_ma
 */
public class CPUUsage {

    /**
     * Method to retrieve the CPU usage
     */
    private CPUUsageRetrieveType cpuURT;
    private double lastProcTime = 0;
    private double lastTotalTime = 0;
    private boolean firstRun = true;

    /**
     * The constructor calls the init() method.
     */
    public CPUUsage() {
        this.init();
    }

    /**
     * @return CPU usage
     * @throws IOException          This exception is thrown if detecting the CPU usage failed
     * @throws InterruptedException
     */
    public double getCPUUsage() throws IOException, InterruptedException {
        double proc = 0;
        if (this.cpuURT != null) {
            switch (this.cpuURT) {
                case PROC:
                    proc = this.getPROC();
                    break;
                case WMIC:
                    proc = this.getWMIC();
                    break;
            }
        }
        return Math.max(0, Math.min(1, proc));
    }

    /**
     * @return CPU usage from Linux using top
     * @throws IOException          if reading file failed
     * @throws InterruptedException
     */
    private double getPROC() throws IOException {


        double cpu = -1, procTime = 0, totalTime;

        FileInputStream fis = new FileInputStream("/proc/stat");
        InputStreamReader inS = new InputStreamReader(fis);
        BufferedReader in = new BufferedReader(inS);

        //first line is the overall cpu performance
        String line = in.readLine();
        in.close();
        inS.close();
        fis.close();
        String tmp;
        if (!"".equals(line)) {
            StringTokenizer st = new StringTokenizer(line);
            //values are: cpu <user-time> <nice-time> <kernel-time> <idle-time> ...
            if (st.countTokens() > 4) {
                st.nextToken(); // cpu id (ignore)
                tmp = st.nextToken(); // user-time
                procTime += Double.parseDouble(tmp);
                tmp = st.nextToken(); // nice-time
                procTime += Double.parseDouble(tmp);
                tmp = st.nextToken(); // kernel-time
                procTime += Double.parseDouble(tmp);
                tmp = st.nextToken(); // idle time
                totalTime = procTime + Double.parseDouble(tmp);
                if (this.firstRun) {
                    cpu = 0;
                    this.firstRun = false;
                } else {
                    cpu = (procTime - this.lastProcTime) / (totalTime - this.lastTotalTime);
                }
                this.lastTotalTime = totalTime;
                this.lastProcTime = procTime;
            }
        }

        return cpu;
    }

    /**
     * @return CPU usage from Windows WMIC process
     * @throws IOException This exception is thrown if the communication with the Windows WMIC process failed
     */
    private double getWMIC() throws IOException {

        //launch our own program to determine the cpu-performance

        //the only purpose of this program is to print the actual cpu-performance on the command line

        ProcessBuilder pb = new ProcessBuilder("CpuUsageCpp.exe");


        Process p = pb.start();
        double proc = 0;
        try {
            // init input reader
            InputStreamReader is = new InputStreamReader(p.getInputStream());
            BufferedReader reader = new BufferedReader(is);

            //read a line
            String line = reader.readLine();

            if (line == null) return 0;
            //read whatever
            while (reader.readLine() != null) {

            }
            // close reader
            reader.close();
            is.close();

            // parse cpu performance
            proc = Double.parseDouble(line) / 100.0;

        } catch (IOException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, e.getMessage(), e);
        }

        //get exitValue (0), this should be done to prevent "zombie-processes",
        //which are terminated but still hold an exit value.
        p.exitValue();
        //kill 'em all!
        p.destroy();


        return proc;
    }

    /**
     * This method tries to find a method to retrieve the CPU usage from the system.
     *
     * @throws RuntimeException This exception is throw if not method was found
     */
    private void init() {
        boolean ok = false;

        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            // windows operating system
            cpuURT = CPUUsageRetrieveType.WMIC;
        } else {
            // other operating systems, we expect a linux system
            cpuURT = CPUUsageRetrieveType.PROC;
        }

        try {
            getCPUUsage();
            ok = true;
        } catch (Exception e0) {
            for (CPUUsageRetrieveType cpuURT : CPUUsageRetrieveType.values()) {
                this.cpuURT = cpuURT;
                try {
                    getCPUUsage();
                    ok = true;
                    break;
                } catch (Exception e1) {
                    // nothing to do here
                }
            }
        }

        if (!ok) {
            this.cpuURT = null;
            //throw new RuntimeException("Found no method to retrieve system cpu usage");
        }
    }

    /**
     * Methods to retrieve the CPU usage for Windows and Linux systems
     *
     * @author mark_ma
     */
    private enum CPUUsageRetrieveType {
        /**
         * Method for LINUX with the command top
         */
        PROC,
        /**
         * Method for windows with the WMIC process
         */
        WMIC
    }
}
