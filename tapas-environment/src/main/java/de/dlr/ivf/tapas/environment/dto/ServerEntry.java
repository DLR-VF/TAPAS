package de.dlr.ivf.tapas.environment.dto;

import de.dlr.ivf.api.io.Column;
import de.dlr.ivf.tapas.environment.model.ServerState;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ServerEntry {
    @Column("server_name")
    private String serverName;

    @Column("server_ip")
    private String serverIp;

    @Column("server_cores")
    private int serverCores;

    @Column("server_online")
    private boolean serverOnline;

    @Column("server_usage")
    private double serverUsage;

    @Column("server_state")
    private ServerState serverState;
}
