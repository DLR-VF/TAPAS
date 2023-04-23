module de.dlr.ivf.api.io {

    requires lombok;
    requires com.fasterxml.jackson.databind;
    requires java.sql;

    exports de.dlr.ivf.api.io.configuration.model;
    exports de.dlr.ivf.api.io;

    opens de.dlr.ivf.api.io.configuration.model to com.fasterxml.jackson.databind;
}