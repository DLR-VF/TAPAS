package de.dlr.ivf.api.io.configuration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UrlLoginFileReference {


    private final String urlFile;


    private final String loginFile;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UrlLoginFileReference(@JsonProperty("urlFile") String urlFile, @JsonProperty("loginFile") String loginFile){
        this.urlFile = urlFile;
        this.loginFile = loginFile;
    }


}
