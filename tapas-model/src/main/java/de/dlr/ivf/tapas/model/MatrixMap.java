package de.dlr.ivf.tapas.model;

import lombok.Getter;

import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;

public class MatrixMap {

    @Getter
    private final String name;
    private final int timeRollOver;
    private final NavigableMap<Integer, IntMatrix> matricesByTime;

    public MatrixMap(String name, int timeRollOver, Supplier<NavigableMap<Integer, IntMatrix>> mapFactory) {
        this.name = name;
        this.timeRollOver = timeRollOver;
        this.matricesByTime = mapFactory.get();
    }

    public void addMatrix(int fromTime, IntMatrix matrix) {
        matricesByTime.put(fromTime, matrix);
    }

    public IntMatrix getMatrix(int fromTime) {

        Map.Entry<Integer, IntMatrix> entry = matricesByTime.floorEntry(fromTime % timeRollOver);

        if(entry == null){
            throw new IllegalArgumentException("No matrix found for time " + fromTime % timeRollOver);
        }

        return entry.getValue();
    }
}
