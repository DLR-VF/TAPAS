package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripToDbWriter;
import de.dlr.ivf.tapas.plan.StateMachineUtils;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachineFactory;
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
    private List<Runnable> active_workers;
    private TPS_TripToDbWriter writer;
    private boolean is_start_time_initialized = false;

    public TPS_PlansExecutor(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm, TPS_TripToDbWriter writer){
        this.plans = plans;
        this.worker_count = worker_count;
        this.pm = pm;
        this.active_worker_count = new AtomicInteger(worker_count);
        this.threads = new ArrayList<>(worker_count);
        this.workers = new ArrayList<>(worker_count);
        this.active_workers = new ArrayList<>(worker_count);
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
            plans.subList(i*num_machines_per_worker,(i+1)*num_machines_per_worker)
                 .forEach(plan -> worker.addStateMachine(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plan, writer, pm, this)));
            workers.add(worker);
        }
        //distribute one additional remaining state machine to some of the workers
        if(num_remaining_machines > 0){
            IntStream.range(0,num_remaining_machines)
                     .forEach(i -> workers.get(i).addStateMachine(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plans.get(plans.size()-num_remaining_machines+i), writer, pm,this)));
        }
        IntStream.range(0,worker_count).forEach(i -> System.out.println("worker "+i+" has "+workers.get(i).getStateMachines().size()+ " state machines"));
    }

    public void runSimulation(){
        int index = 0;
        for(TPS_StateMachineWorker worker : workers){
            threads.add(new Thread(worker,"SequentialWorker-"+index++));
            active_workers.add(worker);
        }
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
        int index = active_workers.indexOf(worker);
        ((TPS_StateMachineWorker)worker).getUnfinishedMachines().forEach(sm -> {
            System.out.println("current: " +sm.getCurrent_state().getName()+" | initital: "+sm.getInitial_state().getName());
            System.out.println(sm.getRepresenting_object().getPlannedTrips());
        });
        if(index > -1) {
            this.active_workers.remove(index);
            active_worker_count.getAndDecrement();
        }
    }
}
