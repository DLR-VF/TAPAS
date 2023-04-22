/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.plan.acceptance;


import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;
import de.dlr.ivf.tapas.plan.TPS_Plan;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.TPS_GX;
import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamValue;

/**
 * This class is an implementation of the {@link TPS_PlanAcceptance} interface and uses the EVA(1) function of Lohse
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class TPS_PlanEVA1Acceptance implements TPS_PlanAcceptance {

    /**
     * Calculates the acceptance probability of the plan constructed resulting from the financial expenditures
     * associated with the plan execution while accounting for the mobility budget available
     *
     * @param plan plan which is used to determine acceptance
     * @return acceptance probability of the plan between 0.0 and 1.0
     */
    private double calculateAcceptanceProbBudgetConstraints(TPS_Plan plan) {
        double budgetAcceptanceProbability = 0.0;
        // children under 25 years living at a household with their parents
        // don't have an own budget due to data restrictions;
        // default value: 99999 each for variable costs pt and car

        double myFinancialBudgetVariable =
                plan.getPerson().getBudget() / 30.5; //30.5 days per month in average: 365d/12m

        // Default value of 99999 each for variable budget pt and car for children resulting in following value for
        // automated acceptance : 2* 99999 / 30 when calculated per day
        // this value is store
        if (myFinancialBudgetVariable == 2 * 99999 / 30) { // TODO integer compared to float?
            budgetAcceptanceProbability = 1;
        } else {
            // Anschmiegeparameter der EVA1-Funktion
            double EBottom = plan.getParameters().getDoubleValue(ParamValue.FINANCE_BUDGET_E);
            // EVA1-Parameter, der das Absinken der Akzeptanz bei geringer Überschreitung beeinflußt
            double FTop = plan.getParameters().getDoubleValue(ParamValue.FINANCE_BUDGET_F);
            // Wendepunkt der Funktion; ein Wendepunkt von 0.5 besagt, dass es bei einer Überschreitung der Budget-
            // vorgaben um 50 % zu einem starken Abfall der Akzeptanz des Plans kommt
            double TurningPoint = plan.getParameters().getDoubleValue(ParamValue.FINANCE_BUDGET_WP);
            // Verhältnis des Ausgaben zum Budget; nur bei Überschreitung (>1) wird auf Akzeptanz getestet, sonst
            // immer 1
            double ratio = plan.getTravelCosts() / myFinancialBudgetVariable;

            if (ratio <= 1) {
                budgetAcceptanceProbability = 1;// Tagepläne bei denen die finanziellen Grenzen eingehalten werden
                // werden
                // akzeptiert
            } else {
                ratio = ratio - 1; // Basis ist immer 0, Abweichung darauf basierend
                budgetAcceptanceProbability = TPS_GX.calculateEVA1Acceptance(ratio, EBottom, FTop, TurningPoint);
            }
            if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
                TPS_Logger.log(SeverenceLogLevel.DEBUG,
                        "Plan with financial costs=" + plan.getTravelCosts() + " and person's budget=" +
                                myFinancialBudgetVariable + " leads to [ratio=" + ratio + ", acceptance=" +
                                budgetAcceptanceProbability + "]");
            }
        }

        plan.setBudgetAcceptanceProbability(budgetAcceptanceProbability);

        return budgetAcceptanceProbability;
    }

    /**
     * Calculates the acceptance probability of the plan constructed resulting from the time expenditures associated
     * with the plan execution while accounting for the available time budget and the flexibility / the time constraints
     * associated with the person type. Function copes both with negative and positive deviance from the reference
     * value.
     *
     * @param plan plan which is used to determine acceptance
     * @return acceptance probability; between 0.0 and 1.0
     */
    private double calculateAcceptanceProbTimeConstraints(TPS_Plan plan) {
        double timeAcceptanceProbability = 0.0;
        // Anschmiegeparameter der EVA1-Funktion
        double EBottom = plan.getParameters().getDoubleValue(ParamValue.TIME_BUDGET_E);
        // EVA1-Parameter, der das Absinken der Akzeptanz bei geringer Überschreitung beeinflusst
        double FTop = plan.getParameters().getDoubleValue(ParamValue.TIME_BUDGET_F);
        // stattdessen Infos aus Tagebuchklasse!
        // double TurningPoint = myTurningPointTime;

        double myTimeBudget = 1.0 / plan.getScheme().getTimeUsageAVG();
        double TurningPoint = plan.getScheme().getTimeUsageSTD() * myTimeBudget;

        // prozentuale Abweichungen müssen normiert werden, um die EVA1-Funktion verwenden zu können (berücksichtigt nur
        // positive Aufwände)
        // heißt: zeitliche Über- als Unterschreitungen werden gleich behandelt
        double ratio = plan.getTravelDuration() * myTimeBudget;

        if (ratio == 0 || ratio == 1 || ratio <= 1.0) {
            timeAcceptanceProbability = 1.0; // Tagespläne ohne Trips haben keine zeitlichen Kosten; wenn die zeitlichen
            // Kosten stimmen
            // direkt akzeptieren
        } else {
            ratio = Math.abs(ratio - 1.0);
            timeAcceptanceProbability = TPS_GX.calculateEVA1Acceptance(ratio, EBottom, FTop, TurningPoint);
        }
        if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(SeverenceLogLevel.DEBUG,
                    "Plan with time costs=" + plan.getTravelDuration() + " and person's budget=" + 1.0 / myTimeBudget +
                            " leads to [ratio=" + ratio + ", acceptance=" + timeAcceptanceProbability + "]");
        }

        plan.setTimeAcceptanceProbability(timeAcceptanceProbability);

        return timeAcceptanceProbability;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.acceptance.plan.tapas.ivf.TPS_PlanAcceptance#isPlanAccepted(de.dlr.de.dlr.ivf.plan.tapas.ivf.TPS_Plan)
     */
    public boolean isPlanAccepted(TPS_Plan plan) {
        boolean accepted = this.isPlanAcceptedByOverallTravelTime(plan);
        // Check additional constraints (average travel time of scheme class and budget) if plan is accepted in general
        if (accepted && plan.getParameters().isTrue(ParamFlag.FLAG_CHECK_BUDGET_CONSTRAINTS)) {
            accepted = this.isPlanAcceptedByAdditionalConstraints(plan);
        }
        return accepted;
    }

    /**
     * This method checks all additional constraints (here: financial costs and travel time acceptance using the average
     * travel time of the whole scheme class of the plan's scheme)
     *
     * @param plan plan which is used to determine acceptance
     * @return true if all additional constraints are accepted, false otherwise
     */
    private boolean isPlanAcceptedByAdditionalConstraints(TPS_Plan plan) {
        boolean accepted = false;
        double acceptanceByTime = this.calculateAcceptanceProbTimeConstraints(plan), acceptanceByBudget = this
                .calculateAcceptanceProbBudgetConstraints(plan);

        /*
         * TODO @Rita: increase selection probs of shorter schemes or schemes with trip chains instead of single
         * trips
         */
        accepted = Randomizer.random() < acceptanceByTime * acceptanceByBudget;
        return accepted;
    }

    /**
     * Determines whether the travel times resulting from the chosen modes and locations for the plan exceed too much
     * the travel times initially scheduled for the scheme. The acceptance limit is defined in the configuration.
     *
     * @param plan the constructed day plan to be checked for acceptance
     * @return true, if the plan with its travel times is not too far off the designated travel times; false if the
     * travel times exceed the maximum difference defined in the configuration
     */
    private boolean isPlanAcceptedByOverallTravelTime(TPS_Plan plan) {
        boolean accepted = false;
        double acceptance = 1.0;
        double originalDuration = plan.getScheme().getOriginalTravelDuration();
        double realDuration = plan.getTravelDuration();
        double ratio = 0;
        if (Double.compare(0.0, originalDuration) == 0 || Double.compare(0.0, realDuration) == 0) {
            // day plans without trips do not have travel times associated --> accept directly
            accepted = true;
            plan.setAcceptanceProbability(1);

        } else {
            // Anschmiegeparameter der EVA1-Funktion
            double EBottom = plan.getParameters().getDoubleValue(ParamValue.OVERALL_TIME_E);
            // EVA1-Parameter, der das Absinken der Akzeptanz bei geringer Überschreitung beeinflußt
            double FTop = plan.getParameters().getDoubleValue(ParamValue.OVERALL_TIME_F);
            double TurningPoint = plan.getParameters().getDoubleValue(ParamValue.MAX_TIME_DIFFERENCE);

            ratio = realDuration / originalDuration;
            ratio = Math.abs(ratio -
                    1.0);  //even shorter plans should be rejected due to the magic "84min travel time"-postulation
//			if(realDuration <originalDuration){
//				ratio /=2; //shorter durations are only "halve bad"
//			}

            acceptance = TPS_GX.calculateEVA1Acceptance(ratio, EBottom, FTop, TurningPoint);
            if (Double.isNaN(acceptance) || Double.isInfinite(acceptance)) {
                TPS_Logger.log(SeverenceLogLevel.FATAL, "Plan has NaN acceptance!");
                plan.setPlanFeasible(false);
                accepted = false;
            } else {
                plan.setAcceptanceProbability(acceptance);
                accepted = Randomizer.random() < acceptance;
            }
        }
        if (TPS_Logger.isLogging(SeverenceLogLevel.DEBUG)) {
            TPS_Logger.log(SeverenceLogLevel.DEBUG,
                    "Plan with travel times [orig=" + originalDuration + ", real=" + realDuration +
                            "] leads to [ratio=" + ratio + ", acceptance=" + acceptance + "] is accepted => " +
                            accepted);
        }

        return accepted;
    }
}
