package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.trafficgeneration.SchemeProviderConfiguration;
import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.simulation.trafficgeneration.PersonGroupTrafficGeneration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TrafficGenerationFactory is responsible for creating various beans related to traffic generation.
 */
@Configuration
public class TrafficGenerationFactory {

    private final TPS_DB_IO dbIo;

    @Autowired
    public TrafficGenerationFactory(TPS_DB_IO dbIo) {
        this.dbIo = dbIo;
    }

    @Bean(name = "personGroupTrafficGeneration")
    public PersonGroupTrafficGeneration createTrafficGeneration() {
        return null;
    }


    @Bean
    public SchemeProviderConfiguration schemeProviderConfiguration(TrafficGenerationConfiguration configuration){
        return configuration.schemeProviderConfiguration();
    }
}
