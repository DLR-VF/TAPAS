package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.util.ExtendedWritable;

/**
 * This tree describes the different specialisations of the mode distributions. At each node there is a different mode
 * distribution stored
 * 
 * @author mark_ma
 * 
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_ModeChoiceTree implements ExtendedWritable {

	/**
	 * Root node
	 */
	protected TPS_Node root = null;

	/**
	 * This constructor builds a mode choice tree without a root
	 */
	public TPS_ModeChoiceTree() {
		this(null);
	}

	/**
	 * Builds the encapsulating object and sets the root
	 * 
	 * @param root
	 */
	public TPS_ModeChoiceTree(TPS_Node root) {
		this.root = root;
	}

	/**
	 * Recursively searches the mode choice tree, normally starting at the root node. Returns either the node it is called
	 * with or the leave associated with the attributes of the person or the day plan a mode should be chosen for
	 * 
	 * @param plan
	 *            day plan
	 * @param node
	 *            node of the tree to start searching at
	 * @return the corresponding leave of the mode choice tree or the root node
	 */
	protected TPS_Node descend(TPS_Plan plan, TPS_Node node) {
		TPS_Node returnNode = node;
		
		if (returnNode.isLeaf()) {
			return returnNode;
		}


		
		int val = plan.getAttributeValue(node.getSplitVariable());
		// if (TPS_Region.WRITE)
		// System.out.print(sv.name()+"->"+val+", ");

		// Look for the matching child.
		for (TPS_Node child : node.getChildren()) {
			if (child.find(val)) {
				returnNode = descend(plan, child);
				break;
			}
		}
		return returnNode;
	}

	/**
	 * Searches recursively for the node with the specified id; starting at the node specified
	 * 
	 * @param node
	 *            node to start searching at
	 * @param id
	 *            id of the node to find
	 * @return the node if found; null else
	 */
	protected TPS_Node find(TPS_Node node, int id) {
		if (node.getId() == id)
			return node;

		for (TPS_Node childNode : node.getChildren()) {
			childNode = find(childNode, id);
			if (childNode != null)
				return childNode;
		}

		return null;
	}

	/**
	 * Returns the node containing the mode choice distribution for the combination of the personal and trip based attributes
	 * 
	 * @param plan
	 *            day plan
	 * @return a node containing the mode choice distribution
	 */
	public TPS_Node getDistributionSet(TPS_Plan plan) {
        // When no matching node is found the root is returned
		// if (pNode == null) {
		// throw new RuntimeException("No distribution found!! ");
		// }
		
		//return a COPY of this distribution!!!!!!
				
		return descend(plan, root);
	}

	/**
	 * Returns the corresponding node to the id specified
	 * 
	 * @param id
	 *            node id
	 * @return node
	 */
	public TPS_Node getNode(int id) {
		return find(this.root, id);
	}

	/**
	 * Returns the root of the mode choice tree
	 * 
	 * @return root node
	 */
	public TPS_Node getRoot() {
		return root;
	}

	/**
	 * Sets the node specified as root node of the mode choice tree
	 * 
	 * @param root
	 *            root node to set
	 */
	public void setRoot(TPS_Node root) {
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
	 * @see de.dlr.ivf.tapas.util.ExtendedWritable#toString(java.lang.String)
	 */
	public String toString(String prefix) {
        return prefix + "ModeChoiceTree\n" + prefix + root.toString(prefix + " ");
	}
}
