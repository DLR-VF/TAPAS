package de.dlr.ivf.tapas.execution.sequential.communication;

public interface TPS_Mediator {
    <T> T request();
    void offer();
}
