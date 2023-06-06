module de.dlr.ivf.tapas.environment.gui {
    requires java.desktop;

    requires org.jfree.jfreechart;
    requires org.apache.commons.lang3;
    requires commons.cli;
    requires java.sql;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.controls;
    requires com.fasterxml.jackson.databind;
    requires lombok;
    requires com.opencsv;

    requires de.dlr.ivf.tapas.analyzer;
    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.util;
    requires de.dlr.ivf.tapas.environment;
    requires transitive javafx.swing;
    requires de.dlr.ivf.tapas.tools;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.tapas.matrixtool;
    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.tapas.model;

    //scenicview
    requires transitive javafx.web;
    requires org.scenicview.scenicview;

    opens de.dlr.ivf.tapas.environment.gui.fx.view.controllers to javafx.fxml;
    opens de.dlr.ivf.tapas.environment.gui.fx.view to javafx.fxml;
    exports de.dlr.ivf.tapas.environment.gui.legacy to javafx.graphics;
    exports de.dlr.ivf.tapas.environment.gui.fx to  javafx.graphics;
}