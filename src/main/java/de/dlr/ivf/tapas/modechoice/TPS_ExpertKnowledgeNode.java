package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;

import java.util.Collection;

public class TPS_ExpertKnowledgeNode extends TPS_Node {

    /**
     * mode distribution
     */
    private final TPS_DiscreteDistribution<TPS_Mode> modificationFactor;

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
    public TPS_ExpertKnowledgeNode(int id, TPS_Attribute splitVariable, Collection<Integer> attributeValues, TPS_DiscreteDistribution<TPS_Mode> summand, TPS_DiscreteDistribution<TPS_Mode> factor, TPS_Node parent) {
        super(id, splitVariable, attributeValues, summand, parent);

        this.modificationFactor = factor;

    }

    /**
     * Function to generate a new ProbabilityDistribution. The input will be cloned. The out put suffices: output[mode] = input[mode]*factor[mode]+summand[mode]
     *
     * @param input The reference probability distribution
     * @return a new instance of probability distribution according to the factors and summands.
     */
    public TPS_DiscreteDistribution<TPS_Mode> modifyDistribution(TPS_DiscreteDistribution<TPS_Mode> input) {
        TPS_DiscreteDistribution<TPS_Mode> modification = new TPS_DiscreteDistribution<>(TPS_Mode.getConstants());
        for (TPS_Mode mode : TPS_Mode.getConstants()) {
            modification.setValueByKey(mode, input.getValueByKey(mode) * this.modificationFactor.getValueByKey(mode) +
                    this.distribution.getValueByKey(mode));
        }

        return modification;
    }


}
