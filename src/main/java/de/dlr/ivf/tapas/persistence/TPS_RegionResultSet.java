package de.dlr.ivf.tapas.persistence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.loc.TPS_TrafficAnalysisZone;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.parameters.ParamValue;

@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
/**
 * Class which represents the ResultSet of possible TPS_Location
 * @author hein_mh
 *
 */
public class TPS_RegionResultSet {

	/**
	 * Inner class for a single result
	 * @author hein_mh
	 *
	 */
	public class Result {
		/**
		 * The reference to the TPS_Location
		 */
		public TPS_Location loc;
		/**
		 * The weight of this Result
		 */
		public Double sumWeight;
		/**
		 * The reference to the TPS_TrafficAnalysisZone, where the location is in
		 */
		public TPS_TrafficAnalysisZone taz;

		/**
		 * Comparator of two Objects
		 * @param obj The object to compare with this one
		 * @return true if taz, loc and sumWeight are the same
		 */
		public boolean equals(Result obj) {
			return taz.equals(obj.taz) && loc.equals(obj.loc) && 0 == Double.compare(sumWeight, obj.sumWeight);

		}

		@Override
		public int hashCode() {
			return taz.hashCode() + loc.hashCode();
		}
	}

	/**
	 * Iterator for a result set. It holds three iterators of tazList, sumWeightList and locList and moves them all three at the same time.
	 * @author hein_mh
	 *
	 */
	private class InternalResultIterable implements Iterable<Result>, Iterator<Result> {

		private Iterator<TPS_Location> locListIterator;

		private Iterator<Double> sumWeightListIterator;

		private Iterator<TPS_TrafficAnalysisZone> tazListIterator;

		/**
		 * Constructor, which sets iterators on some inner variables
		 */
		public InternalResultIterable() {
			this.tazListIterator = tazList.iterator();
			this.sumWeightListIterator = sumWeightList.iterator();
			this.locListIterator = locList.iterator();
		}

		/**
		 * Returns true if more elements are present.
		 */
		public boolean hasNext() {
			return this.tazListIterator.hasNext();
		}

		/**
		 * returns a reference of itself
		 */
		public Iterator<Result> iterator() {
			return this;
		}

		/**
		 * moves all three iterators and puts themin a result.
		 */
		public Result next() {
			Result result = new Result();
			result.taz = this.tazListIterator.next();
			result.sumWeight = this.sumWeightListIterator.next();
			result.loc = this.locListIterator.next();
			return result;
		}

		public void remove() {
			throw new RuntimeException("Method not implemented");
		}

	}

	public static Map<ParamValue, Integer> VELOCITY_2_INDEX_MAP;

	static {
		VELOCITY_2_INDEX_MAP = new HashMap<>();
		int counter = 0;
		for (ParamValue pv : ParamValue.values()) {
			if (pv.name().startsWith("VELOCITY_")) {
				VELOCITY_2_INDEX_MAP.put(pv, 0);
			}
		}
		for (ParamValue pv : VELOCITY_2_INDEX_MAP.keySet()) {
			VELOCITY_2_INDEX_MAP.put(pv, counter++);
		}
	}

	private List<TPS_Location> locList;

	private List<Double> sumWeightList;

	private List<TPS_TrafficAnalysisZone> tazList;

	/**
	 * Constructor
	 */
	public TPS_RegionResultSet() {
		this.locList = new LinkedList<>();
		this.tazList = new LinkedList<>();
		this.sumWeightList = new LinkedList<>();
	}

	/**
	 * This method adds one element to the lists.
	 * @param taz The taz of this location
	 * @param loc The location
	 * @param sumWeight The weight of this location
	 */
	public void add(TPS_TrafficAnalysisZone taz, TPS_Location loc, Double sumWeight) {
		this.tazList.add(taz);
		this.locList.add(loc);
		this.sumWeightList.add(sumWeight);
	}
	
	/**
	 * Method to clear all results.
	 */
	public void clear(){
		this.tazList.clear();
		this.locList.clear();
		this.sumWeightList.clear();		
	}

	/**
	 * This Method gets a iterator which provides a Result with the correct aligned taz, location and weights
	 * @return The iterator
	 */
	public Iterable<Result> getResultIterable() {
		return new InternalResultIterable();
	}

	/**
	 * Method to check if the result set is empty
	 * @return true if no valid data is attached
	 */
	public boolean isEmpty() {
		return this.tazList.isEmpty();
	}

	/**
	 * Returns the amount of added data to this TPS_RegionResultSet.
	 * @return The number of locations attached.
	 */
	public int size() {
		return this.tazList.size();
	}
}
