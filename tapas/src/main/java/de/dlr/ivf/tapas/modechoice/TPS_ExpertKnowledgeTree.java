/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.mode.TPS_ModeChoiceContext;
import de.dlr.ivf.tapas.mode.TPS_ModeSet;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.SimulationType;

/**
 * This tree describes the different specialisations of the mode distributions. At each node there is a different mode
 * distribution stored
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_ExpertKnowledgeTree extends TPS_ModeChoiceTree {

    /**
     * This constructor builds a mode choice tree without a root
     */
    public TPS_ExpertKnowledgeTree() {
        this(null);
    }

    /**
     * Builds the encapsulating object and sets the root
     *
     * @param root
     */
    public TPS_ExpertKnowledgeTree(TPS_ExpertKnowledgeNode root) {
        super(root);
    }

    /**
     * This method looks for expert knowledge data and applies them for the given distribution set
     *
     * @param modeSet             The used set of mode
     * @param plan                THe actual plan
     * @param distanceNet         the actual net distance
     * @param checkForbiddenModes flag to enable check for forbidden modes
     * @param dist                The distribution to adopt
     */
    public static boolean applyExpertKnowledge(TPS_ModeSet modeSet, TPS_Plan plan, double distanceNet, TPS_ModeChoiceContext mcc, boolean checkForbiddenModes, TPS_DiscreteDistribution<TPS_Mode> dist) {

        if (checkForbiddenModes) {
            //set modes with no service for this connection to zero
            boolean changed = false;
            int numEmpty = 0;
            for (TPS_Mode.ModeType m : TPS_Mode.MODE_TYPE_ARRAY) {
                TPS_Mode mode = TPS_Mode.get(m);
                double tt = mode.getTravelTime(mcc.fromStayLocation, mcc.toStayLocation, mcc.startTime,
                        SimulationType.SCENARIO, TPS_ActivityConstant.DUMMY, TPS_ActivityConstant.DUMMY,
                        plan.getPerson(), mcc.carForThisPlan);
                if (TPS_Mode.noConnection(tt) || (!mcc.isBikeAvailable && mode.isType(TPS_Mode.ModeType.BIKE)) ||
                        (mcc.carForThisPlan == null && mode.isType(TPS_Mode.ModeType.MIT)) || (mode.equals(
                        TPS_Mode.ModeType.TAXI) && mode.getParameters().isFalse(ParamFlag.FLAG_USE_TAXI)) //disable TAXI
                ) { // FIXME TPS_Mode comparison with ModeType is always false, right?
                    dist.setValueByKey(mode, TPS_UtilityFunction.minModeProbability);
                    changed = true;
                }
                if (dist.getValueByKey(mode) == 0) {
                    numEmpty++;
                }
            }
            if (changed || numEmpty == TPS_Mode.MODE_TYPE_ARRAY.length) {
                if (numEmpty == TPS_Mode.MODE_TYPE_ARRAY.length || !dist.normalize()) {
                    if (TPS_Logger.isLogging(HierarchyLogLevel.EPISODE,
                            TPS_LoggingInterface.SeverenceLogLevel.SEVERE)) {
                        TPS_Logger.log(HierarchyLogLevel.EPISODE, TPS_LoggingInterface.SeverenceLogLevel.SEVERE,
                                "\"Check forbidden modes\" erased all possible modes!");
                    }
                    return false;
                }
            }
        }
        //now apply the expert


        TPS_DiscreteDistribution<TPS_Mode> mod = modeSet.getExpertKnowledgeTree().getDistributionSet(plan)
                                                        .modifyDistribution(dist);
        for (int i = 0; i < mod.size(); ++i) {
            dist.setValueByPosition(i, mod.getValueByPosition(i));
        }
        dist.normalize();

        //reducing WALK for long distances!
        double walkShare;
        if (distanceNet > modeSet.getParameters().getIntValue(ParamValue.MAX_WALK_DIST)) {
            walkShare = 0;
            dist.setValueByKey(TPS_Mode.get(TPS_Mode.ModeType.WALK), walkShare);
            return dist.normalize();
        }
        return true;
    }


    /**
     * Searches recursively for the node with the specified id; starting at the node specified
     *
     * @param node node to start searching at
     * @param id   id of the node to find
     * @return the node if found; null else
     */
    protected TPS_ExpertKnowledgeNode find(TPS_Node node, int id) {
        return (TPS_ExpertKnowledgeNode) super.find(node, id);
    }

    /**
     * Returns the node containing the mode choice distribution for the combination of the personal and trip based attributes
     *
     * @param plan day plan
     * @return a node containing the mode choice distribution
     */
    public TPS_ExpertKnowledgeNode getDistributionSet(TPS_Plan plan) {
        return (TPS_ExpertKnowledgeNode) super.getDistributionSet(plan);
    }

    /**
     * Returns the corresponding node to the id specified
     *
     * @param id node id
     * @return node
     */
    public TPS_ExpertKnowledgeNode getNode(int id) {
        return (TPS_ExpertKnowledgeNode) super.find(this.root, id);
    }

    /**
     * Returns the root of the mode choice tree
     *
     * @return root node
     */
    public TPS_ExpertKnowledgeNode getRoot() {
        return (TPS_ExpertKnowledgeNode) root;
    }

    /**
     * Sets the node specified as root node of the mode choice tree
     *
     * @param root root node to set
     */
    public void setRoot(TPS_ExpertKnowledgeNode root) {
        this.root = root;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.util.tapas.ivf.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        return prefix + "ModeChoiceTree\n" + prefix + root.toString(prefix + " ");
    }
}
