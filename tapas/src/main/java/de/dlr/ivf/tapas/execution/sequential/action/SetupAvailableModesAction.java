package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.model.vehicle.CarController;
import de.dlr.ivf.tapas.model.vehicle.CarFleetManager;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;

import java.util.List;

public class SetupAvailableModesAction implements TPS_PlanStateAction{

    private final TPS_PersistenceManager pm;
    private TourContext tour_context;
    private CarFleetManager car_provider;
    private TPS_Person person;
    private TPS_PlanningContext planning_context;

    public SetupAvailableModesAction(TourContext tour_context, CarFleetManager car_provider, TPS_Person person, TPS_PlanningContext planning_context, TPS_PersistenceManager pm){
        this.tour_context = tour_context;
        this.car_provider = car_provider;
        this.person = person;
        this.planning_context = planning_context;
        this.pm = pm;
    }


    @Override
    public void run() {

        if(tour_context.getCurrentStay().isAtHome()){
            planning_context.isBikeAvailable = person.hasBike();

            List<CarController> cars = car_provider.getAvailableCars(0,1);
            if(person.mayDriveACar(cars.get(0).getCar(),0,0)) {
                planning_context.carForThisPlan = cars.get(0).getCar();
                planning_context.setHhCar(cars.get(0));
            }

        }
        planning_context.setCarPooler(person.isCarPooler());
    }
}
