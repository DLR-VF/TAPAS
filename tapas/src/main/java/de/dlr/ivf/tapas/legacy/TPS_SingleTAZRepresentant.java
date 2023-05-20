/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

public class TPS_SingleTAZRepresentant extends TPS_MultipleTAZRepresentant {
    public TPS_SingleTAZRepresentant(TPS_ParameterClass parameterClass) {
        super(parameterClass);
        super.numOfTazRepresentants = 1;
    }
}
