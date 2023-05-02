/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.fileModifier.persistence;


public interface ITPS_ParameterDAO {
    String[] header = new String[]{"name", "value", "comment"};

    void addAdditionalParameter(String name, String value, String comment);

    void readParameter();

    void writeParameter();
}
