package de.dlr.ivf.tapas.constants;


import java.util.EnumMap;
import java.util.HashMap;

/**
 * Settlement system constants.
 */
public class TPS_SettlementSystem {

    /**
     * maps ids from the db to {@link TPS_SettlementSystem} objects constructed from corresponding db data
     */
    private static HashMap<Integer, TPS_SettlementSystem> SETTLEMENT_SYSTEMS_MAP = new HashMap<>();

    /**
     * mapping of the {@link TPS_SettlementSystemType} to the internal representation
     */
    private EnumMap<TPS_SettlementSystemType, TPS_InternalConstant<TPS_SettlementSystemType>> map;

    /**
     * settlement system id obtained from the db
     */
    private int id;

    /**
     * Basic constructor for a TPS_SettlementSystem
     *
     * @param id         corresponding id from the db
     * @param attributes attributes have to have a length of 3*n where each 3-segment is of the form (name, code,
     *                   type) like ("R1, K1, Kernstadt > 500000", "1", "FORDCP")
     */
    public TPS_SettlementSystem(int id, String[] attributes) {
        this.id = id;
        map = new EnumMap<>(TPS_SettlementSystemType.class);
        TPS_InternalConstant<TPS_SettlementSystemType> iss;

        if (attributes.length % 3 != 0) {
            throw new RuntimeException(
                    "TPS_SettlementSystem need n*3 attributes n*(name, code, type): " + attributes.length);
        }

        for (int index = 0; index < attributes.length; index += 3) {
            iss = new TPS_InternalConstant<>(attributes[index], Integer.parseInt(attributes[index + 1]),
                    TPS_SettlementSystemType.valueOf(attributes[index + 2]));
            this.map.put(iss.getType(), iss);
        }

        for (TPS_SettlementSystemType type : TPS_SettlementSystemType.values()) {
            if (!this.map.containsKey(type)) {
                throw new RuntimeException("SettlementSystem code for type " + type.name() + " not defined");
            }
        }
    }

    /**
     * Empties settlement system map
     */
    public static void clearSettlementSystemMap() {
        SETTLEMENT_SYSTEMS_MAP.clear();
    }

    /**
     * @param type like FORDCP or TAPAS, see {@link TPS_SettlementSystemType}
     * @param code settlement system code from the db
     * @return settlement system object for a given {@link TPS_SettlementSystemType} and code
     */
    public static TPS_SettlementSystem getSettlementSystem(TPS_SettlementSystemType type, int code) {
        for (TPS_SettlementSystem tss : SETTLEMENT_SYSTEMS_MAP.values()) {
            if (tss.getCode(type) == code) {
                return tss;
            }
        }
        return null;
    }

    /**
     * Add to static collection of all settlement system constants.
     * The objects are accessed by their ids from the db.
     */
    public void addSettlementSystemToMap() {
        SETTLEMENT_SYSTEMS_MAP.put(id, this);
    }

    /**
     * Convenience method to obtain the settlement system code for a given {@link TPS_SettlementSystemType}
     *
     * @param type like FORDCP or TAPAS, see {@link TPS_SettlementSystemType}
     * @return the corresponding code as int
     */
    public int getCode(TPS_SettlementSystemType type) {
        return this.map.get(type).getCode();
    }

    /**
     * @return the id corresponding to a settlement system object from the db
     */
    public int getId() {
        return id;
    }

    /**
     * There exist two types of settlement system declarations. The FOBIRD is the official hierarchy of the settlement
     * systems. This hierarchy is modified for TAPAS.
     */
    public enum TPS_SettlementSystemType {
        /**
         * Settlement system
         * source: Federal Office For Building Industry And Regional Development [FOBIRD = BBR (german: Bundesamt für
         * Bauwesen und Raumordnung)]
         */
        FORDCP,
        /**
         * Modified settlement system for TAPAS
         */
        TAPAS
    }
}
