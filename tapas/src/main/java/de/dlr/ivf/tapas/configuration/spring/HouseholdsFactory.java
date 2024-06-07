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
 * The HouseholdsFactory class is responsible for creating TPS_Household objects based on the provided configuration and data sources.
 * It uses various beans and dependencies to create the households.
 */
@Configuration
public class HouseholdsFactory {

    HouseholdConfiguration householdConfiguration;
    TPS_DB_IO dbIo;

    @Autowired
    public HouseholdsFactory(HouseholdConfiguration householdConfiguration, TPS_DB_IO dbIo) {
        this.dbIo = dbIo;
        this.householdConfiguration = householdConfiguration;
    }

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

    @Bean
    public Map<Integer, Collection<TPS_Person>> personsByHouseholdId(Collection<PersonDto> personDtos, PersonDtoToPersonConverter converter){

        return converter.convertCollectionToMapWithSourceKey(personDtos, PersonDto::getHhId);
    }

    @Bean
    public PersonDtoToPersonConverter personDtoToPersonConverter(AgeClasses ageClasses, PersonGroups personGroups){
        return new PersonDtoToPersonConverter(
                householdConfiguration.availabilityFactorBike(), householdConfiguration.availabilityFactorCarSharing(),
                householdConfiguration.useShoppingMotives(), householdConfiguration.useDrivingLicense(),
                householdConfiguration.rejuvenationThreshold(), householdConfiguration.rejuvenateByYears(),
                householdConfiguration.minAgeCarSharing(), ageClasses, personGroups);
    }

    @Bean
    public Collection<AgeClassDto> ageClassDtos(){
        return dbIo.readFromDb(householdConfiguration.ageClasses(), AgeClassDto.class, AgeClassDto::new);
    }

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

    @Bean
    public PersonGroups personGroups(Collection<PersonCodeDto> personCodeDtos){

        PersonGroups.PersonGroupsBuilder personGroupsBuilder = PersonGroups.builder();

        for(PersonCodeDto personCodeDto : personCodeDtos) {
            TPS_PersonGroup personGroup = TPS_PersonGroup.builder()
                    .description(personCodeDto.getDescription())
                    .code(personCodeDto.getCode())
                    .personType(TPS_PersonType.valueOf(personCodeDto.getPersonType()))
                    .carCode(TPS_CarCode.getEnum(personCodeDto.getCodeCars()))
                    .hasChildCode(TPS_HasChildCode.valueOf(personCodeDto.getHasChild()))
                    .minAge(personCodeDto.getMinAge())
                    .maxAge(personCodeDto.getMaxAge())
                    .workStatus(TPS_WorkStatus.valueOf(personCodeDto.getWorkStatus()))
                    .sex(TPS_Sex.getEnum(personCodeDto.getCodeSex()))
                    .build();

            personGroupsBuilder.personGroup(personGroup.getCode(), personGroup);
        }

        return personGroupsBuilder.build();
    }

    @Bean
    public Collection<PersonCodeDto> personCodeDtos(){
        return dbIo.readFromDb(householdConfiguration.personGroups(), PersonCodeDto.class, PersonCodeDto::new);
    }

    @Bean
    public CarsConfiguration carsConfiguration(){
        return householdConfiguration.carsConfiguration();
    }

    @Lazy
    @Bean
    public Comparator<TPS_Person> memberOrder(){
        return PersonComparators.ofSorting(TPS_Household.Sorting.valueOf(householdConfiguration.memberOrder()));
    }

    @Bean
    public Collection<IncomeDto> incomeDtos(){
        return dbIo.readFromDb(householdConfiguration.incomeClasses(), IncomeDto.class, IncomeDto::new);
    }

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

    @Lazy
    @Bean
    public Collection<HouseholdDto> householdDtos(){
        return dbIo.readFromDb(householdConfiguration.households(), HouseholdDto.class, HouseholdDto::new);
    }

    @Lazy
    @Bean
    public Collection<PersonDto> personDtos(){
        return dbIo.readFromDb(householdConfiguration.persons(), PersonDto.class, PersonDto::new);
    }
}
