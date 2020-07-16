package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripToDbWriter;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachine;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachineFactory;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TPS_PlansExecutor {

    private List<TPS_Plan> plans;
    private List<TPS_StateMachineWorker> workers;
    private int worker_count;
    private TPS_DB_IOManager pm;

    private CyclicBarrier cb;

    private AtomicInteger simulation_time_stamp = new AtomicInteger();
    private int simulation_start_time = -1000;
    private int simulation_end_time = 1440;
    private TPS_PlanEvent next_simulation_event;
    private List<Thread> threads;
    private TPS_TripToDbWriter writer;

    public TPS_PlansExecutor(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm, TPS_TripToDbWriter writer){
        this.plans = plans;
        this.worker_count = worker_count;
        this.pm = pm;
        this.threads = new ArrayList<>(worker_count);
        this.workers = new ArrayList<>(worker_count);
        this.writer = writer;
        cb = new CyclicBarrier(worker_count,
                () -> {
                    System.out.println(simulation_time_stamp.get());
                        next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP, simulation_time_stamp.incrementAndGet());
                      });
        createStateMachines();
        initSimulationStartAndEndTime();
    }

    private void initSimulationStartAndEndTime(){

        int min_start_time = Integer.MAX_VALUE;
        int max_end_time = 1440;

        for(TPS_StateMachineWorker worker : workers){
            OptionalInt potential_start_time = worker.getStateMachines().stream().mapToInt(TPS_PlanStateMachine::getSimStartTime).min();
            if(potential_start_time.isPresent() && potential_start_time.getAsInt()< min_start_time)
                min_start_time = potential_start_time.getAsInt();
        }

        System.out.println("SIMULATION START TIME = "+min_start_time);
        simulation_start_time = min_start_time;
        simulation_time_stamp.set(min_start_time);
        simulation_end_time = max_end_time;

        next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,min_start_time);

    }

    private void createStateMachines(){

        int num_machines_per_worker = plans.size() / worker_count;
        int num_remaining_machines = plans.size() % worker_count;

        for(int i = 0; i < worker_count; i++){
            TPS_StateMachineWorker worker = new TPS_StateMachineWorker(new ArrayList<>(num_machines_per_worker+1), this);
            plans.subList(i*num_machines_per_worker,(i+1)*num_machines_per_worker)
                 .forEach(plan -> worker.addStateMachine(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plan, writer, pm)));
            workers.add(worker);
            System.out.println("worker "+i+" has "+worker.getStateMachines().size()+ "Elements");
        }
        //distribute one additional remaining state machine to some of the workers
        if(num_remaining_machines > 0){
            IntStream.range(0,num_remaining_machines)
                     .forEach(i -> workers.get(i).addStateMachine(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plans.get(plans.size()-num_remaining_machines+i), writer, pm)));
        }


    }

    public void runSimulation(){
        int index = 0;
        for(TPS_StateMachineWorker worker : workers){
            threads.add(new Thread(worker,"SequentialWorker-"+index));
        }
        for(Thread t : threads){
            t.start();
        }

    }

    public CyclicBarrier getCyclicBarrier(){
        return this.cb;
    }

    public int getSimulationTimeStamp(){
        return this.simulation_time_stamp.get();
    }

    public int getSimulationStartTime(){
        return this.simulation_start_time;
    }

    public int getSimulationEndTime(){
        return this.simulation_end_time;
    }

    public TPS_PlanEvent getNextSimulationTimeEvent(){
        return this.next_simulation_event;
    }

}
