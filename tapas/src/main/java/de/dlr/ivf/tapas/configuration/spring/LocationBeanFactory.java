package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.region.LocationConfiguration;
import de.dlr.ivf.tapas.dto.LocationDto;
import de.dlr.ivf.tapas.model.location.LocationData;
import de.dlr.ivf.tapas.model.location.TPS_Coordinate;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The LocationBeanFactory class is responsible for creating and configuring TPS_Location objects.
 * It uses a TPS_DB_IO instance for reading location data from a database and a LocationConfiguration
 * instance for providing configuration settings.
 *
 * This class is a Spring configuration class.
 */
@Configuration
public class LocationBeanFactory {

    private final TPS_DB_IO dbIo;
    private final LocationConfiguration configuration;

    @Autowired
    public LocationBeanFactory(TPS_DB_IO dbIo, LocationConfiguration configuration) {
        this.dbIo = dbIo;
        this.configuration = configuration;
    }

    @Lazy
    @Bean
    public Collection<TPS_Location> locations(Collection<LocationDto> locationDtos){

        Collection<TPS_Location> locations = new ArrayList<>(locationDtos.size());

        for(LocationDto dto : locationDtos){

            TPS_Location.TPS_LocationBuilder locationBuilder = TPS_Location.builder()
                    .id(dto.getLocId())
                    .groupId(configuration.useLocationsGroups() ? dto.getLocGroupId() : -1)
                    .locType(dto.getLocCode())
                    .coordinate(new TPS_Coordinate(dto.getX(), dto.getY()))
                    .tazId(dto.getTazId());

            //init LocationData
            LocationData locationData = LocationData.builder()
                    .updateLocationWeights(configuration.updateLocationWeights())
                    .weightOccupancy(configuration.occupancyWeight())
                    .capacity(dto.getLocCapacity())
                    .fixCapacity(dto.isHasFixCapacity())
                    .occupancy(0)
                    .build();

            locationData.init();
            locationBuilder.data(locationData);

            TPS_Location location = locationBuilder.build();

            locations.add(location);
        }
        return locations;
    }

    @Lazy
    @Bean
    public Collection<LocationDto> locationDtos() {
        return dbIo.readFromDb(configuration.locations(), LocationDto.class, LocationDto::new);
    }
}
