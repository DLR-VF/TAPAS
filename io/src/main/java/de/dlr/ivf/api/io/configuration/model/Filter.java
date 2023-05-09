package de.dlr.ivf.api.io.configuration.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Filter {

    private String column;

    private String value;
}
