package de.dlr.ivf.tapas.execution.sequential.io;

import de.dlr.ivf.tapas.execution.sequential.statemachine.HouseholdBasedStateMachineController;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.TPS_Logger;
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
import de.dlr.ivf.tapas.model.parameter.ParamValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 *
 */

public class TPS_PlanGenerator{

    TPS_PersistenceManager pm;


    List<TPS_Plan> plans;
    List<HouseholdBasedStateMachineController> household_controllers = new ArrayList<>();

    public TPS_PlanGenerator(TPS_PersistenceManager pm){
        this.pm = pm;
    }

    public List<TPS_Plan> generatePlansAndGet(List<TPS_Household> households){

        int plan_count = 0;
        int max_household_size = 0;

        //determine the total plan count and the biggest household with highest member count
        for(TPS_Household household : households){
            int household_member_count = household.getNumberOfMembers();
            plan_count += household_member_count;
            max_household_size = Math.max(max_household_size, household_member_count);
        }

        //allocate preference instances
        List<TPS_Preference> preferenceModels = new ArrayList<>(max_household_size);
        IntStream.range(0,max_household_size).forEach(i -> preferenceModels.add(new TPS_Preference()));

        TPS_PreferenceParameters prefParams = new TPS_PreferenceParameters();
        prefParams.readParams();

        this.plans = new ArrayList<>(plan_count);

        for(TPS_Household hh : households){

            List<TPS_Plan> household_plans = new ArrayList<>();

            //calculate the shopping motives or set the default values
            int number = 0;
            for (TPS_Person person : hh.getMembers(TPS_Household.Sorting.AGE)) {
                person.preferenceValues = preferenceModels.get(number);
                number++; //we advance to the next number
                if (this.pm.getParameters().isTrue(ParamFlag.FLAG_USE_SHOPPING_MOTIVES)) {
                    //calc the prefrence for shopping choice
                    person.preferenceValues.computePreferences(prefParams, person);
                }
            }
            TPS_Household.Sorting sortAlgo = TPS_Household.Sorting.AGE;
            if (this.pm.getParameters().isDefined(ParamString.HOUSEHOLD_MEMBERSORTING)) {
                sortAlgo = TPS_Household.Sorting.valueOf(
                        this.pm.getParameters().getString(ParamString.HOUSEHOLD_MEMBERSORTING));
            }

            for (TPS_Person person : hh.getMembers(sortAlgo)) {

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

                // check if age adaptation should occur
                if (this.pm.getParameters().isTrue(ParamFlag.FLAG_REJUVENATE_RETIREE)) {
                    if (person.getAge() >= this.pm.getParameters().getIntValue(ParamValue.REJUVENATE_AGE)) {
                        if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
                               TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG,
                                       "Person's age gets adapted");
                        }
                         person.setAgeAdaption(true, this.pm.getParameters().getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS));
                    }
                }

                TPS_PlanEnvironment pe = new TPS_PlanEnvironment(person);
                TPS_Scheme scheme = pm.getSchemesSet().findScheme(person);
                TPS_Plan the_plan = new TPS_Plan(person,pe,scheme,this.pm);
                TPS_PlanningContext pc = new TPS_PlanningContext(pe, null, person.hasBike());
                the_plan.setPlanningContext(pc);
                //we only take plans into account that have at least one trip //todo fixme?
                if(the_plan.getScheme().getSchemeParts().size() > 1)
                    household_plans.add(the_plan);
                person.setAgeAdaption(false, pm.getParameters().getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS));
            }

            this.plans.addAll(household_plans);
            //now initialize the household car mediator
            hh.initializeCarMediator(hh, household_plans);
        }
        return plans;
    }
}
