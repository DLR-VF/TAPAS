/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.plan;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.person.TPS_Person;
import de.dlr.ivf.tapas.plan.acceptance.TPS_PlanAcceptance;
import de.dlr.ivf.tapas.plan.acceptance.TPS_PlanEVA1Acceptance;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the plan environment for the simulation of one person. It stores all fix locations which were selected
 * for this person. Also there are all plans stored and after generating some of them you can choose the best plan you have
 * generated.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_PlanEnvironment {
    int numberOfRejectedPlans = 0;
    int numberOfRejectedSchemes = 0;
    int numberOfFeasiblePlans = 0;
    /// The person with this plan environment
    private final TPS_Person person;
    /// All plans which were generated for this person
    private final List<TPS_Plan> plans;
    private TPS_Plan plan;
    ///
    private TPS_PlanAcceptance acceptance = null;


    /**
     * Constructor
     *
     * @param person The person that gets this plan environment
     */
    public TPS_PlanEnvironment(TPS_Person person) {
        this.acceptance = new TPS_PlanEVA1Acceptance();
        this.plans = new ArrayList<>();
        this.person = person;
    }


    /**
     * This method adds a plan to all stored plans in this environment
     *
     * @param plan The plan to add
     */
    public void addPlan(TPS_Plan plan) {
        this.plans.add(plan);
        if (plan.isPlanAccepted()) {
            numberOfFeasiblePlans++;
        } else {
            numberOfRejectedPlans++;
        }
    }

    public void dismissScheme() {
        ++numberOfRejectedSchemes;
    }

    /**
     * @return best plan of all stored
     */
    public TPS_Plan getBestPlan() {
        this.plans.sort((o1, o2) -> {
            // TODO minor thing: wouldn't it be possible that negative acceptance probabilities indicate the plan is not feasible?
            // TODO Would save some lines, here
            boolean feasible1 = o1.isPlanFeasible();
            boolean feasible2 = o2.isPlanFeasible();
            if (feasible1 && feasible2) {
                if (o1.getAcceptanceProbability() == o2.getAcceptanceProbability()) {
                    return 0;
                }
                return o1.getAcceptanceProbability() < o2.getAcceptanceProbability() ? 1 : -1;
            } else if (feasible1) {
                return -1;
            } else if (feasible2) {
                return 1;
            } else {
                if (o1.getAcceptanceProbability() == o2.getAcceptanceProbability()) {
                    return 0;
                }
                return o1.getAcceptanceProbability() < o2.getAcceptanceProbability() ? 1 : -1;
            }
        });
		/* validation check
TODO: remove after a while
		double lastAcceptance = 100000;
		boolean lastWasFeasible = true;
		boolean correct = true;
		for(TPS_Plan plan: this.getPlans()) {
			if(!plan.isPlanFeasible()) {
				if(lastWasFeasible) {
					// ok, switching from feasible to not feasible
					lastWasFeasible = false;
					lastAcceptance = plan.getAcceptanceProbability();
					continue;
				}
			}
			// feasibility as before, check acceptance
			if(lastAcceptance<plan.getAcceptanceProbability()) {
				correct = false;
			}
			lastAcceptance = plan.getAcceptanceProbability();
		}
		if(!correct) {
			for(TPS_Plan plan: this.getPlans()) {
				System.err.println(plan.isPlanFeasible() + ";" + plan.getAcceptanceProbability());
			}
		}
*/
        return this.getPlans().get(0);
    }

    /**
     * @return last added plan
     */
    public TPS_Plan getLastPlan() {
        return this.getPlans().get(this.getPlans().size() - 1);
    }

    public int getNumberOfFeasiblePlans() {
        return numberOfFeasiblePlans;
    }

    public int getNumberOfRejectedPlans() {
        return numberOfRejectedPlans;
    }

    /**
     * @return the person corresponding to this plan environment
     */
    public TPS_Person getPerson() {
        return this.person;
    }

    /**
     * @return all plans which were generated for this person
     */
    public List<TPS_Plan> getPlans() {
        return plans;
    }

    public boolean isPlanAccepted(TPS_Plan plan) {
        //		if(!accepted){
//			//release all cars used for this plan
//			for (TPS_SchemePart schemePart : plan.getScheme()) {
//				if (!schemePart.isHomePart()) {
//					TPS_TourPart tourpart = (TPS_TourPart) schemePart;
//					tourpart.releaseCar();
//				}
//			}
//		}
//		else{
//
//			//release car from 2nd best plan
//			if(this.getPlans().size()>=2){
//				//sort
//				Collections.sort(this.plans, new Comparator<TPS_Plan>() {public int compare(TPS_Plan o1, TPS_Plan o2) {
//					boolean feasible1 = o1.isPlanFeasible(), feasible2 = o2.isPlanFeasible();
//					if(feasible1 && feasible2)
//						return o1.getAcceptanceProbability() < o2.getAcceptanceProbability() ? 1 : -1;
//					else if(feasible1)
//						return 1;
//					else if(feasible2)
//						return -1;
//					else
//						return o1.getAcceptanceProbability() < o2.getAcceptanceProbability() ? 1 : -1;
//				}});
//				//select 2nd best plan and detach car
//				for (TPS_SchemePart schemePart : this.getPlans().get(this.getPlans().size()-2).getScheme()) {
//					if (!schemePart.isHomePart()) {
//						TPS_TourPart tourpart = (TPS_TourPart) schemePart;
//						tourpart.releaseCar();
//					}
//				}
//			}
//		}
        return this.acceptance.isPlanAccepted(plan);
    }
    public TPS_Plan getFirstPlan(){
        return this.plans.get(0);
    }

}
