package de.dlr.ivf.api.io.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.DataReader;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.configuration.model.Filter;
import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

        Optional<Collection<Filter>> dataFilter = dataSource.getFilter();
        String whereClause = dataFilter.isPresent() ? dataFilterAsSqlString(dataFilter.get()) : "";

        String query = "SELECT * FROM "+ dataSource.getUri() +" "+ whereClause;

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

    String dataFilterAsSqlString(Collection<Filter> dataFilters) {
        return dataFilters.stream()
                .map(filter -> filter.getColumn()+" = "+filterValueAsSqlString(filter.getValue()))
                .collect(Collectors.joining(" AND ", "WHERE ", ""));
    }

    private String filterValueAsSqlString(String filterValue) {

        if(NumberUtils.isCreatable(filterValue)){
            return filterValue;
        }else{
            return "'"+filterValue+"'";
        }
    }
}
