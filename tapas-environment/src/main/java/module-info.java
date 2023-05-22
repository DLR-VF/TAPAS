module de.dlr.ivf.tapas.environment {

    requires java.sql;
    requires de.dlr.ivf.api.io;
    requires lombok;
    requires com.fasterxml.jackson.annotation;

    exports de.dlr.ivf.tapas.environment;
    exports de.dlr.ivf.tapas.environment.configuration;
    exports de.dlr.ivf.tapas.environment.dto;
    exports de.dlr.ivf.tapas.environment.model;

    opens de.dlr.ivf.tapas.environment.configuration to com.fasterxml.jackson.databind;
    opens de.dlr.ivf.tapas.environment.dto to de.dlr.ivf.api.io;
}