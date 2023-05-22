package de.dlr.ivf.tapas;

import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.configuration.model.Filter;
import de.dlr.ivf.tapas.converters.PersonDtoToPersonConverter;
import de.dlr.ivf.tapas.dto.PersonCodeDto;
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


import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TapasInitializer {

    private final TPS_ParameterClass parameters;
    private final TPS_DB_IO dbIo;

    public TapasInitializer(TPS_ParameterClass parameters, Supplier<Connection> connectionSupplier){
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
        DataSource activityConstantsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY),null);
        Collection<TPS_ActivityConstant> activityConstants = dbIo.readActivityConstantCodes(activityConstantsDs);
        dataStoreBuilder.activityConstants(activityConstants);

        //age classes
        DataSource ageClassesDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_AGE), null);
        AgeClasses ageClasses = dbIo.readAgeClasses(ageClassesDs);
        dataStoreBuilder.ageClasses(ageClasses);

        //distance classes
        DataSource distanceClasses = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_DISTANCE), null);
        dataStoreBuilder.distanceClasses(dbIo.readDistanceCodes(distanceClasses));

        //income classes
        DataSource incomeClasses = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_INCOME), null);
        dataStoreBuilder.incomes(dbIo.readIncomeCodes(incomeClasses));

        //location constants
        DataSource locationConstants = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_LOCATION), null);
        dataStoreBuilder.locationConstants(dbIo.readLocationConstantCodes(locationConstants));

        //init modes and mode set
        DataSource modeDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_MODE), null);
        Modes modes = dbIo.readModes(modeDataSource);
        dataStoreBuilder.modes(modes);

        DataSource mctDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_MCT),
                List.of(new Filter("name", parameters.getString(ParamString.DB_NAME_MCT))));
        TPS_ModeChoiceTree modeChoiceTree = dbIo.readModeChoiceTree(mctDataSource, modes.getModes());

        DataSource ektDataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_EKT),
                List.of(new Filter("name", parameters.getString(ParamString.DB_NAME_EKT))));
        TPS_ExpertKnowledgeTree expertKnowledgeTree = dbIo.readExpertKnowledgeTree(ektDataSource,modes.getModes());

        //todo init utility function
        TPS_UtilityFunction utilityFunction = null;
        ModeDistributionCalculator modeDistributionCalculator = new ModeDistributionCalculator(modes, utilityFunction);
        dataStoreBuilder.modeSet(new TPS_ModeSet(modeChoiceTree,expertKnowledgeTree, parameters, modes, modeDistributionCalculator));

        //person groups
        DataSource personGroupsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_PERSON),
                List.of(new Filter("key", parameters.getString(ParamString.DB_PERSON_GROUP_KEY))));
        Collection<TPS_PersonGroup> personGroupCollection = dbIo.readPersonGroupCodes(personGroupsDs);
        PersonGroupsBuilder personGroupsWrapper = PersonGroups.builder();
        personGroupCollection.forEach(group -> personGroupsWrapper.personGroup(group.getCode(), group));
        PersonGroups personGroups = personGroupsWrapper.build();
        dataStoreBuilder.personGroups(personGroups);

        //settlement systems
        DataSource settlementSystemsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_SETTLEMENT), null);
        Collection<TPS_SettlementSystem> settlementSystems = dbIo.readSettlementSystemCodes(settlementSystemsDs);
        dataStoreBuilder.settlementSystems(settlementSystems);

        //activity to location mappings
        DataSource activityToLocationMappings = new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION),
                List.of(new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_2_LOCATION_KEY))));
        dataStoreBuilder.activityAndLocationCodeMapping(dbIo.readActivity2LocationCodes(activityToLocationMappings));

        //read utility function data
        DataSource utilityFunctionData = new DataSource(parameters.getString(ParamString.DB_NAME_MODEL_PARAMETERS),
                List.of(new Filter("key", parameters.getString(ParamString.UTILITY_FUNCTION_KEY)),
                        new Filter("utility_function_class", parameters.getString(ParamString.UTILITY_FUNCTION_NAME))));
        dataStoreBuilder.utilityFunctionData(dbIo.readUtilityFunction(utilityFunctionData));


        //read SchemeSet
        DataSource schemeClasses = new DataSource(parameters.getString(ParamString.DB_TABLE_SCHEME_CLASS),
                List.of(new Filter("key", parameters.getString(ParamString.DB_SCHEME_CLASS_KEY))));
        DataSource schemes = new DataSource(parameters.getString(ParamString.DB_TABLE_SCHEME),
                List.of(new Filter("key", this.parameters.getString(ParamString.DB_SCHEME_KEY))));
        DataSource episodes = new DataSource(parameters.getString(ParamString.DB_TABLE_EPISODE),
                List.of(new Filter("key", parameters.getString(ParamString.DB_EPISODE_KEY))));
        DataSource schemeClassDistributions = new DataSource(parameters.getString(ParamString.DB_TABLE_SCHEME_CLASS_DISTRIBUTION),
                List.of(new Filter("key", parameters.getString(ParamString.DB_SCHEME_CLASS_DISTRIBUTION_KEY))));
        var schemeSet = dbIo.readSchemeSet(schemeClasses,schemes,episodes,schemeClassDistributions, activityConstants, personGroups);
        dataStoreBuilder.schemeSet(schemeSet);

        //read households, persons and cars

        //read cars
        DataSource cars = new DataSource(parameters.getString(ParamString.DB_TABLE_CARS),
                List.of(new Filter("car_key", parameters.getString(ParamString.DB_CAR_FLEET_KEY))));
        //init FuelTypes
        FuelTypes fuelTypes = initFuelTypes();
        Cars carFleet = dbIo.loadCars(cars,fuelTypes);

        //read households
        DataSource households = new DataSource(parameters.getString(ParamString.DB_TABLE_HOUSEHOLD),
                List.of(new Filter("hh_key", parameters.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY))));
        Map<Integer, TPS_HouseholdBuilder> hhBuilders = dbIo.readHouseholds(households, carFleet);

        //load persons
        DataSource persons = new DataSource(parameters.getString(ParamString.DB_TABLE_PERSON),
                List.of(new Filter("p_key", parameters.getString(ParamString.DB_HOUSEHOLD_AND_PERSON_KEY))));

        Map<Integer, Collection<TPS_Person>> personsByHhId = dbIo.loadPersons(persons, new PersonDtoToPersonConverter(parameters, ageClasses));

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









        return new Tapas();
    }

    private void setUpCodes(){

        dbIo.readCarCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_CARS), null));
        dbIo.readDrivingLicenseCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION), null));
        dbIo.readSexCodes(new DataSource(parameters.getString(ParamString.DB_TABLE_CONSTANT_SEX), null));
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
