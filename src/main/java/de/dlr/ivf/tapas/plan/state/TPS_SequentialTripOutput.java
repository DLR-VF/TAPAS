package de.dlr.ivf.tapas.plan.state;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.plan.TPS_PlannedTrip;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.OptionalInt;

public class TPS_SequentialTripOutput {

    private PreparedStatement ps;
    private TPS_DB_IOManager pm;

    public TPS_SequentialTripOutput(String statement, TPS_DB_IOManager pm) throws SQLException {
        this.ps = pm.getDbConnector().getConnection(this).prepareStatement(statement);
        this.pm = pm;
    }

    public void addTripOutput(TPS_Plan plan, TPS_TourPart tp, TPS_Trip trip){

        int index = 1;
        int lastStart = -10000000;
        int start;


        try {
            ps.setInt(index++, plan.getPerson().getId());
            ps.setInt(index++, plan.getPerson().getHousehold().getId());
            ps.setInt(index++, plan.getScheme().getId());
            if (Double.isNaN(plan.getAcceptanceProbability()) || Double.isInfinite(
                plan.getAcceptanceProbability())) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                        "NaN detected in getAcceptanceProbability for person " + plan.getPerson().getId());
            }
            ps.setDouble(index++, plan.getAcceptanceProbability());
            if (Double.isNaN(plan.getBudgetAcceptanceProbability()) || Double.isInfinite(
                plan.getBudgetAcceptanceProbability())) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getBudgetcceptanceProbability for person " + plan.getPerson().getId());
            }
            ps.setDouble(index++, plan.getBudgetAcceptanceProbability());
            if (Double.isNaN(plan.getTimeAcceptanceProbability()) || Double.isInfinite(
                plan.getTimeAcceptanceProbability())) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getTimeAcceptanceProbability for person " + plan.getPerson().getId());
            }
            ps.setDouble(index++, plan.getTimeAcceptanceProbability());

            TPS_Stay prevStay = tp.getPreviousStay(trip);
            TPS_Stay nextStay = tp.getNextStay(trip);
            TPS_Location prevLoc = plan.getLocatedStay(prevStay).getLocation();
            TPS_LocatedStay nextStayLocated = plan.getLocatedStay(nextStay);
            TPS_Location nextLoc = nextStayLocated.getLocation();
            ps.setInt(index++, prevLoc.getTrafficAnalysisZone().getTAZId());
            ps.setBoolean(index++,
            prevLoc.getTrafficAnalysisZone().hasToll(pm.getParameters().getSimulationType()));
            ps.setInt(index++, (prevLoc.hasBlock() ? prevLoc.getBlock().getId() : -1));
            ps.setInt(index++, prevLoc.getId());
            ps.setDouble(index++, prevLoc.getCoordinate().getValue(0));
            ps.setDouble(index++, prevLoc.getCoordinate().getValue(1));

            ps.setInt(index++, nextLoc.getTrafficAnalysisZone().getTAZId());
            ps.setBoolean(index++,
                nextLoc.getTrafficAnalysisZone().hasToll(pm.getParameters().getSimulationType()));
            ps.setInt(index++, (nextLoc.hasBlock() ? nextLoc.getBlock().getId() : -1));
            ps.setInt(index++, nextLoc.getId());
            ps.setDouble(index++, nextLoc.getCoordinate().getValue(0));
            ps.setDouble(index++, nextLoc.getCoordinate().getValue(1));

            TPS_PlannedTrip pt = plan.getPlannedTrip(trip);
            start = pt.getStart();
            if (start - lastStart < 1.0) {
                start = lastStart + 1;
            }
            //lastStart = start;

            ps.setInt(index++, (int) ((start * 1.66666666e-2) + 0.5)); //sec to min incl round
            if (Double.isNaN(pt.getDuration()) || Double.isInfinite(pt.getDuration())) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getDuration for person " + plan.getPerson().getId());
            }
            ps.setDouble(index++, pt.getDuration());
            ps.setInt(index++, pt.getMode().getMCTCode());
            ps.setInt(index++, tp.getCar() == null ? -1 : tp.getCar().getId());
            if (Double.isNaN(pt.getDistanceBeeline()) || Double.isInfinite(pt.getDistanceBeeline())) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getDistanceBeeline for person " + plan.getPerson().getId());
            }
            ps.setDouble(index++, pt.getDistanceBeeline());
            if (Double.isNaN(pt.getDistance()) || Double.isInfinite(pt.getDistance())) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.FATAL,
                    "NaN detected in getDistance for person " + plan.getPerson().getId());
            }
            ps.setDouble(index++, pt.getDistance());
            ps.setInt(index++, nextStay.getActCode().getCode(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE));
            ps.setBoolean(index++, nextStay.isAtHome());
            ps.setInt(index++,
                (int) ((nextStayLocated.getStart() * 1.66666666e-2) + 0.5)); // secs to min incl round
            int duration = nextStayLocated.getDuration();
            //sum durations of concurring stays
            while (tp.getNextEpisode(nextStay).isStay()) {
                nextStay = (TPS_Stay) tp.getNextEpisode(nextStay);
                duration += plan.getLocatedStay(nextStay).getDuration();
            }
            ps.setInt(index++, (int) ((duration * 1.66666666e-2) + 0.5)); // secs to min incl round
            if (tp.getCar() != null) {
                ps.setInt(index++, tp.getCar().index);
                ps.setBoolean(index++, tp.getCar().isRestricted());
            } else {
                ps.setInt(index++, -1);
                ps.setBoolean(index++, false);
            }
            ps.setInt(index++, plan.getPerson().getPersGroup().getCode());
            ps.setInt(index++,
                prevLoc.getTrafficAnalysisZone().getBbrType().getCode(TPS_SettlementSystem.TPS_SettlementSystemType.FORDCP));
            ps.setInt(index++, plan.getPerson().getHousehold().getLocation().getTrafficAnalysisZone().getBbrType()
                .getCode(TPS_SettlementSystem.TPS_SettlementSystemType.FORDCP));
            ps.setInt(index++, nextStay.locationChoiceMotive.code);// the location_selection_motive
            ps.setInt(index++, nextStay.locationChoiceMotiveSupply.code);// the location_selection_motive
            ps.addBatch();
            ps.clearParameters();
        } catch (SQLException e) {
            e.printStackTrace();
        }



    }

    public boolean persistTrips(){

        try {
            OptionalInt optional_execution_error = Arrays.stream(ps.executeBatch())
                                                         .filter(i -> i == PreparedStatement.EXECUTE_FAILED)
                                                         .findFirst();

            if (optional_execution_error.isPresent()) {
                TPS_Logger.log(TPS_LoggingInterface.SeverenceLogLevel.ERROR, "Storing of trips failed!");
                return false;
            }
            ps.clearBatch();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
