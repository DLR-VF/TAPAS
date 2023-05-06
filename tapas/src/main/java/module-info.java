module de.dlr.ivf.tapas {

    requires java.sql;
    requires java.desktop;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires commons.cli;
    requires disruptor;
    requires zip4j;
    requires jxl;
    requires commons.math3;

    requires org.postgresql.jdbc;
    requires java.rmi;
    requires java.management;
    requires poi;
    requires lombok;
    requires com.fasterxml.jackson.databind;

    requires de.dlr.ivf.tapas.matrixtool;
    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.util;
    requires de.dlr.ivf.tapas.model;
    requires de.dlr.ivf.tapas.tools;
    requires de.dlr.ivf.api.io;

    exports de.dlr.ivf.tapas;

}