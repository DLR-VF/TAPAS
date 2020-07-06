package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TPS_SequentialWorker implements Runnable {

    private List<TPS_PlanStateMachine<TPS_Plan>> state_machines;
    private TPS_SequentialTripOutput tripOutput;
    private CyclicBarrier cb;
    private TPS_PlansExecutor executor;
    private int end_time;

    public TPS_SequentialWorker(List<TPS_PlanStateMachine<TPS_Plan>> state_machines, TPS_SequentialTripOutput tripOutput, TPS_PlansExecutor executor){
        this.state_machines = state_machines;
        this.tripOutput = tripOutput;
        this.cb = executor.getCyclicBarrier();
        this.executor = executor;
        this.end_time = executor.getSimulationEndTime();
    }

    public TPS_SequentialWorker(TPS_SequentialTripOutput tripOutput){

        this.tripOutput  = tripOutput;
    }

    public void setStateMachines(List<TPS_PlanStateMachine<TPS_Plan>> state_machines){
        this.state_machines = state_machines;
    }

    public void addStateMachine(TPS_PlanStateMachine<TPS_Plan> state_machine){
        this.state_machines.add(state_machine);
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

    public void addTripToOutput(TPS_Plan plan, TPS_TourPart tp, TPS_Trip trip) {
        tripOutput.addTripOutput(plan, tp, trip);
    }

    public boolean persistTripsInDb(){
        return this.tripOutput.persistTrips();
    }
}
