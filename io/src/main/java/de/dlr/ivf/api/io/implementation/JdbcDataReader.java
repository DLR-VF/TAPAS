package de.dlr.ivf.api.io.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.DataReader;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.configuration.model.Filter;
import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public class JdbcDataReader implements DataReader<ResultSet> {

    private final Supplier<Connection> connectionProvider;

    public JdbcDataReader(Supplier<Connection> connectionProvider){
        this.connectionProvider = connectionProvider;
    }


    @Override
    public <T> Collection<T> read(Converter<ResultSet, T> objectFactory, DataSource dataSource) {

        if(!(dataSource instanceof RemoteDataSource remoteDataSource))
            throw new IllegalArgumentException("The provided datasource: " + dataSource + " is not a RemoteDataSource");

        Collection<T> readObjects = new ArrayList<>();

        Optional<Filter> dataFilter = dataSource.getFilter();
        String whereClause = dataFilter.isPresent() ? " WHERE "+dataFilter.map(filter -> filter.getColumn()+ " = '"+filter.getValue()) +"'" : "";
        String query = "SELECT * FROM "+ dataSource.getUri() + whereClause;

        try(Connection connection = connectionProvider.get();
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet  = statement.executeQuery()){

            while(resultSet.next()){
                readObjects.add(objectFactory.convert(resultSet));
            }

        }catch (SQLException e){
            e.printStackTrace();
        }

        return readObjects;
    }
}
