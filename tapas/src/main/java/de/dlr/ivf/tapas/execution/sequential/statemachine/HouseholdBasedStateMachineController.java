package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.execution.sequential.communication.EndOfSimulationCallback;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;
import de.dlr.ivf.tapas.execution.sequential.statemachine.util.StateMachineUtils;
import de.dlr.ivf.tapas.logger.TPS_Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The controller is being used to control all state machines that belong to the same household.
 * As an {@link EventDelegator} it delegates external events to state machines that are part of this controller.
 * It is a means to control the order in which every state machine is receiving the event. Depending on the implementation
 * it can also be used to make an early transition of certain state machines.
 *
 */
public class HouseholdBasedStateMachineController implements EventDelegator, ErrorHandler, EndOfSimulationCallback {

    /**
     * the household that is represented
     */
    private TPS_Household hh;

    private List<TPS_StateMachine> call_backs = new ArrayList<>();

    /**
     * A mapping between each {@link TPS_Person} and its corresponding {@link TPS_StateMachine}
     */
    private Map<TPS_Person,TPS_StateMachine> state_machines;

    /**
     * This constructor with only {@link TPS_Person} and {@link TPS_StateMachine} mappings generates a controller
     * with an empty household.
     * @param state_machines
     */
    public HouseholdBasedStateMachineController(Map<TPS_Person, TPS_StateMachine> state_machines){
        this(state_machines, StateMachineUtils.EmptyHouseHold());
    }

    public Collection<TPS_StateMachine> getStateMachines(){
        return state_machines.values();
    }

    /**
     * Sets up a controller with an associated household and person/state machine mappings.
     * @param state_machines
     * @param hh
     */
    public HouseholdBasedStateMachineController(Map<TPS_Person, TPS_StateMachine> state_machines, TPS_Household hh){

        this.state_machines = state_machines;
        this.hh = hh;

        this.state_machines.values().forEach(state_machine -> state_machine.setController(this));
    }

    /**
     * This method delegates an {@link TPS_Event} to every {@link TPS_StateMachine} that will accept it.
     * @param event the event to be delegated
     */
    @Override
    public void delegateEvent(TPS_Event event){

        state_machines.values()
                      .stream()
                      .filter(state_machine -> state_machine.willHandleEvent(event))
                      .forEach(state_machine -> state_machine.handleEvent(event)
                      );

        //now handle end of simulation callbacks
        call_backs.forEach(TPS_StateMachine::transitionToEndState);
    }

    /**
     * Gets all {@link TPS_StateMachine} that will transition based on the {@link TPS_Event}.
     * @param event
     * @return a {@link Stream} of {@link TPS_StateMachine} that will transition.
     */
    private Stream<Map.Entry<TPS_Person,TPS_StateMachine>> getTransitioningStateMachines(TPS_Event event){

        return state_machines.entrySet()
                             .stream()
                             .filter(entry -> entry.getValue().willHandleEvent(event));
    }

    /**
     * Returns the associated {@link TPS_Household}
     * @return the household
     */
    public TPS_Household getHousehold(){
        return this.hh;
    }

    /**
     *
     * @return the count of unfinished {@link TPS_StateMachine}.
     */
    public int getUnfinishedCount(){
        return Math.toIntExact(this.state_machines.values()
                                                  .stream()
                                                  .filter(Predicate.not(TPS_StateMachine::hasFinished))
                                                  .count()
                              );
    }

    /**
     * This returns true if the controller has a {@link TPS_StateMachine} that will transition upon this event.
     * @param simulation_event the event to be delegated
     * @return true if at least one {@link TPS_StateMachine} will pass the event.
     */
    @Override
    public boolean willPassEvent(TPS_Event simulation_event){
        return this.state_machines.values()
                                  .stream()
                                  .anyMatch(state_machine -> state_machine.willHandleEvent(simulation_event));
    }

    public int getStateMachinesCount(){
        return this.state_machines.size();
    }

    /**
     * This method will be called in case of an exception during a state machine transition. In its current
     * implementation the state machine will transition to its error state.
     * Other implementations could delegate the event to the erroneous {@link TPS_StateMachine} again.
     * @param t the throwable that has been thrown elsewhere in the code
     */
    @Override
    public void handleError(Throwable t) {
        if(t instanceof TPS_StateTransitionException){
            //todo more detailed information
            TPS_StateTransitionException exception = (TPS_StateTransitionException) t;
            TPS_StateMachine state_machine = exception.getStateMachine();

            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverityLogLevel.SEVERE, exception.getMessage());
            state_machine.transitionToErrorState();
        }else{
            if(!(t instanceof RuntimeException))
                throw new RuntimeException("Did someone break LST?!");
        }
    }

    @Override
    public void endOfSimulationFor(TPS_StateMachine state_machine) {
        call_backs.add(state_machine);
    }
}
