/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.util;

import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

abstract class ControlProperties {
    /**
     * Properties object to hold the key value pairs
     */
    Properties props;
    String configFile;
    boolean changed;

    public ControlProperties(File file) {
        this.changed = false;
        this.props = new Properties();
        configFile = file.getPath();
    }


    public boolean isChanged() {
        return changed;
    }


    /**
     * Method to store the new values to file
     */
    public void updateFile() {
        try {
            FileOutputStream output = new FileOutputStream(this.configFile);
            this.props.store(output, null);
            output.close();
            this.changed = false;
        } catch (IOException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, e);
        }
    }
}
