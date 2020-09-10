package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.plan.state.TPS_WritableTrip;

public class TPS_WritableTripEvent {
    private byte[] trip_byte_array;

    public TPS_WritableTripEvent(){}

    public byte[] getTripAsByteArray(){
        return this.trip_byte_array;
    }

    public void setTripByteArray(TPS_WritableTrip trip){
        char csv_delimiter = ';';
        this.trip_byte_array = (""+trip.getPersonId()+ csv_delimiter +trip.getHouseholdId()+ csv_delimiter +trip.getSchemeId()+ csv_delimiter +trip.getScoreCombined()+ csv_delimiter +trip.getScoreFinance()+ csv_delimiter +trip.getScoreTime()+ csv_delimiter +trip.getTazIdStart()+
                csv_delimiter +trip.getTazHasTollStart()+ csv_delimiter +trip.getBlockIdStart()+ csv_delimiter +trip.getLocIdStart()+ csv_delimiter +trip.getLonStart()+ csv_delimiter +trip.getLatStart()+ csv_delimiter +trip.getTazIdEnd()+ csv_delimiter +trip.getTazHasTollEnd()+
                csv_delimiter +trip.getBlockIdEnd()+ csv_delimiter +trip.getLocIdEnd()+ csv_delimiter +trip.getLonEnd()+ csv_delimiter +trip.getLatEnd()+ csv_delimiter +trip.getStartTimeMin()+ csv_delimiter +trip.getTravelTimeSec()+ csv_delimiter +trip.getMode()+
                csv_delimiter +trip.getCarType()+ csv_delimiter +trip.getDistanceBlMeter()+ csv_delimiter +trip.getDistanceRealMeter()+ csv_delimiter +trip.getActivity()+ csv_delimiter +trip.getIsAtHome()+ csv_delimiter +trip.getActivityStartMin()+
                csv_delimiter +trip.getActivityDurationMin()+ csv_delimiter +trip.getCarIndex()+ csv_delimiter +trip.getIsRestrictedCar()+ csv_delimiter +trip.getPersonGroup()+ csv_delimiter +trip.getTazBbrTypeStart()+ csv_delimiter +trip.getBbrTypeHome()+
                csv_delimiter +trip.getLocSelectionMotive()+ csv_delimiter +trip.getLocSelectionMotiveSupply()+"\n").getBytes();
    }

    public void clear(){
        this.trip_byte_array = null;
    }
}
