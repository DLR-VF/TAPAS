package de.dlr.ivf.api.io;

import de.dlr.ivf.api.io.implementation.JdbcDataReader;
import de.dlr.ivf.api.io.implementation.JdbcLargeTableReader;
import de.dlr.ivf.api.io.implementation.NonClosingConnectionJdbcReader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.function.Supplier;

public class DataReaderFactory {

    public static DataReader<ResultSet> newJdbcReader(Supplier<Connection> connectionSupplier){

        return new JdbcDataReader(connectionSupplier);
    }

    public static DataReader<ResultSet> newJdbcLargeTableReader(Supplier<Connection> connectionSupplier){

        return new JdbcLargeTableReader(connectionSupplier);
    }

    public static DataReader<ResultSet> newOpenConnectionJdbcReader(Supplier<Connection> connectionSupplier){
        return new NonClosingConnectionJdbcReader(connectionSupplier);
    }
}
