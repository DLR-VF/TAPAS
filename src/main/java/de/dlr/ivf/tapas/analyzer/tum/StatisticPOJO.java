package de.dlr.ivf.tapas.analyzer.tum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatisticPOJO {

    private final List<RegionPOJO> analyses = new ArrayList<>();
    private int cntIgnored;
    private int nrTrips;
    private boolean regionalDifferentiation;

    public StatisticPOJO() {

    }

    public void addRegion(RegionPOJO analysis) {
        analyses.add(analysis);
        Collections.sort(analyses);
    }

    public List<RegionPOJO> getAnalyses() {
        return analyses;
    }

    public int getCntIgnored() {
        return cntIgnored;
    }

    public void setCntIgnored(int cntIgnored) {
        this.cntIgnored = cntIgnored;
    }

    public int getNrTrips() {
        return nrTrips;
    }

    public void setNrTrips(int size) {
        this.nrTrips = size;
    }

    public boolean isRegionalDifferentiation() {
        return regionalDifferentiation;
    }

    public void setRegionalDifferentiation(boolean regionalDifferentiation) {
        this.regionalDifferentiation = regionalDifferentiation;
    }

    public void setTripsIgnored(int cntIgnored) {
        this.cntIgnored = cntIgnored;

    }

}
