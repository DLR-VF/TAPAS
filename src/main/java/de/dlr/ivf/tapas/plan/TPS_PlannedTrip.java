package de.dlr.ivf.tapas.plan;

import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_Mode.ModeType;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import de.dlr.ivf.tapas.scheme.TPS_SchemePart;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import de.dlr.ivf.tapas.util.parameters.*;

/**
 * @author cyga_ri
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_PlannedTrip extends TPS_AdaptedEpisode implements ExtendedWritable {
    // variable costs of the trip in Euro
    private double costs;
    // mode selected for the trip
    private TPS_ExtMode mode;
    // Reference to the trip of this planned trip
    private final TPS_Trip trip;


    /**
     * This constructor calls the super constructor with the trip and sets the internal reference of the trip.
     *
     * @param plan Reference to the whole plan
     * @param trip Reference to the original trip
     */
    public TPS_PlannedTrip(TPS_Plan plan, TPS_Trip trip, TPS_ParameterClass parameterClass) {
        super(plan, trip);
        this.trip = trip;
    }

    /**
     * Returns the variable costs of the trip in the given currency
     *
     * @param currency currency to return in the costs
     * @return cost of the trip
     */
    public double getCosts(CURRENCY currency) {
        return currency.convert(costs, CURRENCY.valueOf(plan.getParameters().getString(ParamString.CURRENCY)));
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_AdaptedEpisode#getEpisode()
     */
    @Override
    public TPS_Episode getEpisode() {
        return this.getTrip();
    }

    /**
     * Returns the mode selected for the trip
     *
     * @return the mode
     */
    public TPS_ExtMode getMode() {
        return mode;
    }

    /**
     * Sets the selected mode for the trip
     *
     * @param mode
     */
    public void setMode(TPS_ExtMode mode) {
        this.mode = mode;
    }

    /**
     * @return reference to the corresponding trip
     */
    public TPS_Trip getTrip() {
        return trip;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_AdaptedEpisode#isLocatedStay()
     */
    @Override
    public boolean isLocatedStay() {
        return false;
    }

    /**
     * Function identifies the modes used for the previous and subsequent stay and determines the modes that can be used
     * for the current change of location. If a previous and subsequent trip exists, one of the modes is chosen and
     * travel time as well as distance are calculated.
     *
     * @param pComingFrom location coming from
     * @param pGoingTo    location going to
     */
    public boolean setTravelTime(TPS_LocatedStay pComingFrom, TPS_LocatedStay pGoingTo) {
        // Do not get confused with 'pComingFrom' and 'myPComingFrom'. 'pComingFrom' is needed for the hierarchy of the
        // trips in the tour.
        if (pComingFrom.getStay().isAtHome() && pGoingTo.getStay().isAtHome() && mode == null) {
            // This shouldn't be a big problem. Skip the message; Use mode 'walk' as this are most of the time walks with dogs.
            mode = TPS_ExtMode.simpleWalk;
        }

        if (!pComingFrom.isLocated()) {
            throw new RuntimeException("tpsTrip.setTravelTime: missing location of the preceding stay: " +
                    pComingFrom.getStay().getActCode());
        }

        if (!pGoingTo.isLocated()) {
            throw new RuntimeException("tpsTrip.setTravelTime: missing location of the following stay: " +
                    pGoingTo.getStay().getActCode());
        }

        TPS_Location pLocComingFrom = pComingFrom.getLocation();
        TPS_Location pLocGoingTo = pGoingTo.getLocation();
        this.setDistanceBeeline(TPS_Geometrics
                .getDistance(pLocComingFrom, pLocGoingTo, plan.getParameters().getDoubleValue(ParamValue.MIN_DIST)));

        TPS_TrafficAnalysisZone goingToTVZ = pLocGoingTo.getTrafficAnalysisZone();
        TPS_TrafficAnalysisZone comingFromTVZ = pLocComingFrom.getTrafficAnalysisZone();
        double tt = -1;

        TPS_Car carForTrip = this.plan.getPlanningContext().carForThisPlan;       //null;
        //pick cars used for this plan
//        for (TPS_SchemePart schemePart : this.plan.getScheme()) {
//            if (!schemePart.isHomePart()) {
//                TPS_TourPart tourpart = (TPS_TourPart) schemePart;
//                if (tourpart.getCar() != null) {
//                    carForTrip = tourpart.getCar();
//                    break;
//                }
//            }
//        }


        /*
         * Beachte: pModeComingFrom = mode, vom Stay pComingFrom wegzukommen (getPModeGo) pModeGoingTo = mode, zum Stay
         * pGoingTo hinzukommen (getPModeCome)
         */
        TPS_ExtMode pModeComingFrom = pComingFrom.getModeDep();
        //TPS_ExtMode pModeGoingTo = pGoingTo.getModeArr();

        // If the person comes from home or is going home, then pModeGoingTo or pModeComingFrom are 0. Usually at least
        // one
        // of them should be set. Note that 'pComingFrom' and 'pGoingTo' have a different meaning here compared to
        // 'Stay'.
        // Here they point to the adjacent episodes.

        // If different modes are used for leave and to get to the locations of these episodes then the priority of the
        // episodes determines the mode choice.
//fixme needed?
//        if (pComingFrom.getStay().getPriority() < pGoingTo.getStay().getPriority()) {
//            if (pComingFrom.getStay().isAtHome()) {
//                setMode(pModeGoingTo);
//            } else {
//                setMode(pModeComingFrom);
//            }
//        } else {
//            if (pGoingTo.getStay().isAtHome()) {
//                setMode(pModeComingFrom);
//            } else {
//                setMode(pModeGoingTo);
//            }
//        }

        setMode(pModeComingFrom);
        this.setDistanceEmptyNet(
                TPS_Mode.get(ModeType.WALK).getDistance(pLocComingFrom, pLocGoingTo, SimulationType.SCENARIO, null));

        /*
         * Da anders als in der Vorlage die Klassen für die verschiedenen Modes nicht in einer Datei definiert werden
         * können, bin ich auch bei der Vererbung und dem Zugriff auf die richtige Funktion skeptisch. Deshalb wird hier
         * explizit nach dem Mode unterschieden und per cast die entsprechende Routine genutzt.
         *
         *
         * TYPECAST UNNÖTIG
         *
         *
         * Für die KFZ-modes MIV, Taxi und Passagier werden die entspr. Funktionen getDistance aufgerufen. Da
         * distanceBeeline immer einen minimalen Wert hat, kann es in diesen Funktionen nicht zu Komplikationen kommen.
         *
         * Den getTravelTime-Routinen werden ebenfalls die actCodes der beteiligten Episoden (comingFrom & goingTo) und
         * die Job-ID der Person übergeben. Derzeit werden diese nur für die Modes PubTrans und Train zur Berechnung der
         * Schulbusreisezeiten genutzt.
         */

        TPS_Mode primaryMode = mode.primary;

        if (primaryMode.isType(ModeType.PT)) {
            tt = primaryMode.getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(), SimulationType.SCENARIO,
                    pComingFrom.getStay().getActCode(), pGoingTo.getStay().getActCode(), plan.getPerson(), null);
            if (pLocComingFrom.getTrafficAnalysisZone().equals(pLocGoingTo.getTrafficAnalysisZone())) {
                if (TPS_Mode.noConnection(tt)) {
                    if (this.plan.getParameters().isFalse(ParamFlag.FLAG_USE_BLOCK_LEVEL)) {
                        tt = TPS_Mode.get(ModeType.WALK).getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(),
                                SimulationType.SCENARIO, pComingFrom.getStay().getActCode(),
                                pGoingTo.getStay().getActCode(), plan.getPerson(), null);
                    } else if (this.plan.getParameters().isTrue(ParamFlag.FLAG_USE_BLOCK_LEVEL)) {
                        if (pLocComingFrom.getBlock() == null || pLocGoingTo.getBlock() == null) {
                            tt = TPS_Mode.get(ModeType.WALK).getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(),
                                    SimulationType.SCENARIO, pComingFrom.getStay().getActCode(),
                                    pGoingTo.getStay().getActCode(), plan.getPerson(), null);
                        } else if (pLocComingFrom.getBlock().equals(pLocGoingTo.getBlock())) {
                            tt = TPS_Mode.get(ModeType.WALK).getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(),
                                    SimulationType.SCENARIO, pComingFrom.getStay().getActCode(),
                                    pGoingTo.getStay().getActCode(), plan.getPerson(), null);

                        }
                    }
                }
            }
        } else if (primaryMode.isType(ModeType.TRAIN) && this.plan.getParameters().isDefined(
                ParamFlag.FLAG_USE_CARSHARING) && this.plan.getParameters().isTrue(
                ParamFlag.FLAG_USE_CARSHARING)) { // carsharing faker!
            tt = TPS_Mode.get(ModeType.MIT).getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(),
                    SimulationType.SCENARIO, pComingFrom.getStay().getActCode(), pGoingTo.getStay().getActCode(),
                    plan.getPerson(), null);
        } else {
            tt = primaryMode.getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(), SimulationType.SCENARIO,
                    pComingFrom.getStay().getActCode(), pGoingTo.getStay().getActCode(), plan.getPerson(), carForTrip);
        }

        if (TPS_Mode.noConnection(tt)) { //you can always walk!
            //TODO: really walk?!?
            primaryMode = TPS_Mode.get(ModeType.WALK);
            this.mode.primary = primaryMode;
            tt = primaryMode.getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(), SimulationType.SCENARIO,
                    pComingFrom.getStay().getActCode(), pGoingTo.getStay().getActCode(), plan.getPerson(), null);
        }

        double dist = primaryMode.getDistance(pLocComingFrom, pLocGoingTo, SimulationType.SCENARIO, carForTrip);

        if (primaryMode.isType(ModeType.WALK) && dist / (Math.max(60, tt)) > 10) {
            //runners!
            tt = primaryMode.getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(), SimulationType.SCENARIO,
                    pComingFrom.getStay().getActCode(), pGoingTo.getStay().getActCode(), plan.getPerson(), null);
            dist = primaryMode.getDistance(pLocComingFrom, pLocGoingTo, SimulationType.SCENARIO, carForTrip);

        }

        this.setDistance(primaryMode.getDistance(pLocComingFrom, pLocGoingTo, SimulationType.SCENARIO, carForTrip));
        if (Double.isNaN(tt)) {
            tt = primaryMode.getTravelTime(pLocComingFrom, pLocGoingTo, this.getStart(), SimulationType.SCENARIO,
                    pComingFrom.getStay().getActCode(), pGoingTo.getStay().getActCode(), plan.getPerson(), null);
        }
        this.setDuration((int) tt);

        /*
         * Abschließend werden für jeden Trip in Abhängigkeit vom Modus die Kosten für den Trip berechnet und in der
         * Variablen myFinancialCosts abgelegt; dabei erfolgt die Rückrechnung der Kosten in Euro
         */

        double costVal = 0, costStay = 0;
        if (!primaryMode.isType(ModeType.MIT)) {
            costVal = primaryMode.getCost_per_km(SimulationType.SCENARIO);
        } else {
            // Bestimmen, ob durch Wegfall der Pendlerpauschale betroffen: erwerbstätig und HHEinkommen über 2600 Euro
            double pendlerkosten = 0.0;

            // determine whether working and income over average such that commuting tax benefit is appropriate
            if (this.plan.getPerson().isWorking() &&
                    plan.getPerson().getHousehold().getIncome() >= this.plan.getParameters().getIntValue(
                            ParamValue.MIT_INCOME_CLASS_COMMUTE)) {
                pendlerkosten += this.plan.getParameters().getDoubleValue(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE);
            }

            costVal = pendlerkosten;
            if (carForTrip == null) {
                // default distance related costs
                costVal += this.plan.getParameters().getDoubleValue(ParamValue.MIT_GASOLINE_COST_PER_KM) +
                        this.plan.getParameters().getDoubleValue(ParamValue.MIT_VARIABLE_COST_PER_KM);
            } else {
                // car-depending distance related costs
                costVal += carForTrip.getCostPerKilometer(SimulationType.SCENARIO) +
                        carForTrip.getVariableCostPerKilometer(SimulationType.SCENARIO);

            }

            // location related costs
            // determine whether parking fee or toll is charged in the destination zone; toll only relevant when coming
            // from an toll free zone
            costStay = 0.0;

            // toll fee
            if (goingToTVZ.hasToll(SimulationType.SCENARIO) && !comingFromTVZ.hasToll(SimulationType.SCENARIO)) {
                // Scenario: toll has to be payed on entrance into a toll zone (cordon toll)
                costStay += goingToTVZ.getTollFee(SimulationType.SCENARIO);
            }

            // toll fees leaving a toll zone
            if (this.plan.getParameters().isTrue(ParamFlag.FLAG_USE_EXIT_MAUT)) {
                if (!goingToTVZ.hasToll(SimulationType.SCENARIO) && comingFromTVZ.hasToll(SimulationType.SCENARIO)) {
                    // Scenario: toll has to be payed leaving a toll zone (cordon toll)
                    costStay += comingFromTVZ.getTollFee(SimulationType.SCENARIO);
                    if (TPS_Logger.isLogging(SeverenceLogLevel.FINE)) {
                        TPS_Logger.log(SeverenceLogLevel.FINE,
                                "MIV - trip has maut: " + comingFromTVZ.getTollFee(SimulationType.SCENARIO) +
                                        " costSum: " + costStay);
                    }
                }
            }

            // parking fee
            if (goingToTVZ.hasParkingFee(SimulationType.SCENARIO)) {
                double costSzen = goingToTVZ.getParkingFee(SimulationType.SCENARIO);
                if (costSzen > 0) {
                    double stayingHours = this.getDuration() * 2.7777777777e-4;// converting into seconds
                    costStay += (costSzen * stayingHours);
                }
                if (TPS_Logger.isLogging(SeverenceLogLevel.FINE)) {
                    TPS_Logger.log(SeverenceLogLevel.FINE,
                            "GoingTo has parkingfee of" + goingToTVZ.getParkingFee(SimulationType.SCENARIO) +
                                    "; duration: " + this.getDuration() + "; costSum: " + costSzen);
                }
            }
        }

        costs = (this.getDistance() * 0.001 * costVal) + costStay;
        if (TPS_Logger.isLogging(SeverenceLogLevel.FINE)) {
            TPS_Logger.log(SeverenceLogLevel.FINE, this.mode.toString() + " - trip-costs: " + costs);
            TPS_Logger.log(SeverenceLogLevel.FINE, "Set travel time " + this.toString());
        }

        return (true);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.util.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        return this.getClass().getSimpleName() + "[tripId=" + this.getTrip().getId() + ", mode=" + mode.getName() +
                ", costs=" + costs + ", distances:[" + getDistance() + "," + getDistanceBeeline() + "," +
                getDistanceEmptyNet() + "]]";
    }
}
