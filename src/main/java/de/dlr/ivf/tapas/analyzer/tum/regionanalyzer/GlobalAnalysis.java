package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.PersonGroup;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.RegionCode;

import java.util.EnumMap;
import java.util.HashSet;

public class GlobalAnalysis {
    private final RegionCode region;
    private final AnalyzerBase<PersonGroup> personGroupAnalyzer;

    private double totalTripLength = 0.0;
    private double minTripLength = Double.MAX_VALUE;
    private double maxTripLength = Double.MIN_VALUE;

    private double totalDuration = 0.0;
    private double minDuration = Double.MAX_VALUE;
    private double maxDuration = Double.MIN_VALUE;

    private long cntTrips = 0;
    private final HashSet<Integer> uniquePersons = new HashSet<>();
    private final EnumMap<PersonGroup, Integer> cntPersons = new EnumMap<>(PersonGroup.class);

    public GlobalAnalysis(RegionCode region, AnalyzerBase<PersonGroup> personGroupAnalyzer) {
        this.region = region;
        this.personGroupAnalyzer = personGroupAnalyzer;
        for (PersonGroup pg : PersonGroup.values()) {
            cntPersons.put(pg, 0);
        }
    }

    public void addTrip(TapasTrip tt) {
        if (uniquePersons.add(tt.getIdPers())) {
            PersonGroup pg = personGroupAnalyzer.assignTrip(tt);
            cntPersons.put(pg, cntPersons.get(pg) + 1);
        }

        totalTripLength += tt.getDistNet();
        totalDuration += tt.getTT();

        maxTripLength = Math.max(tt.getDistNet(), maxTripLength);
        minTripLength = Math.min(tt.getDistNet(), minTripLength);

        maxDuration = Math.max(tt.getTT(), maxDuration);
        minDuration = Math.min(tt.getTT(), minDuration);

        cntTrips++;
    }

    public int getCntPersons() {
        return uniquePersons.size();
    }

    public int getCntPersons(PersonGroup pg) {
        return cntPersons.get(pg);
    }

    public long getCntTrips() {
        return cntTrips;
    }

    public double getMaxDuration() {
        return maxDuration;
    }

    public double getMaxTripLength() {
        return maxTripLength;
    }

    public double getMinDuration() {
        return minDuration;
    }

    public double getMinTripLength() {
        return minTripLength;
    }

    // getters
    public RegionCode getRegion() {
        return region;
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public double getTotalTripLength() {
        return totalTripLength;
    }

}
