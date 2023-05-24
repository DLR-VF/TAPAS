package de.dlr.ivf.api.io.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.configuration.model.DataSource;
import de.dlr.ivf.api.io.configuration.model.Filter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public class JdbcLargeTableReader extends JdbcDataReader {

    public JdbcLargeTableReader(Supplier<Connection> connectionSupplier) {
        super(connectionSupplier);
    }

    @Override
    public <T> Collection<T> read(Converter<ResultSet, T> objectFactory, DataSource dataSource) {

        Optional<Collection<Filter>> dataFilter = dataSource.getFilter();
        String whereClause = dataFilter.isPresent() ? dataFilterAsSqlString(dataFilter.get()) : "";

        Collection<T> readObjects;

        try(Connection connection = connectionProvider.get()){

            connection.setAutoCommit(false);

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
            readObjects = new ArrayList<>(totalCount);

            PreparedStatement personStatement = connection.prepareStatement(query);
            personStatement.setFetchSize(1000);
            try(personStatement;
                ResultSet rs = personStatement.executeQuery()){
                while(rs.next()){
                    readObjects.add(objectFactory.convert(rs));
                }
            }catch (SQLException e){
                throw new IllegalArgumentException("unable to read datasource "+dataSource.getUri(),e);
            }
        }catch (SQLException e){
            throw new IllegalArgumentException("unable to establish a connection to the database", e);
        }

        return readObjects;
    }
}
