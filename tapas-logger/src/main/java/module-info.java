module de.dlr.ivf.tapas.logger {

    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;

    exports de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.util;



}