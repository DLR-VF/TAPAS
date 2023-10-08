package de.dlr.ivf.tapas.environment.dao.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.util.PreparedStatementContext;
import de.dlr.ivf.api.io.util.PreparedStatementParameterSetter;
import de.dlr.ivf.api.io.util.QueryFactory;
import de.dlr.ivf.tapas.environment.dao.SimulationsDao;
import de.dlr.ivf.tapas.environment.dao.exception.DaoDeleteException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoInsertException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.environment.dao.exception.DaoUpdateException;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.model.SimulationState;
import lombok.Builder;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Builder
public class SimulationsJdbcDao implements SimulationsDao {

    private final DataSource simulationsTable;
    private final Converter<ResultSet, SimulationEntry> inputConverter;
    private final ConnectionPool connectionPool;
    private final PreparedStatementContext preparedStatementContext;
    private final PreparedStatementParameterSetter<SimulationEntry> insertParameterSetter;
    private final PreparedStatementParameterSetter<SimulationEntry> updateParameterSetter;
    private final PreparedStatementParameterSetter<SimulationEntry> removeParameterSetter;

    @Override
    public Collection<SimulationEntry> load() throws DaoReadException {



        String query = "SELECT * FROM "+ simulationsTable.getUri() ;

        Collection<SimulationEntry> simulations = new ArrayList<>();

        try(Connection connection = connectionPool.borrowObject();
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery()){
            while(rs.next()){
                simulations.add(inputConverter.convert(rs));
            }
        }catch (SQLException e){
            e.printStackTrace();
            throw new DaoReadException("unable to read datasource "+simulationsTable.getUri(),e);
        }

        return simulations;
    }

    @Override
    public int save(SimulationEntry simulationEntry) throws DaoInsertException {

        Connection connection = connectionPool.borrowObject();

        String query = QueryFactory.newInsertQuery(preparedStatementContext.getUpdatableColumnNames(), simulationsTable);
        String[] autogeneratedColumns = preparedStatementContext.getAutogeneratedColumns().toArray(new String[0]);

        try (PreparedStatement preparedStatement = connection.prepareStatement(query, autogeneratedColumns)){

            insertParameterSetter.set(preparedStatement,simulationEntry);

            preparedStatement.executeUpdate();

            try(ResultSet generatedSimulationId = preparedStatement.getGeneratedKeys()){
                if(generatedSimulationId.next()){
                    return generatedSimulationId.getInt(1);
                }
            }
        }catch (SQLException e){
            throw new DaoInsertException(e);
        }finally {
            connectionPool.returnObject(connection);
        }

        return -1;
    }

    @Override
    public void update(int simId, SimulationEntry simulation) throws DaoUpdateException {
        Connection connection = connectionPool.borrowObject();

        String query = QueryFactory.generateUpdateQuery(simulationsTable, preparedStatementContext);

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){

            //set parameters
            updateParameterSetter.set(preparedStatement, simulation);

            //now set the value for the where clause
            preparedStatement.executeUpdate();

        }catch (SQLException e){
            e.printStackTrace();
            throw new DaoUpdateException("unable to update simulation entry.",e);
        }finally {
            connectionPool.returnObject(connection);
        }
    }

    @Override
    public void remove(int simId) throws DaoDeleteException {
        Connection connection = connectionPool.borrowObject();

        String query = QueryFactory.deleteQuery(simulationsTable,preparedStatementContext);

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setInt(1,simId);
            preparedStatement.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
            throw new DaoDeleteException("unable to remove simulation entry.", e);
        }finally {
            connectionPool.returnObject(connection);
        }
    }

    /**
     * As simulation requests happen in a concurrent environment from different servers, manual control is taken over
     * the transaction.
     * @param serverIp the server ip that this simulation will be registered to
     * @return the simulation to process
     */
    @Override
    public Optional<SimulationEntry> requestSimulation(String serverIp) {

        SimulationEntry simulationEntry = null;
        Connection connection = connectionPool.borrowObject();
        try {

            String query = "UPDATE "+simulationsTable.getUri()+ " SET simulation_state = '"+ SimulationState.RUNNING.name() +
                    "' WHERE id = (SELECT id FROM "+simulationsTable.getUri()+" WHERE simulation_state = '"+SimulationState.READY+
                    "' LIMIT 1 FOR UPDATE SKIP LOCKED) RETURNING id";

            try(connection;
                PreparedStatement ps = connection.prepareStatement(query);
                ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    int simId = rs.getInt("id");
                    query = "SELECT * FROM "+ simulationsTable.getUri() + " WHERE id = "+simId;
                    try(PreparedStatement simSelect = connection.prepareStatement(query);
                        ResultSet simulation = simSelect.executeQuery()){
                        if(simulation.next()){
                            simulationEntry = inputConverter.convert(simulation);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return Optional.ofNullable(simulationEntry);
    }
}
