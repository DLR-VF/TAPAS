package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.util.Iterator;

public interface TapasTripReader {

    void close();

    Iterator<TapasTrip> getIterator();

    /**
     * Returns an estimated progress between <code>0</code> and <code>100</code>
     * .
     */
    int getProgress();

    /**
     * @return a human readable description of the source of the trips.
     */
    String getSource();

    /**
     * @return (estimated) number of elements to be read.
     */
    long getTotal();

}
