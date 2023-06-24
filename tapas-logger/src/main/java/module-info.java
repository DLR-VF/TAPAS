module de.dlr.ivf.tapas.logger {

    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires lombok;

    requires org.slf4j;

    requires de.dlr.ivf.tapas.util;

    provides java.lang.System.LoggerFinder
            with de.dlr.ivf.tapas.logger.Slf4jLoggerFinder;

    exports de.dlr.ivf.tapas.logger.legacy;
    exports de.dlr.ivf.tapas.logger;
}