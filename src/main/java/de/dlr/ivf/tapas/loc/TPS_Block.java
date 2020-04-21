package de.dlr.ivf.tapas.loc;

import java.util.ArrayList;
import java.util.List;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;

/**
 * A traffic analysis zone is territorially subdivided in blocks. Each traffic analysis zone contains of at least one block
 * which fills the whole traffic analysis zone. The amount of blocks is not limited.
 * 
 * @author mark_ma
 * 
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.EPISODE)
public class TPS_Block implements Comparable<TPS_Block> {

	/**
	 * Block id
	 */
	private int id;

	/**
	 * List of all locations in this block
	 */
	private List<TPS_Location> locations;

	/**
	 * Distance to the nearest public transport stop in this block in meters
	 */
	private double nearestPubTransStop;

	/**
	 * Reference to the traffic analysis zone this block is inside
	 */
	private TPS_TrafficAnalysisZone taz;

	/**
	 * score value
	 */
	private double score;

	/**
	 * Category of score for the public transport quality
	 */
	private int scoreCat;

	/**
	 * Center coordinate of this block
	 */
	private TPS_Coordinate center;

	/**
	 * Constructs a block with the given id and an empty location list.
	 * 
	 * @param id
	 *            id of the block
	 */
	public TPS_Block(int id) {
		this.id = id;
		this.locations = new ArrayList<>();
		this.nearestPubTransStop = -1;
	}

	/**
	 * This method adds the given location to the block's list and sets the reference of the location to this block, i.e. the
	 * mutual connection is built up.
	 * 
	 * @param location
	 *            Location the reference should be set for
	 * @return true if the location was added, false otherwise
	 */
	public boolean addLocation(TPS_Location location) {
		return locations.add(location);
	}

	@Override
	public int compareTo(TPS_Block o) {
		return Integer.compare(this.id, o.id);
	}

	/**
	 * Checks if the block has a reference to the location provided
	 * 
	 * @param loc location a reference should be checked for
	 * @return true if the block has a reference to the location, false otherwise
	 */
	public boolean containsLocation(TPS_Location loc) {
		return locations.contains(loc);
	}

	/**
	 * Returns the ID of the block
	 * 
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the distance to the next public transport stop in this block in meters
	 * 
	 * @return distance to the nearest public transport stop
	 */
	public double getNearestPubTransStop() {
		return nearestPubTransStop;
	}

	/**
	 * Returns the traffic analysis zone of the block
	 * 
	 * @return traffic analysis zone
	 */
	public TPS_TrafficAnalysisZone getTrafficAnalysisZone() {
		return taz;
	}

	/**
	 * Returns the score of the public transport quality for the block
	 * 
	 * @return score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * Returns the score category of the public transport quality for the block
	 * 
	 * @return score category
	 */
	public int getScoreCat() {
		return scoreCat;
	}

	/**
	 * Sets the distance in meters to the nearest public transport stop in this block
	 * 
	 * @param nearestPubTransStop
	 */
	public void setNearestPubTransStop(double nearestPubTransStop) {
		this.nearestPubTransStop = nearestPubTransStop;
	}

	/**
	 * Sets reference to the traffic analysis zone.
	 * 
	 * @param taz
	 *            the traffic analysis zone (tvz)
	 */
	public void setTrafficAnalysisZone(TPS_TrafficAnalysisZone taz) {
		this.taz = taz;
	}

	/**
	 * Sets the score of the public transport quality for the block
	 * 
	 * @param score
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/**
	 * Sets score category of the public transport quality for the block
	 * 
	 * @param scoreCat
	 */
	public void setScoreCat(int scoreCat) {
		this.scoreCat = scoreCat;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [id:" + id + " nearestPubTransStop:" + nearestPubTransStop + " scores:"
				+ score + "/" + scoreCat + "]";
	}

	/**
	 * Gets the center of this block
	 * @return A TPS_Coordinate containing the block center
	 */
	public TPS_Coordinate getCenter() {
		return center;
	}

	/**
	 * Sets the center of this block
	 * @param center The bölock center
	 */
	public void setCenter(TPS_Coordinate center) {
		this.setCenter(center.getValue(0), center.getValue(1));
	}

	/**
	 * Sets the center according to the given coordinates
	 * @param x x-value
	 * @param y y-value
	 */
	public void setCenter(double x, double y) {
		if (this.center == null) {
			this.center = new TPS_Coordinate(x, y);
		} else {
			this.center.setValues(x,y);
		}
	}
}
