package de.dlr.ivf.tapas.choice;

import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.model.TPS_AttributeReader;
import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.model.location.TPS_Region;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.parameter.SimulationType;
import de.dlr.ivf.tapas.model.plan.TPS_LocatedStay;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.scheme.TPS_Stay;
import de.dlr.ivf.tapas.model.scheme.TPS_TourPart;

import java.util.function.Supplier;

/**
 * temporary class during the process of refactoring
 */

public class LocationSelector {


    private final TPS_Region region;

    public LocationSelector(TPS_Region region){
        this.region = region;
    }

    /**
     * Selects a location for the stay
     *
     * @param plan day plan to be executed
     * @param pc
     */
    public TPS_LocatedStay selectLocation(TPS_Plan plan, TPS_PlanningContext pc, Supplier<TPS_Stay> coming_from, Supplier<TPS_Stay> going_to, TPS_Stay currentStay) {
        if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
            TPS_Logger.log(SeverityLogLevel.DEBUG,
                    "Start select procedure for stay (id=" + currentStay.getId() + ")");
        }

        TPS_ActivityConstant currentActCode = currentStay.getActCode();
        plan.setAttributeValue(TPS_AttributeReader.TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_MCT,
                currentActCode.getCode(TPS_ActivityConstant.TPS_ActivityCodeType.MCT));
        plan.setAttributeValue(TPS_AttributeReader.TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_VOT,
                currentActCode.getCode(TPS_ActivityConstant.TPS_ActivityCodeType.VOT));
        plan.setAttributeValue(TPS_AttributeReader.TPS_Attribute.CURRENT_EPISODE_ACTIVITY_CODE_TAPAS,
                currentActCode.getCode(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS));

        // can never happen:
//        if (this.isLocated()) {
//            throw new RuntimeException(
//                    "The location should not be set because in a previous call, because we only call this method once for every TPS_LocatedStay");
//        } else if (this.stay.isAtHome()) {
//            throw new RuntimeException(
//                    "The initialisation of the home parts should be done in the constructor of TPS_Plan");
//        }

        // Has to be a tour part because all home parts are at home; home parts will never reach this method
        TPS_TourPart tourpart = (TPS_TourPart) currentStay.getSchemePart();
        TPS_Stay comingFrom = coming_from.get();
        TPS_Stay goingTo = going_to.get();

        TPS_LocatedStay locatedStay = new TPS_LocatedStay(plan,currentStay);

        if (!plan.isLocated(comingFrom) || !plan.isLocated(goingTo)) {
            // this case should be impossible because we now iterate over the priorised stays,
            // so every stay where we can come from or where we go to is located
            throw new IllegalStateException("Found no location for a higher priorised stay");
        }

        if (currentActCode.hasAttribute(TPS_ActivityConstant.TPS_ActivityConstantAttribute.E_COMMERCE_OUT_OF_HOME)) {
            // Is this an activity that (if it not takes place at home anyway) should be executed in the very
            // vicinity of the home residence?
            if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
                TPS_Logger.log(SeverityLogLevel.DEBUG,
                        "Activity: " + currentActCode + " assumed to be performed at home");
            }
            locatedStay.setLocation(plan.getPerson().getHousehold().getLocation());
        } else {
            //TPS_Region region = PM.getRegion();
            locatedStay.setLocation(region.selectLocation(plan, pc, locatedStay, coming_from,going_to));
            if (locatedStay.getLocation() == null) {
                TPS_Logger.log(SeverityLogLevel.ERROR,
                        "End select procedure for stay (id=" + currentStay.getId() + ") with no location");
                //throw new RuntimeException("End select procedure for stay (id=" + this.getStay().getId() + ") with no location");
            } else {
                if (TPS_Logger.isLogging(SeverityLogLevel.DEBUG)) {
                    TPS_Logger.log(SeverityLogLevel.DEBUG,
                            "End select procedure for stay (id=" + currentStay.getId() + ") with location (id=" +
                                    locatedStay.getLocation().getId() + ")");
                }
            }
        }

        // Get distance from home The MIV-mode is used to get distances on the net.
        locatedStay.setDistance(TPS_Mode.get(TPS_Mode.ModeType.MIT)
                .getDistance(plan.getLocatedStay(comingFrom).getLocation(), locatedStay.getLocation(),
                        SimulationType.SCENARIO, null));

        return locatedStay;
    }
}
