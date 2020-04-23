package de.dlr.ivf.tapas.util;

import de.dlr.ivf.tapas.util.TPS_AttributeReader.TPS_Attribute;

import java.util.*;

/**
 * This map is a dynamic nested map for attributed values, i.t. you can define a file with a tree structure. At each leaf
 * there has to be a double value. The values for the branches have to be readable by the {@link TPS_AttributeReader}.
 */
public class TPS_VariableMap {

    private final Map<Thread, Map<TPS_Attribute, Integer>> attributeMap;
    /**
     * List of attributes which describe each layer of the tree
     */
    private final List<TPS_Attribute> attributes;
    /**
     * Debug string which stores the last request
     */
    private final Map<Thread, String> lastRequest;
    /**
     * This value is returned when no appropriate leaf is found
     */
    private double value;
    /**
     * Source map of the tree
     */
    private Map<Integer, ?> vMap;

    public TPS_VariableMap() {
        this.attributes = new LinkedList<>();
        this.attributeMap = new HashMap<>();
        this.lastRequest = new HashMap<>();
    }

    /**
     * Default constructor initialises the attributes list and the source map
     *
     * @param attributes
     */
    public TPS_VariableMap(Collection<TPS_Attribute> attributes) {
        this(attributes, -1);
    }

    /**
     * Calls default constructor and sets the default value
     *
     * @param attributeList
     * @param defaultValue
     */
    public TPS_VariableMap(Collection<TPS_Attribute> attributeList, double defaultValue) {
        this();
        this.init(attributeList, defaultValue);
    }

    /**
     * Adds a new leaf to the tree
     *
     * @param codes list of codes corresponding to the attributes list
     * @param value the leaf value
     */
    @SuppressWarnings("unchecked")
    public void addValue(List<Integer> codes, double value) {
        if (codes.size() != this.attributes.size()) {
            throw new IllegalArgumentException(
                    "Sizes of attributes and codes do not fit! [" + this.attributes.size() + "!=" + codes.size() + "]");
        }

        Map<Integer, Map<Integer, ?>> mappedMap = null;
        Map<Integer, Double> valueMap = null;
        int index = 0;
        if (codes.size() == 1) {
            valueMap = (Map<Integer, Double>) this.vMap;
            valueMap.put(codes.get(0), value);
        } else {
            mappedMap = (Map<Integer, Map<Integer, ?>>) this.vMap;
            for (Integer code : codes) {
                index++;
                if (index == codes.size()) {
                    valueMap.put(code, value);
                } else if (index == codes.size() - 1) {
                    if (!mappedMap.containsKey(code)) {
                        mappedMap.put(code, new HashMap<Integer, Double>());
                    }
                    valueMap = (Map<Integer, Double>) mappedMap.get(code);
                } else {
                    if (!mappedMap.containsKey(code)) {
                        mappedMap.put(code, new HashMap<Integer, Map<Integer, ?>>());
                    }
                    mappedMap = (Map<Integer, Map<Integer, ?>>) mappedMap.get(code);
                }
            }
        }
    }

    public Map<TPS_Attribute, Integer> getAttributeMap() {
        Map<TPS_Attribute, Integer> map = this.attributeMap.get(Thread.currentThread());
        if (map == null) {
            map = new HashMap<>();
            this.attributeMap.put(Thread.currentThread(), map);
        } else {
            map.clear();
        }
        return map;
    }

    /**
     * This method returns the last request which was consumed by the variable map
     *
     * @return last request
     */
    public String getLastRequest() {
        return lastRequest.get(Thread.currentThread());
    }

//	/**
//	 * This method walks through the tree by reading the attributes out of the plan down to a leaf and return the value. If
//	 * the leaf does not exist a default value is returned
//	 * 
//	 * @return leaf value
//	 */
//	@SuppressWarnings("unchecked")
//	public double getValue() {
//		double value = -1;
//		int attributeValue = -1;
//		Map<Integer, Map<Integer, ?>> mappedMap = null;
//		Map<Integer, Double> valueMap = null;
//		int index = 0;
//
//		StringBuffer sb = new StringBuffer();
//
//		if (this.attributes.size() == 1) {
//			valueMap = (Map<Integer, Double>) this.vMap;
//			value = valueMap.get(this.attributes.get(0).getValue());
//		} else {
//			mappedMap = (Map<Integer, Map<Integer, ?>>) this.vMap;
//			for (TPS_Attribute attribute : this.attributes) {
//				index++;
//				attributeValue = attribute.getValue();
//				sb.append(attribute.name() + ": " + attributeValue + ", ");
//				if (index == this.attributes.size()) {
//					if (!valueMap.containsKey(attributeValue)) {
//						value = this.value;
//						break;
//					}
//					value = valueMap.get(attributeValue);
//				} else if (index == this.attributes.size() - 1) {
//					if (!mappedMap.containsKey(attributeValue)) {
//						value = this.value;
//						break;
//					}
//					valueMap = (Map<Integer, Double>) mappedMap.get(attributeValue);
//				} else {
//					if (!mappedMap.containsKey(attributeValue)) {
//						value = this.value;
//						break;
//					}
//					mappedMap = (Map<Integer, Map<Integer, ?>>) mappedMap.get(attributeValue);
//				}
//			}
//		}
//		this.lastRequest.put(Thread.currentThread(), sb.toString());
//		return value;
//	}

