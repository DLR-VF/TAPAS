package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.model.constants.Activities;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.constants.TPS_InternalConstant;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemeSet;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.simulation.trafficgeneration.PersonGroupTrafficGeneration;
import de.dlr.ivf.tapas.simulation.trafficgeneration.SchemeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayDeque;
import java.util.Collection;

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

    @Bean
    public TPS_SchemeSet createSchemeSet() {

//        //read SchemeSet
//        DataSource schemeClasses = new DataSource(configuration.schemeClasses().uri());
//        Collection<SchemeClassDto> schemeClassDtos = dbIo.readSchemeClasses(schemeClasses, configuration.schemeClasses().filterBy());
//
//        DataSource schemes = new DataSource(configuration.schemes().uri());
//        Collection<SchemeDto> schemeDtos = dbIo.readSchemes(schemes, configuration.schemes().filterBy());
//
//        DataSource episodes = new DataSource(configuration.episodes().uri());
//        Collection<EpisodeDto> episodeDtos = dbIo.readEpisodes(episodes, configuration.episodes().filterBy());
//
//        DataSource schemeClassDistributions = new DataSource(configuration.schemeClassDistributions().uri());
//        Collection<SchemeClassDistributionDto> distributionDtos = dbIo.readSchemeClassDistributions(schemeClassDistributions, configuration.schemeClassDistributions().filterBy());
//
//        var schemeSet = dbIo.readSchemeSet(schemeClassDtos,schemeDtos,episodeDtos,distributionDtos, activityConstants, personGroups);
        return null;
    }

    @Bean(name = "personGroupTrafficGeneration")
    public PersonGroupTrafficGeneration createTrafficGeneration() {
        return null;
    }

    @Bean(name = "schemeProvider")
    public SchemeProvider createSchemeProvider() {
        return null;
    }

    @Lazy
    @Bean
    public Activities activities(Collection<TPS_ActivityConstant> activities) {

        return new Activities(activities);
    }

    @Lazy
    @Bean
    public Collection<TPS_ActivityConstant> activityConstants(Collection<ActivityDto> activityDtos){

        Collection<TPS_ActivityConstant> activities = new ArrayDeque<>(activityDtos.size());

        for(ActivityDto activityDto : activityDtos){

            String attribute = activityDto.getAttribute();

            var mctConstant = new TPS_InternalConstant<>(activityDto.getNameMct(), activityDto.getCodeMct(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeMct()));
            var tapasConstant = new TPS_InternalConstant<>(activityDto.getNameTapas(), activityDto.getCodeTapas(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeTapas()));
            var priorityConstant = new TPS_InternalConstant<>(activityDto.getNamePriority(), activityDto.getCodePriority(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypePriority()));
            var zbeConstant = new TPS_InternalConstant<>(activityDto.getNameZbe(), activityDto.getCodeZbe(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeZbe()));
            var votConstant = new TPS_InternalConstant<>(activityDto.getNameVot(), activityDto.getCodeVot(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeVot()));

            TPS_ActivityConstant act = TPS_ActivityConstant.builder()
                    .id(activityDto.getId())
                    .internalConstant(mctConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.MCT,mctConstant)
                    .internalConstant(tapasConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS,tapasConstant)
                    .internalConstant(priorityConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.PRIORITY, priorityConstant)
                    .internalConstant(zbeConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, zbeConstant)
                    .internalConstant(votConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.VOT, votConstant)
                    .attribute(!(attribute == null || attribute.equals("null"))
                            ? TPS_ActivityConstant.TPS_ActivityConstantAttribute.valueOf(activityDto.getAttribute()) : TPS_ActivityConstant.TPS_ActivityConstantAttribute.DEFAULT)
                    .isFix(activityDto.isFix())
                    .isTrip(activityDto.isTrip())
                    .build();

            activities.add(act);
        }

        return activities;
    }

    @Lazy
    @Bean
    public Collection<ActivityDto> activityDtos(){
        return dbIo.readFromDb(configuration.activities(), ActivityDto.class, ActivityDto::new);
    }
}
