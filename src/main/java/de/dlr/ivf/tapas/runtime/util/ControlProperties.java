package de.dlr.ivf.tapas.runtime.util;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;

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
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.ERROR, e);
        }
    }
}
