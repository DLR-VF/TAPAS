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
    private final String url;

    @JsonProperty
    private final Login login;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RemoteDataSource(String url, Login login, String uri){
        super(uri);
        this.url = url;
        this.login = login;
    }
}
