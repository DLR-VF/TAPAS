package de.dlr.ivf.tapas.environment.dto;



import de.dlr.ivf.api.io.Column;

import java.sql.Timestamp;

public class SimulationEntry {

    @Column("id")
    private int id;

    @Column("sim_key")
    private String simKey;

    @Column("sim_started")
    private boolean simStarted;

    @Column("sim_finished")
    private boolean simFinished;

    @Column("sim_progress")
    private double simProgress;

    @Column("timestamp_insert")
    private Timestamp simInsertedTime;

    @Column("timestamp_started")
    private Timestamp simStartedTime;

    @Column("timestamp_finished")
    private Timestamp simFinishedTime;
}