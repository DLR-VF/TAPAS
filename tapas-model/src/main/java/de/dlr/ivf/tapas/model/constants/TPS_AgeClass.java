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

import java.util.Collection;
import java.util.EnumMap;

/**
 * This class represents the age classes constants. There exist two layers of distributions. The first layer should include a
 * continuous amount of intervals e.g. [10,15][16,18] and so on. The second layer consists of one constant, the NON-RELEVANT
 * constant. It includes all ages from 0 to infinite. When there exists no appropriate age class in the first layer, this
 * constant is used as the default age class.
 */
@Builder
@Getter
public class TPS_AgeClass {

    /**
     * Included maximum vale
     */
    private final int max;

    /**
     * Included minimum value
     */
    private final int min;
    /**
     * id obtained from the db
     */
    private final int id;
    /**
     * mapping of the {@link TPS_AgeCodeType} to the internal representation
     */
    private final EnumMap<TPS_AgeCodeType, TPS_InternalConstant<TPS_AgeCodeType>> internalAgeConstants;

    @Singular
    private final Collection<TPS_InternalConstant<TPS_AgeCodeType>> attributes;

    /**
     * Convenience method to obtain the age class code for a given {@link TPS_AgeCodeType}
     *
     * @param type like STBA or PersGroup, see {@link TPS_AgeCodeType}
     * @return the corresponding code as int
     */
    public int getCode(TPS_AgeCodeType type) {
        return this.internalAgeConstants.get(type).getCode();
    }

    /**
     * There exist two types of age classes. The official distribution from the FSO and the modified one for the TAPAS
     * person groups.
     */
    public enum TPS_AgeCodeType {
        /**
         * Special distribution used in TAPAS for the person groups
         */
        PersGroup,
        /**
         * Age class distribution<br>
         * source: Federal Statistical Office [FSO = STBA (german: 'Statistisches Bundesamt')]
         */
        STBA
    }

}
