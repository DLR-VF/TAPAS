package de.dlr.ivf.api.io.crud.read;

import de.dlr.ivf.api.io.crud.read.implementation.JdbcDataReader;

import java.sql.Connection;
import java.sql.ResultSet;

public class DataReaderFactory {

    /**
     * Returns a basic JDBC data reader that closes that closes the used connection after reading.
     *
     * @param connection that should be used to read the data.
     * @return a {@link DataReader} instance for a specific database
     */
    public static DataReader<ResultSet> newJdbcReader(Connection connection){
        return JdbcDataReader.builder()
                .asLargeTable(false)
                .connection(connection)
                .build();
    }

    /**
     * Returns a JDBC data reader that uses preset fetch size and disabled autocommit during the reading process.
     * This data reader does close the connection after reading.
     *
     * @param connection that should be used to read the data.
     * @return a {@link DataReader} instance for a specific database
     */

    public static DataReader<ResultSet> newJdbcLargeTableReader(Connection connection){

        return JdbcDataReader.builder()
                .asLargeTable(true)
                .connection(connection)
                .build();
    }
}
