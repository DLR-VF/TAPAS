package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.trafficgeneration.SchemeProviderConfiguration;
import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * TrafficGenerationBeanFactory is responsible for creating various beans related to traffic generation.
 */
@Lazy
@Configuration
public class TrafficGenerationBeanFactory {

    private final TPS_DB_IO dbIo;

    @Autowired
    public TrafficGenerationBeanFactory(TPS_DB_IO dbIo) {
        this.dbIo = dbIo;
    }

    @Bean
    public SchemeProviderConfiguration schemeProviderConfiguration(TrafficGenerationConfiguration configuration){
        return configuration.schemeProviderConfiguration();
    }
}
