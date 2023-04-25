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

public class DaemonControlProperties extends ControlProperties {
    /**
     * The constructor builds the properties object and fills it with the key value pairs from the file.
     *
     * @param file the daemon.properties file
     */
    public DaemonControlProperties(File file) {
        super(file);
        try {
            FileInputStream input = new FileInputStream(file);
            this.props.load(input);
            input.close();
            boolean changed = false;
            for (DaemonControlPropKey key : DaemonControlPropKey.values()) {
                if (!this.props.containsKey(key.name())) {
                    this.props.put(key.name(), null);
                    changed = true;
                }
            }
            if (changed) {
                this.updateFile();
            }
        } catch (FileNotFoundException e) {
            for (DaemonControlPropKey key : DaemonControlPropKey.values()) {
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
    public String get(DaemonControlPropKey key) {
        return this.props.getProperty(key.name());
    }

    /**
     * Sets a key value pair for this property.
     *
     * @param key   simulation property key
     * @param value simulation property value
     */
    public void set(DaemonControlPropKey key, String value) {
        this.props.setProperty(key.name(), value);
        this.changed = true;
    }


    /**
     * Simulation Property Keys which have to be defined in the simulation.properties file
     *
     * @author mark_ma
     */
    public enum DaemonControlPropKey {
        /**
         * the login file
         */
        LOGIN_CONFIG
    }
}
