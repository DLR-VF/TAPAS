/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.scheme;

import de.dlr.ivf.tapas.model.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;

/**
 * This class indicates a trip which is part of a tour part.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_Trip extends TPS_Episode {


    public TPS_Trip(int id, TPS_ActivityConstant actCode, int start, int duration){
        super(id,actCode,start,duration);
    }


    /**
     * constructor
     *
     * @param id       id of the trip
     * @param actCode  activity code of the trip, e.g. 80. Don't confuse with the tour number. The tour number is only stored
     *                 in the scheme.
     * @param start    start time of the trip in seconds
     * @param duration original duration of the trip determined by the scheme in seconds
     */
    public TPS_Trip(int id, TPS_ActivityConstant actCode, int start, int duration, double startEarlier, double startLater,
                    double durationMinus, double durationPlus, double scaleShift, double scaleStretch){
        super(id, actCode, start, duration, start - startEarlier,start + startLater,
                duration *durationMinus,duration * durationPlus, scaleShift, scaleStretch);
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