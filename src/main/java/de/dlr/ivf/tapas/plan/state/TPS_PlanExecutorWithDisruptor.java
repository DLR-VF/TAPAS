package de.dlr.ivf.tapas.plan.state;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
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
    private int worker_count;
    private TPS_DB_IOManager pm;

    private int plan_count;

    private AtomicInteger simulation_time_stamp = new AtomicInteger(0);
    private TPS_PlanEvent next_simulation_event;
    private TPS_TripWriter writer;
    private List<TPS_PlanStateMachine> state_machines;
    private TPS_StateMachineHandler[] workers;

    public TPS_PlanExecutorWithDisruptor(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm, TPS_TripWriter writer){
        super();

        this.plans = plans;
        this.plan_count = plans.size();
        this.state_machines = new ArrayList<>(plan_count);
        this.worker_count = worker_count/2;
        this.workers = new TPS_StateMachineHandler[this.worker_count];
        this.pm = pm;
        this.writer = writer;

        createStateMachines();
        preInitialize();
    }
    private void preInitialize(){

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Initializing first simulation event...");
        //the first simulation event
        simulation_time_stamp.set((int) (plans.stream().mapToInt(plan -> plan.getScheme().getSchemeParts().get(0).getFirstEpisode().getOriginalDuration()).min().getAsInt()* 1.66666666e-2 + 0.5));
        next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,  simulation_time_stamp.get());
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "First simulation event is of type: "+next_simulation_event.getEventType()+" at time: "+next_simulation_event.getData());
    }

    private void createStateMachines() {
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Generating "+plans.size()+" state machines...");
        plans.forEach(plan -> state_machines.add(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plan, writer, pm, this)));
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "All state machines ready...");
    }


    public void run(){
        int bufferSize = (int)Math.pow(2,20);

        EventFactory<TPS_StateMachineEvent> ef = () -> new TPS_StateMachineEvent();


        IntStream.range(0,worker_count).forEach(i -> workers[i] = new TPS_StateMachineHandler("worker_"+i));

        Executor executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);

        RingBuffer<TPS_StateMachineEvent> ring_buffer = RingBuffer.createSingleProducer(ef,bufferSize,new BusySpinWaitStrategy());
        WorkerPool<TPS_StateMachineEvent> worker_pool = new WorkerPool<>(ring_buffer, ring_buffer.newBarrier(), new StateMachineEventExceptionHandler(),workers);
        ring_buffer.addGatingSequences(worker_pool.getWorkerSequences());
        worker_pool.start(executor);

        boolean all_finished = false;
        long total_count = 0;
        long sim_start = System.currentTimeMillis();
        while(!all_finished && simulation_time_stamp.get() < 2000){

            all_finished = true;
            int unfinished = 0;
            int event_count = 0;
            long start = System.currentTimeMillis();
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Publishing events for simulation time: "+simulation_time_stamp.get());
            for(TPS_PlanStateMachine state_machine: state_machines){
                all_finished = all_finished && state_machine.hasFinished();
                if(!state_machine.hasFinished())
                    unfinished++;
                if (state_machine.willHandleEvent(next_simulation_event)) {
                    event_count++;
                    total_count++;
                    long sequenceId = ring_buffer.next();
                    TPS_StateMachineEvent event = ring_buffer.get(sequenceId);
                    event.setStateMachine(state_machine);
                    event.setEvent(next_simulation_event);
                    ring_buffer.publish(sequenceId);
                }
            }



            long cursor = ring_buffer.getCursor();
            long start_spin = System.currentTimeMillis();
            while(Arrays.stream(worker_pool.getWorkerSequences()).filter(sequence -> sequence.get() < cursor).findAny().isPresent()){
                Thread.onSpinWait();
            };
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, event_count+" events handled in "+(start-System.currentTimeMillis())/1000+" seconds for simulation time: "+simulation_time_stamp.get());
            //System.out.println("spinning at simtime: "+simulation_time_stamp.get()+" with event count: "+event_count+" for: "+(System.currentTimeMillis()-start_spin)+"ms");
            next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,  simulation_time_stamp.incrementAndGet());
        }
        worker_pool.drainAndHalt();
        writer.finish();

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Simulation finished! "+total_count+" events handled in "+(sim_start-System.currentTimeMillis())/60000+" minutes");
    }
}
