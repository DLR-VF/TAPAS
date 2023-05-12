package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ReferencedDataSource extends DataSource {


    private final ConnectionReference reference;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ReferencedDataSource(@JsonProperty("reference") ConnectionReference reference, @JsonProperty("uri") String uri){
        super(uri, null);
        this.reference = reference;
    }
}
