package de.dlr.ivf.tapas.environment.model;

/**
 * Possible states of a simulation. Each state holds an action string and a hierarchy value. The action string tells which is the next possible action with a simulation in this state, e.g. READY
 * -> "Start". The hierarchy value is used for detecting the progress of the simulation.
 *
 * @author mark_ma
 */
public enum SimulationState {
    /**
     * The simulation is stopped and all households are simulated
     */
    FINISHED(3),
    /**
     * The simulation is added to the database but is not ready to start
     */
    INSERTED(0),
    /**
     * The simulation is ready to start
     */
    STOPPED(1),
    /**
     * The simulation is started and not finished
     */
    STARTED(2);

    /**
     * Possible action the this state. This action string can be shown in a GUI.
     */
    private String action;
    /**
     * Hierarchy value of the state
     */
    private final int value;

    /**
     * Constructor initialises members.
     *
     * @param value hierarchy value
     */
    SimulationState(int value) {
        this.value = value;
    }

    /**
     * @param simulation simulation to get the state of
     * @return state of the current simulation
     */
    public static SimulationState getState(SimulationData simulation) {
        if (simulation.isFinished()) {
            if (simulation.getProgress() == simulation.getTotal()) return SimulationState.FINISHED;
            return SimulationState.STOPPED;
        } else if (simulation.isStarted()) {
            return SimulationState.STARTED;
        } else if (simulation.isReady()) {
            return SimulationState.STOPPED;
        }
        return SimulationState.INSERTED;
    }
}
