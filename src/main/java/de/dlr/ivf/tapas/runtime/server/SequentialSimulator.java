package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.execution.sequential.TPS_SequentialSimulator;
import de.dlr.ivf.tapas.execution.sequential.action.ActionProvider;
import de.dlr.ivf.tapas.execution.sequential.communication.SharingMediator;
import de.dlr.ivf.tapas.execution.sequential.communication.SimpleCarSharingOperator;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_EventType;
import de.dlr.ivf.tapas.execution.sequential.event.TPS_PlanEvent;
import de.dlr.ivf.tapas.execution.sequential.io.HouseholdBasedPlanGenerator;
import de.dlr.ivf.tapas.execution.sequential.statemachine.HouseholdBasedStateMachineController;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachine;
import de.dlr.ivf.tapas.execution.sequential.statemachine.TPS_StateMachineFactory;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.mode.TPS_ModeValidator;
import de.dlr.ivf.tapas.mode.TazBasedCarSharingDelegator;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.persistence.db.TPS_HouseholdAndPersonLoader;
import de.dlr.ivf.tapas.persistence.db.TPS_PipedDbWriter;
import de.dlr.ivf.tapas.person.*;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import de.dlr.ivf.tapas.util.FuncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SequentialSimulator implements TPS_Simulator{

    private final TPS_PersistenceManager pm;
    private int simulation_start_time_minute = Integer.MAX_VALUE;

    public SequentialSimulator(TPS_PersistenceManager pm){
        this.pm = pm;
    }

    private TPS_PlanEvent first_simulation_event;

    @Override
    public void run(int num_threads) {

        try {

            //load households, persons and assign cars
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Loading all households, persons and cars");
            TPS_HouseholdAndPersonLoader hh_pers_loader = new TPS_HouseholdAndPersonLoader((TPS_DB_IOManager)this.pm);
            List<TPS_Household> hhs = hh_pers_loader.initAndGetAllHouseholds();
            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Finished loading all households, persons and cars");

            //generate all plans
//            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Generating all plans.");
//            TPS_PlanGenerator plan_generator = new TPS_PlanGenerator(this.PM);
//            List<TPS_Plan> plans = plan_generator.generatePlansAndGet(hhs);

            List<TPS_Preference> preference_models = generatePreferenceModels(hhs);
            TPS_PreferenceParameters preference_parameters = new TPS_PreferenceParameters();
            preference_parameters.readParams();

//            var trip_count = (int)plans.stream()
//                    .parallel()
//                    .map(plan -> plan.getScheme().getSchemeParts())
//                    .flatMap(Collection::stream)
//                    .filter(TPS_SchemePart::isTourPart)
//                    .map(TPS_SchemePart::getEpisodes)
//                    .flatMap(Collection::stream)
//                    .filter(TPS_Episode::isTrip)
//                    .count();
            int trip_count = 12000000;
            TPS_PipedDbWriter writer = new TPS_PipedDbWriter(pm, trip_count, 1 << 19);



            //HouseholdBasedStateMachineGenerator statemachine_generator = new HouseholdBasedStateMachineGenerator(writer, PM, plan_generator);
            HouseholdBasedPlanGenerator plan_generator = new HouseholdBasedPlanGenerator(this.pm, preference_models,preference_parameters);

            TazBasedCarSharingDelegator car_sharing_delegator = new TazBasedCarSharingDelegator(initCS());

            TPS_ModeValidator mode_validator = new TPS_ModeValidator(car_sharing_delegator);

            BiFunction<TPS_Episode, Integer, Integer> guard_adaption_function = (episode, time_deviation) -> episode.getOriginalEnd() + time_deviation;

            ActionProvider transition_actions_provider = new ActionProvider((TPS_DB_IOManager) this.pm, writer, guard_adaption_function, mode_validator, car_sharing_delegator);

            TPS_StateMachineFactory state_machine_factory = new TPS_StateMachineFactory(transition_actions_provider);


            List<HouseholdBasedStateMachineController> statemachine_controllers = generateHouseholdStateMachineControllers(hhs, plan_generator, state_machine_factory);

            TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.THREAD, TPS_LoggingInterface.SeverenceLogLevel.INFO, "Finished generating plans.");

            //now init the first event of the simulation
            this.first_simulation_event = new TPS_PlanEvent(TPS_EventType.SIMULATION_STEP, this.simulation_start_time_minute);






            //initialize the database pipeline

            Thread persisting_thread = new Thread(writer);
            persisting_thread.start();



            //set up the simulation thread
            TPS_SequentialSimulator simulator = new TPS_SequentialSimulator(statemachine_controllers, Math.max(1, num_threads / 2 - 2), (TPS_DB_IOManager) this.pm, writer,  1 << 20, first_simulation_event);
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



    private List<HouseholdBasedStateMachineController> generateHouseholdStateMachineControllers(List<TPS_Household> households, HouseholdBasedPlanGenerator plan_generator, TPS_StateMachineFactory state_machine_factory) {



        List<HouseholdBasedStateMachineController> controllers = new ArrayList<>(households.size());

        for(TPS_Household hh : households){
            List<TPS_Plan> household_plans = plan_generator.generatePersonPlansAndGet(hh);

            //todo this should be handled somewhere else
            updateSimulationStartTime(household_plans);



            Map<TPS_Person, TPS_StateMachine> state_machines = household_plans.stream()
                    .collect(Collectors.toMap(
                            plan -> plan.getPerson(),
                            plan -> state_machine_factory.createStateMachineWithSimpleStates(plan)
                    ));

            controllers.add(new HouseholdBasedStateMachineController(state_machines));

        }


        return controllers;
    }

    private void updateSimulationStartTime(List<TPS_Plan> household_plans) {

        int first_trip_time = household_plans.stream().mapToInt(plan -> plan.getScheme().getSchemeParts().get(0).getFirstEpisode().getOriginalDuration()).min().getAsInt();
        this.simulation_start_time_minute = Math.min(this.simulation_start_time_minute, FuncUtils.secondsToRoundedMinutes.apply(first_trip_time));

    }

    private Map<Integer, SharingMediator<TPS_Car>> initCS(){

        Map<Integer, SharingMediator<TPS_Car>> cs = this.pm.getRegion().getTrafficAnalysisZoneKeys().stream().collect(Collectors.toMap(Function.identity(), i -> new SimpleCarSharingOperator(50)));
        return cs;

    }
}
