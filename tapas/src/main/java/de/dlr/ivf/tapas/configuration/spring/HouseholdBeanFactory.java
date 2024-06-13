package de.dlr.ivf.tapas.configuration.spring;


import de.dlr.ivf.tapas.configuration.json.agent.CarsConfiguration;
import de.dlr.ivf.tapas.configuration.json.agent.HouseholdConfiguration;
import de.dlr.ivf.tapas.converters.PersonDtoToPersonConverter;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.model.Incomes;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.location.TPS_Coordinate;
import de.dlr.ivf.tapas.model.location.TPS_Location;
import de.dlr.ivf.tapas.model.location.TrafficAnalysisZones;
import de.dlr.ivf.tapas.model.person.PersonComparators;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.vehicle.*;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.*;

/**
 * The HouseholdBeanFactory class is responsible for creating TPS_Household objects based on the provided configuration and data sources.
 * It uses various beans and dependencies to create the households.
 */
@Configuration
public class HouseholdBeanFactory {

    HouseholdConfiguration householdConfiguration;
    TPS_DB_IO dbIo;

    @Autowired
    public HouseholdBeanFactory(HouseholdConfiguration householdConfiguration, TPS_DB_IO dbIo) {
        this.dbIo = dbIo;
        this.householdConfiguration = householdConfiguration;
    }

    /**
     * Retrieves a collection of TPS_Household objects based on the given HouseholdDto collection and other parameters.
     *
     * @param householdDtos          The collection of HouseholdDto objects representing the households.
     * @param personsByHouseholdId   A map where the keys are household IDs and the values are collections of TPS_Person objects.
     * @param cars                   The Cars object representing the car information.
     * @param incomes                The Incomes object representing the income information.
     * @param trafficAnalysisZones   The TrafficAnalysisZones object representing the traffic analysis zone information.
     * @param memberOrder            The Comparator object defining the order in which members of a household are sorted.
     * @return A collection of TPS_Household objects.
     */
    @Lazy
    @Bean
    public Collection<TPS_Household> households(Collection<HouseholdDto> householdDtos,
                                                Map<Integer, Collection<TPS_Person>> personsByHouseholdId,
                                                Cars cars,
                                                Incomes incomes,
                                                TrafficAnalysisZones trafficAnalysisZones,
                                                Comparator<TPS_Person> memberOrder) {

        Collection<TPS_Household> households = new ArrayList<>(householdDtos.size());

        for (HouseholdDto householdDto : householdDtos) {
            TPS_Location homeLocation = TPS_Location.builder()
                    .id(-1 * householdDto.getHhId())
                    .groupId(-1)
                    .locType(-1)
                    .coordinate(new TPS_Coordinate(householdDto.getXCoordinate(), householdDto.getYCoordinate()))
                    .block(null)
                    .tazId(householdDto.getTazId())
                    .taz(trafficAnalysisZones.getTrafficAnalysisZoneById(householdDto.getTazId()))
                    .build();

            //init the fleet manager
            CarFleetManager.CarFleetManagerBuilder fleetManager = CarFleetManager.builder();
            if (householdDto.getCarIds() != null) {
                Arrays.stream(householdDto.getCarIds())
                        .boxed()
                        .map(cars::getCar)
                        .map(CarController::new)
                        .forEach(fleetManager::addCarController);
            }

            Collection<TPS_Person> orderedMembers = personsByHouseholdId.get(householdDto.getHhId()).stream()
                    .sorted(memberOrder)
                    .toList();

            TPS_Household.TPS_HouseholdBuilder householdBuilder = TPS_Household.builder()
                    .id(householdDto.getHhId())
                    .type(householdDto.getHhType())
                    .realIncome(householdDto.getHhIncome())
                    .location(homeLocation)
                    .income(incomes.getIncomeClass((int) householdDto.getHhIncome()))
                    .carFleetManager(fleetManager.build())
                    .members(orderedMembers);

            households.add(householdBuilder.build());
        }

        return households;
    }

    /**
     * Retrieves a map of persons grouped by household ID.
     *
     * @param personDtos   The collection of PersonDto objects.
     * @param converter    The converter used to convert PersonDto objects to TPS_Person objects.
     * @return A map where the keys are household IDs and the values are collections of TPS_Person objects.
     */
    @Lazy
    @Bean
    public Map<Integer, Collection<TPS_Person>> personsByHouseholdId(Collection<PersonDto> personDtos, PersonDtoToPersonConverter converter){

        return converter.convertCollectionToMapWithSourceKey(personDtos, PersonDto::getHhId);
    }

    /**
     * Provides a {@link de.dlr.ivf.api.converter.Converter} that converts {@link PersonDto} objects to a
     * {@link TPS_Person} objects using the provided AgeClasses and PersonGroups.
     *
     * @param ageClasses The AgeClasses object used for mapping age information.
     * @param personGroups The PersonGroups object used for mapping person group information.
     * @return the converter that converts {@link PersonDto} to {@link TPS_Person} objects.
     */
    @Lazy
    @Bean
    public PersonDtoToPersonConverter personDtoToPersonConverter(AgeClasses ageClasses, PersonGroups personGroups){
        return new PersonDtoToPersonConverter(
                householdConfiguration.availabilityFactorBike(), householdConfiguration.availabilityFactorCarSharing(),
                householdConfiguration.useShoppingMotives(), householdConfiguration.useDrivingLicense(),
                householdConfiguration.rejuvenationThreshold(), householdConfiguration.rejuvenateByYears(),
                householdConfiguration.minAgeCarSharing(), ageClasses, personGroups);
    }

