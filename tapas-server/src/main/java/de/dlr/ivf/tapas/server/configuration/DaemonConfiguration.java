package de.dlr.ivf.tapas.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import de.dlr.ivf.tapas.environment.configuration.EnvironmentConfiguration;
import lombok.*;

/**
 * A configuration skeleton for the tapas-daemon,
 *
 * @author Alain Schengen
 */
@ToString
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DaemonConfiguration {


    /**
     * the polling rate in seconds that the daemon should check the simulations table
     */
    @JsonProperty
    @JsonSetter(nulls = Nulls.SKIP)
    private final int simTablePollingRate = 30;

    /**
     * the rate in seconds a server should be updated in the server table.
     */
    @JsonProperty
    @JsonSetter(nulls = Nulls.SKIP)
    private final int serverUpdateRate = 30;


    /**
     * The configuration skeleton for a tapas-environment
     */
   @JsonProperty
    private final EnvironmentConfiguration tapasEnvironment;
}
