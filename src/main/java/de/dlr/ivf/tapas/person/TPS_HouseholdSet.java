package de.dlr.ivf.tapas.person;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.ExtendedWritable;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Set with all households
 * 
 * @author mark_ma
 * 
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.THREAD)
public class TPS_HouseholdSet implements Iterable<TPS_Household>, ExtendedWritable {

	/**
	 * Map with all households
	 */
	private SortedMap<Integer, TPS_Household> households;

	/**
	 * Default constructor
	 */
	public TPS_HouseholdSet() {
		this.households = new TreeMap<>();
	}

	/**
	 * This method creates a new household and adds it to the set of households.
	 * 
	 * @param id
	 * 
	 * @throws RuntimeException
	 *             this exception is thrown if the household already exist
	 * 
	 * @return the household with its id
	 */
	public TPS_Household addHousehold(Integer id) {
		if (this.containsHousehold(id)) {
			throw new RuntimeException("Household already exists: " + id + " " + this.households.keySet());
		}
		TPS_Household hh = new TPS_Household(id);
		this.households.put(hh.getId(), hh);
		return hh;
	}

	/**
	 * States if a household with the id supplied exists
	 * 
	 * @param id
	 *            id of the household
	 * @return true if a household with this id exists, false otherwise
	 */
	public boolean containsHousehold(Integer id) {
		return households.containsKey(id);
	}

	/**
	 * Returns the household with the id specified
	 * 
	 * @param id
	 *            household id
	 * 
	 * @return household with this id, null if the household does not exist
	 */
	public TPS_Household getHousehold(Integer id) {
		return households.get(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<TPS_Household> iterator() {
		return this.households.values().iterator();
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
		StringBuilder sb = new StringBuilder();
		sb.append(prefix + "HouseholdSet\n");
		for (TPS_Household hh : this.households.values()) {
			sb.append(hh.toString(prefix + " ") + "\n");
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

}
