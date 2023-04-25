/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util;

import java.util.Vector;

public class Permuter {


    public static <T> void permute(T[] a, int k, Vector<T[]> into) {
        if (k == a.length) {
            into.add(a.clone());
        } else {
            for (int i = k; i < a.length; i++) {
                T temp = a[k];
                a[k] = a[i];
                a[i] = temp;
                permute(a, k + 1, into);
                temp = a[k];
                a[k] = a[i];
                a[i] = temp;
            }
        }
    }
}
