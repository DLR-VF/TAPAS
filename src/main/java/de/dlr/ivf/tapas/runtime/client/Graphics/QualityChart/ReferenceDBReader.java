package de.dlr.ivf.tapas.runtime.client.Graphics.QualityChart;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategoryDefault;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Mode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * This class provides an interface to the public table <code>reference</code>.
 * 
 * @author boec_pa
 * 
 */
public class ReferenceDBReader {

	private static final String loginInfo = "T:\\Simulationen\\runtime_perseus.csv";
	/** number of persons in B times avg mobility rate */
	private static final double BERLIN_TRIPS = 3416255 * 3.4;

	private final TPS_DB_Connector dbCon;
	private final String referenceKey;

	/**
	 * Connects to the database.
	 * 
	 * @param referenceKey
	 *            the key to the reference (like <code>mid2008</code>).
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public ReferenceDBReader(String referenceKey) throws IOException,
			ClassNotFoundException {
		try {
			TPS_ParameterClass parameterClass = new TPS_ParameterClass();
			parameterClass.loadRuntimeParameters(new File(loginInfo));
			dbCon = new TPS_DB_Connector(parameterClass);
		} catch (IOException | ClassNotFoundException e) {
			throw e; // handle that outside of the class
		}

		this.referenceKey = referenceKey;

	}

	/**
	 * Assembles absolute numbers in the categories of Mode/DistanceCategory of
	 * the given reference.
	 * 
	 * @return <code>null</code> if a problem with the database occurs.
	 */
	public HashMap<CategoryCombination, QualityChartData> getMoDcValues() {

		// get dc split
		String oKey = referenceKey + "_dc";
		String q = "SELECT inner_key,cnt_trips,quality FROM reference WHERE outer_key='"
				+ oKey + "'";
		/* number of trips by DC in percent */
		HashMap<DistanceCategoryDefault, QualityChartData> dcData = new HashMap<>();

		try {
			ResultSet rs = dbCon.getConnection(this).createStatement()
					.executeQuery(q);

			while (rs.next()) {
				DistanceCategoryDefault dc = DistanceCategoryDefault.getById(rs
						.getInt("inner_key"));
				double cnt = rs.getDouble("cnt_trips");
				Quality quality = Quality.getById(rs.getInt("quality"));

				dcData.put(dc,
						new QualityChartData(-1, cnt, dc.getDescription(),
								quality));

			}

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

		// get modal split by dc

		HashMap<CategoryCombination, QualityChartData> modcData = new HashMap<>();
		oKey = referenceKey + "_mo_dc";
		q = "SELECT inner_key,cnt_trips,quality FROM reference WHERE outer_key='"
				+ oKey + "'";

		try {
			ResultSet rs = dbCon.getConnection(this).createStatement()
					.executeQuery(q);

			while (rs.next()) {
				String[] keys = rs.getString("inner_key").split(";");
				Mode mo = Mode.getById(Integer.parseInt(keys[0]));
				DistanceCategoryDefault dc = DistanceCategoryDefault
						.getById(Integer.parseInt(keys[1]));

				double cnt = rs.getDouble("cnt_trips");
				Quality quality = Quality.getById(rs.getInt("quality"));

				CategoryCombination cc = new CategoryCombination(mo, dc);
				modcData.put(cc, new QualityChartData(-1, cnt, cc.toString(),
						quality));

			}

			rs.close();

		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

		// merge lists

		HashMap<CategoryCombination, QualityChartData> result = new HashMap<>();

		for (DistanceCategoryDefault dc : DistanceCategoryDefault.values()) {

			for (Mode mo : Mode.values()) {
				CategoryCombination cc = new CategoryCombination(mo, dc);
				QualityChartData dcElem = dcData.get(dc);
				QualityChartData modcElem = modcData.get(cc);

				double value = dcElem.getReference() / 100
						* modcElem.getReference() / 100 * BERLIN_TRIPS;

				// take minimum of qualities
				Quality quality = (dcElem.getQuality().compareTo(
						modcElem.getQuality()) < 0) ? dcElem.getQuality()
						: modcElem.getQuality();

				result.put(cc, new QualityChartData(-1, value, cc.toString(),
						quality));
			}
		}

		return result;
	}

	public double getCntTrips() {
		return BERLIN_TRIPS;
	}

	/**
	 * For testing purposes only.
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		ReferenceDBReader rr = new ReferenceDBReader("mid2008");
		System.out.println(rr.getMoDcValues());

	}

}
