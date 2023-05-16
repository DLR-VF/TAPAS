/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.persistence.db;


import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.DataReader;
import de.dlr.ivf.api.io.DataReaderFactory;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.configuration.model.Filter;
import de.dlr.ivf.api.io.implementation.ResultSetConverter;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.legacy.TPS_Region;
import de.dlr.ivf.tapas.mode.Modes;
import de.dlr.ivf.tapas.model.mode.ModeParameters;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.TPS_ModeBuilder;
import de.dlr.ivf.tapas.model.*;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.constants.TPS_Distance.TPS_DistanceCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_AgeClass.TPS_AgeCodeType;
import de.dlr.ivf.tapas.model.constants.AgeClasses.AgeClassesBuilder;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.legacy.TPS_ExpertKnowledgeNode;
import de.dlr.ivf.tapas.legacy.TPS_ExpertKnowledgeTree;
import de.dlr.ivf.tapas.legacy.TPS_ModeChoiceTree;
import de.dlr.ivf.tapas.legacy.TPS_Node;
import de.dlr.ivf.tapas.model.location.*;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.TPS_ModeCodeType;
import de.dlr.ivf.tapas.mode.Modes.ModesBuilder;
import de.dlr.ivf.tapas.model.vehicle.CarFleetManager.CarFleetManagerBuilder;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_LocationConstant.TPS_LocationCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.model.parameter.*;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.*;
import de.dlr.ivf.tapas.model.vehicle.Cars.CarsBuilder;
import de.dlr.ivf.tapas.model.vehicle.FuelTypes.FuelTypesBuilder;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager.Behaviour;
import de.dlr.ivf.tapas.model.person.TPS_Household.TPS_HouseholdBuilder;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.Incomes.IncomesBuilder;

