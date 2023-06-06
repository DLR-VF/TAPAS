package de.dlr.ivf.tapas.environment;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;

public class EnvironmentHelpers {

    public static String generateSimulationKey(){
        Calendar c = Calendar.getInstance();
        NumberFormat f00 = new DecimalFormat("00");
        NumberFormat f000 = new DecimalFormat("000");

        return c.get(Calendar.YEAR) + "y_" + f00.format(c.get(Calendar.MONTH) + 1) + "m_" + f00.format(
                c.get(Calendar.DAY_OF_MONTH)) + "d_" + f00.format(c.get(Calendar.HOUR_OF_DAY)) + "h_" +
                f00.format(c.get(Calendar.MINUTE)) + "m_" + f00.format(c.get(Calendar.SECOND)) + "s_" +
                f000.format(c.get(Calendar.MILLISECOND)) + "ms";
    }
}
