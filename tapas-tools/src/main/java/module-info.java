module de.dlr.ivf.tapas.tools {
    exports de.dlr.ivf.tapas.tools;
    exports de.dlr.ivf.tapas.tools.fileModifier;
    requires java.sql;
    requires java.desktop;

    requires org.apache.commons.lang3;
    requires com.fasterxml.jackson.databind;
    requires javacsv;

    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.tapas.model;
    requires poi;
    requires jfreechart;
}