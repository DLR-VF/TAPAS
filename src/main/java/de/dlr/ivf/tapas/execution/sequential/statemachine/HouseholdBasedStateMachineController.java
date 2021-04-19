package de.dlr.ivf.tapas.execution.sequential.statemachine;

import de.dlr.ivf.tapas.execution.sequential.communication.SharingMediator;
import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_EventType;
import de.dlr.ivf.tapas.person.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HouseholdBasedStateMachineController {

    private TPS_Household hh;
    private SharingMediator<TPS_Car> car_mediator;

    private SortedMap<TPS_Person,TPS_StateMachine> state_machines = new TreeMap<>(Comparator.comparingDouble(TPS_Person::primaryDriver).reversed());

    public HouseholdBasedStateMachineController(TPS_Household hh, Map<TPS_Person, TPS_StateMachine> state_machines){

        this.hh = hh;

        this.state_machines.putAll(state_machines);

    }

    public HouseholdBasedStateMachineController(Map<TPS_Person, TPS_StateMachine> state_machines){

        this.state_machines.putAll(state_machines);

    }


    public void delegateSimulationEvent(TPS_PlanEvent event){

        state_machines.values()
                      .stream()
                      .filter(state_machine -> state_machine.willHandleEvent(event))
                      .forEach(state_machine -> state_machine.handleEvent(event));

    }

    private void initCarMediator(TPS_Household hh){


    }

//    @Override
//    public void earlyTransition(TPS_Person person) {
//        TPS_StateMachine sm  = this.state_machines.get(person);
//        TPS_PlanState current_state = sm.getCurrentState();
//
//        if(sm.getCurrentState().getStateType() == EpisodeType.HOME){
//            sm.makeTransition(current_state.getHandler(TPS_EventType.SIMULATION_STEP));
//        }
//    }

    public List<TPS_StateMachine> requestTransitioningStateMachines(TPS_PlanEvent event){

        return Stream.concat(getCarDependantStateMachinesAtHome(),getTransitioningStateMachines(event))
                     .distinct()
                     .sorted(Map.Entry.comparingByKey(Comparator.comparingDouble(TPS_Person::primaryDriver).reversed()))
                     .map(Map.Entry::getValue)
                     .collect(Collectors.toList());
    }

    private Stream<Map.Entry<TPS_Person,TPS_StateMachine>> getTransitioningStateMachines(TPS_PlanEvent event){

        return state_machines.entrySet()
                             .stream()
                             .filter(entry -> entry.getValue().willHandleEvent(event));
    }

    private Stream<Map.Entry<TPS_Person,TPS_StateMachine>> getCarDependantStateMachinesAtHome(){

        return state_machines.entrySet()
                             .stream()
                             .filter(entry -> entry.getValue().getCurrentState().getStateType() == EpisodeType.HOME)
                             .filter(entry -> entry.getKey().mayDriveACar());
    }

    public TPS_Household getHousehold(){
        return this.hh;
    }

    public long getUnfinishedCount(){
        return this.state_machines.values().stream().filter(Predicate.not(TPS_StateMachine::hasFinished)).count();
    }

    public boolean willPassEvent(TPS_PlanEvent simulation_event){
        return this.state_machines.entrySet()
                                  .stream()
                                  .map(Map.Entry::getValue)
                                  .filter(state_machine -> state_machine.willHandleEvent(simulation_event))
                                  .findAny()
                                  .isPresent();
    }
}
