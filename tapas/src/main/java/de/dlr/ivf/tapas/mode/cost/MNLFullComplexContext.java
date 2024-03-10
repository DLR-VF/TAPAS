package de.dlr.ivf.tapas.mode.cost;

import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.scheme.TPS_TourPart;
import lombok.Getter;

@Getter
public class MNLFullComplexContext {

    private final int personAge;
    private final int numHouseholdCars;
    private final boolean hasPtAbo;
    private final boolean personMayDriveCar;
    private final boolean hasWorkActivity;
    private final boolean hasEducationActivity;
    private final boolean hasShoppingActivity;
    private final boolean hasErrantActivity;
    private final boolean hasLeisureActivity;

    public MNLFullComplexContext(TPS_Person person, TPS_TourPart tourPart){
        this.personAge = person.getAge();
        this.numHouseholdCars = person.getHousehold().getNumberOfCars();
        this.hasPtAbo = person.hasAbo();
        this.personMayDriveCar = person.getDrivingLicenseInformation() == TPS_DrivingLicenseInformation.CAR;
        this.hasWorkActivity = tourPart.hasWorkActivity;
        this.hasEducationActivity = tourPart.hasEducationActivity;
        this.hasShoppingActivity = tourPart.hasShoppingActivity;
        this.hasErrantActivity = tourPart.hasErrantActivity;
        this.hasLeisureActivity = tourPart.hasLeisureActivity;
    }

    public int getPersonQuadraticAge(){
        return personAge * personAge;
    }
}
