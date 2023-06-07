package de.dlr.ivf.tapas.environment.dto;



import de.dlr.ivf.api.io.annotation.Column;
import de.dlr.ivf.tapas.environment.model.SimulationState;
import lombok.*;

import java.sql.Timestamp;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SimulationEntry {

    @Setter
    @Column(value = "id", ignoreWrite = true)
    private int id;

    @Column("sim_key")
    private String simKey;

    @Column("sim_description")
    private String simDescription;

    @Column("sim_progress")
    private double simProgress;

    @Column("timestamp_insert")
    private Timestamp simInsertedTime;

    @Column("timestamp_started")
    private Timestamp simStartedTime;

    @Column("timestamp_finished")
    private Timestamp simFinishedTime;

    @Column("simulation_server")
    private String simServer;

    @Column("simulation_state")
    private SimulationState simState;
}
