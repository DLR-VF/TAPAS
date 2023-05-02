module de.dlr.ivf.tapas.tools {
    exports de.dlr.ivf.tapas.tools;
    requires java.sql;

    requires org.apache.commons.lang3;
    requires com.fasterxml.jackson.databind;

    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.tapas.model;
}