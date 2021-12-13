package de.dlr.ivf.tapas.execution.sequential.io;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.execution.sequential.context.PlanContext;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.mode.TPS_ExtMode;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import de.dlr.ivf.tapas.util.FuncUtils;

/**
 * This class is a data structure wrapper for all trips to be written to any output writer.
 */

public class TPS_WritableTrip {
    private TPS_PersistenceManager pm;
    private PlanContext plan_context;
    private TourContext tour_context;
    private TPS_Plan plan;
    private TPS_Trip trip;
    private TPS_Car used_car;
    private TPS_Stay nextStay;
    private TPS_Location prevLoc;
    private TPS_LocatedStay nextStayLocated;
    private TPS_Location nextLoc;

    public TPS_WritableTrip(PlanContext plan_context, TourContext tour_context, TPS_PersistenceManager pm){
        this.pm = pm;
        this.plan_context = plan_context;
        this.tour_context = tour_context;
        this.plan = plan_context.getPlan();
        this.used_car = getUsedCar(plan);
        TPS_Stay prevStay = tour_context.getCurrentStay();
        this.nextStay = tour_context.getNextStay();
        this.prevLoc = tour_context.getCurrentLocation();
        this.nextStayLocated = this.plan.getLocatedStay(nextStay);
        this.nextLoc = nextStayLocated.getLocation();
        this.trip = tour_context.getNextTrip();
    }

    private TPS_Car getUsedCar(TPS_Plan plan) {
        TPS_PlanningContext pc = plan.getPlanningContext();
        return pc.getHouseHoldCar() == null ? pc.getCarSharingCar() : pc.getHouseHoldCar();
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
        return FuncUtils.secondsToRoundedMinutes.apply(plan.getPlannedTrip(trip).getStart()); //sec to min incl round
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
        TPS_Mode.ModeType mode = plan.getPlannedTrip(trip).getMode().primary.getModeType();
        if(mode == TPS_Mode.ModeType.MIT || mode == TPS_Mode.ModeType.CAR_SHARING)
            return this.used_car == null ? -1 : used_car.getId();
        else
            return 9999999;
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
        return used_car != null ? used_car.getId() : 99999;
    }

    public boolean getIsRestrictedCar(){
        return used_car != null && used_car.isRestricted();
    }

    public int getPersonGroup(){
        return  plan.getPerson().getPersonGroup().getCode();
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
