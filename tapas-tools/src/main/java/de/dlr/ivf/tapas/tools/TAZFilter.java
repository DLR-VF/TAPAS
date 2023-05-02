/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This class provides an interface to the table <code>core.berlin_taz_multiline</code>.
 *
 * @author boec_pa
 */
public class TAZFilter {

    /**
     * Fetches the existing mapping names in the database. All results may be used in
     * {\@link TAZFilter#getTAZValues(String)}.
     *
     * @return May be empty but never <code>null</code>.
     * @throws SQLException If the connection or query fails.
     */
    public static Set<String> getMappingNames(Supplier<Connection> connectionSupplier) {

        String query = "SELECT DISTINCT name FROM core.berlin_taz_mapping_values";

        try(Connection connection = connectionSupplier.get();
            PreparedStatement st = connection.prepareStatement(query);
            ResultSet rs = st.executeQuery()) {

            HashSet<String> result = new HashSet<>();
            while (rs.next()) {
                result.add(rs.getString("name"));
            }

            return result;
        }catch (SQLException e){
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns all TAZ ids from the database under the given mapping. All mapping values will be aggregated.
     *
     * @param name The name of the mapping.
     * @return Result may be empty but never <code>null</code>.
     * @throws SQLException If the connection or query fails.
     */
    public static Set<Integer> getTAZValues(String name, Supplier<Connection> connectionSupplier){
        String query = "SELECT taz_values  FROM  core.berlin_taz_mapping_values" + " WHERE name='" + name + "'";

        try(Connection connection = connectionSupplier.get();
            PreparedStatement st = connection.prepareStatement(query);
            ResultSet rs =st.executeQuery()) {

            HashSet<Integer> result = new HashSet<>();
            while (rs.next()) {
                Integer[] newTAZs = (Integer[]) rs.getArray("taz_values").getArray();
                result.addAll(Arrays.asList(newTAZs));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

        return null;
    }
}
