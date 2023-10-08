package de.dlr.ivf.tapas;

import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.converters.PersonDtoToPersonConverter;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.legacy.*;
import de.dlr.ivf.tapas.misc.PrimaryDriverScoreFunction;
import de.dlr.ivf.tapas.mode.ModeDistributionCalculator;
import de.dlr.ivf.tapas.mode.Modes;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.vehicle.Cars;
import de.dlr.ivf.tapas.model.vehicle.FuelType;
import de.dlr.ivf.tapas.model.vehicle.FuelTypeName;
import de.dlr.ivf.tapas.model.vehicle.FuelTypes;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.persistence.io.DataStore;
import de.dlr.ivf.tapas.persistence.io.DataStore.DataStoreBuilder;
import de.dlr.ivf.tapas.model.person.TPS_Household.TPS_HouseholdBuilder;
import de.dlr.ivf.tapas.model.constants.PersonGroups.PersonGroupsBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TapasInitializer {

    private final TPS_ParameterClass parameters;
    private final TPS_DB_IO dbIo;

    public TapasInitializer(TPS_ParameterClass parameters, ConnectionPool connectionSupplier){
        this.parameters = parameters;
        this.dbIo = new TPS_DB_IO(connectionSupplier, parameters);
    }

    /**
     * this will be mostly stuff from TPS_DB_IO
     * @return a fully initialized TAPAS instance.
     */
    public Tapas init(){

        this.setUpCodes();

        //set up the data store
        DataStoreBuilder dataStoreBuilder = DataStore.builder();

        //activities
        DataSource activityConstantsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY));
        Collection<TPS_ActivityConstant> activityConstants = dbIo.readActivityConstantCodes(activityConstantsDs);
        dataStoreBuilder.activityConstants(activityConstants);

        //age classes
        DataSource ageClassesDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_AGE));
        AgeClasses ageClasses = dbIo.readAgeClasses(ageClassesDs);
        dataStoreBuilder.ageClasses(ageClasses);

        //distance classes
        DataSource distanceClasses = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_DISTANCE));
        dataStoreBuilder.distanceClasses(dbIo.readDistanceCodes(distanceClasses));

        //income classes
        DataSource incomeClasses = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_INCOME));
        dataStoreBuilder.incomes(dbIo.readIncomeCodes(incomeClasses));

        //location constants
        DataSource locationConstants = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_LOCATION));
        dataStoreBuilder.locationConstants(dbIo.readLocationConstantCodes(locationConstants));

        //init modes and mode set
        DataSource modeDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_MODE));
        Modes modes = dbIo.readModes(modeDataSource);
        dataStoreBuilder.modes(modes);

        DataSource mctDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_MCT));
        Filter modeFilter = new Filter("name", parameters.getString(ParamString.DB_NAME_MCT));
        TPS_ModeChoiceTree modeChoiceTree = dbIo.readModeChoiceTree(mctDataSource, modeFilter, modes.getModes());

        DataSource ektDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_EKT));
        Filter ektFilter = new Filter("name", parameters.getString(ParamString.DB_NAME_EKT));
        TPS_ExpertKnowledgeTree expertKnowledgeTree = dbIo.readExpertKnowledgeTree(ektDataSource,ektFilter, modes.getModes());

        //todo init utility function
        TPS_UtilityFunction utilityFunction = null;
        ModeDistributionCalculator modeDistributionCalculator = new ModeDistributionCalculator(modes, utilityFunction);
        dataStoreBuilder.modeSet(new TPS_ModeSet(modeChoiceTree,expertKnowledgeTree, parameters, modes, modeDistributionCalculator));

        //person groups
        DataSource personGroupsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_PERSON));
        Filter personGroupFilter = new Filter("key", parameters.getString(ParamString.DB_PERSON_GROUP_KEY));
        Collection<TPS_PersonGroup> personGroupCollection = dbIo.readPersonGroupCodes(personGroupsDs, personGroupFilter);
        PersonGroupsBuilder personGroupsWrapper = PersonGroups.builder();
        personGroupCollection.forEach(group -> personGroupsWrapper.personGroup(group.getCode(), group));
        PersonGroups personGroups = personGroupsWrapper.build();
        dataStoreBuilder.personGroups(personGroups);

        //settlement systems
        DataSource settlementSystemsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_SETTLEMENT));
        Collection<TPS_SettlementSystem> settlementSystems = dbIo.readSettlementSystemCodes(settlementSystemsDs);
        dataStoreBuilder.settlementSystems(settlementSystems);

        //activity to location mappings
        DataSource activityToLocationMappings = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION));
        Filter actToLocFilter = new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_2_LOCATION_KEY));
        dataStoreBuilder.activityAndLocationCodeMapping(dbIo.readActivity2LocationCodes(activityToLocationMappings, actToLocFilter));

        //read utility function data
        DataSource utilityFunctionData = new DataSource(parameters.getString(ParamString.DB_NAME_MODEL_PARAMETERS));
        Collection<Filter> utilityFunctionFilters = List.of(new Filter("key", parameters.getString(ParamString.UTILITY_FUNCTION_KEY)),
                new Filter("utility_function_class", parameters.getString(ParamString.UTILITY_FUNCTION_NAME)));

        dataStoreBuilder.utilityFunctionData(dbIo.readUtilityFunction(utilityFunctionData, utilityFunctionFilters));


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
        dataStoreBuilder.schemeSet(schemeSet);

        //read households, persons and cars

        //read cars
        DataSource cars = new DataSource(parameters.getString(ParamString.DB_TABLE_CARS));
        Filter carFilter = new Filter("car_key", parameters.getString(ParamString.DB_CAR_FLEET_KEY));
        //init FuelTypes
        FuelTypes fuelTypes = initFuelTypes();
        Cars carFleet = dbIo.loadCars(cars, carFilter, fuelTypes);

        //read households
        DataSource households = new DataSource(parameters.getString(ParamString.DB_TABLE_HOUSEHOLD));
        Filter hhFilter = new Filter("hh_key", parameters.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY));
        Map<Integer, TPS_HouseholdBuilder> hhBuilders = dbIo.readHouseholds(households, hhFilter, carFleet);

        //load persons
        DataSource persons = new DataSource(parameters.getString(ParamString.DB_TABLE_PERSON));
        Filter personFilter = new Filter("p_key", parameters.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY));

        Map<Integer, Collection<TPS_Person>> personsByHhId = dbIo.loadPersons(persons, personFilter, new PersonDtoToPersonConverter(parameters, ageClasses));

        //add persons to households
        for(Map.Entry<Integer,TPS_HouseholdBuilder> hhBuilderEntry : hhBuilders.entrySet()){
            TPS_HouseholdBuilder builder = hhBuilderEntry.getValue();
            builder.members(personsByHhId.get(hhBuilderEntry.getKey()));
        }

        //now set the driver scores to each person
        PrimaryDriverScoreFunction driverScoreFunction = new PrimaryDriverScoreFunction();
        personsByHhId.values()
                .forEach(hhMembers ->
                        hhMembers.forEach(member ->
                                member.setDriverScore(driverScoreFunction.apply(member,hhMembers,0))
                        )
                );

        //read region
        Map<Integer, TPS_SettlementSystem> settlementSystemMap = settlementSystems.stream()
                .collect(Collectors.toMap(
                        TPS_SettlementSystem::getId,
                        system -> system
                ));

       // TPS_Region region = dbIo.readRegion();









        return Tapas.init(null, null);
    }

    private void setUpCodes(){

        dbIo.readCarCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_CARS)));
        dbIo.readDrivingLicenseCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION)));
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
