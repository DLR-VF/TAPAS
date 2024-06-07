package de.dlr.ivf.tapas.converters;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.tapas.dto.PersonDto;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.person.TPS_Person.TPS_PersonBuilder;
import de.dlr.ivf.tapas.util.Randomizer;
import lombok.NonNull;

public class PersonDtoToPersonConverter implements Converter<PersonDto, TPS_Person> {

    private final AgeClasses ageClasses;
    private final PersonGroups personGroups;
    private final double availabilityFactorBike;
    private final double availabilityFactorCarSharing;
    private final boolean useShoppingMotives;
    private final boolean useDrivingLicense;
    private final int rejuvenationThreshold;
    private final int rejuvenateByYears;
    private final int minAgeCarSharing;


    public PersonDtoToPersonConverter(double availabilityFactorBike, double availabilityFactorCarSharing, boolean useShoppingMotives,
                                      boolean useDrivingLicense, int rejuvenationThreshold, int rejuvenateByYears, int minAgeCarSharing,
                                      AgeClasses ageClasses, PersonGroups personGroups){

        this.availabilityFactorBike = availabilityFactorBike;
        this.availabilityFactorCarSharing = availabilityFactorCarSharing;
        this.useShoppingMotives = useShoppingMotives;
        this.useDrivingLicense = useDrivingLicense;
        this.rejuvenationThreshold = rejuvenationThreshold;
        this.rejuvenateByYears = rejuvenateByYears;
        this.minAgeCarSharing = minAgeCarSharing;
        this.ageClasses = ageClasses;
        this.personGroups = personGroups;
    }

    @Override
    public TPS_Person convert(@NonNull PersonDto dto) {

        TPS_PersonGroup personGroup = personGroups.getPersonGroupByCode(dto.getPersonGroup());
        TPS_PersonBuilder personBuilder = TPS_Person.builder()
                .hasBike(dto.isHasBike() && Randomizer.random() < availabilityFactorBike) // TODO: make a better model
                .working(dto.getWorkingAmount())
                .budget((dto.getBudgetIt() + dto.getBudgetPt()) / 100.0)
                .id(dto.getPersonId())
                .personGroup(personGroups.getPersonGroupByCode(dto.getPersonGroup()))
                .isChild(personGroup.isChild())
                .isPupil(personGroup.isPupil())
                //todo check what status is in new db structure
                .status(1)
                .sex(TPS_Sex.getEnum(dto.getSex()))
                .age(dto.getAge())
                .abo(dto.getHasAbo() == 1)
                .workLocationID(dto.getWorkId())
                .educationLevel(dto.getEducation())
                .errorTerm(useShoppingMotives ? Randomizer.randomGumbelDistribution(Randomizer::random, 0,1) : 0);

        //establish driving license information
        TPS_DrivingLicenseInformation drivingLicenseInformation = useDrivingLicense ?
                TPS_DrivingLicenseInformation.getEnum(dto.getDriverLicense()) : TPS_DrivingLicenseInformation.UNKNOWN;
        personBuilder.drivingLicenseInformation(drivingLicenseInformation);

        //establish age class and whether retirees should get an adapted age class
        TPS_AgeClass ageClass = dto.getAge() >= rejuvenationThreshold ?
                ageClasses.getClassByAge(dto.getAge() - rejuvenateByYears) :
                ageClasses.getClassByAge(dto.getAge());
        personBuilder.ageClass(ageClass);

        //establish whether person is a carpooler
        boolean isCarPooler = Randomizer.random() < availabilityFactorCarSharing &&
                dto.getAge() >= minAgeCarSharing &&
                drivingLicenseInformation == TPS_DrivingLicenseInformation.CAR;
        personBuilder.carPooler(isCarPooler);

        return personBuilder.build();
    }
}
