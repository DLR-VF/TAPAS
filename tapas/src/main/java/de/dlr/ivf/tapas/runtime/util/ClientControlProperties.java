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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ClientControlProperties extends ControlProperties {
    /**
     * The constructor builds the properties object and fills it with the key value pairs from the file.
     *
     * @param file the client.properties file
     */
    public ClientControlProperties(File file) {
        super(file);
        try {
            FileInputStream input = new FileInputStream(file);
            this.props.load(input);
            input.close();
            boolean changed = false;
            for (ClientControlPropKey key : ClientControlPropKey.values()) {
                if (!this.props.containsKey(key.name())) {
                    this.props.put(key.name(), "");
                    changed = true;
                }
            }
            if (changed) {
                this.updateFile();
            }
        } catch (FileNotFoundException e) {
            for (ClientControlPropKey key : ClientControlPropKey.values()) {
                this.props.put(key.name(), "");
                this.updateFile();
            }
        } catch (IOException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, e);
        }
    }

    /**
     * @param key simulation property key
     * @return value corresponding to the given key
     */
    public String get(ClientControlPropKey key) {
        return this.props.getProperty(key.name());
    }

    /**
     * Sets a key value pair for this property.
     *
     * @param key   simulation property key
     * @param value simulation property value
     */
    public void set(ClientControlPropKey key, String value) {
        this.props.setProperty(key.name(), value);
        this.changed = true;
    }


    /**
     * Simulation Property Keys which have to be defined in the simulation.properties file
     *
     * @author mark_ma
     */
    public enum ClientControlPropKey {
        /**
         * The directory of the last loaded simulation
         */
        LAST_SIMULATION_DIR,
        /**
         * The filename of the last loaded simulation
         */
        LAST_SIMULATION,

        /**
         * the directory of the tapas environment under windows
         */
        TAPAS_DIR_WIN,

        /**
         * the directory of the tapas environment under linux
         */
        TAPAS_DIR_LINUX,
        /**
         * the login file
         */
        LOGIN_CONFIG,
        /**
         * last TUM export folder
         */
        LAST_TUM_EXPORT
    }
}
