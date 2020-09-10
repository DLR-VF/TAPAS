package de.dlr.ivf.tapas.plan.state;

import com.lmax.disruptor.*;
import com.lmax.disruptor.util.DaemonThreadFactory;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_PipedDbWriter;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.state.event.*;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachine;
import de.dlr.ivf.tapas.plan.state.statemachine.TPS_PlanStateMachineFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class TPS_PlanExecutorWithDisruptor extends TPS_PlansExecutor implements Runnable{
    private List<TPS_Plan> plans;
    private int worker_count;
    private int buffer_size;
    private TPS_DB_IOManager pm;

    private int plan_count;

    private AtomicInteger simulation_time_stamp = new AtomicInteger(0);
    private TPS_PlanEvent next_simulation_event;
    private TPS_PipedDbWriter writer;
    private List<TPS_PlanStateMachine> state_machines;
    private TPS_StateMachineHandler[] workers;

    public TPS_PlanExecutorWithDisruptor(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm, TPS_TripWriter writer, int buffer_size){
        super();

        this.plans = plans;
        this.plan_count = plans.size();
        this.state_machines = new ArrayList<>(plan_count);
        this.worker_count = worker_count/2;
        this.buffer_size = buffer_size;
        this.workers = new TPS_StateMachineHandler[this.worker_count];
        this.pm = pm;
        this.writer = (TPS_PipedDbWriter) writer;

        createStateMachines();
        preInitialize();

    }
    private void preInitialize(){

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Initializing first simulation event...");

        //set simulation start time
        plans.stream().mapToInt(plan -> plan.getScheme().getSchemeParts().get(0).getFirstEpisode().getOriginalDuration()).min().ifPresentOrElse( i -> simulation_time_stamp.set((int) (i* 1.66666666e-2 + 0.5)), () -> simulation_time_stamp.set(0));

        next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,  simulation_time_stamp.get());
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "First simulation event is of type: "+next_simulation_event.getEventType()+" at time: "+next_simulation_event.getData());
    }

    private void createStateMachines() {
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Generating "+this.plan_count+" state machines...");
        plans.forEach(plan -> state_machines.add(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plan, writer, pm, this)));
        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "All state machines ready...");
    }

    public void run(){

        //event factory for event pre-allocation on the ring
        EventFactory<TPS_StateMachineEvent> event_factory = TPS_StateMachineEvent::new;

        //set up our event consumers
        IntStream.range(0,worker_count).forEach(i -> workers[i] = new TPS_StateMachineHandler("worker_"+i));

        //set up the ring and worker pool
        RingBuffer<TPS_StateMachineEvent> ring_buffer = RingBuffer.createSingleProducer(event_factory, buffer_size, new BusySpinWaitStrategy());
        WorkerPool<TPS_StateMachineEvent> worker_pool = new WorkerPool<>(ring_buffer, ring_buffer.newBarrier(), new StateMachineEventExceptionHandler(), workers);
        ring_buffer.addGatingSequences(worker_pool.getWorkerSequences());

        //start the worker pool
        Executor executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);
        worker_pool.start(executor);

        boolean all_finished = false;
        long total_count = 0;
        long sim_start = System.currentTimeMillis();
        int last_written_trip_count = 0;

        //launch the simulation progress update thread
        writer.startSimulationProgressUpdateTask();

        while(!all_finished && simulation_time_stamp.get() < 2000){

            all_finished = true;
            int unfinished = 0;
            int event_count = 0;
            long start = System.currentTimeMillis();

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Publishing and consuming events for simulation time: "+simulation_time_stamp.get());

            for(TPS_PlanStateMachine state_machine: state_machines){

                all_finished = all_finished && state_machine.hasFinished();

                if(!state_machine.hasFinished())
                    unfinished++;

                if (state_machine.willHandleEvent(next_simulation_event)) {
                    event_count++;
                    total_count++;

                    //get sequence id of the next available event slot
                    long sequenceId = ring_buffer.next();

                    //get the associated event
                    TPS_StateMachineEvent event = ring_buffer.get(sequenceId);

                    //set event values
                    event.setStateMachine(state_machine);
                    event.setEvent(next_simulation_event);

                    //finally publish the event being ready to be picked up by a worker
                    ring_buffer.publish(sequenceId);
                }
            }

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, event_count+" events published for "+unfinished+" unfinished state machines in "+(System.currentTimeMillis()-start)+" ms");

            //the position of the last published event
            long cursor = ring_buffer.getCursor();

            //wait until all workers have finished the latest batch
            while(Arrays.stream(worker_pool.getWorkerSequences()).anyMatch(sequence -> sequence.get() < cursor)){
                Thread.onSpinWait();
            }

            int written_trip_count = writer.getWrittenTripCount();
            int trips_in_pipeline = writer.getRegisteredTripCount() - written_trip_count;

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, event_count+" events handled, "+( written_trip_count - last_written_trip_count)+
                    " trips written  in "+(System.currentTimeMillis()-start)/1000+" seconds for simulation time: "+simulation_time_stamp.get()+
                    " ( "+trips_in_pipeline+" trips in writer pipeline )");

            last_written_trip_count = written_trip_count;

            next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,  simulation_time_stamp.incrementAndGet());
        }

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Simulation finished! "+total_count+" events handled in "+(System.currentTimeMillis()-sim_start)/60000+" minutes");

        worker_pool.drainAndHalt();
        writer.finish();


    }
}
