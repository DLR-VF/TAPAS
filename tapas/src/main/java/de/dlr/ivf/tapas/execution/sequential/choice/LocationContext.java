package de.dlr.ivf.tapas.execution.sequential.choice;

import de.dlr.ivf.tapas.constants.TPS_ActivityConstant;
import de.dlr.ivf.tapas.execution.sequential.context.ContextUpdateable;
import de.dlr.ivf.tapas.loc.TPS_Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LocationContext implements ContextUpdateable {

    Map<TPS_ActivityConstant, TPS_Location> fixLocations = new HashMap<>();

    private TPS_Location home_location;
    private TPS_Location current_location;
    private TPS_Location next_location;

    public LocationContext(TPS_Location home_location){
        this.home_location = home_location;
        this.current_location = home_location;
    }

    public Optional<TPS_Location> getFromFixLocations(TPS_ActivityConstant next_activity_code){

        return Optional.ofNullable(fixLocations.get(next_activity_code));
    }

    public TPS_Location getHomeLocation() {
        return this.home_location;
    }

    public void setNextLocation(TPS_Location next_location){

        this.next_location = next_location;
    }

    public void addToFixLocations(TPS_ActivityConstant activity, TPS_Location location){
        this.fixLocations.putIfAbsent(activity,location);
    }

    public TPS_Location getCurrentLocation() {
        return this.current_location;
    }

    public TPS_Location getNextLocation(){
        return this.next_location;
    }

    @Override
    public void updateContext() {
        this.current_location = next_location;
        this.next_location = null;
    }
}
