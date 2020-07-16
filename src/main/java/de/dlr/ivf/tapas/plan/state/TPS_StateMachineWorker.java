package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachine;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TPS_StateMachineWorker implements Runnable {

    private List<TPS_PlanStateMachine<TPS_Plan>> state_machines;
    private CyclicBarrier cb;
    private TPS_PlansExecutor executor;
    private int end_time;

    public TPS_StateMachineWorker(List<TPS_PlanStateMachine<TPS_Plan>> state_machines, TPS_PlansExecutor executor){
        this.state_machines = state_machines;
        this.cb = executor.getCyclicBarrier();
        this.executor = executor;
        this.end_time = executor.getSimulationEndTime();
    }


    public void addStateMachine(TPS_PlanStateMachine<TPS_Plan> state_machine){
        this.state_machines.add(state_machine);
    }

    public List<TPS_PlanStateMachine<TPS_Plan>> getStateMachines(){
        return this.state_machines;
    }

    @Override
    public void run() {
        while(this.executor.getSimulationTimeStamp() <= end_time+1){
            TPS_PlanEvent event = this.executor.getNextSimulationTimeEvent();
            for(TPS_PlanStateMachine<TPS_Plan> state_machine : state_machines){
                state_machine.handleEvent(event);
            }
            try {
                cb.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }

    }
}
