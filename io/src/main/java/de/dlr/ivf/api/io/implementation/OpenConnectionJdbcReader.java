package de.dlr.ivf.api.io.implementation;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.api.io.configuration.model.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

public class OpenConnectionJdbcReader extends JdbcDataReader{
    public OpenConnectionJdbcReader(Supplier<Connection> connectionProvider) {
        super(connectionProvider);
    }

    @Override
    public <T> Collection<T> read(Converter<ResultSet, T> objectFactory, DataSource dataSource) {

        Collection<T> readObjects = new ArrayList<>();

        String query = generateSelectQuery(dataSource);

        Connection connection = this.connectionProvider.get();
        try(PreparedStatement statement = connection.prepareStatement(query);
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
