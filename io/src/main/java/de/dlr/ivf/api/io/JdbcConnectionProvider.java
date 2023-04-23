package de.dlr.ivf.api.io;

import de.dlr.ivf.api.io.configuration.model.Login;
import de.dlr.ivf.api.io.configuration.model.RemoteDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * A simple JDBC connection provider interface with a default {@link #get(RemoteDataSource)} method implementation.
 *
 * @author Alain Schengen
 */
public interface JdbcConnectionProvider extends ConnectionProvider<Connection> {

    @Override
    default Connection get(RemoteDataSource dataSource) {
        String url = dataSource.getUrl();
        Login login = dataSource.getLogin();

        try {
            return DriverManager.getConnection(url, login.getUser(), login.getPassword());

        } catch (SQLException e) {
            String message = """
                    Unable to establish JDBC connection to '%1$s' for resource '%2$s'.
                    '%1$s' might not be accessible and/or supplied login credentials are wrong.
                    """
                    .formatted(
                            url,
                            dataSource.getUri()
                    );

            throw new RuntimeException(message, e);
        }
    }
}
