package de.dlr.ivf.api.io.util;

import org.apache.commons.lang3.ArrayUtils;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlArrayUtils {

    /**
     * Helper function to extract an sql-array to a Java double array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A double array
     * @throws SQLException
     */
    public static double[] extractDoubleArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof double[]) {
            return (double[]) array;
        } else if (array instanceof Double[]) {
            return ArrayUtils.toPrimitive((Double[]) array); // like casting Double[] to double[]
        } else {
            throw new SQLException("Cannot cast to int array");
        }
    }

    /**
     * Helper function to extract an sql-array to a Java int array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A int array
     * @throws SQLException
     */
    public static int[] extractIntArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof int[]) {
            return (int[]) array;
        } else if (array instanceof Integer[]) {
            return ArrayUtils.toPrimitive((Integer[]) array); // like casting Integer[] to int[]
        } else {
            throw new SQLException("Cannot cast to int array");
        }
    }

    public static int[] extractIntArray(Array sqlArray) throws SQLException {

        Object array = sqlArray.getArray();

        if (array instanceof int[] intArray) {
            return intArray;
        } else if (array instanceof Integer[] intArray) {
            return ArrayUtils.toPrimitive(intArray); // like casting Integer[] to int[]
        } else {
            return null;
        }
    }

    public static double[] extractDoubleArray(Array sqlArray) throws SQLException {

        Object array = sqlArray.getArray();

        if (array instanceof double[] doubleArray) {
            return doubleArray;
        } else if (array instanceof Double[] doubleArray) {
            return ArrayUtils.toPrimitive(doubleArray); // like casting Integer[] to int[]
        } else {
            return null;
        }
    }

    /**
     * Helper function to extract an sql-array to a Java String array
     *
     * @param rs    The ResultSet containing a sql-Array
     * @param index The index position of the SQL-Array
     * @return A String array
     * @throws SQLException
     */
    public static String[] extractStringArray(ResultSet rs, String index) throws SQLException {
        Object array = rs.getArray(index).getArray();
        if (array instanceof String[]) {
            return (String[]) array;
        } else {
            throw new SQLException("Cannot cast to string array");
        }
    }
}
