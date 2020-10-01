package de.dlr.ivf.tapas.plan.sequential;

import com.lmax.disruptor.*;
import com.lmax.disruptor.util.DaemonThreadFactory;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_PipedDbWriter;
import de.dlr.ivf.tapas.persistence.db.TPS_TripWriter;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.sequential.event.*;
import de.dlr.ivf.tapas.plan.sequential.statemachine.TPS_PlanStateMachine;
import de.dlr.ivf.tapas.plan.sequential.statemachine.TPS_PlanStateMachineFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * This should run inside its own thread. Upon construction the simulator will set up a {@link TPS_PlanStateMachine}
 * for each plan passed to the constructor and determines the first {@link TPS_PlanEvent}.
 *
 * When the {@link TPS_SequentialSimulator} is started, a single producer of {@link TPS_StateMachineEvent}s
 * and a {@link WorkerPool} of {@link TPS_StateMachineHandler}s as consumers are set up around a {@link RingBuffer}.
 * It is recommended that the count of {@link TPS_StateMachineHandler} does not exceed the count of PHYSICAL cpu cores.
 * Best results in performance have been achieved with "physical core count" - 2.
 *
 * During iteration, every simulation time step is wrapped into a {@link TPS_PlanEvent} of type {@link TPS_PlanEventType#SIMULATION_STEP}
 * and passed to every {@link TPS_PlanStateMachine}. If a state machine will handle the event, it is wrapped into a
 * {@link TPS_StateMachineEvent} and put onto the {@link RingBuffer} for transition.
 *
 * After each iteration (simulation time stamp), the {@link TPS_SequentialSimulator} will wait until all {@link TPS_StateMachineHandler}s
 * have finished working on their dedicated events. When all handlers are done, a new {@link TPS_PlanEvent} with an incremented
 * simulation time stamp is generated.
 *
 * When an exception occurs during computations it will be handled by the {@link StateMachineEventExceptionHandler}.
 *
 */

public class TPS_SequentialSimulator implements Runnable{
    private int worker_count;
    private int buffer_size;
    private TPS_DB_IOManager pm;

    private AtomicInteger simulation_time_stamp = new AtomicInteger(0);
    private TPS_PlanEvent next_simulation_event;
    private TPS_PipedDbWriter writer;
    private List<TPS_PlanStateMachine> state_machines;
    private TPS_StateMachineHandler[] workers;

    /**
     * Initializes all {@link TPS_PlanStateMachine}s and the first simulation time event.
     * @param plans will form the foundation of the state machines.
     * @param worker_count the number of {@link TPS_StateMachineHandler}s. It is recommended that the worker count does
     *                     not exceed the number of PHYSICAL cores in the cpu.
     * @param pm the persistence manager
     * @param writer that handles persisting the data
     * @param buffer_size of the {@link RingBuffer}. Must be a power of 2.
     */
    public TPS_SequentialSimulator(List<TPS_Plan> plans, int worker_count, TPS_DB_IOManager pm, TPS_TripWriter writer, int buffer_size){

        this.worker_count = worker_count;
        this.buffer_size = buffer_size;
        this.workers = new TPS_StateMachineHandler[this.worker_count];
        this.pm = pm;
        this.writer = (TPS_PipedDbWriter) writer;

        this.state_machines = createAndGetStateMachines(plans, writer);
        this.next_simulation_event = initAndGetFirstSimulationTimeEvent(plans);
        this.simulation_time_stamp.set((int) next_simulation_event.getData());
    }

    /**
     * Extracts the start time for the simulation and generates the first {@link TPS_PlanEvent}.
     * @param plans containing all {@link de.dlr.ivf.tapas.scheme.TPS_SchemePart}s and their {@link de.dlr.ivf.tapas.scheme.TPS_Episode}s
     * @return a {@link TPS_PlanEvent} of type {@link TPS_PlanEventType#SIMULATION_STEP} where its payload is equal to the time
     *         the first person is leaving the house.
     */

    private TPS_PlanEvent initAndGetFirstSimulationTimeEvent(List<TPS_Plan> plans){

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Initializing first simulation event...");

        AtomicInteger simulation_start_time = new AtomicInteger(0);

        plans.stream().parallel()
                .mapToInt(plan -> plan.getScheme().getSchemeParts().get(0).getFirstEpisode().getOriginalDuration())
                .min()
                .ifPresent( i -> simulation_start_time.set((int) (i * 1.66666666e-2 + 0.5)));

        TPS_PlanEvent first_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,  simulation_start_time.get());

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "First simulation event is of type: "+first_simulation_event.getEventType()+" at time: "+first_simulation_event.getData());

        return first_simulation_event;
    }

    /**
     * Creates a {@link TPS_PlanStateMachine} with a set of states representing the behaviour for every {@link de.dlr.ivf.tapas.scheme.TPS_Episode}
     * inside a {@link TPS_Plan}.
     * @param plans that need to be represented as a {@link TPS_PlanStateMachine}
     * @param writer for the {@link de.dlr.ivf.tapas.plan.sequential.action.TPS_PlanStatePersistenceAction}s
     * @return a list of {@link TPS_PlanStateMachine}s
     */

    private List<TPS_PlanStateMachine> createAndGetStateMachines(List<TPS_Plan> plans, TPS_TripWriter writer) {

        var plan_count = plans.size();
        var state_machines = new ArrayList<TPS_PlanStateMachine>(plan_count);

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Generating "+plan_count+" state machines...");

        plans.forEach(plan -> state_machines.add(TPS_PlanStateMachineFactory.createTPS_PlanStateMachineWithSimpleStates(plan, writer, pm)));

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "All state machines ready...");

        return state_machines;
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

        var all_finished = false;
        var total_count = 0;
        var sim_start = System.currentTimeMillis();
        var last_written_trip_count = 0;

        //launch the simulation progress update thread
        writer.startSimulationProgressUpdateTask();

        //start the simulation
        while(!all_finished && simulation_time_stamp.get() < 2000){

            all_finished = true;
            var unfinished = 0;
            var event_count = 0;
            var current_iteration_start_time = System.currentTimeMillis();

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Publishing and consuming events for simulation time: "+simulation_time_stamp.get());

            for(TPS_PlanStateMachine state_machine : state_machines){
                all_finished = all_finished && state_machine.hasFinished();

                if(!state_machine.hasFinished()) {
                    unfinished++;
                }

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

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, event_count+" events published for "+unfinished+" unfinished state machines in "+(System.currentTimeMillis()-current_iteration_start_time)+" ms");

            //the position of the last published event
            long cursor = ring_buffer.getCursor();

            //wait until all workers have finished the latest batch
            while(Arrays.stream(worker_pool.getWorkerSequences()).anyMatch(sequence -> sequence.get() < cursor)){
                Thread.onSpinWait();
            }

            var written_trip_count = writer.getWrittenTripCount();
            var trips_in_pipeline_count = writer.getRegisteredTripCount();

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, event_count+" events handled, "+( written_trip_count - last_written_trip_count)+
                    " trips written  in "+(System.currentTimeMillis()-current_iteration_start_time)/1000+" seconds for simulation time: "+simulation_time_stamp.get()+
                    " ( "+trips_in_pipeline_count+" trips in writer pipeline )");

            last_written_trip_count = written_trip_count;

            next_simulation_event = new TPS_PlanEvent(TPS_PlanEventType.SIMULATION_STEP,  simulation_time_stamp.incrementAndGet());
        }

        TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Simulation finished! "+total_count+" events handled in "+(System.currentTimeMillis()-sim_start)/60000+" minutes");

        worker_pool.drainAndHalt();
        writer.finish();
    }
}
