package de.dlr.ivf.tapas.constants;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * This class represents the constants for the distances. They are all stored in one distribution to retrieve the best
 * fitting distance constant to each distant value.
 */
public class TPS_Distance {

    /**
     * DISTANCE_MAP maps ids to TPS_Distance objects containing (name, code, type)
     */
    private static HashMap<Integer, TPS_Distance> DISTANCE_MAP = new HashMap<>();
    /**
     * MAX_VALUES maps max values of distance to ids (=keys in DISTANCE_MAP)
     */
    private static TreeMap<Integer, Integer> MAX_VALUES = new TreeMap<>();


    /**
     * maximum value of a distance category
     */
    private int max;
    /**
     * id corresponding to the db entry
     */
    private int id;
    /**
     * mapping of distance code types to (name, code, type) triples
     * Example:
     * VOT -> (under 10k, 2, VOT)
     * MCT -> (under 7k, 7000, MCT)
     */
    private EnumMap<TPS_DistanceCodeType, TPS_InternalConstant<TPS_DistanceCodeType>> map;

    /**
     * Basic constructor for a TPS_Distance object
     *
     * @param id         corresponding to the db entry
     * @param attributes list of triples like (name, code, type)
     * @param max        maximum value of the distance constant category
     */
    public TPS_Distance(int id, String[] attributes, int max) {
        if (attributes.length % 3 != 0) {
            throw new RuntimeException("TPS_Distance need n*3 attributes n*(name, code, type): " + attributes.length);
        }
        TPS_InternalConstant<TPS_DistanceCodeType> tic;

        this.id = id;
        this.map = new EnumMap<>(TPS_DistanceCodeType.class);
        this.max = max;

        for (int i = 0; i < attributes.length; i += 3) {
            tic = new TPS_InternalConstant<>(attributes[i], Integer.parseInt(attributes[i + 1]),
                    TPS_DistanceCodeType.valueOf(attributes[i + 2]));
            this.map.put(tic.getType(), tic);
        }

        for (TPS_DistanceCodeType type : TPS_DistanceCodeType.values()) {
            if (!this.map.containsKey(type)) {
                throw new RuntimeException(
                        "Distance code for " + type.name() + " for type " + type.name() + " not defined");
            }
        }
    }

    /**
     * Empties the global static distance map and the max values map
     */
    public static void clearDistanceMap() {
        DISTANCE_MAP.clear();
        MAX_VALUES.clear();
    }

    /**
     * This method searches the distance value which is the next greater (or equal) than the given value. To this
     * constant the code is returned.
     *
     * @param type     distance type (enum)
     * @param distance determines the corresponding distance category, e.g. distance=2.3km corresponds to category
     *                 'under 5km'
     * @return code corresponding to the given enumeration type and the distance
     */
    public static int getCode(TPS_DistanceCodeType type, double distance) {
        return DISTANCE_MAP.get(MAX_VALUES.ceilingEntry((int) distance).getValue()).getCode(type);
    }

    /**
     * Add distance instance to the global static distance map
     * Additionally add (max_val, id)-key-value-pair to the MAX_VALUES map for better searchability of the DISTANCE_MAP
     */
    public void addDistanceToMap() {
        DISTANCE_MAP.put(this.id, this);
        MAX_VALUES.put(this.max, this.id);
    }

    /**
     * Returns the distance code for this object for a given distance code type
     *
     * @param type distance code type like MCT or VOT
     * @return returns the distance code
     */
    public int getCode(TPS_DistanceCodeType type) {
        return this.map.get(type).getCode();
    }


    /**
     * There exist two different distributions for the distances. One for the mode choice and one for the values of
     * time.
     */
    public enum TPS_DistanceCodeType {
        /**
         * Distance distribution for the mode choice tree
         */
        MCT,
        /**
         * Distance distribution for the values of time
         */
        VOT
    }


}
