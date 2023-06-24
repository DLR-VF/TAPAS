package de.dlr.ivf.tapas.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Slf4jLogger implements System.Logger{

    private final String name;
    private final Logger logger;

    public Slf4jLogger(String name){
        this.name = name;
        this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isLoggable(Level level) {
        return switch (level){
            case TRACE -> logger.isTraceEnabled();
            case WARNING -> logger.isWarnEnabled();
            case INFO -> logger.isInfoEnabled();
            case DEBUG -> logger.isDebugEnabled();
            case ERROR -> logger.isErrorEnabled();
            case OFF -> false;
            default -> true;
        };
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        if (!isLoggable(level)) {
            return;
        }

        switch (level) {
            case TRACE -> logger.trace(msg, thrown);
            case DEBUG -> logger.debug(msg, thrown);
            case WARNING -> logger.warn(msg, thrown);
            case ERROR -> logger.error(msg, thrown);
            default -> logger.info(msg, thrown);
        }
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        if (!isLoggable(level)) {
            return;
        }
        String message = MessageFormat.format(format, params);

        switch (level) {
            case TRACE -> logger.trace(message);
            case DEBUG -> logger.debug(message);
            case WARNING -> logger.warn(message);
            case ERROR -> logger.error(message);
            default -> logger.info(message);
        }
    }
}
