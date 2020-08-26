package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.plan.state.TPS_WritableTrip;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class TPS_PipedDbWriter implements Runnable, TPS_TripWriter{

    private char csv_delimiter = ';';
    private final Object POISON_PILL = new Object();

    private PipedOutputStream os;
    private PipedInputStream is;
    private LinkedBlockingQueue<Object> trips_to_store = new LinkedBlockingQueue<>();

    public TPS_PipedDbWriter(PipedInputStream is, TPS_PersistenceManager pm){
        this.is = is;
        init();
    }

    @Override
    public void run() {
        TPS_WritableTrip trip;
        Object next_queue_item;

        try {
            while ((next_queue_item = trips_to_store.take()) != POISON_PILL) {
                if (next_queue_item instanceof TPS_WritableTrip){
                    trip = (TPS_WritableTrip) next_queue_item;
                    writeTrip(trip);
                }else {
                    System.out.println("closing the pipe");
                    finish();
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
    private void init(){
        try {
            this.os = new PipedOutputStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeTrip(TPS_WritableTrip trip) throws IOException {
        String csv_string =  ""+trip.getPersonId()+csv_delimiter+trip.getHouseholdId()+csv_delimiter+trip.getSchemeId()+csv_delimiter+trip.getScoreCombined()+csv_delimiter+trip.getScoreFinance()+csv_delimiter+trip.getScoreTime()+csv_delimiter+trip.getTazIdStart()+
                csv_delimiter+trip.getTazHasTollStart()+csv_delimiter+trip.getBlockIdStart()+csv_delimiter+trip.getLocIdStart()+csv_delimiter+trip.getLonStart()+csv_delimiter+trip.getLatStart()+csv_delimiter+trip.getTazIdEnd()+csv_delimiter+trip.getTazHasTollEnd()+
                csv_delimiter+trip.getBlockIdEnd()+csv_delimiter+trip.getLocIdEnd()+csv_delimiter+trip.getLonEnd()+csv_delimiter+trip.getLatEnd()+csv_delimiter+trip.getStartTimeMin()+csv_delimiter+trip.getTravelTimeSec()+csv_delimiter+trip.getMode()+
                csv_delimiter+trip.getCarType()+csv_delimiter+trip.getDistanceBlMeter()+csv_delimiter+trip.getDistanceRealMeter()+csv_delimiter+trip.getActivity()+csv_delimiter+trip.getIsAtHome()+csv_delimiter+trip.getActivityStartMin()+
                csv_delimiter+trip.getActivityDurationMin()+csv_delimiter+trip.getCarIndex()+csv_delimiter+trip.getIsRestrictedCar()+csv_delimiter+trip.getPersonGroup()+csv_delimiter+trip.getTazBbrTypeStart()+csv_delimiter+trip.getBbrTypeHome()+
                csv_delimiter+trip.getLocSelectionMotive()+csv_delimiter+trip.getLocSelectionMotiveSupply()+"\r"+"\n";
        os.write(csv_string.getBytes());

    }

    public void finish(){
        try {
            this.trips_to_store.put(POISON_PILL);
            this.os.flush();
            this.os.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
