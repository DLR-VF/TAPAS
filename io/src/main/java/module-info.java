module de.dlr.ivf.api.io {

    requires lombok;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires org.postgresql.jdbc;

    requires de.dlr.ivf.api.converter;

    requires java.desktop;
    requires org.apache.commons.lang3;
    requires com.zaxxer.hikari;

    exports de.dlr.ivf.api.io.configuration;

    opens de.dlr.ivf.api.io.configuration to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.api.io.crud.read;
    exports de.dlr.ivf.api.io.annotation;
    exports de.dlr.ivf.api.io.util;
    exports de.dlr.ivf.api.io.conversion;
    exports de.dlr.ivf.api.io.crud.write;
    exports de.dlr.ivf.api.io.connection;
}