package de.dlr.ivf.tapas.util.parameters;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.EnumMap;

public class ParamFlagClass {
    private final EnumMap<ParamFlag, MutablePair<ParamType, Boolean>> paramFlags;

    ParamFlagClass() {
        this.paramFlags = new EnumMap<>(ParamFlag.class);
        this.paramFlags.put(ParamFlag.FLAG_CHECK_BUDGET_CONSTRAINTS, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_DELETE_TEMPORARY_TABLES, new MutablePair<>(ParamType.DB, true));
        this.paramFlags.put(ParamFlag.FLAG_DELETE_TEMPORARY_VISUM_FILES, new MutablePair<>(ParamType.RUN, true));
        this.paramFlags.put(ParamFlag.FLAG_INFLUENCE_RANDOM_NUMBER, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_INTRA_INFOS_MATRIX, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_LOCATION_POCKET_COSTS, new MutablePair<>(ParamType.RUN, true));
        this.paramFlags.put(ParamFlag.FLAG_PREFETCH_ALL_HOUSEHOLDS, new MutablePair<>(ParamType.OPTIONAL, false));
        this.paramFlags.put(ParamFlag.FLAG_REJUVENATE_RETIREE, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_RESTRICT_TOLL_ZONE, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_RUN_SZENARIO, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_BY_WORK_AT_HOME,
                new MutablePair<>(ParamType.DEFAULT, false));
        this.paramFlags.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_BY_WORKINGCHAINS,
                new MutablePair<>(ParamType.DEFAULT, false));
        this.paramFlags.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_SELECTION_PROBS,
                new MutablePair<>(ParamType.DEFAULT, false));
        this.paramFlags.put(ParamFlag.FLAG_SELECT_LOCATIONS_DIFF_PERSON_GROUP,
                new MutablePair<>(ParamType.DEFAULT, false));
        this.paramFlags.put(ParamFlag.FLAG_SEQUENTIAL_EXECUTION, new MutablePair<>(ParamType.RUN,false));
        this.paramFlags.put(ParamFlag.FLAG_USE_SHOPPING_MOTIVES, new MutablePair<>(ParamType.DEFAULT, false));
        this.paramFlags.put(ParamFlag.FLAG_FILTER_SHOPPING_CHOICE_SET, new MutablePair<>(ParamType.DEFAULT, true));
        this.paramFlags.put(ParamFlag.FLAG_UPDATE_LOCATION_WEIGHTS, new MutablePair<>(ParamType.RUN, true));
        this.paramFlags.put(ParamFlag.FLAG_USE_BLOCK_LEVEL, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_DRIVING_LICENCE, new MutablePair<>(ParamType.RUN, true));
        this.paramFlags.put(ParamFlag.FLAG_USE_EXIT_MAUT, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_CHARGE_PASSENGERS_WITH_EVERYTHING,
                new MutablePair<>(ParamType.OPTIONAL, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_FIXED_LOCS_ON_BASE, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_SCHOOLBUS, new MutablePair<>(ParamType.DEFAULT, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_CARSHARING, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_ROBOTAXI, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_INTERMODAL, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_TAXI, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_FIX_PT_COSTS, new MutablePair<>(ParamType.RUN, false));
        this.paramFlags.put(ParamFlag.FLAG_USE_LOCATION_GROUPS, new MutablePair<>(ParamType.OPTIONAL, true));
    }

    /**
     * clears the flag value to the default flag
     */
    public void clear(ParamFlag param) {
        this.setFlag(param, new ParamFlagClass().getFlag(param));
    }

    /**
     * Gets the flag of the param parameter
     *
     * @param param enum parameter of ParamFlag
     * @return bool value of param
     */
    public boolean getFlag(ParamFlag param) throws RuntimeException {
        if (!this.isDefined(param)) throw new RuntimeException("Enum-Flag is not defined: " + param);
        return this.paramFlags.get(param).getRight();
    }

    /**
     * TODO ugly hack maybe improve
     *
     * @param param flag parameter enum
     * @return the default boolean value of the enum
     */
    public Boolean getPreset(ParamFlag param) {
        try {
            return new ParamFlagClass().getFlag(param);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * @return parameter type
     */
    public ParamType getType(ParamFlag param) {
        return this.paramFlags.get(param).getLeft();
    }

    /**
     * checks if the parameter is not null
     *
     * @param param parameter to be checked
     * @return true if defined, false otherwise
     */
    public boolean isDefined(ParamFlag param) {
        return this.paramFlags.get(param).getRight() != null;
    }

    /**
     * States if the parameter is defined and false.
     *
     * @param param Parameter enum ParamFlag
     * @return true if flag is false
     * @throws RuntimeException This exception is thrown if the constant
     *                          was not defined
     */
    public boolean isFalse(ParamFlag param) {
        if (this.paramFlags.get(param).getRight() == null) throw new RuntimeException(
                "Enum-Flag is not defined: " + param);
        return !this.paramFlags.get(param).getRight();
    }

    /**
     * States if the parameter is defined and true.
     *
     * @param param Parameter enum ParamFlag
     * @return true if flag is true
     * @throws RuntimeException This exception is thrown if the constant
     *                          was not defined
     */
    public boolean isTrue(ParamFlag param) {
        if (this.paramFlags.get(param).getRight() == null) throw new RuntimeException(
                "Enum-Flag is not defined: " + param);
        return this.paramFlags.get(param).getRight();
    }

    /**
     * Sets the flag of this parameter
     *
     * @param param enum parameter to be set
     * @param flag  flag value
     */
    public void setFlag(ParamFlag param, boolean flag) {
        this.paramFlags.get(param).setRight(flag);
    }
}