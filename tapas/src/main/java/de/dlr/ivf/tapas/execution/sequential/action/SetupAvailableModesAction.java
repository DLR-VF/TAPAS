package de.dlr.ivf.tapas.execution.sequential.action;

import de.dlr.ivf.tapas.execution.sequential.communication.TPS_HouseholdCarMediator;
import de.dlr.ivf.tapas.execution.sequential.context.TourContext;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.TPS_PlanningContext;

public class SetupAvailableModesAction implements TPS_PlanStateAction{

    private final TPS_PersistenceManager pm;
    private TourContext tour_context;
    private TPS_HouseholdCarMediator car_provider;
    private TPS_Person person;
    private TPS_PlanningContext planning_context;

    public SetupAvailableModesAction(TourContext tour_context, TPS_HouseholdCarMediator car_provider, TPS_Person person, TPS_PlanningContext planning_context, TPS_PersistenceManager pm){
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

            if(person.mayDriveACar(pm,null))
                planning_context.carForThisPlan = car_provider.request(pred -> true).orElse(null);

        }
        planning_context.setCarPooler(person.isCarPooler());
    }
}
