module de.dlr.ivf.tapas {

    requires java.sql;
    requires java.desktop;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires disruptor;
    requires commons.math3;

    requires org.postgresql.jdbc;
    requires java.rmi;
    requires java.management;
    requires lombok;
    requires com.fasterxml.jackson.databind;

    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.util;
    requires de.dlr.ivf.tapas.model;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.api.converter;
    requires de.dlr.ivf.api.service;
    requires javacsv;

    opens de.dlr.ivf.tapas to com.fasterxml.jackson.databind;
    opens de.dlr.ivf.tapas.dto to de.dlr.ivf.api.io;
    exports de.dlr.ivf.tapas;
    exports de.dlr.ivf.tapas.simulation;
    opens de.dlr.ivf.tapas.simulation to com.fasterxml.jackson.databind;
}