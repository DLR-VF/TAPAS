package de.dlr.ivf.tapas.plan.sequential.communication;

public interface TPS_Mediator {
    <T> T request();
    void offer();
}
