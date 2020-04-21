package de.dlr.ivf.tapas.util.parameters;

/**
 * These enum constants provide factors to convert values between different
 * currencies.
 *
 * @author mark_ma
 */
public enum CURRENCY {/**
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
    private double factor;

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
    }}