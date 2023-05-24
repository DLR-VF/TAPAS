package de.dlr.ivf.api.io;

import de.dlr.ivf.api.io.configuration.model.ConnectionDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ConnectionRequest {

    private final Class<?> requestingClass;
    private final ConnectionDetails connectionDetails;

}
