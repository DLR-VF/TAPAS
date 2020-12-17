/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.loc;

public class TPS_SingleTAZRepresentant extends TPS_MultipleTAZRepresentant {
    public TPS_SingleTAZRepresentant() {
        super.numOfTazRepresentants = 1;
    }
}
