/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.legacy;

import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;

import java.util.Collection;

public class TPS_ExpertKnowledgeNode extends TPS_Node {

    /**
     * mode distribution
     */
    private final TPS_DiscreteDistribution<TPS_Mode> modificationFactor;
    private final Collection<TPS_Mode> modes;

    /**
     * Constructor for a TPS_ExpertKnowledge node. It stores the factor and the summand for modifying a given modal split: new = summand+original*factor
     *
     * @param id              The id for this node
     * @param splitVariable   the current split variable
     * @param attributeValues the attribute values of this node
     * @param summand         the summands of this distribution modification
     * @param factor          the factors of this distribution modification
     * @param parent          a link to the parent node
     */
    public TPS_ExpertKnowledgeNode(int id, TPS_Attribute splitVariable, Collection<Integer> attributeValues, TPS_DiscreteDistribution<TPS_Mode> summand, TPS_DiscreteDistribution<TPS_Mode> factor, TPS_Node parent,Collection<TPS_Mode> modes) {
        super(id, splitVariable, attributeValues, summand, parent);

        this.modificationFactor = factor;
        this.modes = modes;

    }

    /**
     * Function to generate a new ProbabilityDistribution. The input will be cloned. The out put suffices: output[mode] = input[mode]*factor[mode]+summand[mode]
     *
     * @param input The reference probability distribution
     * @return a new instance of probability distribution according to the factors and summands.
     */
    public TPS_DiscreteDistribution<TPS_Mode> modifyDistribution(TPS_DiscreteDistribution<TPS_Mode> input) {

        TPS_DiscreteDistribution<TPS_Mode> modification = new TPS_DiscreteDistribution<>(modes);
        for (TPS_Mode mode : modes) {
            modification.setValueByKey(mode, input.getValueByKey(mode) * this.modificationFactor.getValueByKey(mode) +
                    this.distribution.getValueByKey(mode));
        }

        return modification;
    }


}
