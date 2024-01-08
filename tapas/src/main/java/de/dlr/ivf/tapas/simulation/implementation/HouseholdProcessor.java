package de.dlr.ivf.tapas.simulation.implementation;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import de.dlr.ivf.tapas.choice.FeasibilityCalculator;
import de.dlr.ivf.tapas.choice.LocationAndModeChooser;
import de.dlr.ivf.tapas.choice.SchemeSelector;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.model.constants.TPS_AgeClass;
import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.constants.TPS_Income;
import de.dlr.ivf.tapas.model.constants.TPS_SettlementSystem;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.TPS_Plan;
import de.dlr.ivf.tapas.model.plan.TPS_PlanEnvironment;
import de.dlr.ivf.tapas.model.plan.TPS_PlanningContext;
import de.dlr.ivf.tapas.model.plan.acceptance.TPS_PlanEVA1Acceptance;
import de.dlr.ivf.tapas.model.scheme.TPS_Scheme;
import de.dlr.ivf.tapas.simulation.Processor;
import lombok.Builder;

import java.util.*;

@Builder
public class HouseholdProcessor implements Processor<TPS_Household, Map<TPS_Person, TPS_PlanEnvironment>> {
    private final Logger logger = System.getLogger(HouseholdProcessor.class.getName());

    private final SchemeSelector schemeSelector;
    private final LocationAndModeChooser locationAndModeChooser;
    private final int maxTriesScheme;
    private final TPS_PlanEVA1Acceptance planEVA1Acceptance;
    private final FeasibilityCalculator feasibilityCalculator;



    @Override
    public Map<TPS_Person, TPS_PlanEnvironment> process(TPS_Household hh) {

        Map<TPS_Person, TPS_PlanEnvironment> result = new HashMap<>();
        //todo car fleet manager should be set up here

        for (TPS_Person person : hh.getMembers(TPS_Household.Sorting.AGE)) {

            if (person.isChild()) {
                continue;
            }

            Map<TPS_Attribute, Integer> planAttributes = initPlanAttributes(person, hh);
            TPS_PlanEnvironment pe = new TPS_PlanEnvironment(person, planEVA1Acceptance);

            boolean finished = false;
            for (int schemeIt = 0; !finished && schemeIt < maxTriesScheme; ++schemeIt) {

                TPS_Scheme scheme = schemeSelector.selectPlan(person);

                //todo think about using a plan processor that returns completely computed plans
                boolean finishedPlanSearch = false;
                while(!finishedPlanSearch) {
                    TPS_Plan plan = new TPS_Plan(hh.getLocation(), pe, scheme, planAttributes);
                    TPS_PlanningContext pc = new TPS_PlanningContext(pe, hh.getLeastRestrictedCar(), person.hasBike(), person,hh);
                    locationAndModeChooser.selectLocationsAndModesAndTravelTimes(pc, scheme, plan);

                    plan.setTravelTimes();
                    plan.balanceStarts();
                    pe.addPlan(plan);

                    if (feasibilityCalculator.isPlanFeasible(plan) && planEVA1Acceptance.isPlanAccepted(plan)) {
                        finished = true;
                    }

                    if (!pc.needsOtherModeAlternatives(plan)) {
                        finishedPlanSearch = true;
                    }
                }

                if(!finished){
                    pe.dismissScheme();
                }
            }

            if(!finished){
                logger.log(Level.DEBUG,"No suitable plan found for person with id %1",person.getId());
            }

            result.put(person, pe);
        }

        return result;
    }

    private Map<TPS_Attribute, Integer> initPlanAttributes(TPS_Person person, TPS_Household household){

        Map<TPS_Attribute, Integer> myAttributes = new HashMap<>();

        myAttributes.put(TPS_Attribute.HOUSEHOLD_INCOME_CLASS_CODE,
                household.getIncomeClass().getCode());
        myAttributes.put(TPS_Attribute.PERSON_AGE, person.getAge());
        myAttributes.put(TPS_Attribute.PERSON_AGE_CLASS_CODE_PERSON_GROUP, person.getPersonGroup().getCode());
        int code;
        if (person.hasDrivingLicenseInformation()) {
            code = person.getDrivingLicenseInformation().getCode();
        } else {
            if (person.getAge() >= 18) {
                code = TPS_DrivingLicenseInformation.CAR.getCode();
            } else {
                code = TPS_DrivingLicenseInformation.NO_DRIVING_LICENSE.getCode();
            }
        }
        if (code == 0) { //BUGFIX: no driving license is coded in MID2008 as 2!
            code = 2;
        }
        myAttributes.put(TPS_Attribute.PERSON_DRIVING_LICENSE_CODE, code);
        myAttributes.put(TPS_Attribute.CURRENT_TAZ_SETTLEMENT_CODE_TAPAS,
                household.getLocation().getTrafficAnalysisZone().getBbrType()
                        .getCode(TPS_SettlementSystem.TPS_SettlementSystemType.TAPAS));
        myAttributes.put(TPS_Attribute.PERSON_AGE_CLASS_CODE_STBA, person.getAgeClass().getCode(TPS_AgeClass.TPS_AgeCodeType.STBA));
        myAttributes.put(TPS_Attribute.PERSON_HAS_BIKE, person.hasBike() ? 1 : 0);
        myAttributes.put(TPS_Attribute.HOUSEHOLD_CARS, person.getHousehold()
                .getNumberOfCars()); // TODO: note that this is set once again in selectLocationsAndModesAndTravelTimes
        myAttributes.put(TPS_Attribute.PERSON_SEX_CLASS_CODE, person.getSex().getCode());

        return myAttributes;
    }

}
