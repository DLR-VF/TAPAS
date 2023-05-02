/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.iteration;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.util.Randomizer;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * This Class provides some basic functionality which are used by more than one iteration method.
 *
 * @author hein_mh
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_BasicIterationManager extends TPS_IterationManagement {

    private final TPS_ParameterClass parameterClass;

    public TPS_BasicIterationManager(TPS_ParameterClass parameterClass) {
        super(parameterClass);
        this.parameterClass = parameterClass;
    }

    public ArrayList<Long> selectPlansForRecalculation(HashMap<Long, PlanDeviation> derivationMap, int actIter, int maxIter) {
        ArrayList<Long> plans = new ArrayList<>();

        String query = "";
        if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
            TPS_Logger.log(SeverityLogLevel.INFO, "Selecting households for recalculation");
        }
        //stupid first attempt: recalculate fix random rate!
        try {
            //query = "SELECT DISTINCT p_id, hh_id FROM "+ParamString.DB_TABLE_TRIPS.getString();
            query = "SELECT DISTINCT hh_id FROM " + this.parameterClass.getString(ParamString.DB_TABLE_TRIPS);
            ResultSet rs = dbManager.executeQuery(query, this);
            int hh_id;
            //int p_id;
            double randomRate = Math.exp(-(actIter + 1) / (maxIter));
            while (rs.next()) {
                if (Randomizer.random() <= randomRate) {
                    //p_id = rs.getInt("p_id");
                    hh_id = rs.getInt("hh_id");
                    //plans.add(this.hhAndPersonIDToHash(hh_id, p_id));
                    plans.add(this.hhAndPersonIDToHash(hh_id, 0));
                }
            }
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "Exception during SQL! Query: " + query, e);
        }
        return plans;
    }
}
