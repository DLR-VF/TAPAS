/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.api.io.configuration.FilterableDataSource;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.io.conversion.ColumnToFieldMapping;
import de.dlr.ivf.api.io.crud.read.DataReader;
import de.dlr.ivf.api.io.crud.read.DataReaderFactory;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;
import de.dlr.ivf.api.io.conversion.ResultSetConverter;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.legacy.*;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.mode.ModeParameters;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.TPS_ModeBuilder;
import de.dlr.ivf.tapas.model.*;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.location.*;
import de.dlr.ivf.tapas.model.mode.TPS_Mode.TPS_ModeCodeType;
import de.dlr.ivf.tapas.model.mode.Modes.ModesBuilder;
import de.dlr.ivf.tapas.model.vehicle.CarFleetManager.CarFleetManagerBuilder;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant.TPS_ActivityCodeType;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import de.dlr.ivf.tapas.model.parameter.*;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.*;
import de.dlr.ivf.tapas.model.person.TPS_Household.TPS_HouseholdBuilder;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.location.ScenarioTypeValues.ScenarioTypeValuesBuilder;

import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class TPS_DB_IO {
    private final Logger logger = System.getLogger(TPS_DB_IO.class.getName());
    /// The ip address of this machine

    private final ConnectionPool connectionSupplier;
    private TPS_ParameterClass parameters;

    public TPS_DB_IO(ConnectionPool connectionSupplier){

        this.connectionSupplier = connectionSupplier;
    }

    public TPS_DB_IO(ConnectionPool connectionSupplier, TPS_ParameterClass parameterClass){
        
        this.connectionSupplier = connectionSupplier;
        this.parameters = parameterClass;
    }

    public Map<Integer, TPS_HouseholdBuilder> readHouseholds(Collection<HouseholdDto> householdDtos, Cars cars, Incomes incomes, Map<Integer, TPS_TrafficAnalysisZone> tazMap){

        Map<Integer, TPS_HouseholdBuilder> householdBuilders = new HashMap<>(householdDtos.size());

        for(HouseholdDto householdDto : householdDtos) {
            TPS_Location homeLocation = TPS_Location.builder()
                    .id(-1 * householdDto.getHhId())
                    .groupId(-1)
                    .locType(-1)
                    .coordinate(new TPS_Coordinate(householdDto.getXCoordinate(), householdDto.getYCoordinate()))
                    .block(null)
                    .tazId(householdDto.getTazId())
                    .taz(tazMap.get(householdDto.getTazId()))
                    .build();

            TPS_HouseholdBuilder householdBuilder = TPS_Household.builder()
                    .id(householdDto.getHhId())
                    .type(householdDto.getHhType())
                    .realIncome(householdDto.getHhIncome())
                    .location(homeLocation)
                    .income(incomes.getIncomeClass((int)householdDto.getHhIncome()));

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
                ExpertKnowledgeTreeDto::new), dataSource, ektFilter);
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
                root = new TPS_ExpertKnowledgeNode(ekNode.getNodeId(), sv, c, summand, factor, null, modes);
            } else {
                TPS_ExpertKnowledgeNode parent = (TPS_ExpertKnowledgeNode) root.getChild(ekNode.getParentNodeId());
                TPS_ExpertKnowledgeNode child = new TPS_ExpertKnowledgeNode(ekNode.getNodeId(), sv, c, summand, factor, parent, modes);

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
        TPS_ExpertKnowledgeNode root = new TPS_ExpertKnowledgeNode(0, null, c, summand, factor, null, modes);

        return new TPS_ExpertKnowledgeTree(root);
    }

    /**
     * Reads all location constant codes from the database and stores them in to a global static map
     * A LocationConstant has the form (id, 3-tuples of (name, code, type))
     * Example: (5, (club, 7, GENERAL), (club, 7, TAPAS))
     */
    public Collection<Integer> readLocationConstantCodes(DataSource dataSource) {
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<LocationCodeDto> locationCodeDtos = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(LocationCodeDto.class),LocationCodeDto::new), dataSource);

        connectionSupplier.returnObject(connection);

        return locationCodeDtos.stream().map(LocationCodeDto::getCodeTapas).collect(toList());
    }

    /**
     * Method to read all matrices for the given region (travel times, distances etc.)
     *
     * @throws SQLException
     */
    public void readMatrices(Collection<TPS_TrafficAnalysisZone> trafficAnalysisZones) throws SQLException {
        final int sIndex = trafficAnalysisZones.stream().mapToInt(TPS_TrafficAnalysisZone::getTAZId).min().getAsInt(); // this is the offset between the TAZ_ids and the matrix index

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
            logger.log(Level.INFO, "Setting walk distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_WALK,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //bike net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE)) {
            DataSource bikeDistances = new DataSource(matrixUri);
            Filter bikeDistFilter = new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_BIKE));
            this.readMatrix(bikeDistances, bikeDistFilter, ParamMatrix.DISTANCES_BIKE, null, sIndex);
        } else {
            logger.log(Level.INFO, "Setting bike distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_BIKE,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //pt net distances
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_DISTANCES_PT)) {
            DataSource ptDistances = new DataSource(matrixUri);
            Filter ptDistFilter = new Filter("matrix_name",parameters.getString(ParamString.DB_NAME_MATRIX_DISTANCES_PT));
            this.readMatrix(ptDistances, ptDistFilter, ParamMatrix.DISTANCES_PT, null, sIndex);
        } else {
            logger.log(Level.INFO, "Setting public transport distances equal to street distances.");
            this.parameters.setMatrix(ParamMatrix.DISTANCES_PT,
                    this.parameters.getMatrix(ParamMatrix.DISTANCES_STREET)); //reference the MIV-matrix
        }

        //beeline dist
        logger.log(Level.INFO, "Calculate beeline distances distances.");
        Matrix bl = new Matrix(trafficAnalysisZones.size(), trafficAnalysisZones.size(),
                sIndex);
        Collection<TPS_TrafficAnalysisZone> sortedTaz = trafficAnalysisZones.stream().sorted(Comparator.comparing(TPS_TrafficAnalysisZone::getTAZId)).toList();
        for (TPS_TrafficAnalysisZone tazfrom : sortedTaz) {
            for (TPS_TrafficAnalysisZone tazto : sortedTaz) {
                double dist = TPS_Geometrics.getDistance(tazfrom.getTrafficAnalysisZone().getCenter(),
                        tazto.getTrafficAnalysisZone().getCenter(),
                        this.parameters.getDoubleValue(ParamValue.MIN_DIST));
                bl.setValue(tazfrom.getTAZId(), tazto.getTAZId(), dist);
            }
        }
        this.parameters.setMatrix(ParamMatrix.DISTANCES_BL, bl);
        logger.log(Level.INFO, "Beeline average value: " +
                this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getAverageValue(false, true) +
                " Size (Elements, Rows, Columns): " + this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getNumberOfElements() + ", "
                + this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getNumberOfRows() + ", "
                + this.parameters.getMatrix(ParamMatrix.DISTANCES_BL).getNumberOfColums());


        SimulationType simulationType = parameters.getSimulationType();
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
        //base values
        if(parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_WALK_BASE)){
            DataSource walkTtBase = new DataSource(matrixMapUri);
            Filter walkTtBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_WALK_BASE));
            this.readMatrixMap(walkTtBase, walkTtBaseFilter, ParamMatrixMap.TRAVEL_TIME_WALK,
                    simulationType, sIndex);
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
        //scenario values
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

        //base values
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE)) {
            DataSource bikeTtBase = new DataSource(matrixMapUri);
            Filter bikeTtBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_BIKE_BASE));
            this.readMatrixMap(bikeTtBase,bikeTtBaseFilter, ParamMatrixMap.TRAVEL_TIME_BIKE, SimulationType.BASE, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE)) {
                DataSource bikeAccessBase = new DataSource(matrixMapUri);
                Filter bikeAccessBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_BIKE_BASE));
                this.readMatrixMap(bikeAccessBase, bikeAccessBaseFilter, ParamMatrixMap.ARRIVAL_BIKE, SimulationType.BASE, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE)) {
                DataSource bikeEgressBase = new DataSource(matrixMapUri);
                Filter bikeEgressBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_BIKE_BASE));
                this.readMatrixMap(bikeEgressBase,bikeEgressBaseFilter, ParamMatrixMap.EGRESS_BIKE, SimulationType.BASE, sIndex);
            }
        }
        //MIT, MIT passenger, Taxi
        //scenario values
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

        //Base values
        if(this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_MIT_BASE)){
            DataSource mitTtBase = new DataSource(matrixMapUri);
            Filter mitTtBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_MIT_BASE));
            this.readMatrixMap(mitTtBase, mitTtBaseFilter, ParamMatrixMap.TRAVEL_TIME_MIT, SimulationType.BASE, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE)) {
                DataSource mitAccessBase = new DataSource(matrixMapUri);
                Filter mitAccessBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_MIT_BASE));
                this.readMatrixMap(mitAccessBase, mitAccessBaseFilter, ParamMatrixMap.ARRIVAL_MIT, SimulationType.BASE, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE)) {
                DataSource mitEgressBase = new DataSource(matrixMapUri);
                Filter mitEgressBaseFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_MIT_BASE));
                this.readMatrixMap(mitEgressBase, mitEgressBaseFilter, ParamMatrixMap.EGRESS_MIT, SimulationType.BASE, sIndex);
            }
        }

        //pt, train
        //scenario values
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

        //base values
        if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_TT_PT_BASE)) {
            DataSource ptTt = new DataSource(matrixMapUri);
            Filter ptTtFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_TT_PT_BASE));
            this.readMatrixMap(ptTt, ptTtFilter, ParamMatrixMap.TRAVEL_TIME_PT, SimulationType.BASE, sIndex);
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE)) {
                DataSource ptAccess = new DataSource(matrixMapUri);
                Filter ptAccessFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_ACCESS_PT_BASE));
                this.readMatrixMap(ptAccess, ptAccessFilter, ParamMatrixMap.ARRIVAL_PT, SimulationType.BASE, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE)) {
                DataSource ptEgress = new DataSource(matrixMapUri);
                Filter ptEgressFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_EGRESS_PT_BASE));
                this.readMatrixMap(ptEgress, ptEgressFilter, ParamMatrixMap.EGRESS_PT, SimulationType.BASE, sIndex);
            }
            if (this.parameters.isDefined(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE)) {
                DataSource ptInterchange = new DataSource(matrixMapUri);
                Filter ptInterFilter = new Filter("matrixMap_name",parameters.getString(ParamString.DB_NAME_MATRIX_INTERCHANGE_PT_BASE));
                this.readMatrixMap(ptInterchange, ptInterFilter, ParamMatrixMap.INTERCHANGES_PT, SimulationType.BASE, sIndex);
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

        logger.log(Level.INFO, "Loading " + matrix);
        Matrix m = readMatrix(matrixDs, filter, sIndex);

        if (simType != null)
            this.parameters.setMatrix(matrix, m, simType);
        else
            this.parameters.setMatrix(matrix, m);

        logger.log(Level.INFO, "Loaded matrix from DB: " +matrix.name() + " Average value: " +
                this.parameters.getMatrix(matrix).getAverageValue(false, true) +
                " Size (Elements, Rows, Columns): " + this.parameters.getMatrix(matrix).getNumberOfElements() + ", "
                + this.parameters.getMatrix(matrix).getNumberOfRows() + ", "
                + this.parameters.getMatrix(matrix).getNumberOfColums());
    }

    public void readMatrixMap(DataSource matrixMapDs, Filter filter, ParamMatrixMap matrixMap, SimulationType simType, int sIndex){

        MatrixMap m = readMatrixMap(matrixMapDs, filter, sIndex);

        if (simType != null) parameters.paramMatrixMapClass.setMatrixMap(matrixMap, m, simType);
        else parameters.paramMatrixMapClass.setMatrixMap(matrixMap, m);
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
                logger.log(Level.INFO,
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
                    .id(modeDto.getCodeMct())
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
            modesBuilder.addModeById(mode.getId(), mode);
            modesBuilder.addModeByName(mode.getName(), mode);
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
    public TPS_Region readRegion(ActivityAndLocationCodeMapping activityToLocationCodeMapping) {
        TPS_Region region = new TPS_Region();

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
    
        TPS_CFN cfn = new TPS_CFN(TPS_ActivityCodeType.TAPAS);

        cfnxDtos.forEach(cfnx -> cfn.addToCFNXMap(cfnx.getCurrentTazSettlementCodeTapas(),cfnx.getValue()));

        //read cfn4
        DataSource cfn4Ds = new DataSource(parameters.getString(ParamString.DB_TABLE_CFN4));
        Filter cfn4Filter = new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_CNF_KEY));
        Collection<CfnFourDto> cfn4Dtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(CfnFourDto.class), CfnFourDto::new), cfn4Ds, cfn4Filter);
        cfn4Dtos.forEach(cfn4 -> cfn.addToCFN4Map(cfn4.getCurrentTazSettlementCodeTapas(),cfn4.getCurrentEpisodeActivityCodeTapas(),cfn4.getValue()));

        region.setCfn(cfn);

        //optional potential parameter, might return no result!
        TPS_CFN potential = new TPS_CFN(TPS_ActivityCodeType.TAPAS);
        DataSource cfnPotentialDs = new DataSource(parameters.getString(ParamString.DB_TABLE_CFN4));
        Filter cfnPotentialFilter = new Filter("key", parameters.getString(ParamString.DB_ACTIVITY_POTENTIAL_KEY));
        Collection<CfnFourDto> cfn4PotentialDtos = reader.read(
                new ResultSetConverter<>(new ColumnToFieldMapping<>(CfnFourDto.class), CfnFourDto::new), cfnPotentialDs, cfnPotentialFilter);
        
        connectionSupplier.returnObject(connection);
        
        cfn4PotentialDtos.forEach(cfn4 -> potential.addToCFN4Map(cfn4.getCurrentTazSettlementCodeTapas(),cfn4.getCurrentEpisodeActivityCodeTapas(),cfn4.getValue()));

        region.setPotential(potential);


        return region;
    }


    public <T> Collection<T> readFromDb(FilterableDataSource dataSource, Class<T> targetClass, Supplier<T> objectFactory){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);

        Collection<T> result = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(targetClass), objectFactory), dataSource);

        connectionSupplier.returnObject(connection);

        return result;

    }

    public <T> Collection<T> readFromDb(DataSource dataSource, Class<T> targetClass, Supplier<T> objectFactory){
        Connection connection = connectionSupplier.borrowObject();
        DataReader<ResultSet> reader = DataReaderFactory.newJdbcReader(connection);
        Collection<T> result = reader.read(new ResultSetConverter<>(new ColumnToFieldMapping<>(targetClass), objectFactory), dataSource);
        connectionSupplier.returnObject(connection);

        return result;
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
                new ResultSetConverter<>(new ColumnToFieldMapping<>(UtilityFunctionDto.class), UtilityFunctionDto::new),
                dataSource,
                utilityFunctionFilters);
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
                logger.log(Level.ERROR, "Unknown attribute: " + set.getMetaData().getColumnName(i), e);
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
