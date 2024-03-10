package de.dlr.ivf.tapas.mode.cost;

import de.dlr.ivf.tapas.util.TPS_FastMath;

public class MNLFullComplexFunction {

    private final double[] parameters;
    public MNLFullComplexFunction(double[] parameters){
        this.parameters = parameters;
    }


    /**
     * Applies the MNLFullComplexFunction using the given parameters and context.
     *
     * @param travelTime The travel time.
     * @param cost The cost.
     * @param numInterchangesPt The number of interchanges for public transportation.
     * @param context The MNLFullComplexContext object.
     *
     * @return The result of the function.
     */
    public double apply(double travelTime, double cost, double numInterchangesPt, MNLFullComplexContext context){
        double result = parameters[0] +  // mode constant
                parameters[1] * travelTime + // beta travel time
                parameters[2] * cost + // beta costs
                parameters[3] * context.getPersonAge() + //alter
                parameters[4] * context.getPersonQuadraticAge() + //quadratisches alter
                parameters[5] * context.getNumHouseholdCars() + // anzahl autos
                parameters[6] * TPS_FastMath.exp(numInterchangesPt) + //umstiege (nur ÖV)
                //ab jetzt binär-Betas, also Ja/nein
                (context.isPersonMayDriveCar() ? parameters[7] : 0) + //führerschein
                (context.isHasPtAbo() ? parameters[8] : 0) + //Öffi -abo
                (context.isHasWorkActivity() ? parameters[9] : 0) + //tourpart mit Arbeit
                (context.isHasEducationActivity() ? parameters[10] : 0) + //tourpart mit Bildung
                (context.isHasShoppingActivity() ? parameters[11] : 0) + //tourpart mit Einkauf
                (context.isHasErrantActivity() ? parameters[12] : 0) + //tourpart mit Erledigung
                (context.isHasLeisureActivity() ? parameters[13] : 0);

        return TPS_FastMath.exp(result);
    }
}
