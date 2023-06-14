package de.dlr.ivf.api.io.connection;

import lombok.Builder;
import lombok.Singular;

import java.util.Map;

@Builder
public final class ConnectionPoolManager {

    @Singular
    private final Map<String, ConnectionPool> connectionPools;

    public ConnectionPool get(String uri) {

        ConnectionPool connectionPool = connectionPools.get(uri);
        if(connectionPool == null){
            String message = "no connection pool configured for datasource: "+uri;
            System.err.println(message);
            throw new RuntimeException(message);
        }

        return connectionPool;
    }
}
