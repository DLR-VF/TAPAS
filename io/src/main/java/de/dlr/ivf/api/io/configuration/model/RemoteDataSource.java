package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;


/**
 * A datasource that can only be accessed remotely.
 *
 * @author Alain Schengen
 */
@Getter
public class RemoteDataSource extends DataSource {

    @JsonProperty
    ConnectionDetails connector;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RemoteDataSource(ConnectionDetails connector, String uri){
        super(uri);
        this.connector = connector;
    }
}
