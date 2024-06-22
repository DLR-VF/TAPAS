package de.dlr.ivf.tapas;

import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.choice.*;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.legacy.*;
import de.dlr.ivf.tapas.misc.PrimaryDriverScoreFunction;
import de.dlr.ivf.tapas.mode.CostCalculator;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.mode.cost.MNLFullComplexFunction;
import de.dlr.ivf.tapas.model.choice.DiscreteDistributionFactory;
import de.dlr.ivf.tapas.model.DistanceClasses;
import de.dlr.ivf.tapas.model.Incomes;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.model.plan.acceptance.TPS_PlanEVA1Acceptance;
import de.dlr.ivf.tapas.model.vehicle.Cars;
import de.dlr.ivf.tapas.model.vehicle.FuelType;
import de.dlr.ivf.tapas.model.vehicle.FuelTypeName;
import de.dlr.ivf.tapas.model.vehicle.FuelTypes;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.simulation.Processor;
import de.dlr.ivf.tapas.simulation.implementation.SimulationWorker;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.lang.System.Logger;

public class TapasInitializer {

    private final Logger logger = System.getLogger(TPS_DB_IO.class.getName());
    private final TPS_ParameterClass parameters;
    private final TPS_DB_IO dbIo;
    private final Runnable runWhenDone;

    public TapasInitializer(TPS_ParameterClass parameters, ConnectionPool connectionSupplier, Runnable runWhenDone){
        this.parameters = parameters;
        this.dbIo = new TPS_DB_IO(connectionSupplier, parameters);
        this.runWhenDone = runWhenDone;
    }

    public TapasInitializer(Map<String, String> parameters, ConnectionPool connectionSupplier, Runnable runWhenDone){

        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.fromMap(parameters);
        this.parameters = parameterClass;
        this.dbIo = new TPS_DB_IO(connectionSupplier, parameterClass);
        this.runWhenDone = runWhenDone;
    }

    /**
     * this will be mostly stuff from TPS_DB_IO
     * @return a fully initialized TAPAS instance.
     */
    public Tapas init(){


        //location constants
        DataSource locationConstantsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_LOCATION));
        Collection<Integer> locationConstants = dbIo.readLocationConstantCodes(locationConstantsDs);




        //init choice models, choice sets, travel time/distance calculators
        try {
            //dbIo.readMatrices(region.getTrafficAnalysisZones());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        LocationChoiceSetFactory locationChoiceSetFactory = new LocationChoiceSetFactory();
        TPS_LocationChoiceSet locationChoiceSet = locationChoiceSetFactory.newLocationChoiceSetModel(
                        parameters.getString(ParamString.LOCATION_CHOICE_SET_CLASS),
                        parameters);

        DataSource mctDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_MCT));
        Filter modeFilter = new Filter("name", parameters.getString(ParamString.DB_NAME_MCT));
        TPS_ModeChoiceTree modeChoiceTree = dbIo.readModeChoiceTree(mctDataSource, modeFilter, null);

        DataSource ektDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_EKT));
        Filter ektFilter = new Filter("name", parameters.getString(ParamString.DB_NAME_EKT));
        TPS_ExpertKnowledgeTree expertKnowledgeTree = dbIo.readExpertKnowledgeTree(ektDataSource,ektFilter, null);

        TravelDistanceCalculator travelDistanceCalculator = new TravelDistanceCalculator(parameters);
        TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(parameters, null);
        TPS_UtilityFunction utilityFunction = locationChoiceSetFactory.newUtilityFunctionInstance(
                parameters.getString(ParamString.UTILITY_FUNCTION_NAME),
                travelDistanceCalculator,
                travelTimeCalculator,
                parameters,
                null
        );

        //read utility function data
        DataSource utilityFunctionData = new DataSource(parameters.getString(ParamString.DB_NAME_MODEL_PARAMETERS));
        Collection<Filter> utilityFunctionFilters = List.of(new Filter("key", parameters.getString(ParamString.UTILITY_FUNCTION_KEY)),
                new Filter("utility_function_class", parameters.getString(ParamString.UTILITY_FUNCTION_NAME)));

        Collection<UtilityFunctionDto> utilityFunctionDtos = dbIo.readUtilityFunction(utilityFunctionData, utilityFunctionFilters);
        Map<TPS_Mode, MNLFullComplexFunction> mnlFunctions = new HashMap<>();
        utilityFunctionDtos
                .forEach(dto -> mnlFunctions.put(null,new MNLFullComplexFunction(dto.getParameters())));

        ModeDistributionCalculator modeDistributionCalculator = new ModeDistributionCalculator(null, utilityFunction);
        utilityFunction.setDistributionCalculator(modeDistributionCalculator);

        DistanceClasses distanceClasses = null;
        TPS_ModeSet modeSet = new TPS_ModeSet(modeChoiceTree,expertKnowledgeTree, parameters, null, modeDistributionCalculator, distanceClasses, utilityFunction);

        TPS_LocationSelectModel locationSelectModel = locationChoiceSetFactory.newLocationSelectionModel(
                parameters.getString(ParamString.LOCATION_SELECT_MODEL_CLASS),
                parameters,
                utilityFunction,
                travelDistanceCalculator,
                modeDistributionCalculator,
                modeSet,
                travelTimeCalculator,
                distanceClasses
        );