    /**
     * Retrieves a collection of AgeClassDto objects from the database.
     * Uses the dbIo.readFromDb method to read the data from the specified data source.
     * The data is converted to AgeClassDto objects using the provided objectFactory.
     *
     * @return A collection of AgeClassDto objects.
     */
    @Lazy
    @Bean
    public Collection<AgeClassDto> ageClassDtos(){
        return dbIo.readFromDb(householdConfiguration.ageClasses(), AgeClassDto.class, AgeClassDto::new);
    }

    /**
     * Constructs an AgeClasses object based on the given collection of AgeClassDto objects.
     *
     * @param ageClassDtos The collection of AgeClassDto objects used to build the AgeClasses.
     * @return The constructed AgeClasses object.
     */
    @Lazy
    @Bean
    public AgeClasses ageClasses(Collection<AgeClassDto> ageClassDtos){
        AgeClasses.AgeClassesBuilder ageClasses = AgeClasses.builder();

        for(AgeClassDto ageClassDto : ageClassDtos){

            TPS_InternalConstant<TPS_AgeClass.TPS_AgeCodeType> stbaCode = new TPS_InternalConstant<>(ageClassDto.getNameStba(),ageClassDto.getCodeStba(),
                    TPS_AgeClass.TPS_AgeCodeType.valueOf(ageClassDto.getTypeStba()));
            TPS_InternalConstant<TPS_AgeClass.TPS_AgeCodeType> persCode = new TPS_InternalConstant<>(ageClassDto.getNamePersgroup(), ageClassDto.getCodePersgroup(),
                    TPS_AgeClass.TPS_AgeCodeType.valueOf(ageClassDto.getTypePersGroup()));

            TPS_AgeClass ageClass = TPS_AgeClass.builder()
                    .id(ageClassDto.getId())
                    .max(ageClassDto.getMax())
                    .min(ageClassDto.getMin())
                    .attribute(stbaCode)
                    .attribute(persCode)
                    .internalAgeConstant(stbaCode.getType(),stbaCode)
                    .internalAgeConstant(persCode.getType(), persCode)
                    .build();
            ageClasses.ageClass(ageClassDto.getId(), ageClass);
        }

        return ageClasses.build();
    }

    /**
     * Retrieves the configuration for cars from the household configuration.
     *
     * @return The {@link CarsConfiguration} object that represents the configuration for cars.
     */
    @Lazy
    @Bean
    public CarsConfiguration carsConfiguration(){
        return householdConfiguration.carsConfiguration();
    }

    /**
     * Returns a {@code Comparator} that defines the order in which members of a household are sorted.
     *
     * @return A {@code Comparator} object that defines the member order.
     */
    @Lazy
    @Bean
    public Comparator<TPS_Person> memberOrder(){
        return PersonComparators.ofSorting(TPS_Household.Sorting.valueOf(householdConfiguration.memberOrder()));
    }

    /**
     * Retrieves a collection of IncomeDto objects from the database using the dbIo.readFromDb method.
     * The data is read from the incomeClasses data source specified in the householdConfiguration.
     *
     * @return A collection of IncomeDto objects.
     */
    @Lazy
    @Bean
    public Collection<IncomeDto> incomeDtos(){
        return dbIo.readFromDb(householdConfiguration.incomeClasses(), IncomeDto.class, IncomeDto::new);
    }

    /**
     * Create an Incomes object based on the collection of IncomeDto objects.
     *
     * @param incomeDtos The collection of IncomeDto objects used to build the Incomes.
     * @return The constructed Incomes object.
     */
    @Lazy
    @Bean
    public Incomes incomes(Collection<IncomeDto> incomeDtos){
        Incomes.IncomesBuilder incomeMappings = Incomes.builder();

        for(IncomeDto incomeDto : incomeDtos){

            TPS_Income income = TPS_Income.builder()
                    .id(incomeDto.getId())
                    .name(incomeDto.getNameIncome())
                    .code(incomeDto.getCodeIncome())
                    .max(incomeDto.getMax())
                    .build();

            incomeMappings.incomeMapping(income.getMax(), income);
        }

        return incomeMappings.build();
    }

    /**
     * Retrieves a collection of HouseholdDto objects from the database.
     * Uses the dbIo.readFromDb method to read the data from the specified data source.
     *
     * @return A collection of HouseholdDto objects.
     */
    @Lazy
    @Bean
    public Collection<HouseholdDto> householdDtos(){
        return dbIo.readFromDb(householdConfiguration.households(), HouseholdDto.class, HouseholdDto::new);
    }

    /**
     * Retrieves a collection of PersonDto objects from the database based on the household configuration.
     * Uses the dbIo.readFromDb method to read the data from the specified data source.
     *
     * @return A collection of PersonDto objects.
     */
    @Lazy
    @Bean
    public Collection<PersonDto> personDtos(){
        return dbIo.readFromDb(householdConfiguration.persons(), PersonDto.class, PersonDto::new);
    }
}
