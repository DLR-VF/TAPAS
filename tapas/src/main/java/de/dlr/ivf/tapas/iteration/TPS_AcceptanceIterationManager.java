/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.iteration;

import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.util.TPS_GX;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This Class provides a method to select plans according to their acceptance and acceptance delta.
 *
 * @author hein_mh
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_AcceptanceIterationManager extends TPS_IterationManagement {

    private final TPS_ParameterClass parameterClass;

    TPS_AcceptanceIterationManager(TPS_ParameterClass parameterClass) {
        super(parameterClass);
        this.parameterClass = parameterClass;
    }

    public ArrayList<Long> selectPlansForRecalculation(HashMap<Long, PlanDeviation> derivationMap, int actIter, int maxIter) {
        ArrayList<Long> plans = new ArrayList<>();

        String query = "";
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Selecting households for recalculation");
        }

        try {
            ResultSet rs;
            // Anschmiegeparameter der EVA1-Funktion
            double EBottom = this.parameterClass.getDoubleValue(ParamValue.OVERALL_TIME_E);
            // EVA1-Parameter, der das Absinken der Akzeptanz bei geringer Überschreitung beeinflußt
            double FTop = this.parameterClass.getDoubleValue(ParamValue.OVERALL_TIME_F);
            double TurningPoint = this.parameterClass.getDoubleValue(ParamValue.MAX_TIME_DIFFERENCE);
            //time slot factor für die Tagespläne
            int timeSlotFactor = this.parameterClass.getIntValue(ParamValue.SEC_TIME_SLOT);

            for (Entry<Long, PlanDeviation> entry : derivationMap.entrySet()) {
                int hh_id = this.hashToHHID(entry.getKey());
                int p_id = this.hashToPersonID(entry.getKey());
                int schemeID;
                int origTT;
                double oldAcc, newAcc, ratio, randomRate;
                //get scheme id
                query = "SELECT scheme_id FROM " + this.parameterClass.getString(ParamString.DB_TABLE_TRIPS) +
                        " WHERE hh_id =" + hh_id + " AND p_id = " + p_id + " LIMIT 1";
                rs = dbManager.executeQuery(query, this);
                if (rs.next()) {
                    schemeID = rs.getInt("scheme_id");
                } else {
                    if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                        TPS_Logger.log(SeverityLogLevel.WARN,
                                "Household " + hh_id + " person " + p_id + " not found in trip-table. Query: " + query);
                    }
                    continue;
                }

                //get scheme-travel time
                origTT = 0;
                query = "SELECT duration FROM " + this.parameterClass.getString(ParamString.DB_TABLE_EPISODE) +
                        " WHERE scheme_id =" + schemeID + " AND act_code_zbe = ANY(ARRAY[]) AND key = '" +
                        this.parameterClass.getString(ParamString.DB_EPISODE_KEY)+ "'";
                rs = dbManager.executeQuery(query, this);
                while (rs.next()) {
                    origTT += rs.getInt("duration");
                }

                origTT *= timeSlotFactor;


                //calc old acceptance
                ratio = entry.getValue().oldTime / origTT;
                ratio = Math.abs(ratio - 1.0);
                oldAcc = TPS_GX.calculateEVA1Acceptance(ratio, EBottom, FTop, TurningPoint);


                //calc new acceptance
                ratio = entry.getValue().newTime / origTT;
                ratio = Math.abs(ratio - 1.0);
                newAcc = TPS_GX.calculateEVA1Acceptance(ratio, EBottom, FTop, TurningPoint);

                //calc random rate
                randomRate = (1 - newAcc) * (1 - Math.abs(oldAcc - newAcc));

                if (Randomizer.random() <= randomRate) {
                    plans.add(this.hhAndPersonIDToHash(hh_id, 0));
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "Exception during SQL! Query: " + query, e);
        }
        return plans;
    }
}
