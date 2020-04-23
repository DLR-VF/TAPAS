package de.dlr.ivf.tapas.modechoice;

import de.dlr.ivf.tapas.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.mode.TPS_Mode;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * This class is a node in the {@link TPS_ModeChoiceTree}. It can either be a real node or leaf. Each real node contains a
 * split variable which describes how the children are separated from each other.
 * 
 * @author mark_ma
 * 
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_Node implements ExtendedWritable, Comparable<TPS_Node> {

	/**
	 * the parent node
	 */
	
	private TPS_Node parent =null;
	
	/**
	 * @return the parent
	 */
	public TPS_Node getParent() {
		return parent;
	}

	/**
	 * direct child nodes
	 */
	private List<TPS_Node> children = new Vector<>();

	/**
	 * mode distribution
	 */
	protected TPS_DiscreteDistribution<TPS_Mode> distribution;

	/**
	 * node id
	 */
	private int id;

	/**
	 * Collection with all values for this node, eg. Distances or age classes
	 */
	private List<Integer> myAttValues = new Vector<>();

	/**
	 * Node's split variable
	 */
	private TPS_Attribute splitVariable;

	/**
	 * Constructor initializes all members with the given values.
	 * 
	 * @param id
	 *            node id
	 * @param splitVariable
	 *            the current split variable
	 * @param attributeValues
	 *            the attribute values of this node
	 * @param distribution
	 *            the mode distribution
	 */
	public TPS_Node(int id, TPS_Attribute splitVariable, Collection<Integer> attributeValues,
                    TPS_DiscreteDistribution<TPS_Mode> distribution, TPS_Node parent) {
		this.id = id;
		this.distribution = distribution;
		this.splitVariable = splitVariable;
		this.myAttValues = new ArrayList<>();
		this.parent=parent;
		if (attributeValues != null)
			this.myAttValues.addAll(attributeValues);
	}

	/**
	 * Adds a child.
	 * 
	 * @param pChild
	 */
	public void addChild(TPS_Node pChild) {
		this.children.add(pChild);
	}

	/**
	 * compares the hashCode of this attribute values with the attribute values of the given node
	 * @return returns zero if the attribute values have the same hashCode
	 */
	public int compareTo(TPS_Node o) {
		return this.myAttValues.hashCode() - o.myAttValues.hashCode();
	}

	/**
	 * Method to check if the attribute values contain a given value
	 * 
	 * @param val
	 * 		value to look for 
	 * @return 
	 * 		true if the value is contained; false else
	 */
	public boolean find(int val) {
		for (Integer integer : this.myAttValues) {
			if (integer == val)
				return true;
		}

		return false;

		// Original correct code
		// return this.attValues.contains(val);
	}

	/**
	 * This method recursively searches the node with the given childId in the branch of this node.
	 * 
	 * @param childId
	 * 
	 * @return child node if found, null otherwise
	 */
	public TPS_Node getChild(int childId) {
		if (this.id == childId) {
			return (this);
		}

		TPS_Node p = null;
		for (TPS_Node child : getChildren()) {
			p = child.getChild(childId);
			if (p != null) {
				return p;
			}
		}

		return null;
	}

	/**
	 * Returns the child nodes of the node
	 * 
	 * @return child nodes
	 */
	public List<TPS_Node> getChildren() {
		return children;
	}

	/**
	 * Returns the id of the node
	 * 
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * returns the attribute values
	 * 
	 * @return
	 */
	public List<Integer> getMyAttValues() {
		return myAttValues;
	}

	/**
	 * Returns the split variable of the node
	 * 
	 * @return the split variable
	 */
	public TPS_Attribute getSplitVariable() {
		return splitVariable;
	}

	/**
	 * Returns the mode distribution of the node
	 * 
	 * @return mode distribution of this node
	 */
	public TPS_DiscreteDistribution<TPS_Mode> getCopyOfValueDistribution() {
		TPS_DiscreteDistribution<TPS_Mode> returnVal = new TPS_DiscreteDistribution<>(
				TPS_Mode.getConstants());
		returnVal.setValues(this.distribution.getValues()); 
		return returnVal;
	}

	/**
	 * States if the node is a leave node
	 * 
	 * @return true if node is a leaf, false otherwise
	 */
	public boolean isLeaf() {
		return children.size() == 0;
	}

	/**
	 * Sets the distribution given
	 * 
	 * @param distribution
	 */
	public void setDistribution(double[] distribution) {
		this.distribution.setSize(distribution.length);
		this.distribution.setValues(distribution);
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
		return this.toString(prefix, true);
	}

	/**
	 * extended toString-method for output
	 * 
	 * @param prefix
	 * @param recursive
	 * @return
	 */
	public String toString(String prefix, boolean recursive) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix + "Node [id=" + id + ", splitvar=" + splitVariable + ", attVal=" + myAttValues + "]\n");
		if (recursive) {
			for (TPS_Node node : this.children) {
				sb.append(node.toString(prefix + " ") + "\n");
			}
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
}
