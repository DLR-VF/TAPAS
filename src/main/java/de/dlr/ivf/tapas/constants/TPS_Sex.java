package de.dlr.ivf.tapas.constants;

/**
 * Sex constant.
 */
public enum TPS_Sex {

    /**
     * All possible values for the sex.
     */

    NON_RELEVANT(0), //resolves to ordinal 0
    MALE(1), //resolves to ordinal 1
    FEMALE(2), //resolves to ordinal 2
    UNKNOWN(3); //resolves to ordinal 3

    public int code;

    TPS_Sex(int code) {
        this.code = code;
    }

    public static TPS_Sex getEnum(int code) {
        for (TPS_Sex s : TPS_Sex.values()) {
            if (s.code == code) return s;
        }
        return TPS_Sex.UNKNOWN;
    }

    public int getCode() {
        return code;
    }
}
