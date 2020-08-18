package de.dlr.ivf.tapas.plan;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface;
import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.person.*;
import de.dlr.ivf.tapas.scheme.TPS_Scheme;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TPS_PlanGenerator{

    TPS_PersistenceManager pm;

    TPS_PreferenceParameters prefParams = new TPS_PreferenceParameters();
    List<TPS_Preference> preferenceModels = new ArrayList<>();

    List<TPS_Plan> plans;

    public TPS_PlanGenerator(TPS_PersistenceManager pm){
        this.pm = pm;
        this.prefParams.readParams();
    }

    public List<TPS_Plan> generatePlansAndGet(List<TPS_Household> households){

        int plans_size = households.stream()
                                   .map(household -> household.getMembers(TPS_Household.Sorting.NONE))
                                   .mapToInt(Collection::size)
                                   .sum();
        this.plans = new ArrayList<>(plans_size);

        for(TPS_Household hh : households){
            while (hh.getNumberOfMembers() > preferenceModels.size()) {
                preferenceModels.add(new TPS_Preference());
            }

            //calculate the shopping motives or set the default values
            int number = 0;
            for (TPS_Person person : hh.getMembers(TPS_Household.Sorting.AGE)) {
                person.preferenceValues = this.preferenceModels.get(number);
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


                if (TPS_Logger.isLogging(TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
                       TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.DEBUG, "Working on person: " + person);
                }
                if (person.isChild()) {
                    if (TPS_Logger.isLogging(TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
                           TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.DEBUG,
                                   "Person is skipped because it is a child");
                    }
                    continue;
                }

                // check if age adaptation should occur
                if (this.pm.getParameters().isTrue(ParamFlag.FLAG_REJUVENATE_RETIREE)) {
                    if (person.getAge() >= this.pm.getParameters().getIntValue(ParamValue.REJUVENATE_AGE)) {
                        if (TPS_Logger.isLogging(TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.DEBUG)) {
                               TPS_Logger.log(TPS_LoggingInterface.HierarchyLogLevel.PERSON, TPS_LoggingInterface.SeverenceLogLevel.DEBUG,
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
                //we only take plans into account that have at least one trip
                if(the_plan.getScheme().getSchemeParts().size() > 1)
                    this.plans.add(the_plan);
                person.setAgeAdaption(false, pm.getParameters().getIntValue(ParamValue.REJUVENATE_BY_NB_YEARS));
            }
        }
        return plans;
    }
}
