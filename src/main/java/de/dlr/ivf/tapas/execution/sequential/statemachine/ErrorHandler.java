package de.dlr.ivf.tapas.execution.sequential.statemachine;

public interface ErrorHandler {

    void handleError(Throwable t);
}
