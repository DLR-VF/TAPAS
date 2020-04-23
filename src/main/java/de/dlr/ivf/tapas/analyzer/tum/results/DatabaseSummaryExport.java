package de.dlr.ivf.tapas.analyzer.tum.results;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.RegionAnalyzer;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Writes the results of a {@link RegionAnalyzer} into the database table calibration_results.<br>
 * More specifically, it writes a full split of Mode x PersonGroup x TripIntention x DistanceCategory
 * 
 * @author boec_pa
 * 
 */
public class DatabaseSummaryExport {

	// TODO handle differentiated regions
	// TODO save distance category filters

	private final TPS_DB_Connector dbCon;
	private boolean update = false;

	private final RegionAnalyzer regionAnalyzer;

	/**
	 * 
	 * @param regionAnalyzer
	 * @throws IOException if the loginInfo file is not found.
	 * @throws ClassNotFoundException if class {@link TPS_DB_Connector} is not found.
	 */
	public DatabaseSummaryExport(RegionAnalyzer regionAnalyzer)	throws IOException, ClassNotFoundException {
		try {
			TPS_ParameterClass parameterClass = new TPS_ParameterClass();
			parameterClass.loadRuntimeParameters(TPS_BasicConnectionClass.getRuntimeFile());
			dbCon = new TPS_DB_Connector(parameterClass);
		} catch (IOException | ClassNotFoundException e) {
			throw e; // handle that outside of the class
		}

		String simkey = regionAnalyzer.getSource();
		try {
			String q = "SELECT * FROM calibration_results WHERE sim_key = '" + simkey + "'";
			ResultSet rs = dbCon.executeQuery(q, this);
			if (rs.next()) {
				System.err.println("Simkey for export exists and will be overwritten.");
				update = true;
			}

		} catch (SQLException e) {
			// should never happen
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

		this.regionAnalyzer = regionAnalyzer;
	}

	/**
	 * Inserts or updates the summary in the database.
	 * 
	 * @return <code>false</code> if the database access failed or the region was differentiated.
	 */
	public boolean writeSummary() {

		try {
			Connection con = dbCon.getConnection(this);
			PreparedStatement updateStatement;

			if (update) {
				updateStatement = con
						.prepareStatement("UPDATE calibration_results "
								+ "SET cnt_persons = ?," + "cnt_trips = ?, "
								+ "avg_dist = ?, " + "avg_time = ? "
								+ "WHERE sim_key = ? AND "
								+ "person_group = ? AND "
								+ "distance_category = ? AND "
								+ "trip_mode = ? AND trip_intention = ?");

			} else {
				updateStatement = con
						.prepareStatement("INSERT INTO calibration_results "
								+ "(cnt_persons, cnt_trips, avg_dist, avg_time, sim_key, "
								+ "person_group, distance_category, trip_mode, trip_intention) "
								+ "VALUES (?,?,?,?,?,?,?,?,?)");
			}

			con.setAutoCommit(false);
			fillBatch(updateStatement);
			/* int[] result = */updateStatement.executeBatch();
			con.commit();
			updateStatement.close();
			con.setAutoCommit(true);
			// System.out.println(result.length);
			// System.out.println(Arrays.toString(result));
		} catch (SQLException e) {
			System.err.println(e.getErrorCode());
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.err.println("Error when executing query.");
			return false;
		}

		return true;

	}

	@SuppressWarnings("rawtypes")
	private void fillBatch(PreparedStatement ps) {
		if (regionAnalyzer.isRegionDifferentiated())
			return;
		String simkey = regionAnalyzer.getSource();

		Categories[] cats = { Categories.Mode, Categories.PersonGroup,
				Categories.TripIntention, Categories.DistanceCategory };
		Enum[] instances = { RegionCode.REGION_0, Mode.BIKE, PersonGroup.PG_1,
				TripIntention.TRIP_31, DistanceCategory.CAT_1 };

		ArrayList<CategoryCombination> combinations = CategoryCombination
				.listAllCombinations(cats);

		Categories[] catsR = { Categories.RegionCode, Categories.Mode,
				Categories.PersonGroup, Categories.TripIntention,
				Categories.DistanceCategory };

		for (CategoryCombination cc : combinations) {

			instances[1] = cc.getCategories()[0];// mode
			instances[2] = cc.getCategories()[1];// person group
			instances[3] = cc.getCategories()[2];// trip intention
			instances[4] = cc.getCategories()[3];// distance category

			long cntTrips = regionAnalyzer.getCntTrips(catsR, instances);
			double dist = regionAnalyzer.getDist(catsR, instances);
			double time = regionAnalyzer.getDur(catsR, instances);
			int cntPersons = regionAnalyzer
					.getCntPersons((PersonGroup) instances[2]);

			// cnt_persons, cnt_trips, avg_dist, avg_time, sim_key,
			// person_group, distance_category, trip_mode, trip_intention

			try {
				ps.setInt(1, cntPersons);
				ps.setLong(2, cntTrips);
				ps.setDouble(3, dist);
				ps.setDouble(4, time);
				ps.setString(5, simkey);
				ps.setInt(6, instances[2].ordinal());
				ps.setInt(7, instances[4].ordinal());
				ps.setInt(8, instances[1].ordinal());
				ps.setInt(9, instances[3].ordinal());
				ps.addBatch();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

}
