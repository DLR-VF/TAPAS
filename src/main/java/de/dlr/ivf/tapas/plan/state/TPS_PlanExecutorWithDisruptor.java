package de.dlr.ivf.tapas.plan.state;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.plan.StateMachineUtils;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.*;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanState;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachine;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachineFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TPS_PlanExecutorWithDisruptor extends TPS_PlansExecutor implements Runnable{
    private List<TPS_Plan> plans;
    private List<TPS_StateMachineWorker> workers;
    private int worker_count;
    private TPS_DB_IOManager pm;

    private int plan_count;

    private CyclicBarrier cb;
    Map<Integer,Integer> event_counts = new HashMap<>();

    private AtomicInteger simulation_time_stamp = new AtomicInteger(0);
    private AtomicInteger simulation_start_time = new AtomicInteger(Integer.MAX_VALUE);
    private AtomicInteger active_worker_count;
    private TPS_PlanEvent next_simulation_event;
    private List<Thread> threads;
    private List<TPS_StateMachineWorker> active_workers;
    private TPS_TripWriter writer;
    private List<TPS_PlanStateMachine> state_machines;
    private boolean is_start_time_initialized = false;

    public TPS_PlanExecutorWithDisruptor(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm, TPS_TripWriter writer){
        super();

        this.plans = plans;
        this.plan_count = plans.size();
        this.state_machines = new ArrayList<>(plan_count);
        this.worker_count = worker_count-5;
        this.pm = pm;
        this.active_worker_count = new AtomicInteger(worker_count);
        this.threads = new ArrayList<>(worker_count);
        this.workers = new ArrayList<>(worker_count);
        // this.active_workers = Collections.synchronizedList(workers);
        this.writer = writer;

        cb = new CyclicBarrier(2);

        createStateMachines();
        preInitialize();
    }
    private void preInitialize(){
        //the first event of the simulation that will be requested by the workers before the first barrier hit.
        next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.INIT_FIRST_STAY, StateMachineUtils.NoEventData());
        simulation_time_stamp.set((int) (plans.stream().mapToInt(plan -> plan.getScheme().getSchemeParts().get(0).getFirstEpisode().getOriginalDuration()).min().getAsInt()* 1.66666666e-2 + 0.5));
    }

    private void createStateMachines() {

        plans.forEach(plan -> state_machines.add(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plan, writer, pm, this)));
    }


    public void run(){
        int bufferSize = (int)Math.pow(2,15);

        EventFactory<TPS_StateMachineEvent> ef = () -> new TPS_StateMachineEvent();
        Disruptor<TPS_StateMachineEvent> disruptor = new Disruptor(ef, bufferSize, DaemonThreadFactory.INSTANCE, ProducerType.SINGLE, new BusySpinWaitStrategy());

        TPS_StateMachineHandler[] workers = new TPS_StateMachineHandler[worker_count];
        IntStream.range(0,worker_count).forEach(i -> workers[worker_count-1-i] = new TPS_StateMachineHandler(i, worker_count));
        RingBuffer<TPS_StateMachineEvent> ring_buffer = disruptor.getRingBuffer();

        EventHandler<TPS_StateMachineEvent> init_next_sim_step = (event, sequence, endOfBatch) -> {
            if(ring_buffer.getCursor() - sequence == 0){
                //System.out.println("working on final sequence for this batch "+sequence);
                cb.await();
            } };



        disruptor.handleEventsWith(workers).then(init_next_sim_step);

        disruptor.start();
        //<TPS_StateMachineEvent> worker_pool = new WorkerPool<>(ring_buffer, barrier, new StateMachineEventExceptionHandler(),workers);
        //ring_buffer.addGatingSequences(worker_pool.getWorkerSequences());


        boolean all_machines_finished = false;
        int sm_count = state_machines.size();

        boolean all_finished = false;
       // EventTranslator<TPS_StateMachineEvent> event_translator = (event,sequence, state_machine,plan_event) -> {event.setEvent(plan_event); event.setStateMachine(state_machine);};
        List<EventTranslator<TPS_StateMachineEvent>> handling_machines = new ArrayList<>();
        long start = System.currentTimeMillis();
        while(!all_finished && simulation_time_stamp.get() < 5000){
            all_finished = true;
            int unfinished = 0;
            for(TPS_PlanStateMachine state_machine: state_machines){
                all_finished = all_finished && state_machine.hasFinished();
                if(!state_machine.hasFinished())
                    unfinished++;
                if (state_machine.willHandleEvent(next_simulation_event)) {
                    handling_machines.add((event, sequence) -> {event.setEvent(next_simulation_event); event.setStateMachine(state_machine);});
//                    long sequenceId = ring_buffer.next();
//                    TPS_StateMachineEvent event = ring_buffer.get(sequenceId);
//                    event.setStateMachine(state_machine);
//                    event.setEvent(next_simulation_event);
//                    ring_buffer.publish(sequenceId);
                }
            }
            EventTranslator<TPS_StateMachineEvent>[] bla = new  EventTranslator[handling_machines.size()];
            handling_machines.toArray(bla);
            handling_machines.clear();
            if(bla.length > 0) {
                ring_buffer.publishEvents(bla);

                try {
                    cb.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
            next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP, simulation_time_stamp.incrementAndGet());
            //System.out.println("Simtime: "+simulation_time_stamp.get()+" Ring remaining Capacity: "+ring_buffer.remainingCapacity()+" handling event count: "+bla.length+" unfinished machines count: "+unfinished);
            //
        }
        disruptor.shutdown();
        writer.finish();

        System.out.println("finishing in: "+((System.currentTimeMillis()-start)/1000));

    }
}
