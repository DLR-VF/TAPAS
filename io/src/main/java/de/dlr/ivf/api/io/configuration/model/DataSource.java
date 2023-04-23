package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReferencedDataSource.class, name = "reference"),
        @JsonSubTypes.Type(value = RemoteDataSource.class, name = "remote")
})
public class DataSource {

    private final String uri;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DataSource(@JsonProperty("uri") String uri){
        this.uri = uri;
    }
}
