package de.dlr.ivf.api.io;

import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import de.dlr.ivf.api.io.configuration.model.Login;
import de.dlr.ivf.api.io.reader.implementation.BaseJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * A simple JDBC connection provider interface with a default {@link #get(ConnectionDetails)} method implementation.
 *
 * @author Alain Schengen
 */
public interface JdbcConnectionProvider extends ConnectionProvider<Connection> {

    @Override
    default Connection get(ConnectionDetails connector) {

        String url = connector.getUrl();
        Login login = connector.getLogin();

        try {
            return DriverManager.getConnection(url, login.getUser(), login.getPassword());

        } catch (SQLException e) {
            String message = """
                    Unable to establish JDBC connection to '%1$s'.
                    '%1$s' might not be accessible and/or supplied login credentials are wrong.
                    """
                    .formatted(url);
            System.err.println(message);
            throw new RuntimeException(message, e);
        }
    }

    static JdbcConnectionProvider newJdbcConnectionProvider(){
        return new BaseJdbcConnectionProvider();
    }
}
