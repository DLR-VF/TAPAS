package de.dlr.ivf.tapas.runtime.client.Graphics.QualityChart;

import org.jfree.data.DomainOrder;
import org.jfree.data.xy.AbstractXYDataset;

import java.util.*;

public class QualityChartDataset extends AbstractXYDataset {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1105064554090557394L;

	public enum Key {
		DATA_HIGH, DATA_MED, DATA_LOW, OPTIMAL, UPPER_CONFIDENCE, LOWER_CONFIDENCE, UPPER_GEH5, LOWER_GEH5;

		/**
		 * 
		 * @param series
		 * @throws ArrayIndexOutOfBoundsException
		 *             if <code>series</code> is not a valid id for
		 *             <code>Key</code>.
		 * @return
		 */
		public static Key getById(int series) {
			return values()[series];
		}

		/**
		 * @return the {@link Key} associated with {@link Quality}
		 *         <code>q</code> or <code>null</code> if no {@link Quality} is
		 *         associated.
		 */
		public static Key getByQuality(Quality q) {
			switch (q) {
			case GOOD:
				return DATA_HIGH;
			case BAD:
				return DATA_LOW;
			case MEDIUM:
				return DATA_MED;

			default:
				return null;
			}
		}

		/**
		 * @return {@link Quality} associated with {@link Key} <code>key</code>
		 *         or <code>null</code> if no {@link Key} is associated.
		 */
		public static Quality getQuality(Key key) {
			switch (key) {
			case DATA_HIGH:
				return Quality.GOOD;
			case DATA_MED:
				return Quality.MEDIUM;
			case DATA_LOW:
				return Quality.BAD;

			default:
				return null;
			}
		}
	}

	public static final EnumSet<Key> DATASERIES = EnumSet.of(Key.DATA_HIGH,
			Key.DATA_MED, Key.DATA_LOW);

	private EnumMap<Key, ArrayList<QualityChartData>> seriesCollection = new EnumMap<>(Key.class);

	private double max;

	// private double conf;

	public QualityChartDataset(ArrayList<QualityChartData> data,
			EnumSet<Key> seriesKeys) {

		for (Key k : seriesKeys)
			seriesCollection.put(k, new ArrayList<>());

		max = -Double.MAX_VALUE;
		for (QualityChartData d : data) {
			max = Math.max(max, d.getReference());
			Key key = Key.getByQuality(d.getQuality());
			if (null != key)
				seriesCollection.get(key).add(d);

		}

		seriesCollection.get(Key.OPTIMAL).add(new QualityChartData(0, 0, ""));
		seriesCollection.get(Key.OPTIMAL).add(
				new QualityChartData(max, max, ""));

		// TODO proper check
		if (seriesKeys.contains(Key.LOWER_CONFIDENCE)) {
			setConfidenceInterval(data);
		}

		if (seriesKeys.contains(Key.LOWER_GEH5)) {
			setGEH5(data);
		}

	}

	public QualityChartDataset(ArrayList<QualityChartData> data) {
		this(data, EnumSet.allOf(Key.class));
	}

	/**
	 * Confidence interval calculated using formulas from <br>
	 * <i>Qualitätssicherung für die Anwendung von Verkehrsnachfragemodellen und
	 * Verkehrsprognosen, Merkblatt, BMVIT</i>
	 * 
	 * @param data
	 * @return
	 */
	private double setConfidenceInterval(ArrayList<QualityChartData> data) {
		double pwa = 0;
		for (QualityChartData d : data) {
			if (d.getReference() > 0) {
				pwa += (d.getModel() - d.getReference()) / d.getReference();
			}
		}

		// pwa = 0.01; // TODO confidence interval stub
		pwa = 1.96 * pwa / data.size();

		seriesCollection.get(Key.LOWER_CONFIDENCE).clear();
		seriesCollection.get(Key.UPPER_CONFIDENCE).clear();

		seriesCollection.get(Key.UPPER_CONFIDENCE).add(
				new QualityChartData(pwa, 0, ""));
		seriesCollection.get(Key.UPPER_CONFIDENCE).add(
				new QualityChartData(max, max - pwa, ""));

		seriesCollection.get(Key.LOWER_CONFIDENCE).add(
				new QualityChartData(0, pwa, ""));
		seriesCollection.get(Key.LOWER_CONFIDENCE).add(
				new QualityChartData(max - pwa, max, ""));
		return pwa;
	}

	private void setGEH5(ArrayList<QualityChartData> data) {
		for (QualityChartData d : data) {
			double s = d.getReference();
			double g5p = 0.25 * (4 * s + 5 * Math.sqrt(16 * s + 25) + 25);
			double g5m = 0.25 * (4 * s - 5 * Math.sqrt(16 * s + 25) + 25);

			seriesCollection.get(Key.LOWER_GEH5).add(
					new QualityChartData(g5m, s, null));
			seriesCollection.get(Key.UPPER_GEH5).add(
					new QualityChartData(g5p, s, null));
		}

	}

	/**
	 * @param series
	 * @return the label for data series and <code>null</code> otherwise.
	 */
	public String getLabel(int series, int item) {

		Key key = (Key) getSeriesKey(series);
		if (DATASERIES.contains(key))
			return seriesCollection.get(key).get(item).getLabel();
		else
			return null;
	}

	@Override
	public int getSeriesCount() {
		return seriesCollection.size();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Comparable getSeriesKey(int series) {
		return seriesCollection.keySet().toArray(new Key[0])[series];
	}

	/**
	 * @param seriesKey
	 *            must be of type {@link Key}.
	 * @throws IllegalArgumentException
	 *             if <code>seriesKey</code> is not of the right type.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public int indexOf(Comparable seriesKey) {
		if (seriesKey instanceof Key) {
			Key[] a = seriesCollection.keySet().toArray(new Key[0]);
			return Arrays.binarySearch(a, (Key) seriesKey,
					new Comparator<Key>() {
						@Override
						public int compare(Key o1, Key o2) {
							return Integer.compare(o1.ordinal(), o2.ordinal());
						}
					});
		}
		throw new IllegalArgumentException("The key must be of class "
				+ Key.class);
	}

	@Override
	public DomainOrder getDomainOrder() {
		return DomainOrder.NONE;
	}

	@Override
	public int getItemCount(int series) {
		Key key = (Key) getSeriesKey(series);
		if (null != key) {
			return seriesCollection.get(key).size();
		}
		throw new ArrayIndexOutOfBoundsException(
				"Requested series does not exist.");
	}

	@Override
	public Number getX(int series, int item) {
		return getXValue(series, item);
	}

	@Override
	public double getXValue(int series, int item) {

		Key key = (Key) getSeriesKey(series);
		if (null != key) {
			return seriesCollection.get(key).get(item).getReference();
		}

		throw new ArrayIndexOutOfBoundsException(
				"Requested series does not exist.");
	}

	@Override
	public Number getY(int series, int item) {
		return getYValue(series, item);
	}

	@Override
	public double getYValue(int series, int item) {

		Key key = (Key) getSeriesKey(series);
		if (null != key) {
			return seriesCollection.get(key).get(item).getModel();
		}

		throw new ArrayIndexOutOfBoundsException(
				"Requested series does not exist.");
	}

	@Override
	public String toString() {
		return seriesCollection.toString();
	}
}
