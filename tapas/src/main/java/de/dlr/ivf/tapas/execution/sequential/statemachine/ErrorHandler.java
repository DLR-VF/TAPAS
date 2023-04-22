package de.dlr.ivf.tapas.execution.sequential.statemachine;

/**
 *  This interface gives the implementing class a means to be used as a callback and handle propagated exceptions.
 *
 */
public interface ErrorHandler {

    /**
     * Error handling method taking a {@link Throwable}
     * @param t the throwable that has been thrown elsewhere in the code
     */
    void handleError(Throwable t);
}
