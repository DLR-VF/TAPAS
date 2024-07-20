package de.dlr.ivf.tapas.simulation.implementation;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import de.dlr.ivf.tapas.choice.FeasibilityCalculator;
import de.dlr.ivf.tapas.model.TPS_AttributeReader.TPS_Attribute;
import de.dlr.ivf.tapas.model.constants.TPS_AgeClass;
import de.dlr.ivf.tapas.model.constants.TPS_DrivingLicenseInformation;
import de.dlr.ivf.tapas.model.person.TPS_Household;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.model.plan.*;
import de.dlr.ivf.tapas.model.plan.acceptance.TPS_PlanEVA1Acceptance;
import de.dlr.ivf.tapas.model.scheme.Scheme;
import de.dlr.ivf.tapas.simulation.Processor;
import de.dlr.ivf.tapas.choice.trafficgeneration.SchemeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;


@Lazy
@Component
public class HouseholdProcessor implements Processor<TPS_Household, Map<TPS_Person, TPS_PlanEnvironment>> {
    private final Logger logger = System.getLogger(HouseholdProcessor.class.getName());

    private final SchemeProvider schemeProvider;
    private final int maxTriesSchemeSelection;
    private final TPS_PlanEVA1Acceptance planEVA1Acceptance;
    private final FeasibilityCalculator feasibilityCalculator;
    private final PersonProcessor personProcessor;

    @Autowired
    public HouseholdProcessor(SchemeProvider schemeProvider, @Qualifier("maxTriesSchemeSelection") int maxTriesSchemeSelection,
                              TPS_PlanEVA1Acceptance planEVA1Acceptance, FeasibilityCalculator feasibilityCalculator,
                              PersonProcessor personProcessor){

        this.schemeProvider = schemeProvider;
        this.maxTriesSchemeSelection = maxTriesSchemeSelection;
        this.planEVA1Acceptance = planEVA1Acceptance;
        this.feasibilityCalculator = feasibilityCalculator;
        this.personProcessor = personProcessor;
    }

    @Override
    public Map<TPS_Person, TPS_PlanEnvironment> process(TPS_Household hh) {

        Map<TPS_Person, TPS_PlanEnvironment> result = new HashMap<>();
        //todo car fleet manager should be set up here

        logger.log(Level.DEBUG,"Processing Household: "+hh.getId());
        for (TPS_Person person : hh.getMembers(TPS_Household.Sorting.AGE)) {

            if (person.isChild()) {
                continue;
            }

            Map<TPS_Attribute, Integer> planAttributes = initPlanAttributes(person, hh);
            TPS_PlanEnvironment pe = new TPS_PlanEnvironment(person, planEVA1Acceptance);

            boolean finished = false;
            for (int schemeIt = 0; !finished && schemeIt < maxTriesSchemeSelection; ++schemeIt) {

                Scheme scheme = schemeProvider.selectScheme(person.getPersonGroup());

                PlanningContext planningContext = new PlanningContext(person, hh.getLocation(), scheme.tours());
                Plan plan = personProcessor.process(planningContext);


                boolean finishedPlanSearch = false;
//                while(!finishedPlanSearch) {
//                    TPS_PlanningContext pc = new TPS_PlanningContext(pe, hh.getLeastRestrictedCar(), person.hasBike(), person,hh);
//                    locationAndModeChooser.selectLocationsAndModesAndTravelTimes(pc, scheme, plan);
//
//                    plan.setTravelTimes();
//                    plan.balanceStarts();
//                    pe.addPlan(plan);
//
//                    if (feasibilityCalculator.isPlanFeasible(plan) && planEVA1Acceptance.isPlanAccepted(plan)) {
//                        finished = true;
//                    }
//
//                    if (!pc.needsOtherModeAlternatives(plan)) {
//                        finishedPlanSearch = true;
//                    }
//                }

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
                household.getLocation().getTrafficAnalysisZone().getBbrType());
        myAttributes.put(TPS_Attribute.PERSON_AGE_CLASS_CODE_STBA, person.getAgeClass().getCode(TPS_AgeClass.TPS_AgeCodeType.STBA));
        myAttributes.put(TPS_Attribute.PERSON_HAS_BIKE, person.hasBike() ? 1 : 0);
        myAttributes.put(TPS_Attribute.HOUSEHOLD_CARS, household.getNumberOfCars()); // TODO: note that this is set once again in selectLocationsAndModesAndTravelTimes
        myAttributes.put(TPS_Attribute.PERSON_SEX_CLASS_CODE, person.getSex().getCode());

        return myAttributes;
    }
}
