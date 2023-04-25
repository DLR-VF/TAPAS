/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.logger;


import de.dlr.ivf.tapas.parameter.ParamString;
import de.dlr.ivf.tapas.parameter.TPS_ParameterClass;

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
    boolean isLogging(Class<?> callerClass, SeverenceLogLevel sLog);

    /**
     * Method to log an info and parse an Exception.
     *
     * @param callerClass The calling class
     * @param sLog        The SeverenceLogLevel
     * @param text        The text to log
     * @param throwable   The exception to log
     */
    void log(Class<?> callerClass, SeverenceLogLevel sLog, String text, Throwable throwable);

    /**
     * Method to log an info.
     *
     * @param callerClass The calling class
     * @param sLog        The SeverenceLogLevel
     * @param text        The text to log
     */
    void log(Class<?> callerClass, SeverenceLogLevel sLog, String text);

    /**
     * The HierarchyLogLevel: specifies the Hierarchy within the application
     *
     * @author hein_mh
     */
    enum HierarchyLogLevel {
        OFF(0), APPLICATION(1), CLIENT(2), THREAD(3), HOUSEHOLD(4), PERSON(5), PLAN(6), EPISODE(7), ALL(8);

        private final int index;

        private final int value;

        private final int mask;

        /**
         * Constructor, which initialises the internal variables for the given index.
         *
         * @param index
         */
        HierarchyLogLevel(int index) {
            this.index = index;
            this.mask = (int) Math.pow(2, index) - 1;
            this.value = (int) Math.pow(2, Math.max(0, index - 1));
        }

        /**
         * Getter for the index of this instance. The higher the more important.
         *
         * @return
         */
        public int getIndex() {
            return index;
        }

        /**
         * This method returns true if the instance is included in the logging
         *
         * @param hLog
         * @return
         */
        public boolean includes(HierarchyLogLevel hLog) {
            return (this.mask & hLog.value) > 0;
        }
    }

    /**
     * The SeverenceLogLevel: specifies the Hierarchy within the severence
     *
     */
    enum SeverenceLogLevel {
        OFF(0, ParamString.LOG_LEVEL_OFF), FATAL(1, ParamString.LOG_LEVEL_FATAL), ERROR(2,
                ParamString.LOG_LEVEL_ERROR), SEVERE(3, ParamString.LOG_LEVEL_SEVERE), WARN(4,
                ParamString.LOG_LEVEL_WARN), INFO(5, ParamString.LOG_LEVEL_INFO), DEBUG(6,
                ParamString.LOG_LEVEL_DEBUG), FINE(7, ParamString.LOG_LEVEL_FINE), FINER(8,
                ParamString.LOG_LEVEL_FINER), FINEST(9, ParamString.LOG_LEVEL_FINEST), ALL(10,
                ParamString.LOG_LEVEL_ALL);
        private final int index;

        /**
         * The corresponding ParamString to this log level
         */
        private final ParamString key;

        /**
         * @param index index of this level
         * @param key   corresponding key of this level
         */
        SeverenceLogLevel(int index, ParamString key) {
            this.index = index;
            this.key = key;
        }

        /**
         * @return index
         */
        public int getIndex() {
            return index;
        }

        /**
         * @param parameterClass parameter class reference
         * @return corresponding ParamString to this log level
         */
        public String getKey(TPS_ParameterClass parameterClass) {
            return (key != null ? parameterClass.getString(key) : null);
        }

        /**
         * @param sll SeverenceLogLevel to test
         * @return true if this level includes the given level
         */
        public boolean includes(SeverenceLogLevel sll) {
            return this.index >= sll.index;
        }

    }


}
