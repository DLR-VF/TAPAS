package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.execution.sequential.TPS_SequentialSimulator;
import de.dlr.ivf.tapas.execution.sequential.action.ActionProvider;
import de.dlr.ivf.tapas.execution.sequential.communication.SharingMediator;
import de.dlr.ivf.tapas.execution.sequential.communication.SimpleCarSharingOperator;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_EventType;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_Event;
import de.dlr.ivf.tapas.execution.sequential.io.HouseholdBasedPlanGenerator;
import de.dlr.ivf.tapas.execution.sequential.statemachine.HouseholdBasedStateMachineController;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachineFactory;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.mode.TPS_ModeValidator;
import de.dlr.ivf.tapas.mode.TazBasedCarSharingDelegator;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_HouseholdAndPersonLoader;
import de.dlr.ivf.tapas.persistence.db.TPS_PipedDbWriter;
import de.dlr.ivf.tapas.person.*;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import de.dlr.ivf.tapas.scheme.TPS_SchemePart;
import de.dlr.ivf.tapas.util.FuncUtils;
import de.dlr.ivf.tapas.util.parameters.ParamString;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SequentialSimulator implements TPS_Simulator{
    /**
     * The TAPAS persistence manager
     */
    private final TPS_PersistenceManager pm;
    private final TPS_DB_Connector dbConnector;

    public SequentialSimulator(TPS_PersistenceManager pm, TPS_DB_Connector dbConnector){
        this.pm = pm;
        this.dbConnector = dbConnector;
    }


    /**
     * This will set up all the needed parts for a sequential TAPAS simulation
     *
     * Steps in brief:
     *  1. initializes the {@link TPS_HouseholdAndPersonLoader} and loads all households from the database.
     *  2. Set up the preference models
     *  3. generate {@link TPS_Plan}
     *  4. determine trip count and simulation start time
     *  5. set up
     *
     * @param num_threads thread count of worker threads
     */
    @Override
    public void run(int num_threads) {

        try {

            //load households, persons and assign cars
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Loading all households, persons and cars");
            TPS_HouseholdAndPersonLoader hh_pers_loader = new TPS_HouseholdAndPersonLoader((TPS_DB_IOManager)this.pm);
            List<TPS_Household> hhs = hh_pers_loader.initAndGetAllHouseholds();
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Finished loading all households, persons and cars");
            int cnt_hh = hhs.size();
            int cnt_person = Math.toIntExact(hhs.stream().mapToInt(TPS_Household::getNumberOfMembers).sum());
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Loaded "+cnt_hh+" households and "+cnt_person+" persons");

            //setup preference models
            List<TPS_Preference> preference_models = generatePreferenceModels(hhs);
            TPS_PreferenceParameters preference_parameters = new TPS_PreferenceParameters();
            preference_parameters.readParams();

            //generate plans
            HouseholdBasedPlanGenerator plan_generator = new HouseholdBasedPlanGenerator(this.pm, preference_models,preference_parameters);

            Map<TPS_Household,List<TPS_Plan>> households_to_plans = generatePlansAndGet(plan_generator,hhs);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, " Generated "+households_to_plans.values().stream().mapToLong(List::size).sum()+" person plans");

            //set up entry time
            IntSummaryStatistics stats = calcStatistics(households_to_plans, TPS_Episode::getOriginalStart);
            long trip_count = stats.getCount();
            int simulation_start_time_minute = FuncUtils.secondsToRoundedMinutes.apply(Math.toIntExact(stats.getMin()));

            //set up sharing delegators
            TazBasedCarSharingDelegator car_sharing_delegator = new TazBasedCarSharingDelegator(initCS());

            //set up the writer
            TPS_PipedDbWriter writer = new TPS_PipedDbWriter(pm, trip_count, 1 << 19);

            //set up handlers for transition actions
            TPS_ModeValidator mode_validator = new TPS_ModeValidator(car_sharing_delegator);

            BiFunction<TPS_Episode, Supplier<Integer>, Integer> guard_adaption_function = (episode, time_deviation) -> FuncUtils.secondsToRoundedMinutes.apply(episode.getOriginalEnd() + time_deviation.get());

            ActionProvider transition_actions_provider = new ActionProvider((TPS_DB_IOManager) this.pm, writer, guard_adaption_function, mode_validator, car_sharing_delegator);


            //set up state machines
            TPS_StateMachineFactory state_machine_factory = new TPS_StateMachineFactory(transition_actions_provider);
            List<HouseholdBasedStateMachineController> statemachine_controllers = generateHouseholdStateMachineControllers(households_to_plans, state_machine_factory);
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO,
                    "Generated "+statemachine_controllers.size()+" state machine controllers with "
                            +statemachine_controllers.stream().mapToInt(HouseholdBasedStateMachineController::getStateMachinesCount).sum()
                            +" state machines | "+state_machine_factory.getImmediatelyFinishedStateMachineCnt()
                            +" state machines immediately finished because the plan contained not trips");


            //now init the first event of the simulation
            TPS_Event first_simulation_event = new TPS_Event(TPS_EventType.SIMULATION_STEP, simulation_start_time_minute);

            //initialize the database pipeline
            Thread persisting_thread = new Thread(writer);
            persisting_thread.start();

            //set up the simulation thread
            TPS_SequentialSimulator simulator = new TPS_SequentialSimulator(statemachine_controllers, Math.max(1, num_threads / 2 - 3), (TPS_DB_IOManager) this.pm, writer,  1 << 20, first_simulation_event);
            Thread simulation_thread = new Thread(simulator);
            simulation_thread.start();

            //block this thread until the writer is shut down
            persisting_thread.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
        TPS_Logger.closeLoggers();

    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isRunningSimulation() {
        return false;
    }

    private List<TPS_Preference> generatePreferenceModels(List<TPS_Household> hhs) {

        int max_model_count = hhs.stream().mapToInt(TPS_Household::getNumberOfMembers).max().orElseThrow(RuntimeException::new);

        return IntStream.range(0, max_model_count)
                .mapToObj(i -> new TPS_Preference())
                .collect(Collectors.toList());
    }



    private List<HouseholdBasedStateMachineController> generateHouseholdStateMachineControllers(Map<TPS_Household,List<TPS_Plan>> households_to_plans, TPS_StateMachineFactory state_machine_factory) {



        List<HouseholdBasedStateMachineController> controllers = new ArrayList<>(households_to_plans.keySet().size());

        for(Map.Entry<TPS_Household, List<TPS_Plan>> entry : households_to_plans.entrySet()) {

            List<TPS_Plan> plans = entry.getValue();

            Map<TPS_Person, TPS_StateMachine> state_machines = plans.stream()
                                                                    .collect(Collectors.toMap(
                                                                                TPS_Plan::getPerson,
                                                                                state_machine_factory::createStateMachineWithSimpleStates)
                                                                            );

            controllers.add(new HouseholdBasedStateMachineController(state_machines, entry.getKey()));

        }

        return controllers;
    }

    private Map<TPS_Household,List<TPS_Plan>> generatePlansAndGet(HouseholdBasedPlanGenerator plan_generator, List<TPS_Household> households){

        return households.stream()//.limit(1)
                         .collect(Collectors.toMap(
                                    Function.identity(),
                                    plan_generator::generatePersonPlansAndGet)
                                 );
    }


    private Map<Integer, SharingMediator<TPS_Car>> initCS(){

        AtomicInteger id = new AtomicInteger(0);
        IntSupplier id_provider = id::incrementAndGet;

        //read car sharing capacities from database
        String table_taz_fees_and_tolls = dbConnector.getParameters().getString(ParamString.DB_TABLE_TAZ_FEES_TOLLS);
        String key_taz_fees_and_tolls = dbConnector.getParameters().getString(ParamString.DB_NAME_FEES_TOLLS);
        Map<Integer,Integer> car_sharing_data = this.dbConnector.readCarSharingData(table_taz_fees_and_tolls, key_taz_fees_and_tolls);

        return car_sharing_data.entrySet()
                               .stream()
                               .collect(Collectors.toMap(Map.Entry::getKey,
                                                         entry -> new SimpleCarSharingOperator(entry.getValue(),id_provider, dbConnector.getParameters())));
    }

    private IntSummaryStatistics calcStatistics(Map<TPS_Household,List<TPS_Plan>> households_to_plans, ToIntFunction<TPS_Episode> collecting_function ){

        return households_to_plans.values()
                .stream()
                .flatMap(Collection::stream)
                .map(plan -> plan.getScheme().getSchemeParts())
                .flatMap(Collection::stream)
                .filter(TPS_SchemePart::isTourPart)
                .map(TPS_SchemePart::getEpisodes)
                .flatMap(Collection::stream)
                .filter(TPS_Episode::isTrip)
                .collect(Collectors.summarizingInt(collecting_function));
    }
}
