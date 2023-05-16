package de.dlr.ivf.tapas.converters;

import de.dlr.ivf.api.converter.Converter;
import de.dlr.ivf.tapas.dto.PersonDto;
import de.dlr.ivf.tapas.model.constants.AgeClasses;
import de.dlr.ivf.tapas.model.constants.TPS_AgeClass;
import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.constants.TPS_Sex;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.person.TPS_Person.TPS_PersonBuilder;
import de.dlr.ivf.tapas.util.Randomizer;
import lombok.NonNull;

public class PersonDtoToPersonConverter implements Converter<PersonDto, TPS_Person> {

    private final TPS_ParameterClass parameters;
    private final AgeClasses ageClasses;


    public PersonDtoToPersonConverter(TPS_ParameterClass parameters, AgeClasses ageClasses){
        this.parameters = parameters;
        this.ageClasses = ageClasses;
    }

    @Override
    public TPS_Person convert(@NonNull PersonDto dto) {
        TPS_PersonBuilder personBuilder = TPS_Person.builder()
                .hasBike(dto.isHasBike() && Randomizer.random() < parameters.getDoubleValue(ParamValue.AVAILABILITY_FACTOR_BIKE)) // TODO: make a better model
                .working(dto.getWorkingAmount())
                .budget((dto.getBudgetIt() + dto.getBudgetPt()) / 100.0)
                .id(dto.getPersonId())
                .group(dto.getPersonGroup())
                //todo check what status is in new db structure
                .status(1)
                .sex(TPS_Sex.getEnum(dto.getSex()))
                .age(dto.getAge())
                .abo(dto.getHasAbo() == 1)
                .workLocationID(dto.getWorkId())
                .educationLevel(dto.getEducation())
                .errorTerm(parameters.isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES) ?
                        Randomizer.randomGumbelDistribution(Randomizer::random, 0,1) : 0);

        //establish driving license information
        TPS_DrivingLicenseInformation drivingLicenseInformation = parameters.isTrue(ParamFlag.FLAG_USE_DRIVING_LICENCE) ?
                TPS_DrivingLicenseInformation.getEnum(dto.getDriverLicense()) : TPS_DrivingLicenseInformation.UNKNOWN;
        personBuilder.drivingLicenseInformation(drivingLicenseInformation);

        //establish age class and whether retirees should get an adapted age class
        TPS_AgeClass ageClass = parameters.isTrue(ParamFlag.FLAG_REJUVENATE_RETIREE) &&
                dto.getAge() >= parameters.getIntValue(ParamValue.REJUVENATE_AGE) ?
                ageClasses.getClassByAge(dto.getAge() - parameters.getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS)) :
                ageClasses.getClassByAge(dto.getAge());
        personBuilder.ageClass(ageClass);

        //establish whether person is a carpooler
        boolean isCarPooler = Randomizer.random() < parameters.getDoubleValue(ParamValue.AVAILABILITY_FACTOR_CARSHARING) &&
                dto.getAge() >= parameters.getIntValue(ParamValue.MIN_AGE_CARSHARING) &&
                drivingLicenseInformation == TPS_DrivingLicenseInformation.CAR;
        personBuilder.carPooler(isCarPooler);

        return personBuilder.build();
    }
}
