package de.dlr.ivf.tapas.model.location;

import java.util.HashMap;
import java.util.Map;

public class TrafficAnalysisZones {

    private final Map<Integer, TPS_TrafficAnalysisZone> tazMap;

    public TrafficAnalysisZones(){
        this.tazMap = new HashMap<>();
    }

    public void addTrafficAnalysisZone(int tazId, TPS_TrafficAnalysisZone trafficAnalysisZone){
        tazMap.put(tazId, trafficAnalysisZone);
    }

    public TPS_TrafficAnalysisZone getTrafficAnalysisZoneById(int tazId){
        return tazMap.get(tazId);
    }
}
