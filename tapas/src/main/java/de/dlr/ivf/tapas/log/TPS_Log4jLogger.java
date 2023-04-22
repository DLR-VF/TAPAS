/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.log;


import de.dlr.ivf.tapas.runtime.util.IPInfo;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import org.apache.log4j.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to connect the TAPAS logging behaviour to the Log4J classes
 *
 * @author hein_mh
 */

class TPS_Log4jLogger implements TPS_LoggingInterface {

    private final Level[] levelArray;

    private final Map<Thread, Map<Class<?>, Logger>> loggerMap;

    private final TPS_ParameterClass parameterClass;

    /**
     * Standard constructor which initializes the pattern and maps.
     */
    public TPS_Log4jLogger(TPS_ParameterClass parameterClass) {
        this.parameterClass = parameterClass;
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%5p %d %m%n")));
        Logger.getRootLogger().setLevel(Level.ALL);
        this.loggerMap = new HashMap<>();
        SeverenceLogLevel[] sLevelArray = SeverenceLogLevel.values();
        this.levelArray = new Level[sLevelArray.length];
        for (SeverenceLogLevel sLog : sLevelArray) {
            this.levelArray[sLog.getIndex()] = Level.toLevel(sLog.getKey(parameterClass));
        }
    }

    public boolean closeLogger() {
        //no solution yet :(

        return true;
    }

    /**
     * Method to convert the SeverenceLogLevel to a log Level class.
     *
     * @param sLog The SeverenceLogLevel
     * @return The according Level
     */
    private Level getLevel(SeverenceLogLevel sLog) {
        return this.levelArray[sLog.getIndex()];
    }

    /**
     * Gets the logger for the specified class
     *
     * @param callerClass Caling class
     * @return Logger for this class
     */
    private synchronized Logger getLogger(Class<?> callerClass) {
        Thread t = Thread.currentThread();
        Map<Class<?>, Logger> map = this.loggerMap.get(t);
        if (map == null) {
            map = new HashMap<>();
            this.loggerMap.put(t, map);
            Logger threadLogger = Logger.getLogger(t.getName());
            String host = "nohost";
            try {
                host = IPInfo.getEthernetInetAddress().getHostAddress();
            } catch (IOException e1) {
                System.err.println("Found no host to name the log files");
                e1.printStackTrace();
            }
            try {
                String filename = "";
                if (this.parameterClass.isDefined(ParamString.FILE_WORKING_DIRECTORY) && this.parameterClass.isDefined(
                        ParamString.RUN_IDENTIFIER)) {
                    filename = this.parameterClass.getString(ParamString.FILE_WORKING_DIRECTORY);

                    if (!filename.endsWith(System.getProperty("file.separator"))) {
                        filename = filename + System.getProperty("file.separator");
                    }

                    //make the dir-part of the file
                    filename = filename + this.parameterClass.LOG_DIR + this.parameterClass.getString(
                            ParamString.RUN_IDENTIFIER) + System.getProperty("file.separator");
                    //mkdir makes the directory only if it NOT exists!
                    new File(filename).mkdirs();


                    filename = filename + this.parameterClass.getString(ParamString.RUN_IDENTIFIER) + "-" + host
                            //+ "-" + t.getName()
                            + ".log";

                    Appender app = new FileAppender(new PatternLayout("%5p %d - %m%n"), filename, true);
                    threadLogger.addAppender(app);
                }
            } catch (RuntimeException | IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
        Logger logger = map.get(callerClass);
        if (logger == null) {
            logger = Logger.getLogger(t.getName() + "." + callerClass.getName());
            map.put(callerClass, logger);

            // These two lines log the instantiation of a new logger for the callerClass.
            // Furthermore they show how to use this class:
            // a, ask if this class with the given log parameter is logged at all -> this should be used when complex
            // texts are created to avoid concatenating the string without use
            // b, call the log method itself
            if (TPS_Logger.isLogging(TPS_Log4jLogger.class, HierarchyLogLevel.CLIENT, SeverenceLogLevel.FINEST)) {
                TPS_Logger.log(TPS_Log4jLogger.class, HierarchyLogLevel.CLIENT, SeverenceLogLevel.FINEST,
                        "Created logger for class: " + callerClass.getName());
            }
        }
        return logger;
    }

    /**
     * Method to check if logging for a specific class and level is enabled
     */
    public boolean isLogging(Class<?> callerClass, SeverenceLogLevel sLog) {
        return this.getLogger(callerClass).isEnabledFor(this.getLevel(sLog));
    }

    /**
     * Log a text for the given class and log level.
     */
    public void log(Class<?> callerClass, SeverenceLogLevel sLog, String text) {
        this.getLogger(callerClass).log(this.getLevel(sLog), text);
    }

    /**
     * Log a text for a given class and log level and attaching a given exception.
     */
    public void log(Class<?> callerClass, SeverenceLogLevel sLog, String text, Throwable throwable) {
        this.getLogger(callerClass).log(this.getLevel(sLog), text, throwable);
    }

}
