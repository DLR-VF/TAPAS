package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_PipedDbWriter;
import de.dlr.ivf.tapas.persistence.db.TPS_TripToDbWriter;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.plan.StateMachineUtils;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachineFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TPS_PlansExecutor implements TPS_FinishingWorkerCallback{

    private List<TPS_Plan> plans;
    private List<TPS_StateMachineWorker> workers;
    private int worker_count;
    private TPS_DB_IOManager pm;

    private int plan_count;

    private CyclicBarrier cb;

    private AtomicInteger simulation_time_stamp = new AtomicInteger();
    private AtomicInteger simulation_start_time = new AtomicInteger(Integer.MAX_VALUE);
    private AtomicInteger active_worker_count;
    private TPS_PlanEvent next_simulation_event;
    private List<Thread> threads;
    private List<TPS_StateMachineWorker> active_workers;
    private TPS_TripWriter writer;
    private boolean is_start_time_initialized = false;

    public TPS_PlansExecutor(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm, TPS_TripWriter writer){
        this.plans = plans;
        this.worker_count = worker_count;
        this.pm = pm;
        this.active_worker_count = new AtomicInteger(worker_count);
        this.threads = new ArrayList<>(worker_count);
        this.workers = new ArrayList<>(worker_count);
       // this.active_workers = Collections.synchronizedList(workers);
        this.writer = writer;
        this.plan_count = plans.size();
        cb = new CyclicBarrier(worker_count,
                () -> {

                        if(!is_start_time_initialized) { //the first time we hit the barrier we set the simulation start time because all state machines have their first trip states initialized
                            simulation_time_stamp.set(simulation_start_time.get());
                            is_start_time_initialized = true;
                        }

                        if(active_worker_count.get() > 0) {
                            next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP, simulation_time_stamp.getAndIncrement());
                        }else {
                            //will let workers finish
                            next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.END_OF_SIMULATION, StateMachineUtils.NoEventData());
                            //tell the trip writer that it can finish
                            writer.finish();

                        }
                      });
        createStateMachines();
        preInitialize();
    }

    private void preInitialize(){
        //the first event of the simulation that will be requested by the workers before the first barrier hit.
        next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.INIT_FIRST_STAY, StateMachineUtils.NoEventData());
    }

    private void createStateMachines(){


        int num_machines_per_worker = plan_count / worker_count;
        int num_remaining_machines = plan_count % worker_count;

        for(int i = 0; i < worker_count; i++){
            TPS_StateMachineWorker worker = new TPS_StateMachineWorker(new ArrayList<>(num_machines_per_worker+1), this);
            workers.add(worker);
        }
//todo commentary besonderheit frquenz auf dem worker count
        int worker_id=0;
        while(plans.size()>0){
            workers.get(worker_id).addStateMachine(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plans.remove(0), writer, pm, this));
            worker_id = (worker_id+1)%worker_count;
        }

        IntStream.range(0,worker_count).forEach(i -> System.out.println("worker "+i+" has "+workers.get(i).getStateMachines().size()+ " state machines"));
    }

    public void runSimulation(){
        int index = 0;
        for(TPS_StateMachineWorker worker : workers){
            threads.add(new Thread(worker,"SequentialWorker-"+index++));
        }
        active_workers = Collections.synchronizedList(workers);
        for(Thread t : threads){
            t.start();
        }
    }

    public CyclicBarrier getCyclicBarrier(){
        return this.cb;
    }

    public TPS_PlanEvent getNextSimulationEvent(){
        return this.next_simulation_event;
    }

    public void updateSimulationStartTime(int state_machine_start_time) {
        simulation_start_time.getAndUpdate(current_start_time -> Math.min(current_start_time, state_machine_start_time));
    }


    @Override
    public void done(Runnable worker) {

        ((TPS_StateMachineWorker)worker).getUnfinishedMachines().forEach(sm -> {
            System.out.println("current: " +sm.getCurrent_state().getName()+" | initital: "+sm.getInitial_state().getName());
            System.out.println(sm.getRepresenting_object().getPlannedTrips());
        });
        if(this.active_workers.remove(worker)) {
            active_worker_count.getAndDecrement();
        }
    }
}
