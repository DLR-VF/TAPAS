package de.dlr.ivf.tapas.scheme;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.execution.sequential.statemachine.EpisodeType;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

/**
 * This class indicates a trip which is part of a tour part.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_Trip extends TPS_Episode {


    /**
     * constructor
     *
     * @param id       id of the trip
     * @param actCode  activity code of the trip, e.g. 80. Don't confuse with the tour number. The tour number is only stored
     *                 in the scheme.
     * @param start    start time of the trip in seconds
     * @param duration original duration of the trip determined by the scheme in seconds
     */
    public TPS_Trip(int id, TPS_ActivityConstant actCode, int start, int duration, TPS_ParameterClass parameterClass) {
        super(id, actCode, start, duration, start - parameterClass.getDoubleValue(ParamValue.DELTA_START_EARLIER),
                start + parameterClass.getDoubleValue(ParamValue.DELTA_START_LATER),
                duration * parameterClass.getDoubleValue(ParamValue.RELATIVE_DURATION_SHORTER),
                duration * parameterClass.getDoubleValue(ParamValue.RELATIVE_DURATION_LONGER), parameterClass);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.plan.TPS_Episode#isStay()
     */
    @Override
    public boolean isStay() {
        return false;
    }

    @Override
    public EpisodeType getEpisodeType() {
        return EpisodeType.TRIP;
    }

}
