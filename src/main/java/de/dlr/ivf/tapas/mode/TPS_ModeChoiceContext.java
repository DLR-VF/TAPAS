/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.mode;

import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.person.TPS_Car;
import de.dlr.ivf.tapas.scheme.TPS_Stay;

public class TPS_ModeChoiceContext {

    public TPS_Stay fromStay, toStay; // !!! fromStay not always set
    //public TPS_LocatedStay fromStayLocated, toStayLocated;
    public TPS_Location fromStayLocation, toStayLocation;
    public double duration;
    public boolean isBikeAvailable;
    public TPS_Car carForThisPlan;
    public int startTime;
    public TPS_Mode combinedMode = null;
    public TPS_ModeChoiceContext() {
    }

}
