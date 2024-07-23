package de.dlr.ivf.tapas.configuration.json;

import de.dlr.ivf.tapas.configuration.json.runner.ChronologicalRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;


/**
 * This sealed interface represents the configuration for the SimulationRunner.
 */
public sealed interface SimulationRunnerConfiguration permits TripPriorityRunnerConfiguration, ChronologicalRunnerConfiguration {}
