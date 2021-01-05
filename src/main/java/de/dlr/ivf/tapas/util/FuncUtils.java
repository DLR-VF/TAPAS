package de.dlr.ivf.tapas.util;

import java.util.function.Function;

public class FuncUtils {

    public static Function<Integer,Integer> secondsToRoundedMinutes = i -> (int) (i * 1.66666666e-2 + 0.5);
}
