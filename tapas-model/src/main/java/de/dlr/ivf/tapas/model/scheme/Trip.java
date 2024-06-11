package de.dlr.ivf.tapas.model.scheme;

public record Trip (

    Stay startStay,
    Stay endStay,
    int startTime,
    int durationSeconds,
    int priority
){}
