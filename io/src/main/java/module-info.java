module de.dlr.ivf.api.io {

    requires lombok;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires org.postgresql.jdbc;

    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.api.converter;

    requires java.desktop;
    requires org.apache.commons.lang3;


    exports de.dlr.ivf.api.io.configuration.model;
    exports de.dlr.ivf.api.io;

    opens de.dlr.ivf.api.io.configuration.model to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.api.io.reader;
    exports de.dlr.ivf.api.io.annotation;
    exports de.dlr.ivf.api.io.util;
    exports de.dlr.ivf.api.io.conversion;
}