package de.dlr.ivf.util.configuration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferencedDataSource extends DataSource{


    private final ConnectionReference reference;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ReferencedDataSource(@JsonProperty("reference") ConnectionReference reference, @JsonProperty("uri") String uri){
        super(uri);
        this.reference = reference;
    }
}
