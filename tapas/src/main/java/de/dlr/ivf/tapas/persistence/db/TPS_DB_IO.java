/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.persistence.db;


import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.crud.read.DataReader;
import de.dlr.ivf.api.io.crud.read.DataReaderFactory;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
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
import de.dlr.ivf.tapas.model.location.TPS_Block.TPS_BlockBuilder;
import de.dlr.ivf.tapas.model.location.TPS_Location.TPS_LocationBuilder;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.TPS_ModeCodeType;
import de.dlr.ivf.tapas.mode.Modes.ModesBuilder;
import de.dlr.ivf.tapas.model.vehicle.CarFleetManager.CarFleetManagerBuilder;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone.TPS_TrafficAnalysisZoneBuilder;


import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityConstantAttribute;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_LocationConstant.TPS_LocationCodeType;
import de.dlr.ivf.tapas.model.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.model.parameter.*;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.*;
import de.dlr.ivf.tapas.model.vehicle.Cars.CarsBuilder;
import de.dlr.ivf.tapas.model.person.TPS_Household.TPS_HouseholdBuilder;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.Incomes.IncomesBuilder;
import de.dlr.ivf.tapas.model.location.ScenarioTypeValues.ScenarioTypeValuesBuilder;

import de.dlr.ivf.tapas.util.IPInfo;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;


