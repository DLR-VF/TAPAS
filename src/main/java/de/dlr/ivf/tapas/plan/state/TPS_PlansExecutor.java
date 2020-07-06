package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.plan.state.event.TPS_PlanEventType;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import de.dlr.ivf.tapas.util.parameters.ParamString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TPS_PlansExecutor {

    private List<TPS_Plan> plans;
    private List<TPS_SequentialWorker> workers;
    private int worker_count;
    private TPS_DB_IOManager pm;

    private CyclicBarrier cb;

    private AtomicInteger simulation_time_stamp = new AtomicInteger();
    private int simulation_start_time;
    private int simulation_end_time;
    private TPS_PlanEvent next_simulation_time_event;
    private List<Thread> threads;

    public TPS_PlansExecutor(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm){
        this.plans = plans;
        this.worker_count = worker_count;
        this.pm = pm;
        this.threads = new ArrayList<>(worker_count);
        cb = new CyclicBarrier(worker_count,
                () -> {
                        for (TPS_SequentialWorker worker : workers) {
                            worker.persistTripsInDb();
                        }
                        next_simulation_time_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP, simulation_time_stamp.incrementAndGet());
                      });
        initSimulationStartAndEndTime();
        createStateMachines();
    }

    private void initSimulationStartAndEndTime(){

        int min_start_time = 0;
        int max_end_time = 0;

        for(TPS_Plan plan : plans){
            for(TPS_TourPart tp : plan.getScheme().getTourPartIterator()){
                for(TPS_Trip trip : tp.getTripIterator()){

                    if(plan.getPlannedTrip(trip).getStart() < min_start_time)
                        min_start_time = plan.getPlannedTrip(trip).getStart();

                    if(plan.getPlannedTrip(trip).getEnd() > max_end_time)
                        max_end_time = plan.getPlannedTrip(trip).getEnd();
                }
            }
        }
        simulation_start_time = min_start_time;
        simulation_time_stamp.set(min_start_time);
        simulation_end_time = max_end_time;

        next_simulation_time_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,min_start_time);

    }

    private void createStateMachines(){

        int num_machines_per_worker = plans.size() / worker_count;
        int num_remaining_machines = plans.size() % worker_count;
        String statement = "INSERT INTO " + pm.getParameters().getString(ParamString.DB_TABLE_TRIPS) + " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,? ,?,?,?,?, ?)";
        for(int i = 0; i < worker_count; i++){
            try {
                TPS_SequentialWorker worker = new TPS_SequentialWorker(new ArrayList<>(num_machines_per_worker+1), new TPS_SequentialTripOutput(statement,pm), this);
                plans.subList(i*num_machines_per_worker,(i+1)*num_machines_per_worker)
                     .forEach(plan -> worker.addStateMachine(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(worker, plan, input -> input, pm)));
                workers.add(worker);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //distribute one additional remaining state machine to some of the workers
        if(num_remaining_machines > 0){
            IntStream.range(0,num_remaining_machines)
                     .forEach(i -> workers.get(i).addStateMachine(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(workers.get(i), plans.get(plans.size()-num_remaining_machines+i), input -> input, pm)));
        }


    }

    public void runSimulation(){
        int index = 0;
        for(TPS_SequentialWorker worker : workers){
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
        return this.next_simulation_time_event;
    }

}
