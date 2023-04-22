/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util.parameters;

/**
 * This enum provides all parameter types
 */
public enum ParamType {
    /**
     * Type for parameters which contain a table name in the database
     */
    DB(2),

    /**
     * Type for parameters which are run-independent
     */
    DEFAULT(3),

    /**
     * Type for parameters which have to be set as a virtual machine
     * argument
     */
    EXEC(-1),

    /**
     * Type for parameters which contains a name of an input file
     */
    FILE(1),

    /**
     * Type for logging parameters
     */
    LOG(4),

    /**
     * Type for parameters which have to be set for every single run
     */
    RUN(0),

    /**
     * Type for parameters which are optional
     */
    OPTIONAL(5),

    /**
     * Temporary Keys
     */
    TMP(-2);

    private final int index;

    ParamType(int index) {
        this.index = index;
    }

    /**
     * Gets the type index of this parameter
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }
}
