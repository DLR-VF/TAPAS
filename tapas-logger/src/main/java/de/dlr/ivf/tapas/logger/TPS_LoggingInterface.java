/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.logger;

/**
 * Interface for the logging function. This interface also holds the enums for the HierarchyLogLevel and SeverenceLogLevel.
 *
 * @author hein_mh
 */
public interface TPS_LoggingInterface {

    boolean closeLogger();

    /**
     * Method to check if logging is enabled for the given parameters at the default HierarchyLogLevel.
     *
     * @param callerClass The calling Class
     * @param sLog        The SeverenceLogLevel
     * @return true if logging is enabled for the given combination
     */
    boolean isLogging(Class<?> callerClass, SeverityLogLevel sLog);

    /**
     * Method to log an info and parse an Exception.
     *
     * @param callerClass The calling class
     * @param sLog        The SeverenceLogLevel
     * @param text        The text to log
     * @param throwable   The exception to log
     */
    void log(Class<?> callerClass, SeverityLogLevel sLog, String text, Throwable throwable);

    /**
     * Method to log an info.
     *
     * @param callerClass The calling class
     * @param sLog        The SeverenceLogLevel
     * @param text        The text to log
     */
    void log(Class<?> callerClass, SeverityLogLevel sLog, String text);


}
