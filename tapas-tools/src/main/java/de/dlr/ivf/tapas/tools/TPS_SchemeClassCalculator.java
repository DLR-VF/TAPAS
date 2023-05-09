/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Supplier;

public class TPS_SchemeClassCalculator {
    private static final double slotFactor = 5.0;
    /**
     * Connection to the database
     */
    private final Supplier<Connection> con;
    private int minSchemeClass;
    private int maxSchemeClass;
    private final HashMap<Integer, double[]> SchemeClassEntries;

    TPS_SchemeClassCalculator(Supplier<Connection> connectionSupplier) throws IOException, SQLException, ClassNotFoundException {
        String statement = "select scheme_class_id from core.global_scheme_class_distributions order by " +
                "scheme_class_id limit 1";

        this.con = connectionSupplier;
        try(Connection connection = con.get()){
            try(PreparedStatement st = connection.prepareStatement(statement);
                ResultSet rs = st.executeQuery()){
                if (rs.next()) {
                    this.minSchemeClass = rs.getInt(1);
                } else {
                    this.minSchemeClass = -1;
                }
            }catch (SQLException e){
                e.printStackTrace();
            }
            statement = "select scheme_class_id from core.global_scheme_class_distributions order by scheme_class_id desc limit 1";
            try(Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(statement)) {
                if (rs.next()) {
                    this.maxSchemeClass = rs.getInt(1);
                } else {
                    this.maxSchemeClass = -1;
                }
            }catch (SQLException e) {
                e.printStackTrace();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        this.SchemeClassEntries = new HashMap<>();
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        File configFile = new File("T:/Simulationen/runtime.csv");
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(configFile);

        Supplier<Connection> connectionSupplier = () -> null;
        TPS_SchemeClassCalculator calculator = new TPS_SchemeClassCalculator(connectionSupplier);
        calculator.readData();
        calculator.writeSchemeClassValues();


    }

    private void readData() throws SQLException {
        double avg;
        double stdDev;
        double tmp;
        double[] values;
        int count;
        Vector<Double> vals = new Vector<>();
        String statement = "select scheme_id,act_code_zbe from core.global_episodes order by scheme_id, start";

        try(Connection connection = con.get()){

            try(PreparedStatement st = connection.prepareStatement(statement);
                ResultSet rs = st.executeQuery()) {

                int lastId = -1, lastActCode = -1, id, actCode;
                while (rs.next()) {
                    id = rs.getInt("scheme_id");
                    actCode = rs.getInt("act_code_zbe");
                    if (id == lastId) {
                        if ((actCode == 80 || actCode == 880) && (lastActCode == 80 || lastActCode == 880))
                            System.out.println("bad scheme: " + id);
                    }
                    lastActCode = actCode;
                    lastId = id;
                }
            }catch (SQLException e){
                e.printStackTrace();
            }

            int i, j;
            for (i = this.minSchemeClass; i <= this.maxSchemeClass; ++i) {
                statement =
                        "select sum(duration) from core.global_episodes epis where act_code_zbe in (80, 880)  and scheme_id IN (select scheme_id from core.global_schemes where scheme_class_id = " +
                                i + ") group by scheme_id";
                count = 0;
                try (PreparedStatement st = connection.prepareStatement(statement);
                     ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        vals.add(rs.getInt(1) * slotFactor);
                        count += 1;
                    }
                }catch (SQLException e){
                    e.printStackTrace();
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
    }

    private void writeSchemeClassValues() throws SQLException {
        double[] values;
        String statement;

        try(Connection connection = con.get()){
            for (Integer id : this.SchemeClassEntries.keySet()) {
                values = this.SchemeClassEntries.get(id);
                statement = "SELECT scheme_class_id from core.global_scheme_classes where scheme_class_id = " + id;
                try(PreparedStatement st = connection.prepareStatement(statement);
                    ResultSet rs = st.executeQuery()) {

                    if (rs.next()) { // class exists: update
                        statement = "UPDATE core.global_scheme_classes SET avg_travel_time = " + values[0] + ", proz_std_dev = " +
                                values[1] + " WHERE scheme_class_id = " + id;
                    } else {//class does not exist: create
                        statement = "INSERT INTO core.global_scheme_classes (scheme_class_id, avg_travel_time , proz_std_dev) VALUES ( " +
                                id + " , " + values[0] + " , " + values[1] + " )";
                    }
                    Statement st2 = connection.createStatement();
                    st2.execute(statement);
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
}
