package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.agent.CarsConfiguration;
import de.dlr.ivf.tapas.dto.CarDto;
import de.dlr.ivf.tapas.model.mode.ModeUtils;
import de.dlr.ivf.tapas.model.vehicle.*;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Collection;

/**
 * CarBeanFactory is a configuration class that creates beans for Cars and FuelTypes.
 * It is responsible for reading car data from a database and initializing Cars.
 */
@Configuration
public class CarBeanFactory {

    private final TPS_DB_IO dbIo;
    private final CarsConfiguration configuration;

    @Autowired
    public CarBeanFactory(TPS_DB_IO dbIo, CarsConfiguration configuration) {
        this.dbIo = dbIo;
        this.configuration = configuration;
    }

    @Lazy
    @Bean
    public Collection<CarDto> carDtos(){
        return dbIo.readFromDb(configuration.cars(), CarDto.class, CarDto::new);
    }

    @Lazy
    @Bean("noVehicle")
    public Vehicle noVehicle(){
        return new NoVehicle();
    }

    @Lazy
    @Bean
    public Cars initCars(Collection<CarDto> carDtos, FuelTypes fuelTypes){

        Cars.CarsBuilder cars = Cars.builder();

        for(CarDto carDto :carDtos){

            FuelType fuelType = fuelTypes.getFuelType(FuelTypeName.getById(carDto.getEngineType()));
            boolean isCompanyCar = carDto.isCompanyCar();
            //build the car first
            TPS_Car car = CarImpl.builder()
                    .id(carDto.getId())
                    .kbaNo(carDto.getKbaNo())
                    .fuelType(fuelType)
                    .emissionClass(EmissionClass.getById(carDto.getEmissionType()))
                    .restricted(carDto.isRestriction())
                    .fixCosts(carDto.getFixCosts())
                    .automationLevel(carDto.getAutomationLevel())
                    .costPerKilometer(isCompanyCar ? 0 : fuelType.getFuelCostPerKm() * carDto.getFixCosts())
                    .variableCostPerKilometer(isCompanyCar ? 0 : fuelType.getVariableCostPerKm()  * ModeUtils.getKBAVariableCostPerKilometerFactor(carDto.getKbaNo()))
                    .build();

            cars.car(car.id(), car);
        }

        return cars.build();
    }

    @Lazy
    @Bean
    public FuelTypes fuelTypes(){

        FuelTypes.FuelTypesBuilder fuelTypesBuilder = FuelTypes.builder();

        for(FuelTypeName fuelType : FuelTypeName.values()) {
            FuelType.FuelTypeBuilder builder = FuelType.builder();

            builder.fuelType(fuelType);

            switch (fuelType) {
                case BENZINE -> builder
                        .fuelCostPerKm(configuration.mitGasolineCostPerKm())
                        .variableCostPerKm(configuration.mitVariableCostPerKm())
                        .range(configuration.mitConventionalRange());
                case DIESEL -> builder
                        .fuelCostPerKm(configuration.mitDieselCostPerKm())
                        .variableCostPerKm(configuration.mitVariableCostPerKm())
                        .range(configuration.mitConventionalRange());
                case GAS, LPG -> builder
                        .fuelCostPerKm(configuration.mitGasCostPerKm())
                        .variableCostPerKm(configuration.mitVariableCostPerKm())
                        .range(configuration.mitConventionalRange());
                case EMOBILE -> builder
                        .fuelCostPerKm(configuration.mitElectricCostPerKm())
                        .variableCostPerKm(configuration.mitVariableCostPerKm())
                        .range(configuration.mitElectricRange());
                case PLUGIN -> builder
                        .fuelCostPerKm(configuration.mitPluginCostPerKm())
                        .variableCostPerKm(configuration.mitVariableCostPerKm())
                        .range(configuration.mitPluginRange());
                case FUELCELL -> builder
                        .fuelCostPerKm(configuration.mitFuelCellCostPerKm())
                        .variableCostPerKm(configuration.mitVariableCostPerKm())
                        .range(configuration.mitConventionalRange());
            }

            fuelTypesBuilder.fuelType(fuelType, builder.build());
        }
        return fuelTypesBuilder.build();
    }
}
