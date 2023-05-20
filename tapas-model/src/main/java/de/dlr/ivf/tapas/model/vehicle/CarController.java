package de.dlr.ivf.tapas.model.vehicle;

import de.dlr.ivf.tapas.model.Timeline;
import lombok.Getter;

@Getter
public class CarController {

    private final Vehicle car;

    private final Timeline timeline;

    private final double maxRange;
    private double remainingRange;
    private boolean tollPaid;

    public CarController(Vehicle car){
        this.car = car;
        this.maxRange = car.fuelType().getRange();
        this.remainingRange = maxRange;
        this.timeline = new Timeline();
    }

    public boolean useCar(int startTime, int duration){
        return timeline.add((int) (startTime + 0.5), (int) (startTime + duration + 0.5));
    }

    public boolean isCarAvailable(int startTime, int duration){
        return this.timeline.clash(startTime, startTime + duration);
    }

    public void reduceRange(double range) {
        this.remainingRange -= range;
    }

    public double remainingCarRange(){
        return remainingRange;
    }

    public boolean isRestricted(){
        return car.isRestricted();
    }

    public void payToll(){
        this.tollPaid = true;
    }

    public void refuel(){
        this.remainingRange = maxRange;
    }

    public int automationLevel(){
        return car.getAutomationLevel();
    }

    public Vehicle getCar(){
        return this.car;
    }
}
