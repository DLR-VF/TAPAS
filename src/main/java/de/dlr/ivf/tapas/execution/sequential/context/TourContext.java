package de.dlr.ivf.tapas.execution.sequential.context;

import de.dlr.ivf.tapas.execution.sequential.choice.LocationContext;
import de.dlr.ivf.tapas.loc.TPS_Location;
import de.dlr.ivf.tapas.scheme.TPS_Episode;
import de.dlr.ivf.tapas.scheme.TPS_Stay;
import de.dlr.ivf.tapas.scheme.TPS_TourPart;
import de.dlr.ivf.tapas.scheme.TPS_Trip;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TourContext implements ContextUpdateable {

    private NavigableMap<TPS_Trip, TPS_Stay> trips_to_stays;

    private TPS_Stay previous_stay;
    private TPS_Stay current_stay;
    private TPS_Stay last_stay;

    private LocationContext location_context;
    private boolean tour_finished = false;
    private TPS_TourPart tour_part;

    private ModeContext mode_context;

    int deltatime;

    TPS_Stay end_stay;
    private TPS_Trip next_trip;
    private TPS_Stay next_stay;
    private TPS_Trip previous_trip;

    public TourContext(TPS_TourPart tour_part, TPS_Stay start_stay, LocationContext location_context, TPS_Stay last_stay){

        this.current_stay = start_stay;
        this.location_context = location_context;
        this.tour_part = tour_part;
        this.mode_context = new ModeContext();
        this.last_stay = last_stay;

        this.trips_to_stays = initTripStayStructure(tour_part, last_stay);

//        TPS_Stay next_episode = tour_part.getNextStay()
//        TPS_Stay departure_stay = tour_part.getPreviousEpisode();
    }


    private NavigableMap<TPS_Trip, TPS_Stay> initTripStayStructure(TPS_TourPart tour_part, TPS_Stay end_stay) {

        NavigableMap<TPS_Trip, TPS_Stay> trips_to_stays  = new TreeMap<>(Comparator.comparingInt(TPS_Episode::getOriginalStart));

        List<TPS_Episode> episodes = tour_part.getEpisodes();

        IntStream.range(0, tour_part.size() / 2).forEach(i -> trips_to_stays.put((TPS_Trip) episodes.get(i), (TPS_Stay) episodes.get(i+1)));

        //episodes count is always odd, so we need to add the last trip and the next home stay to the map
        trips_to_stays.put((TPS_Trip) episodes.get(episodes.size() - 1), end_stay);

        return trips_to_stays;
    }


    public TPS_TourPart getTourPart() {
        return tour_part;
    }

    public TPS_Stay getCurrentStay(){ return this.current_stay; }

    public Supplier<TPS_Stay> getNextHomeStay(){

        return () -> trips_to_stays.lastEntry().getValue();
    }

    public TPS_Stay getNextStay(){ return this.next_stay; }
    public TPS_Trip getNextTrip(){ return this.next_trip; }

    public boolean isFinished() {
        return this.trips_to_stays.lastEntry() == null;
    }

    public TPS_Location getCurrentLocation(){
        return this.location_context.getCurrentLocation();
    }

    public TPS_Location getNextLocation(){
        return this.location_context.getNextLocation();
    }

    public ModeContext getModeContext(){
        return this.mode_context;
    }

    @Override
    public void updateContext() {
        Map.Entry<TPS_Trip, TPS_Stay> next_trip_entry = trips_to_stays.pollFirstEntry();
        
        this.current_stay = next_stay;
        this.previous_trip = next_trip;

        if(next_trip_entry == null) {
            this.next_trip = null;
            this.next_stay = null;
        }else{
            this.next_trip  = next_trip_entry.getKey();
            this.next_stay = next_trip_entry.getValue();
        }
    }

    public void updateDeltaTravelTime(int delta_traveltime) {
        this.deltatime += delta_traveltime;
    }
}