//        region.initLocationSelectModel(locationSelectModel);
//        region.initLocationChoiceSet(locationChoiceSet);

        int numWorkers = parameters.getIntValue(ParamValue.NUM_WORKERS);
        CountDownLatch countDownLatch = new CountDownLatch(numWorkers);

        //SchemeSelector schemeSelector = new SchemeSelector(schemeSet);
        TPS_Region region = null;
        LocationSelector locationSelector = new LocationSelector(region, travelDistanceCalculator);
        FeasibilityCalculator feasibilityCalculator = new FeasibilityCalculator();

        DiscreteDistributionFactory<TPS_Mode> modeDistributionFactory = new DiscreteDistributionFactory<>();
        ModeSelector modeSelector = new ModeSelector(modeSet,parameters, modeDistributionFactory,mnlFunctions,null);
        CostCalculator costCalculator = new CostCalculator(parameters,null);
        LocationAndModeChooser locationAndModeChooser = new LocationAndModeChooser(parameters, locationSelector, modeSelector, travelDistanceCalculator, travelTimeCalculator, costCalculator);
        TPS_PlanEVA1Acceptance acceptance = new TPS_PlanEVA1Acceptance(parameters);
        Processor<TPS_Household, Map<TPS_Person, TPS_PlanEnvironment>> hhProcessor = null;

        List<TPS_Household> households = null;
        Queue<TPS_Household> householdsToProcess = new ConcurrentLinkedDeque<>(List.of(households.getFirst()));

        Collection<SimulationWorker<?>> workers = IntStream.range(0,numWorkers)
                .mapToObj(i -> SimulationWorker.<TPS_Household>builder()
                        .countDownLatch(countDownLatch)
                        .processor(hhProcessor)
                        .keepRunning(true)
                        .householdsToProcess(householdsToProcess)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));


        return new Tapas(runWhenDone,workers,hhProcessor, countDownLatch);
    }

    private List<TPS_Household> initHouseHolds(Cars carFleet, PersonGroups personGroups, Incomes incomes, Collection<TPS_TrafficAnalysisZone> trafficAnalysisZones) {
        //read households
        PrimaryDriverScoreFunction driverScoreFunction = new PrimaryDriverScoreFunction();
//        personsByHhId.values()
//                .forEach(hhMembers ->
//                        hhMembers.forEach(member ->
//                                member.setDriverScore(driverScoreFunction.apply(member,hhMembers,0))
//                        )
//                );


        return null;
    }


    public FuelTypes initFuelTypes(){
        return switch (parameters.getSimulationType()){
            case BASE -> initFuelTypeDataBase();
            case SCENARIO -> initFuelTypeDataScenario();
        };
    }

    private FuelTypes initFuelTypeDataScenario() {

        FuelTypes.FuelTypesBuilder fuelTypesBuilder = FuelTypes.builder();

        for(FuelTypeName fuelType : FuelTypeName.values()) {
            FuelType.FuelTypeBuilder builder = FuelType.builder();

            builder.fuelType(fuelType);

            switch (fuelType) {
                case BENZINE -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM)).variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM)).range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case DIESEL -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_DIESEL_COST_PER_KM)).variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM)).range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case GAS, LPG -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_GAS_COST_PER_KM)).variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM)).range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case EMOBILE -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_ELECTRO_COST_PER_KM)).variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM)).range(parameters.getDoubleValue(ParamValue.MIT_RANGE_EMOBILE));
                case PLUGIN -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_PLUGIN_COST_PER_KM)).variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM)).range(parameters.getDoubleValue(ParamValue.MIT_RANGE_PLUGIN));
                case FUELCELL -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_FUELCELL_COST_PER_KM)).variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM)).range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
            }

            fuelTypesBuilder.fuelType(fuelType, builder.build());
        }
        return fuelTypesBuilder.build();
    }

    private FuelTypes initFuelTypeDataBase(){

        FuelTypes.FuelTypesBuilder fuelTypesBuilder = FuelTypes.builder();

        for(FuelTypeName fuelType : FuelTypeName.values()) {
            FuelType.FuelTypeBuilder builder = FuelType.builder();

            builder.fuelType(fuelType);

            switch (fuelType) {
                case BENZINE -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM_BASE))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case DIESEL -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_DIESEL_COST_PER_KM_BASE))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case GAS, LPG -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_GAS_COST_PER_KM_BASE))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case EMOBILE -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_ELECTRO_COST_PER_KM_BASE))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_EMOBILE));
                case PLUGIN -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_PLUGIN_COST_PER_KM_BASE))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_PLUGIN));
                case FUELCELL -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_FUELCELL_COST_PER_KM_BASE))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
            }
            fuelTypesBuilder.fuelType(fuelType, builder.build());
        }
        return fuelTypesBuilder.build();
    }


}
