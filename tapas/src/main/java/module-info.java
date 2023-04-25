module de.dlr.ivf.tapas {

    requires java.sql;
    requires java.desktop;
    requires javacsv;
    requires org.apache.commons.lang3;
    requires commons.cli;
    requires disruptor;
    requires zip4j;
    requires jxl;

    requires org.postgresql.jdbc;
    requires javafx.fxml;
    requires java.rmi;
    requires javafx.swing;
    requires java.management;
    requires poi;
    requires org.apache.commons.collections4;
    requires jfreechart;
    requires jgoodies.forms;
    requires commons.math3;
    requires javafx.controls;

    requires de.dlr.ivf.tapas.matrixtool;
    requires de.dlr.ivf.tapas.logger;

    exports de.dlr.ivf.tapas;

}