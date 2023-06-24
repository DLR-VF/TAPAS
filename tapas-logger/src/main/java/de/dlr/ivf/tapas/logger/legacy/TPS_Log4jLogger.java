/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.logger.legacy;


import de.dlr.ivf.tapas.util.IPInfo;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final Path logDirectory;

    private final String runIdentifier;

    private final PatternLayout layout;

    /**
     * Standard constructor which initializes the pattern and maps.
     */
    public TPS_Log4jLogger(Path logDirectory, String runIdentifier, Level[] levelArray) {
        this.logDirectory = logDirectory;
        this.runIdentifier = runIdentifier;
        this.levelArray = levelArray;

       this.layout = PatternLayout.newBuilder()
                .withPattern("%5p %d %m%n")
                .build();

        ConsoleAppender appender = ConsoleAppender.newBuilder()
                .setLayout(layout)
                .build();

        LoggerContext loggerContext = Configurator.initialize(new DefaultConfiguration());
        loggerContext.getConfiguration().addAppender(appender);
        Configurator.setLevel(LogManager.getRootLogger(),Level.ALL);
        this.loggerMap = new HashMap<>();
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
    private Level getLevel(SeverityLogLevel sLog) {
        return this.levelArray[sLog.ordinal()];
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
            Logger threadLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(t.getName());
            String host = "nohost";
            try {
                host = IPInfo.getEthernetInetAddress().getHostAddress();
            } catch (IOException e1) {
                System.err.println("Found no host to name the log files");
                e1.printStackTrace();
            }
            try {
                String filename = this.runIdentifier + "-" + host+ ".log";
                Path logOutputFile = logDirectory.resolve(filename);
                Files.createFile(logOutputFile);


                PatternLayout layout = PatternLayout.newBuilder().withPattern("%5p %d - %m%n").build();
                Appender app = FileAppender.newBuilder()
                        .withFileName(logOutputFile.toString())
                        .setLayout(layout)
                        .withAppend(true)
                        .build();
                threadLogger.addAppender(app);

            } catch (RuntimeException | IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
        Logger logger = map.get(callerClass);
        if (logger == null) {
            logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(t.getName() + "." + callerClass.getName());
            map.put(callerClass, logger);

            // These two lines log the instantiation of a new logger for the callerClass.
            // Furthermore they show how to use this class:
            // a, ask if this class with the given log parameter is logged at all -> this should be used when complex
            // texts are created to avoid concatenating the string without use
            // b, call the log method itself
            if (TPS_Logger.isLogging(TPS_Log4jLogger.class, HierarchyLogLevel.CLIENT, SeverityLogLevel.FINEST)) {
                TPS_Logger.log(TPS_Log4jLogger.class, HierarchyLogLevel.CLIENT, SeverityLogLevel.FINEST,
                        "Created logger for class: " + callerClass.getName());
            }
        }
        return logger;
    }

    /**
     * Method to check if logging for a specific class and level is enabled
     */
    public boolean isLogging(Class<?> callerClass, SeverityLogLevel sLog) {
        return true;//this.getLogger(callerClass).isEnabledFor(this.getLevel(sLog));
    }

    /**
     * Log a text for the given class and log level.
     */
    public void log(Class<?> callerClass, SeverityLogLevel sLog, String text) {
        this.getLogger(callerClass).log(this.getLevel(sLog), text);
    }

    /**
     * Log a text for a given class and log level and attaching a given exception.
     */
    public void log(Class<?> callerClass, SeverityLogLevel sLog, String text, Throwable throwable) {
        this.getLogger(callerClass).log(this.getLevel(sLog), text, throwable);
    }

}
