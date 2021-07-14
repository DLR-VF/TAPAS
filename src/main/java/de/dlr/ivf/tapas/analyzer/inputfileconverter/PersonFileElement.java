/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.util.HashMap;

/**
 * Representation of an element in a personFile style.
 */
public class PersonFileElement {
    // TODO: Declare all constants as type-safe enumerations
    /**
     * Sex defines an enumeration to realize type-safe constant declaration
     */

    private static HashMap<String, Integer> keyMapping;

    // final da diese Objekte nicht geändert werden können, keine Attribute
    // auslassen, um "Verschiebefehler" schnell zu finden

    private String[] values;

    // public static void setKeyMapping(HashMap<String, Integer> keyMapping) {
    // TapasTrip.keyMapping = keyMapping;
    // }
    public PersonFileElement() {

    }

    public void generateKeyMap(String[] headerArray) {
        keyMapping = new HashMap<>();
        for (int i = 0; i < headerArray.length; i++) {
            keyMapping.put(headerArray[i], i);
        }
    }

    /**
     * @return the Personengruppe
     */
    public String getPersonengruppe() {
        return values[keyMapping.get("Personengruppe")];
    }

    /**
     * @return the Personenzahl
     */

    public int getPersonenzahl() {
        return Integer.parseInt(values[keyMapping.get("Personenzahl")]);
    }

    public void setValues(String[] values) {
        this.values = values;
    }

}
