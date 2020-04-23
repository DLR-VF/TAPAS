package de.dlr.ivf.tapas.constants;

/**
 * Internal representation of a constant
 * containing name (i.e. description), code and an enum type like TPS_LocationCodeType or TPS_ActivityCodeType
 */
public class TPS_InternalConstant<EnumType extends Enum<EnumType>> {

    private final int code;
    private final String name;
    private final EnumType type;

    /**
     * @param name of the internal constant
     * @param code of the internal constant
     * @param type EnumType of the internal constant
     */
    public TPS_InternalConstant(String name, int code, EnumType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    /**
     * @return return code of the internal constant
     */
    public int getCode() {
        return code;
    }

    /**
     * @return name of the internal constant
     */
    public String getName() {
        return name;
    }

    /**
     * @return EnumType of the internal constant
     */
    public EnumType getType() {
        return type;
    }
}

