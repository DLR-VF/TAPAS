package de.dlr.ivf.api.io.reader.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.reader.DataReader;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.configuration.model.Filter;
import lombok.Builder;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Builder
public class JdbcDataReader implements DataReader<ResultSet> {

    private final Supplier<Connection> connectionProvider;
    private final boolean asLargeTable;
    private final boolean autoCloseConnection;

    @Override
    public <T> Collection<T> read(Converter<ResultSet, T> resultSetConverter,
                                  DataSource dataSource,
                                  Collection<Filter> filters) {

        Connection connection = connectionProvider.get();

        String whereClause = filters != null && filters.size() > 0 ? dataFilterAsSqlString(filters) : "";

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

            if(autoCloseConnection){
                connection.close();
            }

        }catch (SQLException e){
            e.printStackTrace();
        }

        return readObjects;
    }


    private String dataFilterAsSqlString(Collection<Filter> dataFilters) {
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