import de.dlr.ivf.tapas.util.IPInfo;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_DB_IO {

    public static final double INTERCHANGE_FACTOR = 0.01;
    private static final int fetchSizePerProcessor = 256;
    private static final int fetchSizePerProcessorPrefetchHouseholds = 256;
    /// The ip address of this machine
    private static InetAddress ADDRESS = null;
    private final Supplier<Connection> connectionSupplier;
    private final TPS_ParameterClass parameters;
    /// The number of households to fetch per cpu
    private int numberToFetch;
    Map<Integer, TPS_Household> houseHoldMap = new TreeMap<>();
    Map<Integer, TPS_Car> carMap = new HashMap<>();
    /// The reference to the persistence manager
    private TPS_DB_IOManager PM;
    /// A queue for prefetched households. Filled by getNextHouseholds .
    private final Queue<TPS_Household> prefetchedHouseholds = new LinkedList<>();
    /// The number of households fetched for this chunk. should be cpu*numberToFetch or the remainders in db for the
    // last fetch
    private int numberOfFetchedHouseholds;
    private int lastCleanUp = -1;

    /**
     * Constructor
     *
     * @param pm the managing class for sql-commands
     * @throws IOException if the machine has no IP address, an exception is thrown.
     */
    public TPS_DB_IO(TPS_DB_IOManager pm, Supplier<Connection> connectionSupplier) throws IOException {
        if (ADDRESS == null) {
            ADDRESS = IPInfo.getEthernetInetAddress();
        }

        this.connectionSupplier = connectionSupplier;

        this.PM = pm;
        this.parameters = pm.getParameters();
        // TODO: use the defined number of threads
        if (this.parameters.isTrue(ParamFlag.FLAG_PREFETCH_ALL_HOUSEHOLDS)) {
            numberToFetch = fetchSizePerProcessorPrefetchHouseholds * Runtime.getRuntime().availableProcessors();
        } else {
            numberToFetch = fetchSizePerProcessor * Runtime.getRuntime().availableProcessors();
        }
        numberOfFetchedHouseholds = -1;
    }
    
    public TPS_DB_IO(Supplier<Connection> connectionSupplier, TPS_ParameterClass parameterClass){
        
        this.connectionSupplier = connectionSupplier;
        this.parameters = parameterClass;
    }

    /**
     * Helper function to extract an sql-array to a Java double array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A double array
     * @throws SQLException
     */
    public static double[] extractDoubleArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof double[]) {
            return (double[]) array;
        } else if (array instanceof Double[]) {
            return ArrayUtils.toPrimitive((Double[]) array); // like casting Double[] to double[]
        } else {
            throw new SQLException("Cannot cast to int array");
        }
    }

    /**
     * Helper function to extract an sql-array to a Java int array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A int array
     * @throws SQLException
     */
    public static int[] extractIntArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof int[]) {
            return (int[]) array;
        } else if (array instanceof Integer[]) {
            return ArrayUtils.toPrimitive((Integer[]) array); // like casting Integer[] to int[]
        } else {
            throw new SQLException("Cannot cast to int array");
        }
    }

    public static int[] extractIntArray(Object array) throws SQLException {

        if (array instanceof int[]) {
            return (int[]) array;
        } else if (array instanceof Integer[]) {
            return ArrayUtils.toPrimitive((Integer[]) array); // like casting Integer[] to int[]
        } else {
            throw new SQLException("Cannot cast to int array");
        }
    }

    /**
     * Helper function to extract an sql-array to a Java String array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A String array
     * @throws SQLException
     */
    public static String[] extractStringArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof String[]) {
            return (String[]) array;
        } else {
            throw new SQLException("Cannot cast to string array");
        }
    }

    /**
     * Method to convert matrixelements to a sql-parsable array
     *
     * @param array         the array
     * @param decimalPlaces number of decimal places for the string
     * @return
     */
    public static String matrixToSQLArray(Matrix array, int decimalPlaces) {
        StringBuilder buffer;
        StringBuilder totalBuffer = new StringBuilder("ARRAY[");
        int size = array.getNumberOfColums();
        for (int j = 0; j < size; ++j) {
            buffer = new StringBuilder();
            for (int k = 0; k < size; ++k) {
                buffer.append(new BigDecimal(array.getValue(j + array.sIndex, k + array.sIndex))
                        .setScale(decimalPlaces, RoundingMode.HALF_UP)).append(",");
            }
            if (j < size - 1) totalBuffer.append(buffer);
            else totalBuffer.append(buffer.substring(0, buffer.length() - 1)).append("]");
        }

        return totalBuffer.toString();
    }

    public Map<Integer, Collection<TPS_Person>> loadPersons(DataSource dataSource, Converter<PersonDto, TPS_Person> converter){

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcLargeTableReader(connectionSupplier);

        Collection<PersonDto> personDtos = reader.read(new ResultSetConverter<>(PersonDto.class, PersonDto::new), dataSource);

        return converter.convertCollectionToMapWithSourceKey(personDtos, PersonDto::getHhId);
    }

    public Cars loadCars(DataSource dataSource, FuelTypes fuelTypes){

        CarsBuilder cars = Cars.builder();

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);
        Collection<CarDto> carDtos = reader.read(new ResultSetConverter<>(CarDto.class, CarDto::new),dataSource);

        for(CarDto carDto :carDtos){

            //build the car first
            TPS_Car car = TPS_Car.builder()
                    .id(carDto.getId())
                    .kbaNo(carDto.getKbaNo())
                    .fuelType(fuelTypes.getFuelType(FuelTypeName.getById(carDto.getEngineType())))
                    .emissionClass(EmissionClass.getById(carDto.getEmissionType()))
                    .restricted(carDto.isRestriction())
                    .fixCosts(carDto.getFixCosts())
                    .automationLevel(carDto.getAutomationLevel())
                    .build();

            //decorate car
            Vehicle decoratedCar = carDto.isCompanyCar() ? new CompanyCar(car) : new HouseholdCar(car);

            cars.car(decoratedCar.id(), decoratedCar);
        }

        return cars.build();
    }

    /**
     * Empty all global static constants maps
     */
    public void clearConstants() {

        TPS_Distance.clearDistanceMap();
        TPS_Income.clearIncomeMap();
        TPS_LocationConstant.clearLocationConstantMap();

        TPS_PersonGroup.clearPersonGroupMap();
        TPS_SettlementSystem.clearSettlementSystemMap();
    }

    /**
     * @return The number of households fetched in this round.
     */
    int getNumberOfFetchedHouseholds() {
        return numberOfFetchedHouseholds;
    }


    /**
     * Method to initializes some variables for starting the new simulation
     */
    public void initStart() {
        this.numberOfFetchedHouseholds = -1;
        this.houseHoldMap.clear(); //just in case...
    }

    public Map<Integer, TPS_HouseholdBuilder> readHouseholds(DataSource dataSource, Cars cars){

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<HouseholdDto> householdDtos = reader.read(new ResultSetConverter<>(HouseholdDto.class,HouseholdDto::new), dataSource);

        Map<Integer, TPS_HouseholdBuilder> householdBuilders = new HashMap<>(householdDtos.size());

        for(HouseholdDto householdDto : householdDtos) {
            TPS_HouseholdBuilder householdBuilder = TPS_Household.builder()
                    .id(householdDto.getHhId())
                    .type(householdDto.getHhType())
                    .income(householdDto.getHhIncome());

            //init the fleet manager
            CarFleetManagerBuilder fleetManager = CarFleetManager.builder();
            Arrays.stream(householdDto.getCarIds())
                    .boxed()
                    .map(cars::getCar)
                    .map(CarController::new)
                    .forEach(fleetManager::addCarController);

            householdBuilder.carFleetManager(fleetManager.build());

            householdBuilders.put(householdDto.getHhId(), householdBuilder);
        }
        return householdBuilders;
    }

    /**
     * Reads and assigns activities to locations and vice versa
     * <p>
     * Note: First the activity and locations must be read,
     * i.e. readActivityConstantCodes and readLocationConstantCodes must be called beforehand
     */
    public ActivityAndLocationCodeMapping readActivity2LocationCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<ActivityToLocationDto> activityToLocationDtos = reader.read(new ResultSetConverter<>(ActivityToLocationDto.class,ActivityToLocationDto::new), dataSource);

        ActivityAndLocationCodeMapping mapping = new ActivityAndLocationCodeMapping();

        for(ActivityToLocationDto activityToLocationDto : activityToLocationDtos){

            TPS_ActivityConstant actCode = TPS_ActivityConstant.getActivityCodeByTypeAndCode(TPS_ActivityCodeType.ZBE, activityToLocationDto.getActCode());
            TPS_LocationConstant locCode = TPS_LocationConstant.getLocationCodeByTypeAndCode(TPS_LocationCodeType.TAPAS, activityToLocationDto.getLocCode());

            mapping.addActivityToLocationMapping(actCode, locCode);
            mapping.addLocationToActivityMapping(locCode, actCode);

        }

        return mapping;
    }

    /**
     * Reads all activity constants codes from the database and stores them in to a global static map
     * An ActivityConstant has the form (id, 3-tuples of (name, code, type), istrip, isfix, attr)
     * Example: (5, ("school", "2", "TAPAS"), ("SCHOOL", "410", "MCT"), True, False, "SCHOOL")
     */
    public Collection<TPS_ActivityConstant> readActivityConstantCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<ActivityDto> activityDtos = reader.read(new ResultSetConverter<>(ActivityDto.class,ActivityDto::new), dataSource);

        Collection<TPS_ActivityConstant> activityConstants = new ArrayList<>();

        for(ActivityDto activityDto : activityDtos){

            String attribute = activityDto.getAttribute();

            TPS_ActivityConstant act = TPS_ActivityConstant.builder()
                    .id(activityDto.getId())
                    .internalConstant(new TPS_InternalConstant<>(activityDto.getNameMct(), activityDto.getCodeMct(),
                            TPS_ActivityCodeType.valueOf(activityDto.getTypeMct())))
                    .internalConstant(new TPS_InternalConstant<>(activityDto.getNameTapas(), activityDto.getCodeTapas(),
                            TPS_ActivityCodeType.valueOf(activityDto.getTypeTapas())))
                    .internalConstant(new TPS_InternalConstant<>(activityDto.getNamePriority(), activityDto.getCodePriority(),
                            TPS_ActivityCodeType.valueOf(activityDto.getTypePriority())))
                    .internalConstant(new TPS_InternalConstant<>(activityDto.getNameZbe(), activityDto.getCodeZbe(),
                            TPS_ActivityCodeType.valueOf(activityDto.getTypeZbe())))
                    .internalConstant(new TPS_InternalConstant<>(activityDto.getNameVot(), activityDto.getCodeVot(),
                            TPS_ActivityCodeType.valueOf(activityDto.getTypeVot())))
                    .attribute(!(attribute == null || attribute.equals("null"))
                            ? TPS_ActivityConstantAttribute.valueOf(activityDto.getAttribute()) : TPS_ActivityConstantAttribute.DEFAULT)
                    .isFix(activityDto.isFix())
                    .isTrip(activityDto.isTrip())
                    .build();

            activityConstants.add(act);
        }

        return activityConstants;
    }


    /**
     * Reads all age classes codes from the database and stores them in to a global static map
     * An AgeClass has the form (id, 3-tuples of (name, code, type), min, max)
     * Example: (5, (under 35, 7, STBA), (under 45, 4, PersGroup), 30, 34)
     */
    public AgeClasses readAgeClasses(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<AgeClassDto> ageClassDtos = reader.read(new ResultSetConverter<>(AgeClassDto.class,AgeClassDto::new), dataSource);

        AgeClassesBuilder ageClasses = AgeClasses.builder();

        for(AgeClassDto ageClassDto : ageClassDtos){
            TPS_AgeClass ageClass = TPS_AgeClass.builder()
                    .id(ageClassDto.getId())
                    .max(ageClassDto.getMax())
                    .min(ageClassDto.getMin())
                    .attribute(new TPS_InternalConstant<>(ageClassDto.getNameStba(),ageClassDto.getCodeStba(),
                            TPS_AgeCodeType.valueOf(ageClassDto.getTypeStba())))
                    .attribute(new TPS_InternalConstant<>(ageClassDto.getNamePersgroup(), ageClassDto.getCodePersgroup(),
                            TPS_AgeCodeType.valueOf(ageClassDto.getTypePersGroup())))
                    .build();
            ageClasses.ageClass(ageClassDto.getId(), ageClass);
        }

        return ageClasses.build();
    }


    /**
     * Reads all car codes from the database and stores them through enums
     * A CarCode has the form (name_cars, code_cars)
     * Example: (YES, 1)
     */
    public void readCarCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<CarCodeDto> carCodeDtos = reader.read(new ResultSetConverter<>(CarCodeDto.class, CarCodeDto::new), dataSource);

        for(CarCodeDto carCodeDto : carCodeDtos){
            try {
                TPS_CarCode s = TPS_CarCode.valueOf(carCodeDto.getNameCars());
                s.code = carCodeDto.getCodeCars();
            } catch (IllegalArgumentException e) {
                TPS_Logger.log(SeverityLogLevel.WARN,
                        "Read invalid car information from DB:" + carCodeDto.getNameCars());
            }

        }
    }

    /**
     * Method to read the constant values from the database. It provides the mapping of the enums to a value, which
     * is used in the survey or similar.
     */
    void readConstants(TPS_ParameterClass parameterClass) {

        //read all constants
        this.readActivityConstantCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY),null));
        this.readAgeClasses(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_AGE), null));
        this.readCarCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_CARS), null));
        this.readDistanceCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_DISTANCE), null));
        this.readDrivingLicenseCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION), null));
        this.readIncomeCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_INCOME), null));
        this.readLocationConstantCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_LOCATION), null));
        this.readModes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_MODE), null));
        this.readPersonGroupCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_PERSON),
                List.of(new Filter("key", parameterClass.getString(ParamString.DB_PERSON_GROUP_KEY)))));
        this.readSettlementSystemCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_SETTLEMENT), null));
        this.readSexCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_SEX), null));

        //must be after reading of activities and locations because they are used in it
        this.readActivity2LocationCodes(new DataSource(parameterClass.getString(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION),
                List.of(new Filter("key",parameterClass.getString(ParamString.DB_ACTIVITY_2_LOCATION_KEY)))));
    }

    /**
     * Reads all distance codes from the database and stores them in to a global static map
     * A Distance has the form (id, 3-tuples of (name, code, type), max)
     * Example: (5, (under 5k, 1, VOT),	(under 2k, 2000, MCT), 2000)
     */
    public Collection<TPS_Distance> readDistanceCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<DistanceCodeDto> distanceCodeDtos = reader.read(new ResultSetConverter<>(DistanceCodeDto.class,DistanceCodeDto::new), dataSource);

        Collection<TPS_Distance> distanceCodes = new ArrayList<>();

        for(DistanceCodeDto distanceCodeDto : distanceCodeDtos){

            TPS_Distance distanceCode = TPS_Distance.builder()
                    .id(distanceCodeDto.getId())
                    .max(distanceCodeDto.getMax())
                    .internalDistanceCode(new TPS_InternalConstant<>(distanceCodeDto.getNameVot(), distanceCodeDto.getCodeVot(),
                            TPS_DistanceCodeType.valueOf(distanceCodeDto.getTypeVot())))
                    .internalDistanceCode(new TPS_InternalConstant<>(distanceCodeDto.getNameMct(), distanceCodeDto.getCodeMct(),
                            TPS_DistanceCodeType.valueOf(distanceCodeDto.getTypeMct())))
                    .build();

            distanceCodes.add(distanceCode);
        }

        return distanceCodes;
    }

    /**
     * Reads all driving license codes from the database and stores them through enums
     * A DrivingLicenseCodes has the form (name_dli, code_dli)
     * Example: (no, 2)
     */
    public void readDrivingLicenseCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<DrivingLicenseInformationDto> drivingLicenseInformationDtos = reader.read(new ResultSetConverter<>(DrivingLicenseInformationDto.class,
                DrivingLicenseInformationDto::new), dataSource);

        for(DrivingLicenseInformationDto drivingLicenseInformationDto : drivingLicenseInformationDtos) {

            try {
                TPS_DrivingLicenseInformation s = TPS_DrivingLicenseInformation.valueOf(drivingLicenseInformationDto.getName());
                s.setCode(drivingLicenseInformationDto.getDrivingLicenseInfoCode());
            } catch (IllegalArgumentException e) {
                TPS_Logger.log(SeverityLogLevel.WARN, "Invalid driving license code: " + drivingLicenseInformationDto.getDrivingLicenseInfoName());
            }

        }
    }

    /**
     * This method reads the mode choice tree used for the pivot-point model.
     *
     * @return The tree read from the db.
     * @throws SQLException
     */
    public TPS_ExpertKnowledgeTree readExpertKnowledgeTree(DataSource dataSource, Collection<TPS_Mode> modes) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<ExpertKnowledgeTreeDto> expertKnowledgeTreeDtos = reader.read(new ResultSetConverter<>(ExpertKnowledgeTreeDto.class,
                ExpertKnowledgeTreeDto::new), dataSource);


        Collection<ExpertKnowledgeTreeDto> sortedNodes = expertKnowledgeTreeDtos.stream()
                .sorted(Comparator.comparingInt(ExpertKnowledgeTreeDto::getNodeId))
                .collect(Collectors.toCollection(ArrayList::new));

        TPS_ExpertKnowledgeNode root = null;

        for(ExpertKnowledgeTreeDto ekNode : sortedNodes) {

            List<Integer> c = Arrays.stream(ekNode.getAttributeValues()).boxed().collect(Collectors.toCollection(LinkedList::new));

            String splitVar = ekNode.getSplitVariable();
            TPS_Attribute sv = (splitVar != null && splitVar.length() > 1) ? TPS_Attribute.valueOf(splitVar) : null;

            double[] summandValues = ekNode.getSummand();
            TPS_DiscreteDistribution<TPS_Mode> summand = new TPS_DiscreteDistribution<>(modes);
            for (int i = 0; i < summandValues.length; i++) {
                summand.setValueByPosition(i, summandValues[i]);
            }

            double[] factorValues = ekNode.getFactor();
            TPS_DiscreteDistribution<TPS_Mode> factor = new TPS_DiscreteDistribution<>(modes);
            for (int i = 0; i < factorValues.length; i++) {
                factor.setValueByPosition(i, factorValues[i]);
            }

            if (root == null) {
                root = new TPS_ExpertKnowledgeNode(ekNode.getNodeId(), sv, c, summand, factor, null);
            } else {
                TPS_ExpertKnowledgeNode parent = (TPS_ExpertKnowledgeNode) root.getChild(ekNode.getParentNodeId());
                TPS_ExpertKnowledgeNode child = new TPS_ExpertKnowledgeNode(ekNode.getNodeId(), sv, c, summand, factor, parent);

                // if (parent.getId() != idParent) {
                // log.error("\t\t\t\t '--> ModeChoiceTree.readTable: Parent not found -> Id: " + idParent);
                // throw new IOException("ModeChoiceTree.readTable: Parent not found -> Id: " + idParent);
                // }

                parent.addChild(child);
            }
        }

        return new TPS_ExpertKnowledgeTree(root);
    }

    public TPS_ExpertKnowledgeTree newDummyExpertKnowledgeTree(Collection<TPS_Mode> modes){
        //no expert knowledge: create a root-node with dummy values
        List<Integer> c = new LinkedList<>();
        c.add(0);

        TPS_DiscreteDistribution<TPS_Mode> summand = new TPS_DiscreteDistribution<>(modes);
        for (int i = 0; i < summand.size(); i++) {
            summand.setValueByPosition(i, 0);
        }

        TPS_DiscreteDistribution<TPS_Mode> factor = new TPS_DiscreteDistribution<>(modes);
        for (int i = 0; i < factor.size(); i++) {
            factor.setValueByPosition(i, 1);
        }
        TPS_ExpertKnowledgeNode root = new TPS_ExpertKnowledgeNode(0, null, c, summand, factor, null);

        return new TPS_ExpertKnowledgeTree(root);
    }

    /**
     * Reads all income codes from the database and stores them in to a global static map
     * An Income has the form (id, name, code, max)
     * Example: (5, under 2600, 4, 2600)
     */
    public Incomes readIncomeCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<IncomeDto> incomeDtos = reader.read(new ResultSetConverter<>(IncomeDto.class,IncomeDto::new), dataSource);

        IncomesBuilder incomeMappings = Incomes.builder();

        for(IncomeDto incomeDto : incomeDtos){

            TPS_Income income = TPS_Income.builder()
                    .id(incomeDto.getId())
                    .name(incomeDto.getNameIncome())
                    .code(incomeDto.getCodeIncome())
                    .max(incomeDto.getMax())
                    .build();

            incomeMappings.incomeMapping(incomeDto.getId(), income);
            incomeMappings.maxValuesMapping(incomeDto.getMax(), incomeDto.getId());
        }

        return incomeMappings.build();
    }

    /**
     * Reads all location constant codes from the database and stores them in to a global static map
     * A LocationConstant has the form (id, 3-tuples of (name, code, type))
     * Example: (5, (club, 7, GENERAL), (club, 7, TAPAS))
     */
    public Collection<TPS_LocationConstant> readLocationConstantCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<LocationCodeDto> locationCodeDtos = reader.read(new ResultSetConverter<>(LocationCodeDto.class,LocationCodeDto::new), dataSource);

        Collection<TPS_LocationConstant> locationConstants = new ArrayList<>();

        for(LocationCodeDto locationCodeDto : locationCodeDtos) {

            TPS_LocationConstant locationConstant = TPS_LocationConstant.builder()
                    .id(locationCodeDto.getId())
                    .internalConstant(new TPS_InternalConstant<>(locationCodeDto.getNameGeneral(),
                            locationCodeDto.getCodeGeneral(), TPS_LocationCodeType.valueOf(locationCodeDto.getTypeGeneral())))
                    .internalConstant(new TPS_InternalConstant<>(locationCodeDto.getNameTapas(),
                            locationCodeDto.getCodeTapas(), TPS_LocationCodeType.valueOf(locationCodeDto.getTypeTapas())))
                    .build();

            locationConstants.add(locationConstant);
        }

        return locationConstants;
    }

    /**
     * Method to read all matrices for the given region (travel times, distances etc.)
     *
     * @param region The region to look for.
     * @throws SQLException
     */
    public void readMatrices(TPS_Region region) throws SQLException {
        final int sIndex = region.getSmallestId(); // this is the offset between the TAZ_ids and the matrix index

        String matrixUri = parameters.getString(ParamString.DB_TABLE_MATRICES);
        String matrixMapUri = parameters.getString(ParamString.DB_TABLE_MATRIXMAPS);

        DataSource streetDist = new DataSource(matrixUri,
                List.of(new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_STREET))));
        this.readMatrix(streetDist, ParamMatrix.DISTANCES_STREET, null, sIndex);

        //walk net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_WALK)) {
            DataSource walkDistances = new DataSource(matrixUri, List.of(new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_WALK))));
            this.readMatrix(walkDistances, ParamMatrix.DISTANCES_WALK, null, sIndex);
        } else {
            TPS_Logger.log(SeverityLogLevel.INFO, "Setting walk distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_WALK,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //bike net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE)) {
            DataSource bikeDistances = new DataSource(matrixUri,List.of(new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE))));
            this.readMatrix(bikeDistances, ParamMatrix.DISTANCES_BIKE, null, sIndex);
        } else {
            TPS_Logger.log(SeverityLogLevel.INFO, "Setting bike distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_BIKE,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //pt net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_PT)) {
            DataSource ptDistances = new DataSource(matrixUri,List.of(new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_PT))));
            this.readMatrix(ptDistances, ParamMatrix.DISTANCES_PT, null, sIndex);
        } else {
            TPS_Logger.log(SeverityLogLevel.INFO, "Setting public transport distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_PT,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //beeline dist
        TPS_Logger.log(SeverityLogLevel.INFO, "Calculate beeline distances distances.");
        Matrix bl = new Matrix(region.getTrafficAnalysisZones().size(), region.getTrafficAnalysisZones().size(),
                sIndex);
        for (TPS_TrafficAnalysisZone tazfrom : region.getTrafficAnalysisZones()) {
            for (TPS_TrafficAnalysisZone tazto : region.getTrafficAnalysisZones()) {
                double dist = TPS_Geometrics.getDistance(tazfrom.getTrafficAnalysisZone().getCenter(),
                        tazto.getTrafficAnalysisZone().getCenter(),
                        this.parameters.getDoubleValue(ParamValue.MIN_DIST));
                bl.setValue(tazfrom.getTAZId(), tazto.getTAZId(), dist);
            }
        }
        this.parameters.setMatrix(ParamMatrix.DISTANCES_BL, bl);
        TPS_Logger.log(SeverityLogLevel.INFO, "Beeline average value: " +
                this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getAverageValue(false, true) +
                " Size (Elements, Rows, Columns): " + this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getNumberOfElements() + ", "
                + this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getNumberOfRows() + ", "
                + this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getNumberOfColums());

        //walk
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_WALK)) {
            DataSource walkTt = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_WALK))));
            this.readMatrixMap(walkTt, ParamMatrixMap.TRAVEL_TIME_WALK,
                    SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK)) {
                DataSource walkAccess = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_WALK))));
                this.readMatrixMap(walkAccess, ParamMatrixMap.ARRIVAL_WALK, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK)) {
                DataSource walkEgress = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_WALK))));
                this.readMatrixMap(walkEgress, ParamMatrixMap.EGRESS_WALK, SimulationType.SCENARIO, sIndex);
            }
        }

        //bike
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE)) {
            DataSource bikeTt = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_BIKE))));
            this.readMatrixMap(bikeTt, ParamMatrixMap.TRAVEL_TIME_BIKE, SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE)) {
                DataSource bikeAccess = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_BIKE))));
                this.readMatrixMap(bikeAccess, ParamMatrixMap.ARRIVAL_BIKE, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE)) {
                DataSource bikeEgress = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_BIKE))));
                this.readMatrixMap(bikeEgress, ParamMatrixMap.EGRESS_BIKE, SimulationType.SCENARIO, sIndex);
            }
        }

        //MIT, MIT passenger, Taxi
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT)) {
            DataSource mitTt = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_MIT))));
            this.readMatrixMap(mitTt, ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT)) {
                DataSource mitAccess = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_MIT))));
                this.readMatrixMap(mitAccess, ParamMatrixMap.ARRIVAL_MIT, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT)) {
                DataSource mitEgress = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_MIT))));
                this.readMatrixMap(mitEgress, ParamMatrixMap.EGRESS_MIT, SimulationType.SCENARIO, sIndex);
            }
        }

        //pt, train
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_PT)) {
            DataSource ptTt = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_PT))));
            this.readMatrixMap(ptTt, ParamMatrixMap.TRAVEL_TIME_PT, SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT)) {
                DataSource ptAccess = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_PT))));
                this.readMatrixMap(ptAccess, ParamMatrixMap.ARRIVAL_PT, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT)) {
                DataSource ptEgress = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_PT))));
                this.readMatrixMap(ptEgress, ParamMatrixMap.EGRESS_PT, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT)) {
                DataSource ptInterchange = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT))));
                this.readMatrixMap(ptInterchange, ParamMatrixMap.INTERCHANGES_PT, SimulationType.SCENARIO, sIndex);
            }
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTBIKE_ACCESS_TAZ)) {
            DataSource ptBikeAccess = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTBIKE_ACCESS_TAZ))));
            this.readMatrixMap(ptBikeAccess, ParamMatrixMap.PTBIKE_ACCESS_TAZ, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTBIKE_EGRESS_TAZ)) {
            DataSource ptBikeEgress = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTBIKE_EGRESS_TAZ))));
            this.readMatrixMap(ptBikeEgress, ParamMatrixMap.PTBIKE_EGRESS_TAZ, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTCAR_ACCESS_TAZ)) {
            DataSource ptCarAccess = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTCAR_ACCESS_TAZ))));
            this.readMatrixMap(ptCarAccess, ParamMatrixMap.PTCAR_ACCESS_TAZ, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTBIKE_INTERCHANGES)) {
            DataSource ptBikeInterchange = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTBIKE_INTERCHANGES))));
            this.readMatrixMap(ptBikeInterchange, ParamMatrixMap.PTBIKE_INTERCHANGES, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTCAR_INTERCHANGES)) {
            DataSource ptCarInterchange = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTCAR_INTERCHANGES))));
            this.readMatrixMap(ptCarInterchange, ParamMatrixMap.PTCAR_INTERCHANGES, SimulationType.SCENARIO, sIndex);
        }

        // providing base case travel times in case they are needed
        if (this.parameters.isTrue(ParamFlag.FLAG_RUN_SZENARIO)) {
            // travel times for the base case
            //walk
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE)) {
                DataSource walkTtBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_WALK_BASE))));
                this.readMatrixMap(walkTtBase, ParamMatrixMap.TRAVEL_TIME_WALK, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE)) {
                    DataSource walkAccessBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE))));
                    this.readMatrixMap(walkAccessBase, ParamMatrixMap.ARRIVAL_WALK, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE)) {
                    DataSource walkEgressBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE))));
                    this.readMatrixMap(walkEgressBase, ParamMatrixMap.EGRESS_WALK, SimulationType.BASE, sIndex);
                }
            }

            //bike
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE)) {
                DataSource bikeTtBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE))));
                this.readMatrixMap(bikeTtBase, ParamMatrixMap.TRAVEL_TIME_BIKE, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE)) {
                    DataSource bikeAccessBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE))));
                    this.readMatrixMap(bikeAccessBase, ParamMatrixMap.ARRIVAL_BIKE, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE)) {
                    DataSource bikeEgressBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE))));
                    this.readMatrixMap(bikeEgressBase, ParamMatrixMap.EGRESS_BIKE, SimulationType.BASE, sIndex);
                }
            }

            //MIT, MIT passenger, Taxi,
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE)) {
                // car
                DataSource carTtBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_MIT_BASE))));
                this.readMatrixMap(carTtBase, ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE)) {
                    DataSource carAccessBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE))));
                    this.readMatrixMap(carAccessBase, ParamMatrixMap.ARRIVAL_MIT, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE)) {
                    DataSource carEgressBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE))));
                    this.readMatrixMap(carEgressBase, ParamMatrixMap.EGRESS_MIT, SimulationType.BASE, sIndex);
                }
            }

            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE)) {
                // public transport
                DataSource ptTtBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_PT_BASE))));
                this.readMatrixMap(ptTtBase, ParamMatrixMap.TRAVEL_TIME_PT, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE)) {
                    DataSource ptAccessBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE))));
                    this.readMatrixMap(ptAccessBase, ParamMatrixMap.ARRIVAL_PT, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE)) {
                    DataSource ptEgressBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE))));
                    this.readMatrixMap(ptEgressBase, ParamMatrixMap.EGRESS_PT, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE)) {
                    DataSource ptInterchangeBase = new DataSource(matrixMapUri, List.of(new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE))));
                    this.readMatrixMap(ptInterchangeBase, ParamMatrixMap.INTERCHANGES_PT, SimulationType.BASE, sIndex);
                }
            }
        }

        if (this.parameters.isDefined(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP)) {

            DataSource nextPtStopBlock = new DataSource(parameters.getString(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP),
                    List.of(new Filter("next_pt_stop_name", parameters.getString(ParamString.DB_NAME_BLOCK_NEXT_PT_STOP))));

            DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);
            Collection<BlockNextPtStopDto> nextPtStopDtos = reader.read(new ResultSetConverter<>(BlockNextPtStopDto.class, BlockNextPtStopDto::new), nextPtStopBlock);

            double avgNextPtStop = nextPtStopDtos.stream()
                    .mapToDouble(BlockNextPtStopDto::getNextPtStopDistance)
                    .average()
                    .orElseThrow(() -> new IllegalArgumentException("Couldn't select average distance pt stop from database"));

            this.parameters.setValue(ParamValue.AVERAGE_DISTANCE_PT_STOP, avgNextPtStop);
        }
    }

    /**
     * Method to read a single specified matrix
     *
     * @param matrixDs   datasource where the matrix is located
     * @param matrix     the matrix to store in
     * @param simType    the simulation type
     * @param sIndex     the index for reading in the db. Should be zero.
     * @throws SQLException
     */
    private void readMatrix(DataSource matrixDs, ParamMatrix matrix, SimulationType simType, int sIndex){

        TPS_Logger.log(SeverityLogLevel.INFO, "Loading " + matrix);
        Matrix m = readMatrix(matrixDs, sIndex);

        if (simType != null)
            this.parameters.setMatrix(matrix, m, simType);
        else
            this.parameters.setMatrix(matrix, m);

        TPS_Logger.log(SeverityLogLevel.INFO, "Loaded matrix from DB: " +matrix.name() + " Average value: " +
                this.parameters.getMatrix(matrix).getAverageValue(false, true) +
                " Size (Elements, Rows, Columns): " + this.parameters.getMatrix(matrix).getNumberOfElements() + ", "
                + this.parameters.getMatrix(matrix).getNumberOfRows() + ", "
                + this.parameters.getMatrix(matrix).getNumberOfColums());
    }

    public void readMatrixMap(DataSource matrixMapDs, ParamMatrixMap matrixMap, SimulationType simType, int sIndex){

        MatrixMap m = readMatrixMap(matrixMapDs, sIndex);

        if (simType != null) this.PM.getParameters().paramMatrixMapClass.setMatrixMap(matrixMap, m, simType);
        else this.PM.getParameters().paramMatrixMapClass.setMatrixMap(matrixMap, m);
    }


    /**
     * Method to read a single specified matrix map
     *
     * @param matrixMapDs the name in the db
     * @param sIndex     the matrixmap  to store in
     * @return the loaded MatrixMap
     */
    public MatrixMap readMatrixMap(DataSource matrixMapDs,  int sIndex) {

        //read the matrix map from db
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);
        Collection<MatrixMapDto> matrixMapDtos = reader.read(new ResultSetConverter<>(MatrixMapDto.class,MatrixMapDto::new),matrixMapDs);

        //some plausibility checks
        if(matrixMapDtos.size() > 1)
            throw new IllegalArgumentException("the provided matrix datasource: '"+matrixMapDs.getUri()+"' does not return a single matrix result.");

        MatrixMapDto matrixMap = matrixMapDtos.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("the provided matrix datasource: '"+matrixMapDs.getUri()+"' did not yield a result."));

        //check sizes
        int numOfMatrices = matrixMap.getMatrixMapNum();
        String[] matrixNames = matrixMap.getMatrixNames();
        double[] matrixDistributions = matrixMap.getMatrixMapDistribution();
        if (numOfMatrices != matrixNames.length || numOfMatrices != matrixDistributions.length) {
            throw new IllegalArgumentException("Couldn't load matrixmap " + matrixMap.getMatrixMapName() +
                    " from database. Different array sizes (num, matrices, distribution): " + numOfMatrices +
                    " " + matrixNames.length + " " + matrixDistributions.length);
        }

        //init matrix map
        Matrix[] matrices = new Matrix[numOfMatrices];
        //load matrix map
        for (int i = 0; i < numOfMatrices; ++i) {

            matrices[i] = this.readMatrix(
                    new DataSource(parameters.getString(ParamString.DB_TABLE_MATRICES), List.of(new Filter("matrix_name",matrixNames[i]))),  sIndex);
            if(matrices[i] != null){
                TPS_Logger.log(SeverityLogLevel.INFO,
                        "Loaded matrix from DB: " + matrixNames[i] + " End time: " + matrixDistributions[i] +
                                " Average value: " + matrices[i].getAverageValue(false, true)+
                                " Size (Elements, Rows, Columns): " + matrices[i].getNumberOfElements() + ", "
                                + matrices[i].getNumberOfRows() + ", "
                                + matrices[i].getNumberOfColums());
            } else {
                throw new IllegalArgumentException(
                        "Couldn't load matrix " + matrixNames[i] + " form matrix map" + matrixMap.getMatrixMapName() +
                                ": No such matrix.");
            }
        }
        return new MatrixMap(matrixDistributions, matrices);
    }

    /**
     * Method to load a single matrix from the DB. The parameter ParamString.DB_TABLE_MATRICES must be defined.
     * @param sIndex the indexoffset of the matrix
     * @return the matrix or null, if nothing is found in the DB
     */
    public Matrix readMatrix(DataSource matrixDs, int sIndex) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);
        Collection<IntMatrixDto> matrixDtos = reader.read(new ResultSetConverter<>(IntMatrixDto.class,IntMatrixDto::new),matrixDs);

        if(matrixDtos.size() > 1)
            throw new IllegalArgumentException("the provided matrix datasource: '"+matrixDs.getUri()+"' does not return a single matrix result.");

        IntMatrixDto matrix = matrixDtos.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("the provided matrix datasource: '"+matrixDs.getUri()+"' did not yield a result."));

        int[] matrixValues = matrix.getMatrix();
        int len = (int) Math.sqrt(matrixValues.length);

        Matrix returnVal = new Matrix(len, len, sIndex);

        for (int index = 0; index < matrixValues.length; index++) {
            returnVal.setRawValue(index, matrixValues[index]);
        }

        return  returnVal;
    }
    /**
     * This method reads the mode choice tree used for the pivot-point model.
     *
     * @return The tree read from the db.
     * @throws SQLException
     */
    public TPS_ModeChoiceTree readModeChoiceTree(DataSource dataSource, Collection<TPS_Mode> modes) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<ModeChoiceTreeNodeDto> mctDtos = reader.read(new ResultSetConverter<>(ModeChoiceTreeNodeDto.class, ModeChoiceTreeNodeDto::new), dataSource);

        Collection<ModeChoiceTreeNodeDto> sortedNodes = mctDtos.stream()
                .sorted(Comparator.comparingInt(ModeChoiceTreeNodeDto::getNodeId))
                .collect(Collectors.toCollection(ArrayList::new));

        TPS_Node root = null;
        for(ModeChoiceTreeNodeDto mctDto : sortedNodes) {
            String splitVariable = mctDto.getSplitVariable();
            TPS_Attribute sv = splitVariable != null && splitVariable.length() > 1 ? TPS_Attribute.valueOf(splitVariable) : null;

            double[] distribution = mctDto.getDistribution();
            TPS_DiscreteDistribution<TPS_Mode> ipd = new TPS_DiscreteDistribution<>(modes);
            for (int i = 0; i < distribution.length; i++) {
                ipd.setValueByPosition(i, distribution[i]);
            }

            Collection<Integer> c = Arrays.stream(mctDto.getAttributeValues())
                    .boxed()
                    .collect(Collectors.toCollection(LinkedList::new));

            // We assume that the first row contains the root node data.
            if (root == null) {
                root = new TPS_Node(mctDto.getNodeId(), sv, c, ipd, null);
            } else {
                TPS_Node parent = root.getChild(mctDto.getParentNodeId());
                TPS_Node child = new TPS_Node(mctDto.getNodeId(), sv, c, ipd, parent);
                parent.addChild(child);
            }
        }

        return new TPS_ModeChoiceTree(root);
    }

    /**
     * Reads all mode constant codes from the database and stores them in to a global static map
     * A Mode has the form (id, 3-tuples of (name, code, type), isfix)
     * Example: (3, (MIT, 2, MCT), (MIT, 1, VOT), true)
     */
    public Modes readModes(DataSource modesTable) {

        DataReader<ResultSet> dr = DataReaderFactory.newJdbcReader(connectionSupplier);

        EnumMap<ModeType, ModeParameters> modeParams = getModeParameters();


        Collection<ModeDto> modeDtos = dr.read(new ResultSetConverter<>(ModeDto.class, ModeDto::new),modesTable);


        ModesBuilder modesBuilder = Modes.builder();
        for(ModeDto modeDto : modeDtos){

            ModeType modeType = ModeType.valueOf(modeDto.getName());

            //initialize with data from database
            TPS_ModeBuilder modeBuilder = TPS_Mode.builder()
                    .name(modeDto.getName())
                    .isFix(modeDto.isFix())
                    .modeType(modeType)
                    .internalConstant(new TPS_InternalConstant<>(modeDto.getNameMct(), modeDto.getCodeMct(),
                            TPS_ModeCodeType.valueOf(modeDto.getTypeMct())))
                    .internalConstant(new TPS_InternalConstant<>(modeDto.getNameVot(), modeDto.getCodeVot(),
                            TPS_ModeCodeType.valueOf(modeDto.getTypeVot())));

            //initialize with data from parameterClass
            ModeParameters modeParameters = modeParams.get(modeType);

            TPS_Mode mode = modeBuilder.beelineFactor(modeParameters.getBeelineFactor())
                    .velocity(modeParameters.getVelocity())
                    .variableCostPerKm(modeParameters.getVariableCostPerKm())
                    .variableCostPerKmBase(modeParameters.getVariableCostPerKmBase())
                    .costPerKm(modeParameters.getCostPerKm())
                    .costPerKmBase(modeParameters.getCostPerKmBase())
                    .useBase(modeParameters.isUseBase())
                    .build();
            modesBuilder.addMode(modeType, mode);
        }
        return modesBuilder.build();
    }

    private EnumMap<ModeType, ModeParameters> getModeParameters(){
        EnumMap<ModeType, ModeParameters> modeParams = new EnumMap<>(ModeType.class);
        //pt params from config
        ModeParameters ptModeParameters =  ModeParameters.builder()
                .beelineFactor(parameters.getDoubleValue(ParamValue.BEELINE_FACTOR_PT))
                .costPerKm(parameters.getDoubleValue(ParamValue.PT_COST_PER_KM))
                .costPerKmBase(parameters.getDoubleValue(ParamValue.PT_COST_PER_KM_BASE))
                .velocity(parameters.getDoubleValue(ParamValue.VELOCITY_TRAIN))
                .useBase(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE))
                .variableCostPerKm(0)
                .variableCostPerKmBase(0)
                .build();
        modeParams.put(ModeType.PT, ptModeParameters);

        //mit params from config
        ModeParameters mitModeParameters =  ModeParameters.builder()
                .beelineFactor(parameters.getDoubleValue(ParamValue.BEELINE_FACTOR_MIT))
                .costPerKm(parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM))
                .costPerKmBase(parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM_BASE))
                .velocity(parameters.getDoubleValue(ParamValue.VELOCITY_CAR))
                .useBase(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE))
                .variableCostPerKm(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM))
                .variableCostPerKmBase(parameters.getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE))
                .build();
        modeParams.put(ModeType.MIT,mitModeParameters);

        //walk params from config
        ModeParameters walkModeParameters =  ModeParameters.builder()
                .beelineFactor(parameters.getDoubleValue(ParamValue.BEELINE_FACTOR_FOOT))
                .costPerKm(parameters.getDoubleValue(ParamValue.WALK_COST_PER_KM))
                .costPerKmBase(parameters.getDoubleValue(ParamValue.WALK_COST_PER_KM_BASE))
                .velocity(parameters.getDoubleValue(ParamValue.VELOCITY_FOOT))
                .useBase(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE))
                .variableCostPerKm(0)
                .variableCostPerKmBase(0)
                .build();
        modeParams.put(ModeType.WALK, walkModeParameters);

        //taxi params from config
        ModeParameters taxiModeParameters =  ModeParameters.builder()
                .beelineFactor(parameters.getDoubleValue(ParamValue.BEELINE_FACTOR_MIT))
                .costPerKm(parameters.getDoubleValue(ParamValue.TAXI_COST_PER_KM))
                .costPerKmBase(parameters.getDoubleValue(ParamValue.TAXI_COST_PER_KM_BASE))
                .velocity(parameters.getDoubleValue(ParamValue.VELOCITY_CAR))
                .useBase(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE))
                .variableCostPerKm(0)
                .variableCostPerKmBase(0)
                .build();
        modeParams.put(ModeType.TAXI, taxiModeParameters);

        //car sharing params from config
        ModeParameters csModeParameters =  ModeParameters.builder()
                .beelineFactor(parameters.getDoubleValue(ParamValue.BEELINE_FACTOR_MIT))
                .costPerKm(parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM))
                .costPerKmBase(parameters.getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM_BASE))
                .velocity(parameters.getDoubleValue(ParamValue.VELOCITY_CAR))
                .useBase(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE))
                .variableCostPerKm(0)
                .variableCostPerKmBase(0)
                .build();
        modeParams.put(ModeType.CAR_SHARING, csModeParameters);

        //mit passenger params from config
        ModeParameters passModeParameters =  ModeParameters.builder()
                .beelineFactor(parameters.getDoubleValue(ParamValue.BEELINE_FACTOR_MIT))
                .costPerKm(parameters.getDoubleValue(ParamValue.PASS_COST_PER_KM))
                .costPerKmBase(parameters.getDoubleValue(ParamValue.PASS_COST_PER_KM_BASE))
                .velocity(parameters.getDoubleValue(ParamValue.VELOCITY_CAR))
                .useBase(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE))
                .variableCostPerKm(0)
                .variableCostPerKmBase(0)
                .build();
        modeParams.put(ModeType.MIT_PASS, passModeParameters);

        return modeParams;
    }

    private void setParameterizableModeData(TPS_ModeBuilder builder, ModeParameters modeParameters){
        builder.beelineFactor(modeParameters.getBeelineFactor())
                .velocity(modeParameters.getVelocity())
                .variableCostPerKm(modeParameters.getVariableCostPerKm())
                .variableCostPerKmBase(modeParameters.getVariableCostPerKmBase())
                .costPerKm(modeParameters.getCostPerKm())
                .costPerKmBase(modeParameters.getCostPerKmBase())
                .useBase(modeParameters.isUseBase());
    }

    /**
     * Reads all person group codes from the database and stores them in to a global static map
     * A PersGroup has the form (id, 3-tuples of (name, code, type), code_ageclass, code_sex, code_cars, persType)
     * Example: (3, (RoP65-74, 12, VISEVA_R), 6, ,1, 2, RETIREE)
     */
    public Collection<TPS_PersonGroup> readPersonGroupCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<PersonCodeDto> personCodeDtos = reader.read(new ResultSetConverter<>(PersonCodeDto.class,PersonCodeDto::new), dataSource);

        Collection<TPS_PersonGroup> personGroups = new ArrayList<>();

        for(PersonCodeDto personCodeDto : personCodeDtos) {
            TPS_PersonGroup personGroup = TPS_PersonGroup.builder()
                    .description(personCodeDto.getDescription())
                    .code(personCodeDto.getCode())
                    .personType(TPS_PersonType.valueOf(personCodeDto.getPersonType()))
                    .carCode(TPS_CarCode.getEnum(personCodeDto.getCodeCars()))
                    .hasChildCode(TPS_HasChildCode.valueOf(personCodeDto.getHasChild()))
                    .minAge(personCodeDto.getMinAge())
                    .maxAge(personCodeDto.getMaxAge())
                    .workStatus(TPS_WorkStatus.valueOf(personCodeDto.getWorkStatus()))
                    .sex(TPS_Sex.getEnum(personCodeDto.getCodeSex()))
                    .build();

            personGroups.add(personGroup);

        }

        return personGroups;
    }

    public TPS_VariableMap readValuesOfTimes(DataSource dataSource){

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<ValueOfTimeDto> votDtos = reader.read(new ResultSetConverter<>(ValueOfTimeDto.class,ValueOfTimeDto::new), dataSource);

        TPS_VariableMap votTree = new TPS_VariableMap(List.of(TPS_Attribute.HOUSEHOLD_INCOME_CLASS_CODE, TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_VOT,
                TPS_Attribute.CURRENT_MODE_CODE_VOT, TPS_Attribute.CURRENT_DISTANCE_CLASS_CODE_VOT));

        for(ValueOfTimeDto votDto : votDtos) {
            votTree.addValue(List.of(votDto.getHhIncomeClassCode(), votDto.getCurrentEpisodeActivityCodeVot(),
                    votDto.getCurrentModeCodeVot(), votDto.getCurrentDistanceClassCodeVot()), votDto.getValue());
        }

        return votTree;
    }

    /**
     * Reads all region specific parameters from the DB
     *
     * @return The region, specified in the Parameter set
     * @throws SQLException Error during SQL-processing
     */
    //todo fix reaading region
    public TPS_Region readRegion() throws SQLException {
        TPS_Region region = new TPS_Region(null, null);
        TPS_Block blk;
        String query;

        // read values of time
        DataSource votDs = new DataSource(parameters.getString(ParamString.DB_TABLE_VOT),
                List.of(new Filter("name", parameters.getString(ParamString.DB_NAME_VOT))));
        region.setValuesOfTime(this.readValuesOfTimes(votDs));


        // read cfn values
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        //read cfnx
        DataSource cfnxDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CFNX),
                List.of(new Filter("key", parameters.getString(ParamString.DB_REGION_CNF_KEY))));

        Collection<CfnxDto> cfnxDtos = reader.read(new ResultSetConverter<>(CfnxDto.class, CfnxDto::new),cfnxDs);

        TPS_CFN cfn = new TPS_CFN(TPS_SettlementSystemType.TAPAS, TPS_ActivityCodeType.TAPAS);

        cfnxDtos.forEach(cfnx -> cfn.addToCFNXMap(cfnx.getCurrentTazSettlementCodeTapas(),cfnx.getValue()));

        //read cfn4
        DataSource cfn4Ds = new DataSource(parameters.getString(ParamString.DB_TABLE_CFN4),
                List.of(new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_CNF_KEY))));
        Collection<CfnFourDto> cfn4Dtos = reader.read(new ResultSetConverter<>(CfnFourDto.class, CfnFourDto::new), cfn4Ds);
        cfn4Dtos.forEach(cfn4 -> cfn.addToCFN4Map(cfn4.getCurrentTazSettlementCodeTapas(),cfn4.getCurrentEpisodeActivityCodeTapas(),cfn4.getValue()));

        region.setCfn(cfn);

        //optional potential parameter, might return no result!
        TPS_CFN potential = new TPS_CFN(TPS_SettlementSystemType.TAPAS, TPS_ActivityCodeType.TAPAS);
        DataSource cfnPotentialDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CFN4),
                List.of(new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_POTENTIAL_KEY))));
        Collection<CfnFourDto> cfn4PotentialDtos = reader.read(new ResultSetConverter<>(CfnFourDto.class, CfnFourDto::new), cfnPotentialDs);
        cfn4PotentialDtos.forEach(cfn4 -> potential.addToCFN4Map(cfn4.getCurrentTazSettlementCodeTapas(),cfn4.getCurrentEpisodeActivityCodeTapas(),cfn4.getValue()));

        region.setPotential(potential);

        //read traffic analysis zone
        DataSource tazDs = new DataSource(parameters.getString(ParamString.DB_TABLE_TAZ),null);
        Collection<TrafficAnalysisZoneDto> tazDtos = reader.read(new ResultSetConverter<>(TrafficAnalysisZoneDto.class, TrafficAnalysisZoneDto::new), null);



        // read traffic analysis zones
        query = "SELECT taz_id, taz_bbr_type, taz_num_id, ST_X(taz_coordinate) as x, ST_Y(taz_coordinate) as y FROM " +
                this.parameters.getString(ParamString.DB_TABLE_TAZ);
        ResultSet rsTAZ = PM.executeQuery(query);
        while (rsTAZ.next()) {
            TPS_TrafficAnalysisZone taz = region.createTrafficAnalysisZone(rsTAZ.getInt("taz_id"));
            taz.setBbrType(TPS_SettlementSystem
                    .getSettlementSystem(TPS_SettlementSystemType.FORDCP, rsTAZ.getInt("taz_bbr_type")));
            taz.setCenter(rsTAZ.getDouble("x"), rsTAZ.getDouble("y"));

            if (rsTAZ.getInt("taz_num_id") != 0) {
                taz.setExternalId(rsTAZ.getInt("taz_num_id"));
            } else {
                taz.setExternalId(-1);
            }
        }

        rsTAZ.close();
        if (this.parameters.isDefined(ParamString.DB_TABLE_TAZ_SCORES)) {
            query = "SELECT * FROM " + this.parameters.getString(ParamString.DB_TABLE_TAZ_SCORES) +
                    " WHERE score_name='" + this.parameters.getString(ParamString.DB_NAME_TAZ_SCORES) + "'";
            rsTAZ = PM.executeQuery(query);

            while (rsTAZ.next()) {
                TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rsTAZ.getInt("score_taz_id"));
                taz.setScore(rsTAZ.getDouble("score"));
                taz.setScoreCat(rsTAZ.getInt("score_cat"));
            }
            rsTAZ.close();
        }

        // read taz infos
        // Check if the travel time matrices have values on the diagonal positions (intra zone times);
        // else read specific intra information files if necessary and provided
        ResultSet rsTAZInfo;
        if (this.parameters.isDefined(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS)) {
            query = "SELECT mi.info_taz_id, mi.beeline_factor_mit, " + "mi.average_speed_mit, pi.average_speed_pt, " +
                    "mi.has_intra_traffic_mit, pi.has_intra_traffic_pt, pi.pt_zone FROM " +
                    this.parameters.getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) + " mi, " +
                    this.parameters.getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS) + " pi " +
                    "WHERE mi.info_taz_id = pi.info_taz_id " + "AND mi.info_name = '" +
                    this.parameters.getString(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS) +
                    "' AND pi.info_name = '" + this.parameters.getString(
                    ParamString.DB_NAME_TAZ_INTRA_PT_INFOS) + "'";
            rsTAZInfo = PM.executeQuery(query);
            this.readTAZInfos(rsTAZInfo, region, SimulationType.SCENARIO);
            rsTAZInfo.close();
        }

        // travel times for the base case
        if (this.parameters.isDefined(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS_BASE) &&
                this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS_BASE) &&
                this.parameters.isTrue(ParamFlag.FLAG_RUN_SZENARIO)) {
            query = "SELECT mi.info_taz_id, mi.beeline_factor_mit, " + "mi.average_speed_mit, pi.average_speed_pt, " +
                    "mi.has_intra_traffic_mit, pi.has_intra_traffic_pt, pi.pt_zone FROM " +
                    this.parameters.getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) + " mi, " +
                    this.parameters.getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS) + " pi " +
                    "WHERE mi.info_taz_id = pi.info_taz_id " + "AND mi.info_name = '" +
                    this.parameters.getString(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS_BASE) +
                    "' AND pi.info_name = '" + this.parameters.getString(
                    ParamString.DB_NAME_TAZ_INTRA_PT_INFOS_BASE) + "'";
            rsTAZInfo = PM.executeQuery(query);
            this.readTAZInfos(rsTAZInfo, region, SimulationType.BASE);
            rsTAZInfo.close();
        }

        // read fees and tolls
        query = "SELECT * FROM " + this.parameters.getString(ParamString.DB_TABLE_TAZ_FEES_TOLLS) +
                " WHERE ft_name='" + this.parameters.getString(ParamString.DB_NAME_FEES_TOLLS) + "'";
        ResultSet rsFeesTolls = PM.executeQuery(query);
        while (rsFeesTolls.next()) {
            TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rsFeesTolls.getInt("ft_taz_id"));
            taz.initFeesTolls(rsFeesTolls.getBoolean("has_toll_base"), rsFeesTolls.getInt("toll_type_base"),
                    rsFeesTolls.getBoolean("has_fee_base"), rsFeesTolls.getInt("fee_type_base"),
                    rsFeesTolls.getBoolean("has_toll_scen"), rsFeesTolls.getInt("toll_type_scen"),
                    rsFeesTolls.getBoolean("has_fee_scen"), rsFeesTolls.getInt("fee_type_scen"),
                    rsFeesTolls.getBoolean("has_car_sharing_base"), rsFeesTolls.getBoolean("has_car_sharing"),
                    this.parameters);
            taz.setRestricted(rsFeesTolls.getBoolean("is_restricted"));
            taz.setPNR(rsFeesTolls.getBoolean("is_park_and_ride"));
        }
        rsFeesTolls.close();

        // read blocks
        if (this.parameters.isDefined(ParamString.DB_TABLE_BLOCK)) {
            query = "SELECT b.blk_id, b.blk_taz_id, ST_X(b.blk_coordinate) as x, ST_Y(b.blk_coordinate) as y, bs.score, bs.score_cat, bn.next_pt_stop FROM " +
                    this.parameters.getString(ParamString.DB_TABLE_BLOCK) + " b, " +
                    this.parameters.getString(ParamString.DB_TABLE_BLOCK_SCORES) + " bs, " +
                    this.parameters.getString(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP) +
                    " bn WHERE bs.score_name='" + this.parameters.getString(ParamString.DB_NAME_BLOCK_SCORES) +
                    "' AND bn.next_pt_stop_name='" + this.parameters.getString(
                    ParamString.DB_NAME_BLOCK_NEXT_PT_STOP) +
                    "' AND b.blk_id = bs.score_blk_id AND b.blk_id = bn.next_pt_stop_blk_id";
            ResultSet rsBLK = PM.executeQuery(query);
            while (rsBLK.next()) {
                TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(rsBLK.getInt("blk_taz_id"));
                blk = taz.getBlock(rsBLK.getInt("blk_id"));
                blk.setScore(rsBLK.getDouble("score"));
                blk.setScoreCat(rsBLK.getInt("score_cat"));
                blk.setNearestPubTransStop(rsBLK.getDouble("next_pt_stop"));
                blk.setCenter(rsBLK.getDouble("x"), rsBLK.getDouble("y"));
            }
            rsBLK.close();
        }

        // read locations
        query = "SELECT loc_id, loc_group_id, loc_code, loc_taz_id, loc_blk_id, loc_has_fix_capacity, loc_capacity, ST_X(loc_coordinate) as x,ST_Y(loc_coordinate) as y FROM " +
                this.parameters.getString(ParamString.DB_TABLE_LOCATION) +
                " WHERE loc_capacity >0 and key = '" + this.parameters.getString(ParamString.DB_LOCATION_KEY) +
                "'";  // do not add zero capacity locations
        ResultSet rsLoc = PM.executeQuery(query);

        double totalCapacity = 0;
        int numOfLocations = 0;
        while (rsLoc.next()) {
            int locId = rsLoc.getInt("loc_id");
            int groupId = -1;
            if (this.parameters.isTrue(ParamFlag.FLAG_USE_LOCATION_GROUPS)) groupId = rsLoc.getInt(
                    "loc_group_id");
            TPS_LocationConstant locCode = TPS_LocationConstant.getLocationCodeByTypeAndCode(TPS_LocationCodeType.TAPAS,
                    rsLoc.getInt("loc_code"));
            double x = rsLoc.getDouble("x");
            double y = rsLoc.getDouble("y");

            int taz_id = rsLoc.getInt("loc_taz_id");
            int block_id = rsLoc.getInt("loc_blk_id");
            if (rsLoc.wasNull()) {
                block_id = -1;
            }
            boolean fixedCap = rsLoc.getBoolean("loc_has_fix_capacity");
            int cap = rsLoc.getInt("loc_capacity");
            TPS_TrafficAnalysisZone taz = region.getTrafficAnalysisZone(taz_id);
            if (taz != null) { //locations in unknown tazes are discarded!
                TPS_Block block = block_id < 0 ? null : taz.getBlock(block_id);

                // build location
                TPS_Location location = new TPS_Location(locId, groupId, locCode, x, y, taz, block,
                        this.parameters);
                // now adapt the capacity to the sample size
                cap = (int) ((cap * this.parameters.getDoubleValue(ParamValue.DB_HH_SAMPLE_SIZE)) +
                        0.5); // including round
                if (cap == 0) cap = 1;// every non-zero capacity has at least one place to go!
                location.initCapacity(cap, fixedCap);
                totalCapacity += cap;
                numOfLocations++;

                // add location to the taz and the block
                taz.addLocation(location);
                region.addLocation(location);
                if (block != null) {
                    block.addLocation(location);
                }
            }
        }
        //now update the occupancy values from the temporary table
        this.updateOccupancyTable(region);
        if (TPS_Logger.isLogging(HierarchyLogLevel.CLIENT, SeverityLogLevel.INFO)) {
            TPS_Logger.log(HierarchyLogLevel.CLIENT, SeverityLogLevel.INFO,
                    "Total number of locations: " + numOfLocations + " capacity sum: " + totalCapacity);
        }
        rsLoc.close();
        return region;
    }

    /**
     * This method reads the scheme set, scheme class distribution values and all episodes from the db.
     *
     * @return The TPS_SchemeSet containing all episodes.
     * @throws SQLException
     */
    public TPS_SchemeSet readSchemeSet(DataSource schemeClasses, DataSource schemes, DataSource episodes, DataSource schemeClassDistributions) {

        int timeSlotLength = this.parameters.getIntValue(ParamValue.SEC_TIME_SLOT);

        TPS_SchemeSet schemeSet = new TPS_SchemeSet();

        // build scheme classes (with time distributions)
        DataReader<ResultSet> dataReader = DataReaderFactory.newJdbcReader(connectionSupplier);
        Collection<SchemeClassDto> schemeClassDtos = dataReader.read(new ResultSetConverter<>(SchemeClassDto.class, SchemeClassDto::new),schemeClasses);
        for(SchemeClassDto schemeClassDto : schemeClassDtos){
            TPS_SchemeClass schemeClass = schemeSet.getSchemeClass(schemeClassDto.getId());
            double mean = schemeClassDto.getAvgTravelTime() * 60;
            schemeClass.setTimeDistribution(mean, mean * schemeClassDto.getProcStdDev());
        }

        // build the schemes, assigning them to the right scheme classes
        Map<Integer, TPS_Scheme> schemeMap = new HashMap<>();
        Collection<SchemeDto> schemeDtos = dataReader.read(new ResultSetConverter<>(SchemeDto.class, SchemeDto::new), schemes);
        for(SchemeDto schemeDto : schemeDtos){
            TPS_SchemeClass schemeClass = schemeSet.getSchemeClass(schemeDto.getSchemeClassId());
            TPS_Scheme scheme = schemeClass.getScheme(schemeDto.getId(), this.parameters);
            schemeMap.put(scheme.getId(), scheme);
        }

        // read the episodes the schemes are made of and add them to the respective schemes
        // we read them into a temporary storage first
        int counter = 1;
        HashMap<Integer, List<TPS_Episode>> episodesMap = new HashMap<>();
        TPS_Episode lastEpisode = null;
        int lastScheme = -1;

        Collection<EpisodeDto> episodeDtos = dataReader.read(new ResultSetConverter<>(EpisodeDto.class, EpisodeDto::new), episodes);

        double scaleShift = this.parameters.getDoubleValue(ParamValue.SCALE_SHIFT);
        double scaleStretch = this.parameters.getDoubleValue(ParamValue.SCALE_STRETCH);

        for (EpisodeDto episodeDto : episodeDtos){

            TPS_ActivityConstant actCode = TPS_ActivityConstant.getActivityCodeByTypeAndCode(TPS_ActivityCodeType.ZBE,
                    episodeDto.getActCodeZbe());
            int actScheme = episodeDto.getSchemeId();
            if (lastScheme != actScheme) {
                lastScheme = actScheme;
                lastEpisode = null;
                episodesMap.put(actScheme, new ArrayList<>());
            }
            TPS_Episode episode = null;
            if (actCode.isTrip()) {
                episode = new TPS_Trip(counter++, actCode, episodeDto.getStart() * timeSlotLength,
                        episodeDto.getDuration() * timeSlotLength);
            } else {
                if (lastEpisode != null && lastEpisode.isStay()) {
                    // two subsequent stays: add their duration and adjust the activity code and the priority
                    TPS_Stay previousStay = (TPS_Stay) lastEpisode;
                    if (previousStay.getPriority() < actCode.getCode(TPS_ActivityCodeType.PRIORITY)) {
                        // the last stay has a different activity code and is "less important" than the current one
                        previousStay.setActCode(actCode); // adjust the activity code!
                    }
                    previousStay.setOriginalDuration(
                            previousStay.getOriginalDuration() + episodeDto.getDuration() * timeSlotLength);
                    previousStay.setOriginalStart(
                            Math.min(previousStay.getOriginalStart(), episodeDto.getStart() * timeSlotLength));
                } else {
                    episode = new TPS_Stay(counter++, actCode, episodeDto.getStart() * timeSlotLength,
                            episodeDto.getDuration() * timeSlotLength, 0, 0, 0, 0, scaleShift,scaleStretch);
                }
            }
            if (episode != null) {
                episode.isHomePart = episodeDto.isHome();
                episode.tourNumber = episodeDto.getTourNumber();
                episodesMap.get(actScheme).add(episode);
                lastEpisode = episode;
            }

        }
        // now we store them into the schemes
        for (Integer schemeID : episodesMap.keySet()) {
            TPS_Scheme scheme = schemeMap.get(schemeID);
            for (TPS_Episode e : episodesMap.get(schemeID)) {
                scheme.addEpisode(e);
            }
        }


        // read the mapping from person group to scheme classes
        HashMap<Integer, HashMap<Integer, Double>> personGroupSchemeProbabilityMap = new HashMap<>();

        Collection<SchemeClassDistributionDto> schemeClassDistributionDtos =
                dataReader.read(new ResultSetConverter<>(SchemeClassDistributionDto.class, SchemeClassDistributionDto::new), schemeClassDistributions);

        for(SchemeClassDistributionDto dto : schemeClassDistributionDtos){
            int personGroupId = dto.getPersonGroup();
            if (!personGroupSchemeProbabilityMap.containsKey(personGroupId)) {
                personGroupSchemeProbabilityMap.put(personGroupId, new HashMap<>());
            }
            personGroupSchemeProbabilityMap.get(personGroupId).put(dto.getSchemeClassId(), dto.getProbability());
        }

        for (Integer key : personGroupSchemeProbabilityMap.keySet()) { //add distributions to the schemeSet
            schemeSet.addDistribution(TPS_PersonGroup.getPersonGroupByCode(key),
                    new TPS_DiscreteDistribution<>(personGroupSchemeProbabilityMap.get(key)));
        }
        schemeSet.init();

        return schemeSet;
    }

    /**
     * Reads all settlement system codes from the database and stores them in to a global static map
     * A SettlementSystem has the form (id, 3-tuples of (name, code, type))
     * Example: (7, ("R1, K1, Kernstadt > 500000", "1", "FORDCP"))
     */
    public Collection<TPS_SettlementSystem> readSettlementSystemCodes(DataSource dataSource) {


        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<SettlementSystemDto> settlementSystemDtos = reader.read(new ResultSetConverter<>(SettlementSystemDto.class,SettlementSystemDto::new), dataSource);

        Collection<TPS_SettlementSystem> settlementSystems = new ArrayList<>();

        for(SettlementSystemDto settlementSystemDto : settlementSystemDtos){
            TPS_SettlementSystem settlementSystem = TPS_SettlementSystem.builder()
                    .id(settlementSystemDto.getId())
                    .internalConstant(new TPS_InternalConstant<>(settlementSystemDto.getNameFordcp(), settlementSystemDto.getCodeFordcp(),
                            TPS_SettlementSystemType.valueOf(settlementSystemDto.getTypeFordcp())))
                    .internalConstant(new TPS_InternalConstant<>(settlementSystemDto.getNameTapas(), settlementSystemDto.getCodeTapas(),
                            TPS_SettlementSystemType.valueOf(settlementSystemDto.getTypeTapas())))
                    .build();

            settlementSystems.add(settlementSystem);
        }
        return settlementSystems;
    }

    /**
     * Reads all sex codes from the database and stores them through enums
     * A SexCodes has the form (name_sex, code_sex)
     * Example: (FEMALE, 2)
     */
    public void readSexCodes(DataSource dataSource) {

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<SexDto> sexDtos = reader.read(new ResultSetConverter<>(SexDto.class, SexDto::new), dataSource);

        for(SexDto sexDto : sexDtos){
            try{
                TPS_Sex s = TPS_Sex.valueOf(sexDto.getNameSex());
                s.code = sexDto.getCodeSex();
            } catch (IllegalArgumentException e) {
                TPS_Logger.log(SeverityLogLevel.WARN,
                        "Read invalid sex type name from DB:" + sexDto.getNameSex());
            }

        }
    }

    /**
     * This method processes the ResultSet for the intra cell-infos like travel speed or pt zone
     *
     * @param set    The ResultSet from a appropriate sql-query. Must contain: info_taz_id, beeline_factor_mit, average_speed_mit, average_speed_pt, has_intra_traffic_mit, has_intra_traffic_pt, pt_zone
     * @param region The region, where the infos are stored in
     * @param type   theSimulationType (BASE/SCENARIO)
     * @throws SQLException
     */
    private void readTAZInfos(ResultSet set, TPS_Region region, SimulationType type) throws SQLException {
        TPS_TrafficAnalysisZone taz;
        ScenarioTypeValues stv;
        while (set.next()) {
            taz = region.getTrafficAnalysisZone(set.getInt("info_taz_id"));
            stv = taz.getSimulationTypeValues(type);
            stv.setBeelineFactorMIT(set.getDouble("beeline_factor_mit"));
            stv.setAverageSpeedMIT(set.getDouble("average_speed_mit"));
            stv.setAverageSpeedPT(set.getDouble("average_speed_pt"));
            stv.setIntraMITTrafficAllowed(set.getBoolean("has_intra_traffic_mit"));
            stv.setIntraPTTrafficAllowed(set.getBoolean("has_intra_traffic_pt"));
            stv.setPtZone(set.getInt("pt_zone"));
        }
    }

    /**
     * Method to read the parameters of the utility function
     *
     * @throws SQLException
     */

    //todo fix reading utility functions
    public Collection<UtilityFunctionDto> readUtilityFunction(DataSource dataSource){

        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connectionSupplier);

        Collection<UtilityFunctionDto> utilityFunctionDtos = reader.read(new ResultSetConverter<>(UtilityFunctionDto.class, UtilityFunctionDto::new), dataSource);

        return utilityFunctionDtos;
    }

    /**
     * Method to read variable maps, like value of time.
     *
     * @param set          The SQL- ResultSet
     * @param offset       an offset for the storing positin in one atribute element
     * @param comment      specifies the number of comments included in the ResultSet
     * @param defaultValue defines a default value in case of null-columns
     * @return The initialized TPS_VariableMap
     * @throws SQLException
     */
    private TPS_VariableMap readVariableMap(ResultSet set, int offset, int comment, double defaultValue) throws SQLException {
        Collection<TPS_Attribute> attributes = new LinkedList<>();
        int i;
        int cols = set.getMetaData().getColumnCount() - comment;
        for (i = offset; i < cols; i++) {
            try {
                attributes.add(TPS_Attribute.valueOf(set.getMetaData().getColumnName(i)));
            } catch (IllegalArgumentException e) {
                TPS_Logger.log(SeverityLogLevel.FATAL, "Unknown attribute: " + set.getMetaData().getColumnName(i), e);
            }
        }
        TPS_VariableMap varMap = new TPS_VariableMap(attributes, defaultValue);
        List<Integer> list = new ArrayList<>();
        while (set.next()) {
            list.clear();
            for (i = offset; i < cols; i++) {
                list.add(set.getInt(i));
            }
            varMap.addValue(list, set.getDouble(i));
        }
        return varMap;
    }

    /**
     * This method returns one household back to the database, e.g. if there where no members in this household found. Usually this indicates an error in the data set
     *
     * @param hh The household to return
     * @throws SQLException
     */
    private void returnHousehold(TPS_Household hh) {

        //removing the location from the location set automatically removes it from the taz, too!
        // TODO: yes, but why???
        //hh.getLocation().getLocSet().removeLocation(hh.getLocation());

        if (!TPS_DB_IOManager.BEHAVIOUR.equals(Behaviour.FAT)) {
            PM.functionExecute("finish_hh", this.parameters.getString(ParamString.DB_TABLE_HOUSEHOLD_TMP),
                    hh.getId());
        }
    }
}
