package de.dlr.ivf.tapas.configuration.json;

import de.dlr.ivf.tapas.configuration.json.runner.ChronologicalRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;


/**
 * This sealed interface represents the configuration for the SimulationRunner.
 */
public sealed interface SimulationRunnerConfiguration permits TripPriorityRunnerConfiguration, ChronologicalRunnerConfiguration {
    TrafficGenerationConfiguration getTrafficGenerationConfiguration();
}
