package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.region.ExtendedTrafficAnalysisZoneConfiguration;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.model.ActivityAndLocationCodeMapping;
import de.dlr.ivf.tapas.model.location.TPS_Coordinate;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.location.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.model.location.TrafficAnalysisZones;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.*;

/**
 * The TrafficAnalysisZoneBeanFactory class is responsible for creating and configuring TrafficAnalysisZones object.
 * It uses the TPS_DB_IO class to read data from the database and the TrafficAnalysisZoneConfiguration to get the necessary configuration.
 */
@Lazy
@Configuration
public class TrafficAnalysisZoneBeanFactory {

    private final TPS_DB_IO dbIo;

    @Autowired
    public TrafficAnalysisZoneBeanFactory(TPS_DB_IO dbIo) {
        this.dbIo = dbIo;
    }

    @Bean
    public TrafficAnalysisZones trafficAnalysisZones(Collection<TrafficAnalysisZoneScoreDto> tazScores,
                                                     Collection<TrafficAnalysisZoneDto> tazDtos,
                                                     Collection<TPS_Location> locations,
                                                     Collection<TazFeesAndTollsDto> tazFeesAndTollsDtos,
                                                     ActivityAndLocationCodeMapping activityAndLocationCodeMapping
                                                     ){

        Map<Integer, Collection<TPS_Location>> locationsByTazId = locations
                .stream()
                .collect(groupingBy(TPS_Location::getTAZId,toCollection(ArrayList::new)));

        Map<Integer, TrafficAnalysisZoneScoreDto> scoresByTazId = tazScores
                .stream()
                .collect(toMap(TrafficAnalysisZoneScoreDto::getTazId, dto -> dto));

        Map<Integer, TazFeesAndTollsDto> feesAndTollsByTazId = tazFeesAndTollsDtos
                .stream()
                .collect(toMap(TazFeesAndTollsDto::getTazId, dto -> dto));

        TrafficAnalysisZones trafficAnalysisZones = new TrafficAnalysisZones();

        for(TrafficAnalysisZoneDto tazDto : tazDtos){

            double tazScore = scoresByTazId.get(tazDto.getTazId()).getScore();
            double tazScoreCat = scoresByTazId.get(tazDto.getTazId()).getScoreCat();

            boolean isPNR = false;
            boolean isRestricted = false;
            TazFeesAndTollsDto feesAndTollsDto = feesAndTollsByTazId.get(tazDto.getTazId());

            if(feesAndTollsDto != null) {
                isPNR = feesAndTollsByTazId.get(tazDto.getTazId()).isParkAndRide();
                isRestricted = feesAndTollsByTazId.get(tazDto.getTazId()).isRestricted();
            }

            TPS_TrafficAnalysisZone trafficAnalysisZone = TPS_TrafficAnalysisZone.builder()
                    .id(tazDto.getTazId())
                    .bbrType(tazDto.getBbrType())
                    .center(new TPS_Coordinate(tazDto.getX(), tazDto.getY()))
                    .externalId(tazDto.getNumId() != 0 ? tazDto.getNumId() : -1)
                    .score(tazScore)
                    .scoreCat((int)tazScoreCat)
                    .isPNR(isPNR)
                    .isRestricted(isRestricted)
                    .build();

            Collection<TPS_Location> tazLocations = locationsByTazId.get(tazDto.getTazId());

            if(tazLocations != null) {
                tazLocations.forEach(location -> trafficAnalysisZone.addLocation(
                        location,
                        activityAndLocationCodeMapping.getActivitiesByLocationCode(location.getLocType()))
                );
            }

            trafficAnalysisZones.addTrafficAnalysisZone(trafficAnalysisZone.getTAZId(), trafficAnalysisZone);

        }

        return trafficAnalysisZones;
    }

    @Bean
    public Collection<TrafficAnalysisZoneScoreDto> trafficAnalysisZoneScoreDtos(@Qualifier("extendedTrafficAnalysisZoneConfiguration") ExtendedTrafficAnalysisZoneConfiguration config){
        return dbIo.readFromDb(config.tazScores(), TrafficAnalysisZoneScoreDto.class, TrafficAnalysisZoneScoreDto::new);
    }

    @Bean
    public Collection<IntraTazInfoMit> intraTazInfoMit(@Qualifier("extendedTrafficAnalysisZoneConfiguration") ExtendedTrafficAnalysisZoneConfiguration config){
        return dbIo.readFromDb(config.intraMitInfo(), IntraTazInfoMit.class, IntraTazInfoMit::new);
    }

    @Bean
    public Collection<IntraTazInfoPt> intraTazInfoPt(@Qualifier("extendedTrafficAnalysisZoneConfiguration") ExtendedTrafficAnalysisZoneConfiguration config){
        return dbIo.readFromDb(config.intraPtInfo(), IntraTazInfoPt.class, IntraTazInfoPt::new);
    }

    @Bean
    public Collection<TazFeesAndTollsDto> tazFeesAndTollsDtos(@Qualifier("extendedTrafficAnalysisZoneConfiguration") ExtendedTrafficAnalysisZoneConfiguration config){
        return dbIo.readFromDb(config.feesAndTolls(), TazFeesAndTollsDto.class, TazFeesAndTollsDto::new);
    }

    @Bean
    public Collection<TrafficAnalysisZoneDto> trafficAnalysisZoneDtos(@Qualifier("extendedTrafficAnalysisZoneConfiguration") ExtendedTrafficAnalysisZoneConfiguration config){
        return dbIo.readFromDb(config.trafficAnalysisZones(), TrafficAnalysisZoneDto.class, TrafficAnalysisZoneDto::new);
    }
}
