package de.dlr.ivf.tapas.configuration.spring;


import de.dlr.ivf.tapas.configuration.json.modechoice.ModeChoiceConfiguration;
import de.dlr.ivf.tapas.configuration.json.modechoice.ModesConfiguration;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class ModeChoiceModelFactory {


    private final TPS_DB_IO dbIo;

    @Autowired
    public ModeChoiceModelFactory(TPS_DB_IO dbIo){
        this.dbIo = dbIo;
    }

    @Lazy
    @Bean
    public ModesConfiguration modesConfiguration(ModeChoiceConfiguration configuration){
        return configuration.modesConfiguration();
    }
}
