package de.dlr.ivf.tapas.logger;

public enum SeverityLogLevel {
/**
 * The SeverenceLogLevel: specifies the Hierarchy within the severence
 *
 */
    OFF,
    FATAL,
    ERROR,
    SEVERE,
    WARN,
    INFO,
    DEBUG,
    FINE,
    FINER,
    FINEST,
    ALL;

    public boolean includes(SeverityLogLevel sll) {
        return this.ordinal() >= sll.ordinal();
    }
}
