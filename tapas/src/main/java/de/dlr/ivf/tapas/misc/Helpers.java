package de.dlr.ivf.tapas.misc;

import de.dlr.ivf.tapas.model.Matrix;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Helpers {

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

    public static int[] extractIntArray(Object array) throws SQLException {

        if (array instanceof int[]) {
            return (int[]) array;
        } else if (array instanceof Integer[]) {
            return ArrayUtils.toPrimitive((Integer[]) array); // like casting Integer[] to int[]
        } else {
            throw new SQLException("Cannot cast to int array");
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

    /**
     * Method to convert matrixelements to a sql-parsable array
     *
     * @param array         the array
     * @param decimalPlaces number of decimal places for the string
     * @return
     */
    public static String matrixToSQLArray(Matrix array, int decimalPlaces) {
        StringBuilder buffer;
        StringBuilder totalBuffer = new StringBuilder("ARRAY[");
        int size = array.getNumberOfColums();
        for (int j = 0; j < size; ++j) {
            buffer = new StringBuilder();
            for (int k = 0; k < size; ++k) {
                buffer.append(new BigDecimal(array.getValue(j + array.sIndex, k + array.sIndex))
                        .setScale(decimalPlaces, RoundingMode.HALF_UP)).append(",");
            }
            if (j < size - 1) totalBuffer.append(buffer);
            else totalBuffer.append(buffer.substring(0, buffer.length() - 1)).append("]");
        }

        return totalBuffer.toString();
    }
}
