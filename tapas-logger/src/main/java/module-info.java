module de.dlr.ivf.tapas.logger {

    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires lombok;

    requires de.dlr.ivf.tapas.util;

    exports de.dlr.ivf.tapas.logger;
}