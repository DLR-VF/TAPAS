package de.dlr.ivf.tapas.configuration.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.dlr.ivf.api.io.configuration.ConnectionDetails;
import de.dlr.ivf.tapas.configuration.json.agent.CarsConfiguration;
import de.dlr.ivf.tapas.configuration.json.agent.HouseholdConfiguration;
import de.dlr.ivf.tapas.configuration.json.region.RegionConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.ChronologicalRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;

import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * This class represents the configuration for the TAPAS application. The TapasConfig is a Spring component, meaning it
 * can be managed and injected by the Spring framework.
 * <p>
 * The class includes the following properties:<br>
 * - connectionDetails: represents the connection details for accessing a resource<br>
 * - simulationRunnerConfiguration: represents the configuration for the simulation runner<br>
 * - simulationRunner: represents the name of the simulation runner to be used<p>
 * <p>
 * Usage note:
 * This class is designed to be set up using the Jackson library.
 */
@Getter
@Component
public class TapasConfig {


    @JsonProperty
    private ConnectionDetails connectionDetails;

    /**
     * Represents the configuration for the simulation runner.<p>
     * Possible implementations by name:<br>
     * - tripPriorityRunner -> {@link TripPriorityRunnerConfiguration}<br>
     * - chronologicalRunner -> {@link ChronologicalRunnerConfiguration}<br>
     */
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "simulationRunnerToUse"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TripPriorityRunnerConfiguration.class, name = "tripPriorityRunner"),
            @JsonSubTypes.Type(value = ChronologicalRunnerConfiguration.class, name = "chronologicalRunner")
    })
    @JsonProperty
    private SimulationRunnerConfiguration simulationRunnerConfiguration;

    /**
     * Represents the name of the simulation runner to be used in the application.
     * See {@link #simulationRunnerConfiguration}
     */
    @JsonTypeId
    @JsonProperty
    private String simulationRunnerToUse;

    @JsonProperty
    private HouseholdConfiguration householdConfiguration;

    @JsonProperty
    private CarsConfiguration carsConfiguration;

    @JsonProperty
    private RegionConfiguration regionConfiguration;
}
