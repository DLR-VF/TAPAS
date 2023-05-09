package de.dlr.ivf.tapas;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A first skeleton for a TAPAS configuration. This class will heavily change in the future as more parameters will be
 * extracted.
 *
 * @author Alain Schengen
 */
@Getter
public class TapasConfig {


    @JsonProperty
    String runTimeFile;
}