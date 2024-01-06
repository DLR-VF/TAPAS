package de.dlr.ivf.tapas.execution.sequential.io;

import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.person.TPS_Preference;
import de.dlr.ivf.tapas.model.person.TPS_PreferenceParameters;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.scheme.TPS_Scheme;
import de.dlr.ivf.tapas.model.parameter.ParamFlag;
import de.dlr.ivf.tapas.model.parameter.ParamString;

import java.util.ArrayList;
import java.util.List;


public class HouseholdBasedPlanGenerator {

    private List<TPS_Preference> preference_models;
    private TPS_PreferenceParameters preference_parameters;
    private TPS_PersistenceManager pm;

    public HouseholdBasedPlanGenerator(TPS_PersistenceManager pm, List<TPS_Preference> preferences, TPS_PreferenceParameters preference_parameters){
        this.pm = pm;
        this.preference_models = preferences;
        this.preference_parameters = preference_parameters;
    }

    public List<TPS_Plan> generatePersonPlansAndGet(TPS_Household household){

        List<TPS_Plan> person_plans = new ArrayList<>(household.getNumberOfMembers());
        int number = 0;
        for (TPS_Person person : household.getMembers(TPS_Household.Sorting.AGE)) {
            person.preferenceValues = preference_models.get(number);
            number++; //we advance to the next number
            if (pm.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES)) {
                //calc the prefrence for shopping choice
                person.preferenceValues.computePreferences(preference_parameters, person);
            }
        }
        TPS_Household.Sorting sortAlgo = TPS_Household.Sorting.AGE;
        if (pm.getParameters().isDefined(ParamString.HOUSEHOLD_MEMBERSORTING)) {
            sortAlgo = TPS_Household.Sorting.valueOf(
                    pm.getParameters().getString(ParamString.HOUSEHOLD_MEMBERSORTING));
        }

        for (TPS_Person person : household.getMembers(sortAlgo)) {

            if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
                TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG, "Working on person: " + person);
            }
            if (person.isChild()) {
                if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
                    TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG,
                            "Person is skipped because it is a child");
                }
                continue;
            }


            TPS_PlanEnvironment pe = null;//new TPS_PlanEnvironment(person, pm.getParameters());
            TPS_Scheme scheme = pm.getSchemesSet().findScheme(person);
            TPS_Plan the_plan = null;//new TPS_Plan(person,pe,scheme);

            TPS_PlanningContext pc = null;//new TPS_PlanningContext(pe, null, person.hasBike());
            the_plan.setPlanningContext(pc);



            person_plans.add(the_plan);
        }

        //now initialize the household car mediator
        //household.initializeCarMediator(household, household_plans);
        return person_plans;
    }
}
