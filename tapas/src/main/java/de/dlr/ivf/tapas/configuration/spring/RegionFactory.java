package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.region.LocationConfiguration;
import de.dlr.ivf.tapas.configuration.json.region.RegionConfiguration;
import de.dlr.ivf.tapas.configuration.json.region.TrafficAnalysisZoneConfiguration;
import de.dlr.ivf.tapas.dto.ActivityToLocationDto;
import de.dlr.ivf.tapas.dto.DistanceCodeDto;
import de.dlr.ivf.tapas.model.ActivityAndLocationCodeMapping;
import de.dlr.ivf.tapas.model.DistanceClasses;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Collection;

/**
 * This class is responsible for creating different beans related to regions.
 */
@Configuration
public class RegionFactory {

    private final TPS_DB_IO dbIo;
    private final RegionConfiguration configuration;


    @Autowired
    public RegionFactory(TPS_DB_IO dbIo, RegionConfiguration configuration) {
        this.dbIo = dbIo;
        this.configuration = configuration;
    }

    @Lazy
    @Bean(name = "trafficAnalysisZoneConfiguration")
    public TrafficAnalysisZoneConfiguration getTrafficAnalysisZoneConfiguration() {
        return configuration.trafficAnalysisZoneConfiguration();
    }

    @Lazy
    @Bean
    public LocationConfiguration locationConfiguration(){
        return configuration.locationConfiguration();
    }

    @Lazy
    @Bean
    public ActivityAndLocationCodeMapping activityAndLocationCodeMapping(Collection<ActivityToLocationDto> activityToLocationDtos, Activities activities){
        ActivityAndLocationCodeMapping mapping = new ActivityAndLocationCodeMapping();

        for(ActivityToLocationDto activityToLocationDto : activityToLocationDtos){

            TPS_ActivityConstant actCode = activities.getActivity(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, activityToLocationDto.getActCode());

            mapping.addActivityToLocationMapping(actCode, activityToLocationDto.getLocCode());
            mapping.addLocationCodeToActivityMapping(activityToLocationDto.getLocCode(), actCode);
        }

        return mapping;
    }

    @Lazy
    @Bean
    public DistanceClasses distanceClasses(Collection<DistanceCodeDto> distanceCodeDtos){
        DistanceClasses.DistanceClassesBuilder distanceClassesBuilder = DistanceClasses.builder();

        for(DistanceCodeDto dto : distanceCodeDtos){

            distanceClassesBuilder
                    .distanceMctMapping(dto.getMax(), new TPS_Distance(dto.getId(), dto.getMax(),dto.getCodeMct()))
                    .distanceVotMapping(dto.getMax(), new TPS_Distance(dto.getId(), dto.getMax(), dto.getCodeVot()));
        }

        return distanceClassesBuilder.build();
    }

    @Lazy
    @Bean
    public Collection<ActivityToLocationDto> activityToLocationDtos(){
        return dbIo.readFromDb(configuration.activityToLocationsMapping(), ActivityToLocationDto.class, ActivityToLocationDto::new);
    }

    @Lazy
    @Bean
    public Collection<DistanceCodeDto> distanceCodeDtos(){
        return dbIo.readFromDb(configuration.distanceClasses(), DistanceCodeDto.class, DistanceCodeDto::new);
    }

}