@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_DB_IO {

    /// The ip address of this machine
    private InetAddress ADDRESS = null;
    private final ConnectionPool connectionSupplier;
    private final TPS_ParameterClass parameters;
    /// The reference to the persistence manager
    private TPS_DB_IOManager PM;


    /**
     * Constructor
     *
     * @param pm the managing class for sql-commands
     * @throws IOException if the machine has no IP address, an exception is thrown.
     */
    public TPS_DB_IO(TPS_DB_IOManager pm, ConnectionPool connectionSupplier) throws IOException {

        ADDRESS = IPInfo.getEthernetInetAddress();

        this.connectionSupplier = connectionSupplier;

        this.PM = pm;
        this.parameters = pm.getParameters();
    }
    
    public TPS_DB_IO(ConnectionPool connectionSupplier, TPS_ParameterClass parameterClass){
        
        this.connectionSupplier = connectionSupplier;
        this.parameters = parameterClass;
    }

    public Map<Integer, Collection<TPS_Person>> loadPersons(DataSource dataSource, Filter personFilter, Converter<PersonDto, TPS_Person> converter){

        Connection connection = connectionSupplier.borrowObject();
                
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcLargeTableReader(connection);

        Collection<PersonDto> personDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(PersonDto.class), PersonDto::new), 
                dataSource, personFilter);

        connectionSupplier.returnObject(connection);

        return converter.convertCollectionToMapWithSourceKey(personDtos, PersonDto::getHhId);
    }

    public Cars loadCars(DataSource dataSource, Filter carFilter, FuelTypes fuelTypes){

        CarsBuilder cars = Cars.builder();

        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
        Collection<CarDto> carDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(CarDto.class), CarDto::new),dataSource, carFilter);
        connectionSupplier.returnObject(connection);
        
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

    public Map<Integer, TPS_HouseholdBuilder> readHouseholds(DataSource dataSource, Filter hhFilter, Cars cars){

        Connection connection = connectionSupplier.borrowObject();
        
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<HouseholdDto> householdDtos =
                reader.read(
                        new ResultSetConverter<>(new ColumnToFieldMapping<>(HouseholdDto.class),HouseholdDto::new),
                        dataSource, hhFilter);
        
        connectionSupplier.returnObject(connection);

        Map<Integer, TPS_HouseholdBuilder> householdBuilders = new HashMap<>(householdDtos.size());

        for(HouseholdDto householdDto : householdDtos) {
            TPS_HouseholdBuilder householdBuilder = TPS_Household.builder()
                    .id(householdDto.getHhId())
                    .type(householdDto.getHhType())
                    .income(householdDto.getHhIncome());

            //init the fleet manager
            CarFleetManagerBuilder fleetManager = CarFleetManager.builder();
            if(householdDto.getCarIds() != null) {
                Arrays.stream(householdDto.getCarIds())
                        .boxed()
                        .map(cars::getCar)
                        .map(CarController::new)
                        .forEach(fleetManager::addCarController);
            }
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
    public ActivityAndLocationCodeMapping readActivity2LocationCodes(DataSource dataSource, Filter actToLocFilter) {

        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
        
        Collection<ActivityToLocationDto> activityToLocationDtos =
                reader.read(
                        new ResultSetConverter<>(new ColumnToFieldMapping<>(ActivityToLocationDto.class),ActivityToLocationDto::new),
                        dataSource, actToLocFilter);

        connectionSupplier.returnObject(connection);
        
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

        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
                
        Collection<ActivityDto> activityDtos =
                reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(ActivityDto.class),ActivityDto::new), dataSource);

        connectionSupplier.returnObject(connection);
        
        Collection<TPS_ActivityConstant> activityConstants = new ArrayList<>();

        for(ActivityDto activityDto : activityDtos){

            String attribute = activityDto.getAttribute();

            var mctConstant = new TPS_InternalConstant<>(activityDto.getNameMct(), activityDto.getCodeMct(),
                    TPS_ActivityCodeType.valueOf(activityDto.getTypeMct()));
            var tapasConstant = new TPS_InternalConstant<>(activityDto.getNameTapas(), activityDto.getCodeTapas(),
                    TPS_ActivityCodeType.valueOf(activityDto.getTypeTapas()));
            var priorityConstant = new TPS_InternalConstant<>(activityDto.getNamePriority(), activityDto.getCodePriority(),
                    TPS_ActivityCodeType.valueOf(activityDto.getTypePriority()));
            var zbeConstant = new TPS_InternalConstant<>(activityDto.getNameZbe(), activityDto.getCodeZbe(),
                    TPS_ActivityCodeType.valueOf(activityDto.getTypeZbe()));
            var votConstant = new TPS_InternalConstant<>(activityDto.getNameVot(), activityDto.getCodeVot(),
                    TPS_ActivityCodeType.valueOf(activityDto.getTypeVot()));

            TPS_ActivityConstant act = TPS_ActivityConstant.builder()
                    .id(activityDto.getId())
                    .internalConstant(mctConstant)
                    .internalAttribute(TPS_ActivityCodeType.MCT,mctConstant)
                    .internalConstant(tapasConstant)
                    .internalAttribute(TPS_ActivityCodeType.TAPAS,tapasConstant)
                    .internalConstant(priorityConstant)
                    .internalAttribute(TPS_ActivityCodeType.PRIORITY, priorityConstant)
                    .internalConstant(zbeConstant)
                    .internalAttribute(TPS_ActivityCodeType.ZBE, zbeConstant)
                    .internalConstant(votConstant)
                    .internalAttribute(TPS_ActivityCodeType.VOT, votConstant)
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

        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<AgeClassDto> ageClassDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(AgeClassDto.class),AgeClassDto::new), dataSource);
        connectionSupplier.returnObject(connection);
        
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
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
        
        Collection<CarCodeDto> carCodeDtos =
                reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(CarCodeDto.class), CarCodeDto::new), dataSource);
        connectionSupplier.returnObject(connection);
        
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
     * Reads all distance codes from the database and stores them in to a global static map
     * A Distance has the form (id, 3-tuples of (name, code, type), max)
     * Example: (5, (under 5k, 1, VOT),	(under 2k, 2000, MCT), 2000)
     */
    public Collection<TPS_Distance> readDistanceCodes(DataSource dataSource) {
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<DistanceCodeDto> distanceCodeDtos =
                reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(DistanceCodeDto.class),DistanceCodeDto::new), dataSource);
        connectionSupplier.returnObject(connection);
        
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
    //todo check if this is still needed since it is a simple enum now
    public void readDrivingLicenseCodes(DataSource dataSource) {
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<DrivingLicenseInformationDto> drivingLicenseInformationDtos =
                reader.read(
                        new ResultSetConverter<>(new ColumnToFieldMapping<>(DrivingLicenseInformationDto.class),
                        DrivingLicenseInformationDto::new), dataSource);
        connectionSupplier.returnObject(connection);
        
        for(DrivingLicenseInformationDto drivingLicenseInformationDto : drivingLicenseInformationDtos) {

            try {
                TPS_DrivingLicenseInformation s = TPS_DrivingLicenseInformation.valueOf(drivingLicenseInformationDto.getName());
                //s.setCode(drivingLicenseInformationDto.getDrivingLicenseInfoCode());
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
    public TPS_ExpertKnowledgeTree readExpertKnowledgeTree(DataSource dataSource, Filter ektFilter, Collection<TPS_Mode> modes) {
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<ExpertKnowledgeTreeDto> expertKnowledgeTreeDtos =
                reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(ExpertKnowledgeTreeDto.class),
                ExpertKnowledgeTreeDto::new), dataSource);
        connectionSupplier.returnObject(connection);

        Collection<ExpertKnowledgeTreeDto> sortedNodes = expertKnowledgeTreeDtos.stream()
                .sorted(Comparator.comparingInt(ExpertKnowledgeTreeDto::getNodeId))
                .collect(toCollection(ArrayList::new));

        TPS_ExpertKnowledgeNode root = null;

        for(ExpertKnowledgeTreeDto ekNode : sortedNodes) {

            List<Integer> c = Arrays.stream(ekNode.getAttributeValues()).boxed().collect(toCollection(LinkedList::new));

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
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
        
        Collection<IncomeDto> incomeDtos =
                reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(IncomeDto.class),IncomeDto::new), dataSource);

        connectionSupplier.returnObject(connection);
        
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
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<LocationCodeDto> locationCodeDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(LocationCodeDto.class),LocationCodeDto::new), dataSource);

        connectionSupplier.returnObject(connection);
        
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

        DataSource streetDist = new DataSource(matrixUri);
        Filter streetDistFilter =  new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_STREET));
        this.readMatrix(streetDist, streetDistFilter, ParamMatrix.DISTANCES_STREET, null, sIndex);

        //walk net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_WALK)) {
            Filter walkDistFilter = new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_WALK));
            DataSource walkDistances = new DataSource(matrixUri);
            this.readMatrix(walkDistances, walkDistFilter, ParamMatrix.DISTANCES_WALK, null, sIndex);
        } else {
            TPS_Logger.log(SeverityLogLevel.INFO, "Setting walk distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_WALK,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //bike net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE)) {
            DataSource bikeDistances = new DataSource(matrixUri);
            Filter bikeDistFilter = new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE));
            this.readMatrix(bikeDistances, bikeDistFilter, ParamMatrix.DISTANCES_BIKE, null, sIndex);
        } else {
            TPS_Logger.log(SeverityLogLevel.INFO, "Setting bike distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_BIKE,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //pt net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_PT)) {
            DataSource ptDistances = new DataSource(matrixUri);
            Filter ptDistFilter = new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_PT));
            this.readMatrix(ptDistances, ptDistFilter, ParamMatrix.DISTANCES_PT, null, sIndex);
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
            DataSource walkTt = new DataSource(matrixMapUri);
            Filter walkTtFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_WALK));
            this.readMatrixMap(walkTt, walkTtFilter, ParamMatrixMap.TRAVEL_TIME_WALK,
                    SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK)) {
                DataSource walkAccess = new DataSource(matrixMapUri);
                Filter walkAccessFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_WALK));
                this.readMatrixMap(walkAccess, walkAccessFilter, ParamMatrixMap.ARRIVAL_WALK, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK)) {
                DataSource walkEgress = new DataSource(matrixMapUri);
                Filter walkEgressFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_WALK));
                this.readMatrixMap(walkEgress, walkEgressFilter, ParamMatrixMap.EGRESS_WALK, SimulationType.SCENARIO, sIndex);
            }
        }

        //bike
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE)) {
            DataSource bikeTt = new DataSource(matrixMapUri);
            Filter bikeTtFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_BIKE));
            this.readMatrixMap(bikeTt,bikeTtFilter, ParamMatrixMap.TRAVEL_TIME_BIKE, SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE)) {
                DataSource bikeAccess = new DataSource(matrixMapUri);
                Filter bikeAccessFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_BIKE));
                this.readMatrixMap(bikeAccess, bikeAccessFilter, ParamMatrixMap.ARRIVAL_BIKE, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE)) {
                DataSource bikeEgress = new DataSource(matrixMapUri);
                Filter bikeEgressFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_BIKE));
                this.readMatrixMap(bikeEgress,bikeEgressFilter, ParamMatrixMap.EGRESS_BIKE, SimulationType.SCENARIO, sIndex);
            }
        }

        //MIT, MIT passenger, Taxi
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT)) {
            DataSource mitTt = new DataSource(matrixMapUri);
            Filter mitTtFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_MIT));
            this.readMatrixMap(mitTt, mitTtFilter, ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT)) {
                DataSource mitAccess = new DataSource(matrixMapUri);
                Filter mitAccessFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_MIT));
                this.readMatrixMap(mitAccess, mitAccessFilter, ParamMatrixMap.ARRIVAL_MIT, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT)) {
                DataSource mitEgress = new DataSource(matrixMapUri);
                Filter mitEgressFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_MIT));
                this.readMatrixMap(mitEgress, mitEgressFilter, ParamMatrixMap.EGRESS_MIT, SimulationType.SCENARIO, sIndex);
            }
        }

        //pt, train
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_PT)) {
            DataSource ptTt = new DataSource(matrixMapUri);
            Filter ptTtFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_PT));
            this.readMatrixMap(ptTt, ptTtFilter, ParamMatrixMap.TRAVEL_TIME_PT, SimulationType.SCENARIO, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT)) {
                DataSource ptAccess = new DataSource(matrixMapUri);
                Filter ptAccessFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_PT));
                this.readMatrixMap(ptAccess, ptAccessFilter, ParamMatrixMap.ARRIVAL_PT, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT)) {
                DataSource ptEgress = new DataSource(matrixMapUri);
                Filter ptEgressFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_PT));
                this.readMatrixMap(ptEgress, ptEgressFilter, ParamMatrixMap.EGRESS_PT, SimulationType.SCENARIO, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT)) {
                DataSource ptInterchange = new DataSource(matrixMapUri);
                Filter ptInterFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT));
                this.readMatrixMap(ptInterchange, ptInterFilter, ParamMatrixMap.INTERCHANGES_PT, SimulationType.SCENARIO, sIndex);
            }
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTBIKE_ACCESS_TAZ)) {
            DataSource ptBikeAccess = new DataSource(matrixMapUri);
            Filter ptBikeAccessFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTBIKE_ACCESS_TAZ));
            this.readMatrixMap(ptBikeAccess, ptBikeAccessFilter, ParamMatrixMap.PTBIKE_ACCESS_TAZ, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTBIKE_EGRESS_TAZ)) {
            DataSource ptBikeEgress = new DataSource(matrixMapUri);
            Filter ptBikeEgressFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTBIKE_EGRESS_TAZ));
            this.readMatrixMap(ptBikeEgress, ptBikeEgressFilter, ParamMatrixMap.PTBIKE_EGRESS_TAZ, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTCAR_ACCESS_TAZ)) {
            DataSource ptCarAccess = new DataSource(matrixMapUri);
            Filter ptCarAccessFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTCAR_ACCESS_TAZ));
            this.readMatrixMap(ptCarAccess, ptCarAccessFilter, ParamMatrixMap.PTCAR_ACCESS_TAZ, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTBIKE_INTERCHANGES)) {
            DataSource ptBikeInterchange = new DataSource(matrixMapUri);
            Filter ptBikeInterFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTBIKE_INTERCHANGES));
            this.readMatrixMap(ptBikeInterchange, ptBikeInterFilter, ParamMatrixMap.PTBIKE_INTERCHANGES, SimulationType.SCENARIO, sIndex);
        }
        if (this.parameters.isDefined(ParamString.DB_NAME_PTCAR_INTERCHANGES)) {
            DataSource ptCarInterchange = new DataSource(matrixMapUri);
            Filter ptCarInterFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_PTCAR_INTERCHANGES));
            this.readMatrixMap(ptCarInterchange, ptCarInterFilter, ParamMatrixMap.PTCAR_INTERCHANGES, SimulationType.SCENARIO, sIndex);
        }

        // providing base case travel times in case they are needed
        if (this.parameters.isTrue(ParamFlag.FLAG_RUN_SZENARIO)) {
            // travel times for the base case
            //walk
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE)) {
                DataSource walkTtBase = new DataSource(matrixMapUri);
                Filter walkTtBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_WALK_BASE));
                this.readMatrixMap(walkTtBase, walkTtBaseFilter, ParamMatrixMap.TRAVEL_TIME_WALK, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE)) {
                    DataSource walkAccessBase = new DataSource(matrixMapUri);
                    Filter walkAccessBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_WALK_BASE));
                    this.readMatrixMap(walkAccessBase, walkAccessBaseFilter, ParamMatrixMap.ARRIVAL_WALK, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE)) {
                    DataSource walkEgressBase = new DataSource(matrixMapUri);
                    Filter walkEgressBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_WALK_BASE));
                    this.readMatrixMap(walkEgressBase, walkEgressBaseFilter, ParamMatrixMap.EGRESS_WALK, SimulationType.BASE, sIndex);
                }
            }

            //bike
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE)) {
                DataSource bikeTtBase = new DataSource(matrixMapUri);
                Filter bikeTtBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE));
                this.readMatrixMap(bikeTtBase, bikeTtBaseFilter, ParamMatrixMap.TRAVEL_TIME_BIKE, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE)) {
                    DataSource bikeAccessBase = new DataSource(matrixMapUri);
                    Filter bikeAccessBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE));
                    this.readMatrixMap(bikeAccessBase, bikeAccessBaseFilter, ParamMatrixMap.ARRIVAL_BIKE, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE)) {
                    DataSource bikeEgressBase = new DataSource(matrixMapUri);
                    Filter bikeEgressBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE));
                    this.readMatrixMap(bikeEgressBase, bikeEgressBaseFilter, ParamMatrixMap.EGRESS_BIKE, SimulationType.BASE, sIndex);
                }
            }

            //MIT, MIT passenger, Taxi,
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE)) {
                // car
                DataSource carTtBase = new DataSource(matrixMapUri);
                Filter carTtBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_MIT_BASE));
                this.readMatrixMap(carTtBase, carTtBaseFilter, ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE)) {
                    DataSource carAccessBase = new DataSource(matrixMapUri);
                    Filter carAccessBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE));
                    this.readMatrixMap(carAccessBase, carAccessBaseFilter, ParamMatrixMap.ARRIVAL_MIT, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE)) {
                    DataSource carEgressBase = new DataSource(matrixMapUri);
                    Filter carEgressBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE));
                    this.readMatrixMap(carEgressBase, carEgressBaseFilter, ParamMatrixMap.EGRESS_MIT, SimulationType.BASE, sIndex);
                }
            }

            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE)) {
                // public transport
                DataSource ptTtBase = new DataSource(matrixMapUri);
                Filter ptTtBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_PT_BASE));
                this.readMatrixMap(ptTtBase, ptTtBaseFilter, ParamMatrixMap.TRAVEL_TIME_PT, SimulationType.BASE, sIndex);
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE)) {
                    DataSource ptAccessBase = new DataSource(matrixMapUri);
                    Filter ptAccessBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE));
                    this.readMatrixMap(ptAccessBase, ptAccessBaseFilter, ParamMatrixMap.ARRIVAL_PT, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE)) {
                    DataSource ptEgressBase = new DataSource(matrixMapUri);
                    Filter ptEgressBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE));
                    this.readMatrixMap(ptEgressBase, ptEgressBaseFilter, ParamMatrixMap.EGRESS_PT, SimulationType.BASE, sIndex);
                }
                if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE)) {
                    DataSource ptInterchangeBase = new DataSource(matrixMapUri);
                    Filter ptInterChangeBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE));
                    this.readMatrixMap(ptInterchangeBase, ptInterChangeBaseFilter, ParamMatrixMap.INTERCHANGES_PT, SimulationType.BASE, sIndex);
                }
            }
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
    private void readMatrix(DataSource matrixDs, Filter filter, ParamMatrix matrix, SimulationType simType, int sIndex){

        TPS_Logger.log(SeverityLogLevel.INFO, "Loading " + matrix);
        Matrix m = readMatrix(matrixDs, filter, sIndex);

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

    public void readMatrixMap(DataSource matrixMapDs, Filter filter, ParamMatrixMap matrixMap, SimulationType simType, int sIndex){

        MatrixMap m = readMatrixMap(matrixMapDs, filter, sIndex);

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
    public MatrixMap readMatrixMap(DataSource matrixMapDs, Filter filter, int sIndex) {

        //read the matrix map from db
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
        Collection<MatrixMapDto> matrixMapDtos;
        if(filter == null){
            matrixMapDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(MatrixMapDto.class),MatrixMapDto::new),matrixMapDs);
        } else {
            matrixMapDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(MatrixMapDto.class),MatrixMapDto::new),matrixMapDs, filter);
        }
        connectionSupplier.returnObject(connection);

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

            var matrixFilter = new Filter("matrix_name",matrixNames[i]);
            var dataSource = new DataSource(parameters.getString(ParamString.DB_TABLE_MATRICES));

            matrices[i] = this.readMatrix(dataSource, matrixFilter, sIndex);

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
    public Matrix readMatrix(DataSource matrixDs, Filter filter,  int sIndex) {
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<IntMatrixDto> matrixDtos;

        if(filter == null){
            matrixDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(IntMatrixDto.class),IntMatrixDto::new),matrixDs);
        }else {
            matrixDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(IntMatrixDto.class),IntMatrixDto::new),matrixDs, filter);
        }
        connectionSupplier.returnObject(connection);
        
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
    public TPS_ModeChoiceTree readModeChoiceTree(DataSource dataSource, Filter modeFilter, Collection<TPS_Mode> modes) {

        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<ModeChoiceTreeNodeDto> mctDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(ModeChoiceTreeNodeDto.class), ModeChoiceTreeNodeDto::new), dataSource, modeFilter);

        connectionSupplier.returnObject(connection);
        
        Collection<ModeChoiceTreeNodeDto> sortedNodes = mctDtos.stream()
                .sorted(Comparator.comparingInt(ModeChoiceTreeNodeDto::getNodeId))
                .collect(toCollection(ArrayList::new));

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
                    .collect(toCollection(LinkedList::new));

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
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> dr = DataReaderFactory.newJdbcReader(connection);

        EnumMap<ModeType, ModeParameters> modeParams = getModeParameters();

        Collection<ModeDto> modeDtos = dr.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(ModeDto.class), ModeDto::new),modesTable);
        connectionSupplier.returnObject(connection);
        
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

        //bike params from config
        ModeParameters bikeModeParameters =  ModeParameters.builder()
                .beelineFactor(parameters.getDoubleValue(ParamValue.BEELINE_FACTOR_BIKE))
                .costPerKm(parameters.getDoubleValue(ParamValue.BIKE_COST_PER_KM))
                .costPerKmBase(parameters.getDoubleValue(ParamValue.BIKE_COST_PER_KM_BASE))
                .velocity(parameters.getDoubleValue(ParamValue.VELOCITY_BIKE))
                .useBase(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE))
                .variableCostPerKm(0)
                .variableCostPerKmBase(0)
                .build();
        modeParams.put(ModeType.BIKE, bikeModeParameters);

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
    public Collection<TPS_PersonGroup> readPersonGroupCodes(DataSource dataSource, Filter personGroupFilter) {
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<PersonCodeDto> personCodeDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(PersonCodeDto.class),PersonCodeDto::new), dataSource, personGroupFilter);

        connectionSupplier.returnObject(connection);
        
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

    public TPS_VariableMap readValuesOfTimes(DataSource dataSource, Filter votFilter){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<ValueOfTimeDto> votDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(ValueOfTimeDto.class),ValueOfTimeDto::new), dataSource, votFilter);

        connectionSupplier.returnObject(connection);
        
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
    public TPS_Region readRegion() {
        TPS_Region region = new TPS_Region(null, null);
        TPS_Block blk;
        String query;

        // read values of time
        DataSource votDs = new DataSource(parameters.getString(ParamString.DB_TABLE_VOT));
        Filter votFilter = new Filter("name", parameters.getString(ParamString.DB_NAME_VOT));
        region.setValuesOfTime(this.readValuesOfTimes(votDs, votFilter));


        // read cfn values
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        //read cfnx
        DataSource cfnxDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CFNX));
        Filter cfnxFilter = new Filter("key", parameters.getString(ParamString.DB_REGION_CNF_KEY));

        Collection<CfnxDto> cfnxDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(CfnxDto.class), CfnxDto::new),cfnxDs, cfnxFilter);
    
        TPS_CFN cfn = new TPS_CFN(TPS_SettlementSystemType.TAPAS, TPS_ActivityCodeType.TAPAS);

        cfnxDtos.forEach(cfnx -> cfn.addToCFNXMap(cfnx.getCurrentTazSettlementCodeTapas(),cfnx.getValue()));

        //read cfn4
        DataSource cfn4Ds = new DataSource(parameters.getString(ParamString.DB_TABLE_CFN4));
        Filter cfn4Filter = new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_CNF_KEY));
        Collection<CfnFourDto> cfn4Dtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(CfnFourDto.class), CfnFourDto::new), cfn4Ds, cfn4Filter);
        cfn4Dtos.forEach(cfn4 -> cfn.addToCFN4Map(cfn4.getCurrentTazSettlementCodeTapas(),cfn4.getCurrentEpisodeActivityCodeTapas(),cfn4.getValue()));

        region.setCfn(cfn);

        //optional potential parameter, might return no result!
        TPS_CFN potential = new TPS_CFN(TPS_SettlementSystemType.TAPAS, TPS_ActivityCodeType.TAPAS);
        DataSource cfnPotentialDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CFN4));
        Filter cfnPotentialFilter = new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_POTENTIAL_KEY));
        Collection<CfnFourDto> cfn4PotentialDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(CfnFourDto.class), CfnFourDto::new), cfnPotentialDs, cfnPotentialFilter);
        
        connectionSupplier.returnObject(connection);
        
        cfn4PotentialDtos.forEach(cfn4 -> potential.addToCFN4Map(cfn4.getCurrentTazSettlementCodeTapas(),cfn4.getCurrentEpisodeActivityCodeTapas(),cfn4.getValue()));

        region.setPotential(potential);

        //read taz and add to region
        Map<Integer, TPS_TrafficAnalysisZone> trafficAnalysisZones = loadTrafficAnalysisZones();
        trafficAnalysisZones.values().forEach(region::addTrafficAnalysisZone);



        return region;
    }

    private Map<Integer, TPS_TrafficAnalysisZone> loadTrafficAnalysisZones() {
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        //read traffic analysis zones
        DataSource tazDs = new DataSource(parameters.getString(ParamString.DB_TABLE_TAZ));
        Collection<TrafficAnalysisZoneDto> tazDtoCollection = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(TrafficAnalysisZoneDto.class), TrafficAnalysisZoneDto::new), tazDs);
        Map<Integer, TrafficAnalysisZoneDto> tazDtos = collectionToMap(TrafficAnalysisZoneDto::getTazId, tazDtoCollection);

        //initialize TAZ builders with TAZ data
        Map<Integer, TPS_TrafficAnalysisZoneBuilder> tazBuilders = new HashMap<>();

        for(Map.Entry<Integer, TrafficAnalysisZoneDto> tazDtoEntry : tazDtos.entrySet()){

            TrafficAnalysisZoneDto dto = tazDtoEntry.getValue();

            TPS_TrafficAnalysisZoneBuilder builder = TPS_TrafficAnalysisZone.builder()
                    .id(dto.getTazId())
                    .bbrType(TPS_SettlementSystem.getSettlementSystem(TPS_SettlementSystemType.FORDCP,dto.getBbrType()))
                    .center(new TPS_Coordinate(dto.getX(), dto.getY()))
                    .externalId(dto.getNumId() != 0 ? dto.getNumId() : -1);

            tazBuilders.put(dto.getTazId(), builder);

        }


        //read taz scores and update taz builders
        if (this.parameters.isDefined(ParamString.DB_TABLE_TAZ_SCORES)){
            DataSource tazScoresDs = new DataSource(parameters.getString(ParamString.DB_TABLE_TAZ_SCORES));
            Filter tazScoreFilter = new Filter("score_name", parameters.getString(ParamString.DB_NAME_TAZ_SCORES));
            Collection<TrafficAnalysisZoneScoreDto> tazScores =
                    reader.read(new ResultSetConverter<>(
                            new ColumnToFieldMapping<>(TrafficAnalysisZoneScoreDto.class), TrafficAnalysisZoneScoreDto::new),
                            tazScoresDs, tazScoreFilter);

            tazScores.forEach(tazScore -> tazBuilders.get(tazScore.getTazId())
                    .score(tazScore.getScore())
                    .scoreCat(tazScore.getScoreCat()));
        }

        //initialize scenario type values
        Map<Integer, ScenarioTypeValuesBuilder> stvBaseBuilders = emptyStvBuilders(tazBuilders.keySet());
        Map<Integer, ScenarioTypeValuesBuilder> stvScenarioBuilders = emptyStvBuilders(tazBuilders.keySet());


        //read intra-taz data and set scenario type values
        if (this.parameters.isDefined(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS) &&
                this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS)) {
            DataSource intraTazInfoMitDs = new DataSource(parameters.getString(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS));
            Filter intraTazMitFilter = new Filter("info_name", parameters.getString(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS));
            DataSource intraTazInfoPtDs = new DataSource(parameters.getString(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS));
            Filter intraTazPtFilter = new Filter("info_name", parameters.getString(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS));

            Collection<IntraTazInfoMit> intraMitDtos = reader.read(
                    new ResultSetConverter<>(new ColumnToFieldMapping<>(IntraTazInfoMit.class), IntraTazInfoMit::new),
                    intraTazInfoMitDs, intraTazMitFilter);
            Collection<IntraTazInfoPt> intraPtDtos = reader.read(
                    new ResultSetConverter<>(new ColumnToFieldMapping<>(IntraTazInfoPt.class), IntraTazInfoPt::new),
                    intraTazInfoPtDs, intraTazPtFilter);



            initIntraTazInformation(intraMitDtos, intraPtDtos, stvScenarioBuilders);

            if( this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS_BASE) &&
                    this.parameters.isDefined(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS_BASE) &&
                    this.parameters.isTrue(ParamFlag.FLAG_RUN_SZENARIO)) {
                //same procedure as above but this time for base scenario values
                Filter intraTazMitBaseFilter = new Filter("info_name", parameters.getString(ParamString.DB_NAME_TAZ_INTRA_MIT_INFOS_BASE));
                Filter intraTazPtBaseFilter = new Filter("info_name", parameters.getString(ParamString.DB_NAME_TAZ_INTRA_PT_INFOS_BASE));
                Collection<IntraTazInfoMit> intraMitBaseDtos = reader.read(
                        new ResultSetConverter<>(new ColumnToFieldMapping<>(IntraTazInfoMit.class), IntraTazInfoMit::new),
                        intraTazInfoMitDs, intraTazMitBaseFilter);
                Collection<IntraTazInfoPt> intraPtBaseDtos = reader.read(
                        new ResultSetConverter<>(new ColumnToFieldMapping<>(IntraTazInfoPt.class), IntraTazInfoPt::new),
                        intraTazInfoPtDs, intraTazPtBaseFilter);


                initIntraTazInformation(intraMitBaseDtos, intraPtBaseDtos, stvBaseBuilders);
            }
        }

        //read taz fees and tolls and set scenario type values
        DataSource tazFeesAndTollsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_TAZ_FEES_TOLLS));
        Filter feesAndTollsFilter = new Filter("ft_name", parameters.getString(ParamString.DB_NAME_FEES_TOLLS));

        Collection<TazFeesAndTollsDto> tazFeesAndTollsDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(TazFeesAndTollsDto.class), TazFeesAndTollsDto::new),
                tazFeesAndTollsDs, feesAndTollsFilter);
        tazFeesAndTollsDtos.forEach(
                dto -> tazBuilders.get(dto.getTazId())
                        .isPNR(dto.isParkAndRide())
                        .isRestricted(dto.isRestricted())
        );
        initScenarioTypeValuesWithFeesAndTolls(tazFeesAndTollsDtos, stvScenarioBuilders, stvBaseBuilders);

        //add scenario type values to traffic analysis zone builders
        for(Map.Entry<Integer, TPS_TrafficAnalysisZoneBuilder> tazBuilderEntry : tazBuilders.entrySet()){

            int tazId = tazBuilderEntry.getKey();
            tazBuilderEntry.getValue()
                    .simulationTypeValue(SimulationType.SCENARIO, stvScenarioBuilders.get(tazId).build())
                    .simulationTypeValue(SimulationType.BASE, stvBaseBuilders.get(tazId).build());
        }

        //read blocks
        Map<Integer, TPS_Block> blockMap = new HashMap<>();

        if(parameters.isDefined(ParamString.DB_TABLE_BLOCK)){
            DataSource blocksDs = new DataSource(parameters.getString(ParamString.DB_TABLE_BLOCK));
            Collection<BlockDto> blockDtos = reader.read(
                    new ResultSetConverter<>(new ColumnToFieldMapping<>(BlockDto.class), BlockDto::new), blocksDs);

            DataSource blockScoresDs = new DataSource(parameters.getString(ParamString.DB_TABLE_BLOCK_SCORES));
            Filter blockScoreFilter = new Filter("score_name", parameters.getString(ParamString.DB_NAME_BLOCK_SCORES));
            Collection<BlockScoreDto> blockScoreDtos = reader.read(
                    new ResultSetConverter<>(new ColumnToFieldMapping<>(BlockScoreDto.class), BlockScoreDto::new),
                    blockScoresDs, blockScoreFilter);
            Map<Integer, BlockScoreDto> blockScoreDtoMap = collectionToMap(BlockScoreDto::getBlockId, blockScoreDtos);

            DataSource blockNextPtStopDs = new DataSource(parameters.getString(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP));
            Filter nextPtStopFilter = new Filter("next_pt_stop_name", parameters.getString(ParamString.DB_NAME_BLOCK_NEXT_PT_STOP));

            Collection<BlockNextPtStopDto> nextPtStopDtos = reader.read(
                    new ResultSetConverter<>(new ColumnToFieldMapping<>(BlockNextPtStopDto.class), BlockNextPtStopDto::new),
                    blockNextPtStopDs, nextPtStopFilter);
            Map<Integer, BlockNextPtStopDto> blockNextPtStopDtoMap = collectionToMap(BlockNextPtStopDto::getNextPtStopBlockId, nextPtStopDtos);

            for(BlockDto blockDto : blockDtos){

                int blockId = blockDto.getBlockId();
                BlockScoreDto scoreDto = blockScoreDtoMap.get(blockId);
                BlockNextPtStopDto nextPtStopDto = blockNextPtStopDtoMap.get(blockId);
                TPS_BlockBuilder blockBuilder = TPS_Block.builder()
                        .id(blockId)
                        .tazId(blockDto.getTazId())
                        .score(scoreDto.getScore())
                        .scoreCat(scoreDto.getScoreCat())
                        .nearestPubTransStop(nextPtStopDto.getNextPtStopDistance())
                        .center(new TPS_Coordinate(blockDto.getX(), blockDto.getY()));
                blockMap.put(blockId,blockBuilder.build());
            }

            double avgNextPtStop = nextPtStopDtos.stream()
                    .mapToDouble(BlockNextPtStopDto::getNextPtStopDistance)
                    .average()
                    .orElseThrow(() -> new IllegalArgumentException("Couldn't select average distance pt stop from database"));

            this.parameters.setValue(ParamValue.AVERAGE_DISTANCE_PT_STOP, avgNextPtStop);
        }

        //add blocks to taz and finalize
        Map<Integer, TPS_TrafficAnalysisZone> tazMap = new HashMap<>();
        Map<Integer, List<TPS_Block>> blocksByTazId = blockMap.values()
                .stream()
                .collect(groupingBy(
                        TPS_Block::getTazId,
                        toCollection(ArrayList::new)));

        for(Map.Entry<Integer, TPS_TrafficAnalysisZoneBuilder> builderEntry : tazBuilders.entrySet()){
            TPS_TrafficAnalysisZoneBuilder builder = builderEntry.getValue();
            int tazId = builderEntry.getKey();

            blocksByTazId.get(tazId).forEach(block -> builder.block(block.getId(), block));

            tazMap.put(tazId, builder.build());
        }

        //finally read locations
        DataSource locationsDs = new DataSource(parameters.getString(ParamString.DB_TABLE_LOCATION));
        Filter locationFilter = new Filter("key",parameters.getString(ParamString.DB_LOCATION_KEY));
        Collection<LocationDto> locationDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(LocationDto.class), LocationDto::new),
                locationsDs, locationFilter);

        for(LocationDto dto : locationDtos){

            TPS_LocationBuilder locationBuilder = TPS_Location.builder()
                    .id(dto.getLocId())
                    .groupId(parameters.isTrue(ParamFlag.FLAG_USE_LOCATION_GROUPS) ? dto.getLocGroupId() : -1)
                    .locType(TPS_LocationConstant.getLocationCodeByTypeAndCode(TPS_LocationCodeType.TAPAS,dto.getLocCode()))
                    .coordinate(new TPS_Coordinate(dto.getX(), dto.getY()))
                    .tazId(dto.getTazId())
                    .blockId(dto.getBlockId())
                    //todo this creates a cyclic dependency between location and taz/block
                    .block(blockMap.get(dto.getBlockId()))
                    .taz(tazMap.get(dto.getTazId()));

            //init LocationData
            LocationData locationData = LocationData.builder()
                    .updateLocationWeights(parameters.isTrue(ParamFlag.FLAG_UPDATE_LOCATION_WEIGHTS))
                    .weightOccupancy(parameters.getDoubleValue(ParamValue.WEIGHT_OCCUPANCY))
                    .fixCapacity(dto.isHasFixCapacity())
                    .occupancy(0)
                    .build();
            locationData.init();
            locationBuilder.data(locationData);
            TPS_Location location = locationBuilder.build();

            //now add to TAZ and block
            tazMap.get(location.getTAZId()).addLocation(location);
            blockMap.get(location.getBlockId()).addLocation(location);
        }
        
        connectionSupplier.returnObject(connection);
        
        return tazMap;
    }

    private Map<Integer, ScenarioTypeValuesBuilder> emptyStvBuilders(Set<Integer> values) {
        return values.stream().collect(Collectors.toMap(
                value -> value,
                value -> ScenarioTypeValues.builder()
        ));
    }


    /**
     * Helper method that reads the intra taz information for MIT and PT and adds that to the TAZ builders.
     *
     * @param mitInfo collection of internal TAZ information for motorized individual transport
     * @param ptInfo collection of internal TAZ information for public transport
     */
    private void initIntraTazInformation(Collection<IntraTazInfoMit> mitInfo, Collection<IntraTazInfoPt> ptInfo,
                                         Map<Integer, ScenarioTypeValuesBuilder> scenarioTypeValuesBuilders){
        Map<Integer,IntraTazInfoMit> intraMitMap = collectionToMap(IntraTazInfoMit::getTazId, mitInfo);
        Map<Integer,IntraTazInfoPt> intraPtMap = collectionToMap(IntraTazInfoPt::getTazId, ptInfo);

        for(Map.Entry<Integer, ScenarioTypeValuesBuilder> builderEntry : scenarioTypeValuesBuilders.entrySet()){

            int tazId = builderEntry.getKey();
            ScenarioTypeValuesBuilder builder = builderEntry.getValue();
            IntraTazInfoPt ptIntraInfo = intraPtMap.get(tazId);
            IntraTazInfoMit mitIntraInfo = intraMitMap.get(tazId);

            initScenarioTypeValuesIntraTaz(ptIntraInfo, mitIntraInfo, builder);
        }
    }

    private void initScenarioTypeValuesWithFeesAndTolls(Collection<TazFeesAndTollsDto> tazFeesAndTollsDtos,
                                                        Map<Integer, ScenarioTypeValuesBuilder> scenarioTypeValuesBuilders,
                                                        Map<Integer, ScenarioTypeValuesBuilder> baseScenarioTypeValuesBuilders){

        for(TazFeesAndTollsDto dto : tazFeesAndTollsDtos){

            int tazId = dto.getTazId();

            ScenarioTypeValuesBuilder scenarioTypeValuesBuilder = scenarioTypeValuesBuilders.get(tazId);
            ScenarioTypeValuesBuilder baseScenarioTypeValuesBuilder = baseScenarioTypeValuesBuilders.get(tazId);

            //toll fee and toll type
            baseScenarioTypeValuesBuilder.hasToll(dto.isHasTollBase());
            scenarioTypeValuesBuilder.hasToll(dto.isHasTollScenario());

            if (dto.isHasTollBase()){
                // make sure that toll category is not null if hasToll is true;
                // default: 1
                int tollType = dto.getTollTypeBase() == 0 ? 1 : dto.getTollTypeBase();
                baseScenarioTypeValuesBuilder.typeToll(tollType);

                double tollFee = switch(tollType){
                    case 2 -> parameters.getDoubleValue(ParamValue.TOLL_CAT_2_BASE);
                    case 3 -> parameters.getDoubleValue(ParamValue.TOLL_CAT_3_BASE);
                    default -> parameters.getDoubleValue(ParamValue.TOLL_CAT_1_BASE);
                };

                baseScenarioTypeValuesBuilder.feeToll(tollFee);
            }

            if (dto.isHasTollScenario()){
                int tollType = dto.getTollTypeScenario() == 0 ? 1 : dto.getTollTypeScenario();
                scenarioTypeValuesBuilder.typeToll(tollType);

                double tollFee = switch (tollType){
                    case 2 -> parameters.getDoubleValue(ParamValue.TOLL_CAT_2);
                    case 3 -> parameters.getDoubleValue(ParamValue.TOLL_CAT_3);
                    default -> parameters.getDoubleValue(ParamValue.TOLL_CAT_1);
                };

                scenarioTypeValuesBuilder.feeToll(tollFee);
            }

            //parking fee and parking type
            baseScenarioTypeValuesBuilder.hasParkingFee(dto.isHasFeeBase());
            scenarioTypeValuesBuilder.hasParkingFee(dto.isHasFeeScenario());

            if(dto.isHasFeeBase()){

                // make sure that ParkingType is not null if hasParkingFee is true;
                // default: 1
                int parkingFeeType = dto.getFeeTypeBase() == 0 ? 1 : dto.getFeeTypeBase();
                baseScenarioTypeValuesBuilder.typeParking(parkingFeeType);

                double parkingFee = switch(parkingFeeType){
                    case 2 -> parameters.getDoubleValue(ParamValue.PARKING_FEE_CAT_2_BASE);
                    case 3 -> parameters.getDoubleValue(ParamValue.PARKING_FEE_CAT_3_BASE);
                    default -> parameters.getDoubleValue(ParamValue.PARKING_FEE_CAT_1_BASE);
                };

                baseScenarioTypeValuesBuilder.feeParking(parkingFee);
            }

            if(dto.isHasFeeScenario()){

                int parkingFeeType = dto.getFeeTypeScenario() == 0 ? 1 : dto.getFeeTypeScenario();
                scenarioTypeValuesBuilder.typeParking(parkingFeeType);

                double parkingFee = switch(parkingFeeType){
                    case 2 -> parameters.getDoubleValue(ParamValue.PARKING_FEE_CAT_2);
                    case 3 -> parameters.getDoubleValue(ParamValue.PARKING_FEE_CAT_3);
                    default -> parameters.getDoubleValue(ParamValue.PARKING_FEE_CAT_1);
                };

                scenarioTypeValuesBuilder.feeParking(parkingFee);
            }


            //car sharing
            baseScenarioTypeValuesBuilder.isCarsharingServiceArea(dto.isHasCarSharingBase());
            scenarioTypeValuesBuilder.isCarsharingServiceArea(dto.isHasCarSharingScenario());
        }
    }

    private <S, T> Map<S, T> collectionToMap(Function<T, S> keyMapper, Collection<T> objects){

        return objects.stream().collect(Collectors.toMap(
                keyMapper,
                dto -> dto
        ));
    }

    private void initScenarioTypeValuesIntraTaz(IntraTazInfoPt ptInfo, IntraTazInfoMit mitInfo, ScenarioTypeValuesBuilder builder){

        builder.ptZone(ptInfo.getPtZone())
                .averageSpeedPT(ptInfo.getAvgSpeedPt())
                .intraPTTrafficAllowed(ptInfo.isHasIntraTrafficPt())
                .intraMITTrafficAllowed(mitInfo.isHasIntraTrafficMit())
                .averageSpeedMIT(mitInfo.getAvgSpeedMit())
                .beelineFactorMIT(mitInfo.getBeelineFactorMit());
    }

    public Collection<SchemeClassDto> readSchemeClasses(DataSource dataSource, Filter filter){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> dataReader = DataReaderFactory.newJdbcReader(connection);
        var converter = new ResultSetConverter<>(new ColumnToFieldMapping<>(SchemeClassDto.class), SchemeClassDto::new);
        var schemeClasses = dataReader.read(converter, dataSource, filter);
        connectionSupplier.returnObject(connection);
        return schemeClasses;
    }

    public Collection<SchemeDto> readSchemes(DataSource dataSource, Filter filter){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> dataReader = DataReaderFactory.newJdbcReader(connection);
        var converter = new ResultSetConverter<>(new ColumnToFieldMapping<>(SchemeDto.class), SchemeDto::new);
        var schemes =  dataReader.read(converter, dataSource, filter);
        connectionSupplier.returnObject(connection);
        return schemes;
    }

    public Collection<EpisodeDto> readEpisodes(DataSource dataSource, Filter filter){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> dataReader = DataReaderFactory.newJdbcReader(connection);
        var converter = new ResultSetConverter<>(new ColumnToFieldMapping<>(EpisodeDto.class), EpisodeDto::new);
        var episodes =  dataReader.read(converter, dataSource, filter);
        connectionSupplier.returnObject(connection);
        return episodes;
    }

    public Collection<SchemeClassDistributionDto> readSchemeClassDistributions(DataSource dataSource, Filter filter){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> dataReader = DataReaderFactory.newJdbcReader(connection);
        var converter = new ResultSetConverter<>(new ColumnToFieldMapping<>(SchemeClassDistributionDto.class), SchemeClassDistributionDto::new);
        var schemeClassDistricutions = dataReader.read(converter, dataSource, filter);
        connectionSupplier.returnObject(connection);
        return schemeClassDistricutions;
    }

    /**
     * This method reads the scheme set, scheme class distribution values and all episodes from the db.
     *
     * @return The TPS_SchemeSet containing all episodes.
     * @throws SQLException
     */
    public TPS_SchemeSet readSchemeSet(Collection<SchemeClassDto> schemeClasses, Collection<SchemeDto> schemes,
                                       Collection<EpisodeDto> episodes, Collection<SchemeClassDistributionDto> schemeClassDistributions,
                                       Collection<TPS_ActivityConstant> activityConstants, PersonGroups personGroups) {

        int timeSlotLength = this.parameters.getIntValue(ParamValue.SEC_TIME_SLOT);

        TPS_SchemeSet schemeSet = new TPS_SchemeSet();

        // build scheme classes (with time distributions)
        for(SchemeClassDto schemeClassDto : schemeClasses){
            TPS_SchemeClass schemeClass = schemeSet.getSchemeClass(schemeClassDto.getId());
            double mean = schemeClassDto.getAvgTravelTime() * 60;
            schemeClass.setTimeDistribution(mean, mean * schemeClassDto.getProcStdDev());
        }

        // build the schemes, assigning them to the right scheme classes
        Map<Integer, TPS_Scheme> schemeMap = new HashMap<>();
        for(SchemeDto schemeDto : schemes){
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


        var sortCriteria = Comparator.comparing(EpisodeDto::getSchemeId)
                .thenComparing(EpisodeDto::getStart);
        var sortedEpisodes = episodes.stream()
                .sorted(sortCriteria)
                .toList();

        double scaleShift = this.parameters.getDoubleValue(ParamValue.SCALE_SHIFT);
        double scaleStretch = this.parameters.getDoubleValue(ParamValue.SCALE_STRETCH);

        for (EpisodeDto episodeDto : sortedEpisodes){

            TPS_ActivityConstant actCode = activityConstants.stream()
                    .filter(act -> act.getCode(TPS_ActivityCodeType.ZBE) == episodeDto.getActCodeZbe())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No activity constant for type ZBE and code: "+episodeDto.getActCodeZbe()));

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
        // the key of the outer map represents the person group id. The key of the inner map represents the scheme class id.
        HashMap<Integer, HashMap<Integer, Double>> personGroupSchemeProbabilityMap = new HashMap<>();

        for(SchemeClassDistributionDto dto : schemeClassDistributions){
            int personGroupId = dto.getPersonGroup();
            if (!personGroupSchemeProbabilityMap.containsKey(personGroupId)) {
                personGroupSchemeProbabilityMap.put(personGroupId, new HashMap<>());
            }
            personGroupSchemeProbabilityMap.get(personGroupId).put(dto.getSchemeClassId(), dto.getProbability());
        }

        for (Integer key : personGroupSchemeProbabilityMap.keySet()) { //add distributions to the schemeSet
            schemeSet.addDistribution(personGroups.getPersonGroupByCode(key),
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

        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<SettlementSystemDto> settlementSystemDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(SettlementSystemDto.class),SettlementSystemDto::new), dataSource);
        connectionSupplier.returnObject(connection);
        
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
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<SexDto> sexDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(SexDto.class), SexDto::new), dataSource);
        connectionSupplier.returnObject(connection);
        
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
    public Collection<UtilityFunctionDto> readUtilityFunction(DataSource dataSource, Collection<Filter> utilityFunctionFilters){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<UtilityFunctionDto> utilityFunctionDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(UtilityFunctionDto.class), UtilityFunctionDto::new), dataSource);
        connectionSupplier.returnObject(connection);
        
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
}
