package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

public class TPS_WritableTrip {
    private TPS_PersistenceManager pm;
    private TPS_Plan plan;
    private TPS_TourPart tour_part;
    private TPS_Trip trip;

    private TPS_Stay nextStay;
    private TPS_Location prevLoc;
    private TPS_LocatedStay nextStayLocated;
    private TPS_Location nextLoc;

    public TPS_WritableTrip(TPS_Plan plan, TPS_TourPart tour_part, TPS_Trip trip){
        this.pm = plan.getPM();
        this.plan = plan;
        this.tour_part = tour_part;
        this.trip = trip;

        TPS_Stay prevStay = tour_part.getPreviousStay(trip);
        this.nextStay = tour_part.getNextStay(trip);
        this.prevLoc = plan.getLocatedStay(prevStay).getLocation();
        this.nextStayLocated = plan.getLocatedStay(nextStay);
        this.nextLoc = nextStayLocated.getLocation();
    }

    public int getPersonId(){
        return plan.getPerson().getId();
    }

    public int getHouseholdId(){
        return plan.getPerson().getHousehold().getId();
    }

    public int getSchemeId(){
        return plan.getScheme().getId();
    }

    public double getScoreCombined(){
        if (Double.isNaN(plan.getAcceptanceProbability()) || Double.isInfinite(
                plan.getAcceptanceProbability())) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getAcceptanceProbability for person " + plan.getPerson().getId());
        }
        return plan.getAcceptanceProbability();
    }

    public double getScoreFinance(){
        if (Double.isNaN(plan.getBudgetAcceptanceProbability()) || Double.isInfinite(
                plan.getBudgetAcceptanceProbability())) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getBudgetcceptanceProbability for person " + plan.getPerson().getId());
        }
        return plan.getBudgetAcceptanceProbability();
    }

    public double getScoreTime(){
        if (Double.isNaN(plan.getTimeAcceptanceProbability()) || Double.isInfinite(
                plan.getTimeAcceptanceProbability())) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getTimeAcceptanceProbability for person " + plan.getPerson().getId());
        }
        return plan.getTimeAcceptanceProbability();
    }

    public int getTazIdStart(){
        return prevLoc.getTrafficAnalysisZone().getTAZId();
    }

    public boolean getTazHasTollStart(){
        return prevLoc.getTrafficAnalysisZone().hasToll(pm.getParameters().getSimulationType());
    }

    public int getBlockIdStart(){
        return prevLoc.hasBlock() ? prevLoc.getBlock().getId() : -1;
    }

    public int getLocIdStart(){
        return prevLoc.getId();
    }

    public double getLonStart(){
        return prevLoc.getCoordinate().getValue(0);
    }

    public double getLatStart(){
        return prevLoc.getCoordinate().getValue(1);
    }

    public int getTazIdEnd(){
        return nextLoc.getTrafficAnalysisZone().getTAZId();
    }

    public boolean getTazHasTollEnd(){
        return nextLoc.getTrafficAnalysisZone().hasToll(pm.getParameters().getSimulationType());
    }

    public int getBlockIdEnd(){
        return nextLoc.hasBlock() ? nextLoc.getBlock().getId() : -1;
    }

    public int getLocIdEnd(){
       return  nextLoc.getId();
    }

    public double getLonEnd(){
        return nextLoc.getCoordinate().getValue(0);
    }

    public double getLatEnd(){
        return nextLoc.getCoordinate().getValue(1);
    }

    public int getStartTimeMin(){
        return (int) ((plan.getPlannedTrip(trip).getStart() * 1.66666666e-2) + 0.5); //sec to min incl round
    }

    public double getTravelTimeSec(){
        TPS_PlannedTrip pt = plan.getPlannedTrip(trip);
        if (Double.isNaN(pt.getDuration()) || Double.isInfinite(pt.getDuration())) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getDuration for person " + plan.getPerson().getId());
        }
        return pt.getDuration();
    }

    public int getMode(){
        return plan.getPlannedTrip(trip).getMode().getMCTCode();
    }

    public int getCarType(){
        return tour_part.getCar() == null ? -1 : tour_part.getCar().getId();
    }

    public double getDistanceBlMeter(){
        TPS_PlannedTrip pt = plan.getPlannedTrip(trip);
        if (Double.isNaN(pt.getDistanceBeeline()) || Double.isInfinite(pt.getDistanceBeeline())) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getDistanceBeeline for person " + plan.getPerson().getId());
        }

        return pt.getDistanceBeeline();
    }

    public double getDistanceRealMeter(){
        TPS_PlannedTrip pt = plan.getPlannedTrip(trip);
        if (Double.isNaN(pt.getDistance()) || Double.isInfinite(pt.getDistance())) {
            TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getDistanceBeeline for person " + plan.getPerson().getId());
        }

        return pt.getDistance();
    }

    public int getActivity(){
        return nextStay.getActCode().getCode(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE);
    }

    public boolean getIsAtHome(){
        return nextStay.isAtHome();
    }

    public int getActivityStartMin(){
        return (int) ((nextStayLocated.getStart() * 1.66666666e-2) + 0.5);
    }

    public int getActivityDurationMin(){
        return (int) ((nextStayLocated.getDuration() * 1.66666666e-2) + 0.5);
    }

    public int getCarIndex(){
        return tour_part.getCar() != null ? tour_part.getCar().index : -1;
    }

    public boolean getIsRestrictedCar(){
        return tour_part.getCar() != null && tour_part.getCar().isRestricted();
    }

    public int getPersonGroup(){
        return  plan.getPerson().getPersGroup().getCode();
    }

    public int getTazBbrTypeStart(){
        return prevLoc.getTrafficAnalysisZone().getBbrType().getCode(TPS_SettlementSystem.TPS_SettlementSystemType.FORDCP);
    }

    public int getBbrTypeHome(){
        return plan.getPerson().getHousehold().getLocation().getTrafficAnalysisZone().getBbrType()
                .getCode(TPS_SettlementSystem.TPS_SettlementSystemType.FORDCP);
    }

    public int getLocSelectionMotive(){
        return nextStay.locationChoiceMotive.code;
    }

    public int getLocSelectionMotiveSupply(){
        return nextStay.locationChoiceMotiveSupply.code;
    }

    public TPS_Trip getTrip(){
        return this.trip;
    }

    public TPS_Stay getStay(){
        return this.nextStay;
    }
}
