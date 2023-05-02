package de.dlr.ivf.api.io;

import de.dlr.ivf.api.io.implementation.JdbcDataReader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.function.Supplier;

public class DataReaderFactory {

    public static <T> DataReader<ResultSet> newJdbcReader(Supplier<Connection> connectionSupplier){

        return new JdbcDataReader(connectionSupplier);
    }
}
