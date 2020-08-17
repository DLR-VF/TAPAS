package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TPS_StateMachineWorker implements Runnable {

    private List<TPS_PlanStateMachine<TPS_Plan>> state_machines;
    private CyclicBarrier cb;
    private TPS_PlansExecutor executor;
    private boolean i_am_done = false;
    private List<TPS_PlanStateMachine> unfinished_mashines = new ArrayList<>();

    public TPS_StateMachineWorker(List<TPS_PlanStateMachine<TPS_Plan>> state_machines, TPS_PlansExecutor executor){
        this.state_machines = state_machines;
        this.cb = executor.getCyclicBarrier();
        this.executor = executor;

    }


    public void addStateMachine(TPS_PlanStateMachine<TPS_Plan> state_machine){
        this.state_machines.add(state_machine);
    }

    @Override
    public void run() {
        while(this.executor.getNextSimulationEvent().getEventType() != TPS_PlanEventType.END_OF_SIMULATION){
            int simcount = (int) executor.getNextSimulationEvent().getData();
            int active_state_machines = 0;
            TPS_PlanEvent event = this.executor.getNextSimulationEvent();
            if(!i_am_done) {
                for (TPS_PlanStateMachine<TPS_Plan> state_machine : state_machines) {
                    if (!state_machine.hasFinished()) {
                        if(simcount > 5000)
                            unfinished_mashines.add(state_machine);

                        active_state_machines++;
                        state_machine.handleEvent(event);
                    }
                }
                i_am_done = simcount > 5000 ? true : active_state_machines == 0;

                //when we are done we need to tell the executor once
                if(i_am_done)
                    this.executor.done(this);
            }
            try {
                cb.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    public List<TPS_PlanStateMachine<TPS_Plan>> getStateMachines(){
        return this.state_machines;
    }
}
