package de.dlr.ivf.tapas.tools.TAZFilter;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

/**
 * This class provides an interface to the table <code>core.berlin_taz_multiline</code>.
 * 
 * @author boec_pa
 * 
 */
public class TAZFilter {

	/**
	 * Fetches the existing mapping names in the database. All results may be used in
	 * {\@link TAZFilter#getTAZValues(String)}.
	 * 
	 * @return May be empty but never <code>null</code>.
	 * @throws SQLException
	 *             If the connection or query fails.
	 */
	public static Set<String> getMappingNames(TPS_DB_Connector dbCon,
			Object caller) throws SQLException {

		String query = "SELECT DISTINCT name FROM core.berlin_taz_mapping_values";
		ResultSet rs = dbCon.executeQuery(query, caller);

		HashSet<String> result = new HashSet<>();
		while (rs.next()) {
			result.add(rs.getString("name"));
		}
		rs.close();

		return result;
	}

	/**
	 * Returns all TAZ ids from the database under the given mapping. All mapping values will be aggregated.
	 * 
	 * @param name
	 *            The name of the mapping.
	 * @return Result may be empty but never <code>null</code>.
	 * @throws SQLException
	 *             If the connection or query fails.
	 */
	public static Set<Integer> getTAZValues(String name,
			TPS_DB_Connector dbCon, Object caller) throws SQLException {
		String query = "SELECT taz_values  FROM  core.berlin_taz_mapping_values"
				+ " WHERE name='" + name + "'";

		ResultSet rs = dbCon.executeQuery(query, caller);
		HashSet<Integer> result = new HashSet<>();
		while (rs.next()) {
			Integer[] newTAZs = (Integer[]) rs.getArray("taz_values")
					.getArray();
			result.addAll(Arrays.asList(newTAZs));
		}
		rs.close();

		return result;
	}

	/**
	 * For testing purposes only.
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException, SQLException {
		String loginInfo = "T:\\Simulationen\\runtime_perseus.csv";
		TPS_ParameterClass parameterClass = new TPS_ParameterClass();
		parameterClass.loadRuntimeParameters(new File(loginInfo));
		TPS_DB_Connector dbCon = new TPS_DB_Connector(parameterClass);

		// TAZFilter filter = new TAZFilter(dbCon);
		String caller = "TAZFilter";
		System.out.println(TAZFilter.getMappingNames(dbCon, caller));

		System.out.println(TAZFilter.getTAZValues("Tempelhofer Feld",dbCon,caller));

	}

}
