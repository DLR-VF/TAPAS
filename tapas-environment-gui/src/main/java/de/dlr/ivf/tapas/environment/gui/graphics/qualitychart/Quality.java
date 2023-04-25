/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.graphics.qualitychart;

import java.awt.*;

public enum Quality {
    NOT_SET(Color.GRAY), //
    BAD(Color.RED), //
    MEDIUM(Color.ORANGE), //
    GOOD(Color.GREEN);

    private final Color color;

    Quality(Color color) {
        this.color = color;
    }

    public static Quality getById(int id) {
        return values()[id];
    }

    public Color getColor() {
        return color;
    }
}
