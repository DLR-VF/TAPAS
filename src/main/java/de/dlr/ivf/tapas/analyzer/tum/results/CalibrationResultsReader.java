package de.dlr.ivf.tapas.analyzer.tum.results;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This class provides an interface to the public table 'calibration_results'.
 * It provides simple methods to retrieve modal splits.
 *
 * @author boec_pa
 */
public class CalibrationResultsReader {

    private final TPS_DB_Connector dbCon;
    private final String simkey;

    /**
     * The constructor tries to connect to the database.
     *
     * @param simkey
     * @throws IOException            if the connection could not be established.
     * @throws ClassNotFoundException
     */
    public CalibrationResultsReader(String simkey) throws IOException, ClassNotFoundException {
        this.simkey = simkey;
        try {
            TPS_ParameterClass parameterClass = new TPS_ParameterClass();
            parameterClass.loadRuntimeParameters(TPS_BasicConnectionClass.getRuntimeFile());
            dbCon = new TPS_DB_Connector(parameterClass);
        } catch (IOException | ClassNotFoundException e) {
            throw e; // handle that outside of the class
        }
    }

    /**
     * For testing purposes only
     */
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        CalibrationResultsReader cr = new CalibrationResultsReader("2013y_03m_07d_16h_43m_41s_859ms");

        System.out.println(cr.getModalSplit());
        System.out.println(cr.getModalSplit(DistanceCategoryDefault.CAT_3));

        System.out.println(cr.getAbsoluteSplit(Categories.Mode, Categories.DistanceCategoryDefault));

    }

    /**
     * Checks if <code>categories</code> are valid.
     *
     * @return
     * @throws IllegalArgumentException if the are not.
     */
    @SuppressWarnings("rawtypes")
    public static void validate(Enum... categories) {
        for (Enum cat : categories) {
            if (!Categories.isValid(cat)) throw new IllegalArgumentException("The category " + cat + " is not valid.");
        }
    }

    private String buildGroupClause(Categories... categories) {
        StringBuilder gc = new StringBuilder();
        for (Categories cat : categories) {
            String columnName = cat.getCalibrationColumn();
            if (null == columnName) {
                throw new IllegalArgumentException("The category " + cat + " is not suitable here.");
            }
            gc.append(columnName).append(",");
        }
        return gc.toString();
    }

    @SuppressWarnings("rawtypes")
    private String buildWhereClause(Enum... categories) {
        StringBuilder wc = new StringBuilder();
        for (Enum cat : categories) {
            wc.append(" AND ");
            String columnName = Categories.getByClass(cat.getClass()).getCalibrationColumn();
            if (null == columnName) {
                throw new IllegalArgumentException("The category " + cat + " is not suitable here.");
            }
            wc.append(" ").append(columnName).append(" ");
            wc.append(" = ").append(cat.ordinal());
        }
        return wc.toString();
    }

    /**
     * Returns the number of counts for all combinations in the given category
     * combination.
     *
     * @param categories
     * @return
     */
    @SuppressWarnings("rawtypes")
    public HashMap<CategoryCombination, Double> getAbsoluteSplit(Categories... categories) {

        HashMap<CategoryCombination, Double> result = new HashMap<>();

        String gc = buildGroupClause(categories);
        String q = "SELECT " + gc + "SUM(cnt_trips) AS sum FROM calibration_results WHERE sim_key = '" + simkey +
                "'GROUP BY " + gc.substring(0, gc.length() - 1);

        try {
            ResultSet rs = dbCon.getConnection(this).createStatement().executeQuery(q);
            while (rs.next()) {
                double value = rs.getDouble("sum");

                // get all enums
                ArrayList<Enum> catList = new ArrayList<>();
                for (Categories cat : categories) {
                    catList.add(cat.getElementById(rs.getInt(cat.getCalibrationColumn())));
                }
                CategoryCombination cc = new CategoryCombination(catList.toArray(new Enum[0]));

                result.put(cc, value);

            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    /**
     * Returns the number of trips within the given category combination.
     *
     * @param categories
     * @return -1 if the connection to database fails.
     */
    @SuppressWarnings("rawtypes")
    public long getCntTrips(Enum... categories) {

        String wc = buildWhereClause(categories);

        String q = "SELECT SUM(cnt_trips) AS sum" + " FROM calibration_results WHERE sim_key = '" + simkey + "' " + wc;

        try {
            ResultSet rs = dbCon.getConnection(this).createStatement().executeQuery(q);
            if (rs.next()) {
                return rs.getLong("sum");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    /**
     * Returns the total number of trips in the given run.
     *
     * @return -1 if the connection to database fails.
     */
    public long getCntTrips() {
        String q = "SELECT SUM(cnt_trips) AS sum FROM calibration_results WHERE sim_key = '" + simkey + "'";

        try {
            ResultSet rs = dbCon.getConnection(this).createStatement().executeQuery(q);

            rs.next();
            return rs.getLong("sum");

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }

    }

    /**
     * This method creates a modal split along some categories (or none). An
     * example would be the modal split of <code>shopping in 3-5 km</code>. It
     * returns the percentage of all trips (count) in the given category for
     * each mode.
     *
     * @param categories may be a combination of elements of {@link PersonGroup},
     *                   {@link TripIntention} and {@link DistanceCategory} (or
     *                   {@link DistanceCategoryDefault}). Contradictory elements will
     *                   result in empty maps.
     * @return <code>null</code> if the connection to the database went wrong.
     * @throws IllegalArgumentException if at least one of the <code>categories</code> are not valid
     *                                  or not suitable for modal splits.
     */
    @SuppressWarnings("rawtypes")
    public HashMap<Mode, Double> getModalSplit(Enum... categories) {

        validate(categories);

        HashMap<Mode, Double> result = new HashMap<>();

        String wc = buildWhereClause(categories);

        // build full query
        String q = "SELECT trip_mode,SUM(cnt_trips) AS sum" + " FROM calibration_results WHERE sim_key = '" + simkey +
                "' " + wc + " GROUP BY trip_mode" + " ORDER BY trip_mode";

        try {
            ResultSet rs = dbCon.getConnection(this).createStatement().executeQuery(q);
            double sum = 0;
            while (rs.next()) {
                Mode mo = Mode.getById(rs.getInt("trip_mode"));
                Double v = (double) rs.getLong("sum");
                result.put(mo, v);
                sum += v;
            }
            rs.close();

            if (sum == 0) return result;

            for (Entry<Mode, Double> e : result.entrySet()) {
                result.put(e.getKey(), e.getValue() / sum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        return result;
    }

    /**
     * Returns the modal split for the given run.
     */
    public HashMap<Mode, Double> getModalSplit() {
        return getModalSplit(new Enum[0]);
    }

}
