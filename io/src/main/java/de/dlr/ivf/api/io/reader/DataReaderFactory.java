package de.dlr.ivf.api.io.reader;

import de.dlr.ivf.api.io.reader.implementation.JdbcDataReader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.function.Supplier;

public class DataReaderFactory {

    /**
     * Returns a basic JDBC data reader that closes that closes the used connection after reading.
     *
     * @param connectionSupplier a {@link Supplier} that supplies {@link Connection}.
     * @return a {@link DataReader} instance for a specific database
     */
    public static DataReader<ResultSet> newJdbcReader(Supplier<Connection> connectionSupplier){
        return JdbcDataReader.builder()
                .asLargeTable(false)
                .autoCloseConnection(true)
                .connectionProvider(connectionSupplier)
                .build();
    }

    /**
     * Returns a JDBC data reader that uses preset fetch size and disabled autocommit during the reading process.
     * This data reader does close the connection after reading.
     *
     * @param connectionSupplier a {@link Supplier} that supplies {@link Connection}.
     * @return a {@link DataReader} instance for a specific database
     */

    public static DataReader<ResultSet> newJdbcLargeTableReader(Supplier<Connection> connectionSupplier){

        return JdbcDataReader.builder()
                .asLargeTable(true)
                .autoCloseConnection(true)
                .connectionProvider(connectionSupplier)
                .build();
    }

    /**
     * Returns a JDBC data reader that does not close the connection after the reading process.
     * Should be used in background tasks that periodically poll a database.
     *
     * @param connectionSupplier a {@link Supplier} that supplies {@link Connection}.
     * @return a {@link DataReader} instance for a specific database
     */
    public static DataReader<ResultSet> newOpenConnectionJdbcReader(Supplier<Connection> connectionSupplier){
        return JdbcDataReader.builder()
                .asLargeTable(false)
                .autoCloseConnection(false)
                .connectionProvider(connectionSupplier)
                .build();
    }
}
