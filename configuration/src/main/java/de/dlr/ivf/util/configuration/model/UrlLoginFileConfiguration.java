package de.dlr.ivf.util.configuration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class UrlLoginFileConfiguration {


    private final String urlFile;


    private final String loginFile;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UrlLoginFileConfiguration(@JsonProperty("urlFile") String urlFile, @JsonProperty("loginFile") String loginFile){
        this.urlFile = urlFile;
        this.loginFile = loginFile;
    }


}
