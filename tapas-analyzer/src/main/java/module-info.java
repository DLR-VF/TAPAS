module de.dlr.ivf.tapas.analyzer {

    requires java.desktop;
    requires java.sql;

    requires org.jfree.jfreechart;
    requires org.apache.commons.collections4;
    requires jgoodies.forms;
    requires javacsv;
    requires jxl;

    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.logger;

    exports de.dlr.ivf.tapas.analyzer.tum.constants;
    exports de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;
    exports de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;
    exports de.dlr.ivf.tapas.analyzer.tum.databaseConnector;
    exports de.dlr.ivf.tapas.analyzer.inputfileconverter;
    exports de.dlr.ivf.tapas.analyzer.tum;
    exports de.dlr.ivf.tapas.analyzer.tum.results;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.tapas.util;
    requires de.dlr.ivf.tapas.tools;
    requires de.dlr.ivf.tapas.model;
}