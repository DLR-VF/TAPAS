/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.constants;


import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * Settlement system constants.
 */
@Builder
@Getter
public class TPS_SettlementSystem {

    /**
     * maps ids from the db to {@link TPS_SettlementSystem} objects constructed from corresponding db data
     */
    private static final HashMap<Integer, TPS_SettlementSystem> SETTLEMENT_SYSTEMS_MAP = new HashMap<>();

    /**
     * mapping of the {@link TPS_SettlementSystemType} to the internal representation
     */
    private final EnumMap<TPS_SettlementSystemType, TPS_InternalConstant<TPS_SettlementSystemType>> map;

    @Singular
    private final Collection<TPS_InternalConstant<TPS_SettlementSystemType>> internalConstants;
    /**
     * settlement system id obtained from the db
     */
    private final int id;

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
