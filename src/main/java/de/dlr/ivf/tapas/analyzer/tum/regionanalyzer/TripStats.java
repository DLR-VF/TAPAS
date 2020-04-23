package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;

/**
 * This mutable class encapsulates distance, number of trips and total
 * duration and offers several ways to add and create them.
 *
 * @author boec_pa
 */
public class TripStats {
    private double dist;
    private long cntTrips;
    private double duration;

    public TripStats(double dist, long cntTrips, double duration) {
        super();
        this.dist = dist;
        this.cntTrips = cntTrips;
        this.duration = duration;
    }

    public TripStats(TapasTrip trip) {
        this(trip.getDistNet(), 1, trip.getTT());
    }

    public void add(double dist, long cnt, double dur) {
        this.cntTrips += cnt;
        this.dist += dist;
        this.duration += dur;
    }

    public void add(TripStats ts) {
        add(ts.getDist(), ts.getCntTrips(), ts.duration);
    }

    public void add(TapasTrip trip) {
        add(trip.getDistNet(), 1, trip.getTT());
    }

    public long getCntTrips() {
        return cntTrips;
    }

    public double getDist() {
        return dist;
    }

    public double getDur() {
        return duration;
    }

    @Override
    public String toString() {
        return "[" + dist + "," + cntTrips + "," + duration + "]";
    }

}