package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.runner.SchemeProviderConfiguration;
import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemeSet;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.simulation.trafficgeneration.PersonGroupTrafficGeneration;
import de.dlr.ivf.tapas.simulation.trafficgeneration.SchemeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TrafficGenerationFactory is responsible for creating various beans related to traffic generation.
 */
@Configuration
public class TrafficGenerationFactory {

    private final TrafficGenerationConfiguration configuration;

    private final TPS_DB_IO dbIo;

    @Autowired
    public TrafficGenerationFactory(TPS_DB_IO dbIo, TrafficGenerationConfiguration configuration) {
        this.configuration = configuration;
        this.dbIo = dbIo;
    }



    @Bean(name = "personGroupTrafficGeneration")
    public PersonGroupTrafficGeneration createTrafficGeneration() {
        return null;
    }


    @Bean
    public SchemeProviderConfiguration schemeProviderConfiguration(){
        return configuration.schemeProviderConfiguration();
    }
}
