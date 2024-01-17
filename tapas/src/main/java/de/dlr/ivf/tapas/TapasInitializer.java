package de.dlr.ivf.tapas;

import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.choice.*;
import de.dlr.ivf.tapas.converters.PersonDtoToPersonConverter;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.legacy.*;
import de.dlr.ivf.tapas.misc.PrimaryDriverScoreFunction;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.mode.Modes;
import de.dlr.ivf.tapas.model.ActivityAndLocationCodeMapping;
import de.dlr.ivf.tapas.model.DistanceClasses;
import de.dlr.ivf.tapas.model.Incomes;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.person.PersonComparators;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.model.plan.acceptance.TPS_PlanEVA1Acceptance;
import de.dlr.ivf.tapas.model.vehicle.Cars;
import de.dlr.ivf.tapas.model.vehicle.FuelType;
import de.dlr.ivf.tapas.model.vehicle.FuelTypeName;
import de.dlr.ivf.tapas.model.vehicle.FuelTypes;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.model.person.TPS_Household.TPS_HouseholdBuilder;
import de.dlr.ivf.tapas.model.constants.PersonGroups.PersonGroupsBuilder;
import de.dlr.ivf.tapas.simulation.Processor;
import de.dlr.ivf.tapas.simulation.implementation.HouseholdProcessor;
import de.dlr.ivf.tapas.simulation.implementation.SimulationWorker;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

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

        //read region
        logger.log(Level.INFO,"Initializing region...");
        //activity to location mappings
        //activities
        DataSource activityConstantsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY));
        Collection<TPS_ActivityConstant> activityConstants = dbIo.readActivityConstantCodes(activityConstantsDs);
        Activities activities = new Activities(activityConstants);

        DataSource activityToLocationMappings = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION));
        Filter actToLocFilter = new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_2_LOCATION_KEY));
        ActivityAndLocationCodeMapping activityToLocationCodeMapping = dbIo.readActivity2LocationCodes(activityToLocationMappings, actToLocFilter,activities);
        TPS_Region region = dbIo.readRegion(activityToLocationCodeMapping);

        this.setUpCodes();

        //read cars
        DataSource cars = new DataSource(parameters.getString(ParamString.DB_TABLE_CARS));
        Filter carFilter = new Filter("car_key", parameters.getString(ParamString.DB_CAR_FLEET_KEY));
        //init FuelTypes
        logger.log(Level.INFO, "Initializing cars...");
        logger.log(Level.INFO, "Reading fuel types...");
        FuelTypes fuelTypes = initFuelTypes();
        logger.log(Level.INFO, "Reading cars...");
        Cars carFleet = dbIo.loadCars(cars, carFilter, fuelTypes);
        logger.log(Level.INFO,"Finished initializing cars.");

        //setup households
        //person groups
        logger.log(Level.INFO, "Initializing households...");
        logger.log(Level.INFO, "Reading person groups...");
        DataSource personGroupsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_PERSON));
        Filter personGroupFilter = new Filter("key", parameters.getString(ParamString.DB_PERSON_GROUP_KEY));
        Collection<TPS_PersonGroup> personGroupCollection = dbIo.readPersonGroupCodes(personGroupsDs, personGroupFilter);
        PersonGroupsBuilder personGroupsWrapper = PersonGroups.builder();
        personGroupCollection.forEach(group -> personGroupsWrapper.personGroup(group.getCode(), group));
        PersonGroups personGroups = personGroupsWrapper.build();

        logger.log(Level.INFO,"Reading income classes...");
        //income classes
        DataSource incomeClassesDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_INCOME));
        Incomes incomeClasses = dbIo.readIncomeCodes(incomeClassesDs);



        List<TPS_Household> households = initHouseHolds(carFleet, personGroups, incomeClasses, region.getTrafficAnalysisZones());

        //distance classes
        DataSource distanceClassesDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_DISTANCE));
        DistanceClasses distanceClasses = dbIo.readDistanceCodes(distanceClassesDs);


        //location constants
        DataSource locationConstantsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_LOCATION));
        Collection<Integer> locationConstants = dbIo.readLocationConstantCodes(locationConstantsDs);

        //init modes and mode set
        DataSource modeDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_MODE));
        Modes modes = dbIo.readModes(modeDataSource);

        DataSource mctDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_MCT));
        Filter modeFilter = new Filter("name", parameters.getString(ParamString.DB_NAME_MCT));
        TPS_ModeChoiceTree modeChoiceTree = dbIo.readModeChoiceTree(mctDataSource, modeFilter, modes.getModes());

        DataSource ektDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_EKT));
        Filter ektFilter = new Filter("name", parameters.getString(ParamString.DB_NAME_EKT));
        TPS_ExpertKnowledgeTree expertKnowledgeTree = dbIo.readExpertKnowledgeTree(ektDataSource,ektFilter, modes.getModes());

        //read utility function data
        DataSource utilityFunctionData = new DataSource(parameters.getString(ParamString.DB_NAME_MODEL_PARAMETERS));
        Collection<Filter> utilityFunctionFilters = List.of(new Filter("key", parameters.getString(ParamString.UTILITY_FUNCTION_KEY)),
                new Filter("utility_function_class", parameters.getString(ParamString.UTILITY_FUNCTION_NAME)));

        Collection<UtilityFunctionDto> utilityFunctionDtos = dbIo.readUtilityFunction(utilityFunctionData, utilityFunctionFilters);


        //read SchemeSet
        DataSource schemeClasses = new DataSource(parameters.getString(ParamString.DB_TABLE_SCHEME_CLASS));
        Filter schemeClassFilter = new Filter("key", parameters.getString(ParamString.DB_SCHEME_CLASS_KEY));
        Collection<SchemeClassDto> schemeClassDtos = dbIo.readSchemeClasses(schemeClasses, schemeClassFilter);

        DataSource schemes = new DataSource(parameters.getString(ParamString.DB_TABLE_SCHEME));
        Filter schemeFilter = new Filter("key", this.parameters.getString(ParamString.DB_SCHEME_KEY));
        Collection<SchemeDto> schemeDtos = dbIo.readSchemes(schemes, schemeFilter);

        DataSource episodes = new DataSource(parameters.getString(ParamString.DB_TABLE_EPISODE));
        Filter episodeFilter = new Filter("key", parameters.getString(ParamString.DB_EPISODE_KEY));
        Collection<EpisodeDto> episodeDtos = dbIo.readEpisodes(episodes, episodeFilter);

        DataSource schemeClassDistributions = new DataSource(parameters.getString(ParamString.DB_TABLE_SCHEME_CLASS_DISTRIBUTION));
        Filter schemeClassDistributionFilter = new Filter("key", parameters.getString(ParamString.DB_SCHEME_CLASS_DISTRIBUTION_KEY));
        Collection<SchemeClassDistributionDto> distributionDtos = dbIo.readSchemeClassDistributions(schemeClassDistributions, schemeClassDistributionFilter);

        var schemeSet = dbIo.readSchemeSet(schemeClassDtos,schemeDtos,episodeDtos,distributionDtos, activityConstants, personGroups);


        //init choice models, choice sets, travel time/distance calculators
        try {
            dbIo.readMatrices(region.getTrafficAnalysisZones());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        LocationChoiceSetFactory locationChoiceSetFactory = new LocationChoiceSetFactory();
        TPS_LocationChoiceSet locationChoiceSet = locationChoiceSetFactory.newLocationChoiceSetModel(
                        parameters.getString(ParamString.LOCATION_CHOICE_SET_CLASS),
                        parameters);

        TravelDistanceCalculator travelDistanceCalculator = new TravelDistanceCalculator(parameters);
        TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(parameters, modes.getModeMap());
        TPS_UtilityFunction utilityFunction = locationChoiceSetFactory.newUtilityFunctionInstance(
                parameters.getString(ParamString.UTILITY_FUNCTION_NAME),
                travelDistanceCalculator,
                travelTimeCalculator,
                parameters,
                modes
        );
        ModeDistributionCalculator modeDistributionCalculator = new ModeDistributionCalculator(modes, utilityFunction);
        utilityFunction.setDistributionCalculator(modeDistributionCalculator);

        TPS_LocationSelectModel locationSelectModel = locationChoiceSetFactory.newLocationSelectionModel(
                parameters.getString(ParamString.LOCATION_SELECT_MODEL_CLASS),
                parameters,
                utilityFunction,
                travelDistanceCalculator,
                modeDistributionCalculator,
                new TPS_ModeSet(modeChoiceTree,expertKnowledgeTree, parameters, modes, modeDistributionCalculator, distanceClasses),
                travelTimeCalculator,
                distanceClasses
        );

        region.initLocationSelectModel(locationSelectModel);
        region.initLocationChoiceSet(locationChoiceSet);

        int numWorkers = parameters.getIntValue(ParamValue.NUM_WORKERS);
        CountDownLatch countDownLatch = new CountDownLatch(numWorkers);

        SchemeSelector schemeSelector = new SchemeSelector(schemeSet);
        LocationSelector locationSelector = new LocationSelector(region, travelDistanceCalculator);
        FeasibilityCalculator feasibilityCalculator = new FeasibilityCalculator(parameters);

        ModeSelector modeSelector = new ModeSelector(new TPS_ModeSet(modeChoiceTree,expertKnowledgeTree,parameters,modes,modeDistributionCalculator, distanceClasses),parameters);
        LocationAndModeChooser locationAndModeChooser = new LocationAndModeChooser(parameters, locationSelector, modeSelector);
        TPS_PlanEVA1Acceptance acceptance = new TPS_PlanEVA1Acceptance(parameters);
        Processor<TPS_Household, Map<TPS_Person, TPS_PlanEnvironment>> hhProcessor = HouseholdProcessor.builder()
                .schemeSelector(schemeSelector)
                .locationAndModeChooser(locationAndModeChooser)
                .maxTriesScheme(parameters.getIntValue(ParamValue.MAX_TRIES_SCHEME))
                .planEVA1Acceptance(acceptance)
                .feasibilityCalculator(feasibilityCalculator)
                .build();

        Queue<TPS_Household> householdsToProcess = new ConcurrentLinkedDeque<>(List.of(households.get(0)));

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
        logger.log(Level.INFO, "Reading households.");
        DataSource householdsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_HOUSEHOLD));
        Filter hhFilter = new Filter("hh_key", parameters.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY));

        var tazMap = trafficAnalysisZones.stream()
                .collect(Collectors.toMap(
                        TPS_TrafficAnalysisZone::getTAZId,
                        taz -> taz
                ));
        Map<Integer, TPS_HouseholdBuilder> hhBuilders = dbIo.readHouseholds(householdsDs, hhFilter, carFleet, incomes, tazMap);

        //age classes
        logger.log(Level.INFO, "Reading age classes.");
        DataSource ageClassesDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_AGE));
        AgeClasses ageClasses = dbIo.readAgeClasses(ageClassesDs);

        //load persons
        logger.log(Level.INFO, "Reading persons.");
        DataSource persons = new DataSource(parameters.getString(ParamString.DB_TABLE_PERSON));
        Filter personFilter = new Filter("p_key", parameters.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY));

        Map<Integer, Collection<TPS_Person>> personsByHhId = dbIo.loadPersons(persons, personFilter, new PersonDtoToPersonConverter(parameters, ageClasses, personGroups));

        //now set the driver scores to each person
        logger.log(Level.INFO, "Computing primary driver scores.");
        PrimaryDriverScoreFunction driverScoreFunction = new PrimaryDriverScoreFunction();
        personsByHhId.values()
                .forEach(hhMembers ->
                        hhMembers.forEach(member ->
                                member.setDriverScore(driverScoreFunction.apply(member,hhMembers,0))
                        )
                );

        //add persons to households in predefined order
        TPS_Household.Sorting sortAlgo = parameters.isDefined(ParamString.HOUSEHOLD_MEMBERSORTING)
                ? TPS_Household.Sorting.valueOf(parameters.getString(ParamString.HOUSEHOLD_MEMBERSORTING))
                : TPS_Household.Sorting.AGE;

        Comparator<TPS_Person> sortComparator = PersonComparators.ofSorting(sortAlgo);

        List<TPS_Household> households = new ArrayList<>();

        for(Map.Entry<Integer,TPS_HouseholdBuilder> hhBuilderEntry : hhBuilders.entrySet()){
            TPS_HouseholdBuilder builder = hhBuilderEntry.getValue();
            Collection<TPS_Person> householdMembers = personsByHhId.get(hhBuilderEntry.getKey())
                    .stream()
                    .sorted(sortComparator)
                    .toList();
            builder.members(householdMembers);

            TPS_Household hh = builder.build();
            householdMembers.forEach(member -> member.setHousehold(hh));
            households.add(builder.build());
        }

        logger.log(Level.INFO, "Finished initializing {0} households with {1} total persons",households.size(), personsByHhId.values().stream().mapToInt(Collection::size).sum());
        return households;
    }

    private void setUpCodes(){
        logger.log(Level.INFO, "Reading car codes.");
        dbIo.readCarCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_CARS)));
        logger.log(Level.INFO, "Reading Driving licence information.");
        dbIo.readDrivingLicenseCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION)));
        logger.log(Level.INFO, "Reading gender codes.");
        dbIo.readSexCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_SEX)));
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
                case BENZINE -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case DIESEL -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_DIESEL_COST_PER_KM))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL))
                        .fuelType(fuelType);
                case GAS, LPG -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_GAS_COST_PER_KM))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
                case EMOBILE -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_ELECTRO_COST_PER_KM))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_EMOBILE));
                case PLUGIN -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_PLUGIN_COST_PER_KM))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_PLUGIN));
                case FUELCELL -> builder.fuelCostPerKm(parameters.getDoubleValue(ParamValue.MIT_FUELCELL_COST_PER_KM))
                        .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM))
                        .range(parameters.getDoubleValue(ParamValue.MIT_RANGE_CONVENTIONAL));
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
