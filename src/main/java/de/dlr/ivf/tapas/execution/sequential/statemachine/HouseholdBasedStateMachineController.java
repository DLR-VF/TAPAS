package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.person.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HouseholdBasedStateMachineController implements EventDelegator, ErrorHandler{

    private TPS_Household hh;

    private SortedMap<TPS_Person,TPS_StateMachine> state_machines = new TreeMap<>(Comparator.comparingDouble(TPS_Person::primaryDriver).reversed());

    public HouseholdBasedStateMachineController(Map<TPS_Person, TPS_StateMachine> state_machines){

        this.state_machines.putAll(state_machines);

    }

    @Override
    public void delegateEvent(TPS_Event event){

        state_machines.values()
                      .stream()
                      .filter(state_machine -> state_machine.willHandleEvent(event))
                      .forEach(state_machine -> state_machine.handleEvent(event)
                      );
    }

    public List<TPS_StateMachine> requestTransitioningStateMachines(TPS_Event event){

        return Stream.concat(getCarDependantStateMachinesAtHome(),getTransitioningStateMachines(event))
                     .distinct()
                     .sorted(Map.Entry.comparingByKey(Comparator.comparingDouble(TPS_Person::primaryDriver).reversed()))
                     .map(Map.Entry::getValue)
                     .collect(Collectors.toList());
    }

    private Stream<Map.Entry<TPS_Person,TPS_StateMachine>> getTransitioningStateMachines(TPS_Event event){

        return state_machines.entrySet()
                             .stream()
                             .filter(entry -> entry.getValue().willHandleEvent(event));
    }

    private Stream<Map.Entry<TPS_Person,TPS_StateMachine>> getCarDependantStateMachinesAtHome(){

        return state_machines.entrySet()
                             .stream()
                             .filter(entry -> entry.getKey().mayDriveACar());
    }

    public TPS_Household getHousehold(){
        return this.hh;
    }

    public int getUnfinishedCount(){
        return Math.toIntExact(this.state_machines.values().stream().filter(Predicate.not(TPS_StateMachine::hasFinished)).count());
    }

    @Override
    public boolean willPassEvent(TPS_Event simulation_event){
        return this.state_machines.values()
                                  .stream()
                                  .anyMatch(state_machine -> state_machine.willHandleEvent(simulation_event));
    }

    @Override
    public void handleError(Throwable t) {
        if(t instanceof TPS_StateTransitionException){
            //todo more detailed information
            TPS_StateTransitionException exception = (TPS_StateTransitionException) t;
            TPS_StateMachine state_machine = exception.getStateMachine();

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.SEVERE, exception.getMessage());
            state_machine.transitionToErrorState();
        }else{
            throw new RuntimeException("Did someone break LST?");
        }
    }
}
