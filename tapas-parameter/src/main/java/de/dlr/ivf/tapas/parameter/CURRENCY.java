/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.parameter;

/**
 * These enum constants provide factors to convert values between different
 * currencies.
 *
 * @author mark_ma
 */
public enum CURRENCY {
    /**
     * German currency until 2002: German Mark (Deutsche Mark)
     */
    DM(1),
    /**
     * European currency: EURO
     */
    EUR(1.95583);

    /**
     * Factor to convert between currencies
     */
    private final double factor;

    /**
     * Constructor sets the factor
     *
     * @param factor
     */
    CURRENCY(double factor) {
        this.factor = factor;
    }

    /**
     * This method converts the given value in the given currency into the
     * currency of this constant
     *
     * @param value           value to convert
     * @param currentCurrency the current currency the value is expressed in
     * @return converted value into the currency of this constant
     */
    public double convert(double value, CURRENCY currentCurrency) {
        return value / currentCurrency.factor * this.factor;
    }
}