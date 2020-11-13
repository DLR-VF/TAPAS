/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.inputfileconverter;

public enum Mode {
    WALK(0, "Fuß"), BIKE(1, "Rad"), MIV(2, "Pkw"), MIV_PASS(3, "PkwMf"), TAXI(4, "Taxi"), PT(5, "ÖV"), TRAIN(6, "Zug");
    // WALK(0, "walk"), BIKE(1, "bike"), MIV(2, "MIV"), MIV_PASS(3, "MIV_pass"), TAXI(4, "taxi"), PT(5, "pubTrans"), TRAIN(
    // 6, "train");

    private final int id;
    private final String description;

    Mode(int id, String description) {
        this.id = id;
        this.description = description;

    }

    /**
     * @param id
     * @return den Mode der dieser ID zugeordnet ist
     * @throws IllegalArgumentException geworfen wenn die ID keinem Wegezweck zugeordnet werden konnte
     */
    public static Mode getById(int id) throws IllegalArgumentException {
        for (Mode mode : Mode.values()) {
            if (mode.getId() == id) {
                return mode;
            }
        }

        throw new IllegalArgumentException();
    }

    public String getDescription() {
        return description;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return description;
    }
}