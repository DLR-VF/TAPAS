/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;

public class TPS_SchemeClassCalculator {
    private static final double slotFactor = 5.0;
    /**
     * Connection to the database
     */
    private final Connection con;
    private final Statement st;
    private final int minSchemeClass;
    private final int maxSchemeClass;
    private final HashMap<Integer, double[]> SchemeClassEntries;

    TPS_SchemeClassCalculator(TPS_ParameterClass parameterClass) throws IOException, SQLException, ClassNotFoundException {
        TPS_DB_Connector manager;
        manager = new TPS_DB_Connector(parameterClass);
        this.con = manager.getConnection(this);
        this.con.setAutoCommit(false);
        this.con.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
        this.st = this.con.createStatement();
        String statement = "select scheme_class_id from core.global_scheme_class_distributions order by " +
                "scheme_class_id limit 1";
        ResultSet rs = this.st.executeQuery(statement);
        this.con.commit();
        if (rs.next()) {
            this.minSchemeClass = rs.getInt(1);
        } else {
            this.minSchemeClass = -1;
        }

        statement = "select scheme_class_id from core.global_scheme_class_distributions order by scheme_class_id desc limit 1";
        rs = this.st.executeQuery(statement);
        this.con.commit();
        if (rs.next()) {
            this.maxSchemeClass = rs.getInt(1);
        } else {
            this.maxSchemeClass = -1;
        }
        this.con.setAutoCommit(true);
        this.SchemeClassEntries = new HashMap<>();
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        File configFile = new File("T:/Simulationen/runtime.csv");
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(configFile);

        TPS_SchemeClassCalculator calculator = new TPS_SchemeClassCalculator(parameterClass);
        calculator.readData();
        calculator.writeSchemeClassValues();


    }

    protected void finalize() throws Throwable {
        this.st.close();
        this.con.close();
        this.SchemeClassEntries.clear();
    }

    private void readData() throws SQLException {
        double avg;
        double stdDev;
        double tmp;
        double[] values;
        int count;
        Vector<Double> vals = new Vector<>();
        String statement = "select scheme_id,act_code_zbe from core.global_episodes order by scheme_id, start";
        ResultSet rs = this.st.executeQuery(statement);
        int lastId = -1, lastActCode = -1, id, actCode;
        while (rs.next()) {
            id = rs.getInt("scheme_id");
            actCode = rs.getInt("act_code_zbe");
            if (id == lastId) {
                if ((actCode == 80 || actCode == 880) && (lastActCode == 80 || lastActCode == 880)) System.out.println(
                        "bad scheme: " + id);
            }
            lastActCode = actCode;
            lastId = id;
        }


        int i, j;
        for (i = this.minSchemeClass; i <= this.maxSchemeClass; ++i) {
            statement =
                    "select sum(duration) from core.global_episodes epis where act_code_zbe in (80, 880)  and scheme_id IN (select scheme_id from core.global_schemes where scheme_class_id = " +
                            i + ") group by scheme_id";
            rs = this.st.executeQuery(statement);
            this.con.commit();

            count = 0;
            while (rs.next()) {
                vals.add(rs.getInt(1) * slotFactor);
                count += 1;
            }
            if (count > 0) {
                avg = 0;
                for (j = 0; j < vals.size(); ++j) {
                    avg += vals.elementAt(j);
                }
                avg = avg / (double) count;
                stdDev = 0;
                for (j = 0; j < vals.size(); ++j) {
                    tmp = (vals.elementAt(j) - avg);
                    stdDev += tmp * tmp;
                }

                stdDev /= (count - 1);
                stdDev = Math.sqrt(stdDev);
            } else { // no entries for this class!
                avg = 1;
                stdDev = 1;
            }

            values = new double[2];
            values[0] = avg;
            values[1] = stdDev / avg;
            this.SchemeClassEntries.put(i, values);
            vals.clear();
        }
        for (Integer ids : this.SchemeClassEntries.keySet()) {
            values = this.SchemeClassEntries.get(ids);
            System.out.println("Klasse: " + ids + " avg: " + values[0] + " sdtDev: " + values[1]);
        }
        System.out.println("Ende Read");
    }

    private void writeSchemeClassValues() throws SQLException {
        double[] values;
        String statement;
        ResultSet rs;

        for (Integer id : this.SchemeClassEntries.keySet()) {
            values = this.SchemeClassEntries.get(id);
            statement = "SELECT scheme_class_id from core.global_scheme_classes where scheme_class_id = " + id;
            rs = this.st.executeQuery(statement);
            this.con.commit();
            if (rs.next()) { // class exists: update
                statement =
                        "UPDATE core.global_scheme_classes SET avg_travel_time = " + values[0] + ", proz_std_dev = " +
                                values[1] + " WHERE scheme_class_id = " + id;
            } else {//class does not exist: create
                statement =
                        "INSERT INTO core.global_scheme_classes (scheme_class_id, avg_travel_time , proz_std_dev) VALUES ( " +
                                id + " , " + values[0] + " , " + values[1] + " )";
            }
            this.st.execute(statement);
        }
    }
}
