package de.dlr.ivf.tapas.misc;

import de.dlr.ivf.tapas.model.MatrixLegacy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Helpers {
    /**
     * Method to convert matrixelements to a sql-parsable array
     *
     * @param array         the array
     * @param decimalPlaces number of decimal places for the string
     * @return
     */
    public static String matrixToSQLArray(MatrixLegacy array, int decimalPlaces) {
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
