module de.dlr.ivf.tapas.daemon {
    requires java.sql;
    requires de.dlr.ivf.tapas;
    requires com.fasterxml.jackson.databind;
    requires lombok;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.tapas.environment;
    requires de.dlr.ivf.tapas.logger;

    opens de.dlr.ivf.tapas.daemon.configuration to com.fasterxml.jackson.databind;

    exports de.dlr.ivf.tapas.daemon.configuration to com.fasterxml.jackson.databind;
}