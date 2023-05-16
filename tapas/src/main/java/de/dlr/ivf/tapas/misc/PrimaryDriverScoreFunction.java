package de.dlr.ivf.tapas.misc;

import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.constants.TPS_Sex;
import de.dlr.ivf.tapas.model.person.TPS_Person;

import java.util.Collection;

public class PrimaryDriverScoreFunction {


    /**
     * This is a hard coded linear regression model to see if the person is the primary driver of a car.
     *
     * @return a probability of being the primary driver
     */
    public double apply(TPS_Person person, Collection<TPS_Person> householdMembers, int hhCarCount) {
        double driverScore = 0;
        int members = 0;
        //int numberOfAdhults=0;
        int numberOfChilds = 0;
        int numberOfWorkingMembers = 0;
        int numberOfMembersWithDrivingLicense = 0;
        int ageYoungestAdhult = 1000; //I hope no one becomes older, ever!
        int ageYoungestChild = 1000;
        int numberOfCars = hhCarCount;

        boolean hasDrivingLicense = person.getDrivingLicenseInformation() == TPS_DrivingLicenseInformation.CAR;
        //shortcut:
        if (numberOfCars == 0 || !hasDrivingLicense) {
            return 0;
        }else{
            driverScore += 10.9458075841947;
        }

        int age = person.getAge();
        TPS_Sex sex = person.getSex();

        driverScore = -25.6959310091439; //constant
        driverScore += age * -0.104309375757471;
        driverScore += Math.log(age) * 3.53025970082386;
        driverScore += sex == TPS_Sex.MALE ? -1.34768311274 : 0;
        driverScore += sex == TPS_Sex.MALE ? age * 0.0449898677627131 : 0;
        driverScore += person.getWorkingAmount() > 0 ? 1.02298146009324 : 0;
        //driverScore += hasDrivingLicense ? 10.9458075841947 : 0; //this is always true

        //sum up all statistics above
        for (TPS_Person p : householdMembers) {
            members++;
            if (p.getWorkingAmount() > 0) numberOfWorkingMembers++;
            if (p.getDrivingLicenseInformation() == TPS_DrivingLicenseInformation.CAR) numberOfMembersWithDrivingLicense++;
            if (p.getAge() >= 18) {
                //numberOfAdhults++;
                ageYoungestAdhult = Math.min(ageYoungestAdhult, p.getAge());
            } else {
                numberOfChilds++;
                ageYoungestChild = Math.min(ageYoungestChild, p.getAge());
            }
        }

        if (numberOfChilds == 0) { //keine Kinder
            if (members == 1) { //ein Erwachsener
                if (ageYoungestAdhult < 30) {
                    driverScore += 0.355021416722072;
                } else if (ageYoungestAdhult < 60) {
                    driverScore += 0.171869345495256;
                } else {
                    driverScore += 1.22484387968388;
                }
            } else if (members == 2) { //zwei erwachsene
                if (ageYoungestAdhult < 30) {
                    driverScore += -0.225129329476169;
                } else if (ageYoungestAdhult < 60) {
                    driverScore += -0.526338766819736;
                } else {
                    driverScore += 0.00349663057568653;
                }
            } else { // mehr als 3
                driverScore += -0.383355892620622;
            }
        } else {                                        //Haushalte mit Kindern
            if (ageYoungestChild < 6) {
                driverScore += -0.422373712001074;
            } else if (ageYoungestChild < 14) {
                driverScore += -0.457447530364251;
            } else {
                driverScore += -0.362948716707795;
            }
        }

        driverScore += numberOfWorkingMembers * -0.358492255297056;
        driverScore += numberOfMembersWithDrivingLicense * 1.20018551149737;
        driverScore += numberOfCars * -1.69765399821086;
        if (numberOfMembersWithDrivingLicense > 0) //avoid div by zero! //TODO is this Integer division correct?
            driverScore += ((double) numberOfCars / numberOfMembersWithDrivingLicense) * 10.6624425307085;


        driverScore = 1.0 / (1.0 + Math.exp(-driverScore));

        //finally cap it (for safety)!
        driverScore = Math.max(0.0, Math.min(1.0, driverScore)); //cap to 0-1



        return driverScore;
    }
}