    /**
     * This method walks through the tree by reading the attributes out of the plan down to a leaf and return the value. If
     * the leaf does not exist a default value is returned
     *
     * @param map the map of attributes
     * @return leaf value
     */
    @SuppressWarnings("unchecked")
    public double getValue(Map<TPS_Attribute, Integer> map) {
        double value = -1;
        int attributeValue = -1;
        Map<Integer, Map<Integer, ?>> mappedMap = null;
        Map<Integer, Double> valueMap = null;
        int index = 0;

        StringBuilder sb = new StringBuilder();

        if (this.attributes.size() == 1) {
            valueMap = (Map<Integer, Double>) this.vMap;
            value = valueMap.get(map.get(this.attributes.get(0)));
        } else {
            mappedMap = (Map<Integer, Map<Integer, ?>>) this.vMap;
            for (TPS_Attribute attribute : this.attributes) {
                index++;
                attributeValue = map.get(attribute);
                sb.append(attribute.name() + ": " + attributeValue + ", ");
                if (index == this.attributes.size()) {
                    if (!valueMap.containsKey(attributeValue)) {
                        value = this.value;
                        break;
                    }
                    value = valueMap.get(attributeValue);
                } else if (index == this.attributes.size() - 1) {
                    if (!mappedMap.containsKey(attributeValue)) {
                        value = this.value;
                        break;
                    }
                    valueMap = (Map<Integer, Double>) mappedMap.get(attributeValue);
                } else {
                    if (!mappedMap.containsKey(attributeValue)) {
                        value = this.value;
                        break;
                    }
                    mappedMap = (Map<Integer, Map<Integer, ?>>) mappedMap.get(attributeValue);
                }
            }
        }
        this.lastRequest.put(Thread.currentThread(), sb.toString());
        return value;
    }

    /**
     * This method initialises the attribute list with the default value of -1.
     *
     * @param attributeList list of attributes
     */
    public void init(Collection<TPS_Attribute> attributeList) {
        this.init(attributeList, -1);
    }

    /**
     * This method initialises the attribute list with the given default value.
     *
     * @param attributeList list of attributes
     * @param defaultValue  default value for all attributes
     */
    public void init(Collection<TPS_Attribute> attributeList, double defaultValue) {
        this.attributes.addAll(attributeList);
        if (this.attributes.size() == 0) {
            throw new IllegalStateException("No attributes are available");
        } else if (this.attributes.size() == 1) {
            this.vMap = new HashMap<Integer, Double>();
        } else {
            this.vMap = new HashMap<Integer, Map<Integer, ?>>();
        }
        this.value = defaultValue;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Map<Integer, Map<Integer, ?>> mappedMap = null;
        Map<Integer, Double> valueMap = null;
        sb.append(this.getClass().getSimpleName() + " " + this.attributes.toString() + "\n");
        if (this.attributes.size() == 1) {
            valueMap = (Map<Integer, Double>) this.vMap;
            sb.append(toString(valueMap, ""));
        } else {
            mappedMap = (Map<Integer, Map<Integer, ?>>) this.vMap;
            sb.append(this.toString(mappedMap, "", 0));
        }
        return sb.toString();
    }

    /**
     * This method prints this map into a String
     *
     * @param valueMap value map at a leaf
     * @param prefix   line prefix
     * @return printed value map
     */
    private String toString(Map<Integer, Double> valueMap, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (Integer key : valueMap.keySet()) {
            sb.append(prefix + ", " + key + " -> " + valueMap.get(key) + "\n");
        }
        return sb.toString();
    }

    /**
     * This method prints this map into a String.
     *
     * @param mappedMap current branch
     * @param prefix    line prefix
     * @param depth     current depth in tree
     * @return printed mapped map
     */
    @SuppressWarnings("unchecked")
    private String toString(Map<Integer, Map<Integer, ?>> mappedMap, String prefix, int depth) {
        StringBuilder sb = new StringBuilder();
        if (depth == this.attributes.size() - 2) {
            for (Integer key : mappedMap.keySet()) {
                sb.append(toString((Map<Integer, Double>) (mappedMap.get(key)), prefix + ", " + key));
            }
        } else {
            for (Integer key : mappedMap.keySet()) {
                sb.append(this.toString((Map<Integer, Map<Integer, ?>>) mappedMap.get(key), prefix + ", " + key,
                        depth + 1));
            }
        }
        return sb.toString();
    }
}
