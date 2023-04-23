module de.dlr.ivf.tapas.daemon {
    requires de.dlr.ivf.tapas;
    requires com.fasterxml.jackson.databind;
    requires lombok;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.tapas.environment;

    opens de.dlr.ivf.tapas.daemon.configuration to com.fasterxml.jackson.databind;

    exports de.dlr.ivf.tapas.daemon.configuration to com.fasterxml.jackson.databind;
}