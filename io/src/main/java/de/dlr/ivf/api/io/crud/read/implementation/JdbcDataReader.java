package de.dlr.ivf.api.io.crud.read.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.crud.read.DataReader;
import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;
import lombok.Builder;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@Builder
public class JdbcDataReader implements DataReader<ResultSet> {

    private final Connection connection;
    private final boolean asLargeTable;

    @Override
    public <T> Collection<T> read(Converter<ResultSet, T> resultSetConverter,
                                  DataSource dataSource,
                                  Collection<Filter> filters) {

        String whereClause = filters != null && !filters.isEmpty() ? dataFilterAsSqlString(filters) : "";

        if(asLargeTable){
            try{
                connection.setAutoCommit(false);
            }catch (SQLException e){
                e.printStackTrace();
            }
        }

        //get total count of the fetch
        String query = "SELECT count(*) as cnt FROM "+ dataSource.getUri() +" "+ whereClause;

        int totalCount = 0;

        try(PreparedStatement st = connection.prepareStatement(query);
            ResultSet rs = st.executeQuery()){
            if (rs.next()) {
                totalCount = rs.getInt("cnt");
            }
        }catch (SQLException e){
            throw new RuntimeException("unable to read datasource "+dataSource.getUri(), e);
        }


        //now read the dataset
        query = "SELECT * FROM "+ dataSource.getUri() +" "+ whereClause;

        Collection<T> readObjects = new ArrayList<>(totalCount);

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            if(asLargeTable) {
                preparedStatement.setFetchSize(1000);
            }
            try(preparedStatement;
                ResultSet rs = preparedStatement.executeQuery()){
                while(rs.next()){
                    readObjects.add(resultSetConverter.convert(rs));
                }
            }catch (SQLException e){
                throw new IllegalArgumentException("unable to read datasource "+dataSource.getUri(),e);
            }

            connection.setAutoCommit(true);

        }catch (SQLException e){
            e.printStackTrace();
        }

        return readObjects;
    }


    private String dataFilterAsSqlString(Collection<Filter> dataFilters) {
        return dataFilters.stream()
                .map(filter -> filter.column()+" = "+filterValueAsSqlString(filter.value()))
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
