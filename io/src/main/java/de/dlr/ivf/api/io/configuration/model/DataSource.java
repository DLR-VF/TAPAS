package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReferencedDataSource.class, name = "reference"),
        @JsonSubTypes.Type(value = RemoteDataSource.class, name = "remote")

})
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DataSource {

    @JsonProperty
    private final String uri;

    @JsonProperty
    private final Filter filter;


    public String getUri() {
        return uri;
    }

    public Optional<Filter> getFilter() {
        return Optional.ofNullable(filter);
    }
}
