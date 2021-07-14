/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util;

/**
 * Interface for extended writable objects.
 *
 * @author mark_ma
 */
public interface ExtendedWritable {

    /**
     * This method should print the object naturally with the given prefix.
     *
     * @param prefix
     * @return prefix + toString();
     */
    String toString(String prefix);
}
