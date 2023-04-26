/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.util.list;

import javax.swing.*;

/**
 * This DefaultSelectionModel subclass enables SINGLE_SELECTION mode and
 * overrides setSelectionInterval so that the first selection update in a
 * gesture (like mouse press, drag, release) toggles the current selection
 * state. A "gesture" starts when the first update to the selection model
 * occurs, and the gesture ends when the isAdjusting ListSelectionModel property
 * is set to false.
 */
public class ToggleSelectionModel extends DefaultListSelectionModel {
    /**
     *
     */
    private static final long serialVersionUID = 8023750036022263174L;
    boolean gestureStarted = false;

    public void setSelectionInterval(int index0, int index1) {
        if (isSelectedIndex(index0) && !gestureStarted) {
            super.removeSelectionInterval(index0, index1);
        } else {
            super.setSelectionInterval(index0, index1);
        }
        gestureStarted = true;
    }

    public void setValueIsAdjusting(boolean isAdjusting) {
        if (!isAdjusting) {
            gestureStarted = false;
        }
    }
}
