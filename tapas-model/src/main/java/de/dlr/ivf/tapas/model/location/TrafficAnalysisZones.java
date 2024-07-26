package de.dlr.ivf.tapas.model.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TrafficAnalysisZones {

    private final Map<Integer, TPS_TrafficAnalysisZone> tazMap;
    private final Collection<TPS_TrafficAnalysisZone> trafficAnalysisZones;

    public TrafficAnalysisZones(){
        this.tazMap = new HashMap<>();
        this.trafficAnalysisZones = new ArrayList<>();
    }

    public void addTrafficAnalysisZone(int tazId, TPS_TrafficAnalysisZone trafficAnalysisZone){
        tazMap.put(tazId, trafficAnalysisZone);
        trafficAnalysisZones.add(trafficAnalysisZone);
    }

    public TPS_TrafficAnalysisZone getTrafficAnalysisZoneById(int tazId){
        return tazMap.get(tazId);
    }

    public Collection<TPS_TrafficAnalysisZone> getTrafficZones(){
        return trafficAnalysisZones;
    }
}
