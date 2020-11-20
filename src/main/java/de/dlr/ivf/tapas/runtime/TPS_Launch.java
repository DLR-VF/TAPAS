/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime;

import com.csvreader.CsvReader;
import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.runtime.client.SimulationControl;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

public final class TPS_Launch {
    // !!! // from SimulationControl add simulation
    private static int loadParameters(File name, Stack<File> parameterFiles, HashMap<String, String> parameters) throws IOException {
        String key;
        String value;
        String parent;
        int counter = 0;
        CsvReader reader = new CsvReader(new FileReader(name.getAbsolutePath()));
        reader.readHeaders();
        while (reader.readRecord()) {
            key = reader.get(0);
            value = reader.get(1);

            if (key.equals("FILE_PARENT_PROPERTIES") || key.equals("FILE_FILE_PROPERTIES") || key.equals(
                    "FILE_LOGGING_PROPERTIES") || key.equals("FILE_PARAMETER_PROPERTIES") || key.equals(
                    "FILE_DATABASE_PROPERTIES")) {
                parent = name.getParent();
                while (value.startsWith("./")) {
                    value = value.substring(2);
                    parent = new File(parent).getParent();
                }
                parameterFiles.push(new File(parent, value));
            } else {
                if (!parameters.containsKey(key)) {
                    counter++;
                    parameters.put(key, value); // this does not overwrites old
                    // values!
                }
            }
        }
        reader.close();

        return counter;
    }

    /**
     * @param args - [0] contains TAPAS network directory
     *             [1] contains relative path and filename of sim_file
     *             [2] contains sim_key
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        File file = new File(args[0], args[1]);
        String sim_key = args[2];
        // !!! from SimulationControl add simulation
        String fullFileName = file.getAbsolutePath();
        // now read the parameters and store them into the db
        HashMap<String, String> parameters = new HashMap<>();
        Stack<File> parameterFiles = new Stack<>();
        parameterFiles.push(new File(fullFileName));
        while (!parameterFiles.empty()) {
            loadParameters(parameterFiles.pop(), parameterFiles, parameters);
        }
        //fix the SUMO-dir by appending the simulation run!
        String paramVal = parameters.get("SUMO_DESTINATION_FOLDER");
        paramVal += "_" + sim_key;
        parameters.put("SUMO_DESTINATION_FOLDER", paramVal);


        Random generator;
        if (parameters.get("RANDOM_SEED_NUMBER") != null && Boolean.parseBoolean(
                parameters.get("FLAG_INFLUENCE_RANDOM_NUMBER"))) {
            generator = new Random(Long.parseLong(parameters.get("RANDOM_SEED_NUMBER")));
        } else {
            generator = new Random();
        }
        double randomSeed = generator.nextDouble(); // postgres needs a double
        // value as seed ranging
        // from 0 to 1
        String array = parameters.get("DB_REGION") + "," + parameters.get("DB_HOUSEHOLD_AND_PERSON_KEY") + "," +
                randomSeed + "," + Double.parseDouble(parameters.get("DB_HH_SAMPLE_SIZE")) + ", ";
        if (TPS_DB_IOManager.Behaviour.FAT.equals(TPS_DB_IOManager.BEHAVIOUR)) array = array.concat("FALSE");
        else array = array.concat("TRUE");
        // new parameter: iteration
        array = array.concat(",0");
        // String query =
        // ("INSERT INTO simulations (sim_key, sim_file, sim_par) "
        // + " VALUES('" + sim_key + "', '"
        // + simulation.getRelativeFileName() + "', '{" + array +
        // "}'::character varying[])");

        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.setValue(ParamString.DB_DRIVER.name(), parameters.get("DB_DRIVER"));
        parameterClass.setValue(ParamString.DB_TYPE.name(), parameters.get("DB_TYPE"));
        parameterClass.setValue(ParamString.DB_HOST.name(), parameters.get("DB_HOST"));
        parameterClass.setValue(ParamValue.DB_PORT.name(), parameters.get("DB_PORT"));
        parameterClass.setValue(ParamString.DB_DBNAME.name(), parameters.get("DB_DBNAME"));
        TPS_DB_Connector dbConnection = new TPS_DB_Connector("dkrajzew", "dead.1", parameterClass);
        String query = ("INSERT INTO simulations (sim_key, sim_file, sim_par, sim_description) " + " VALUES('" +
                sim_key + "', '"
//				+ this.getParameters().SIM_DIR
//				+ new File(this.props.get(ClientControlPropKey.LOGIN_CONFIG)).getName()
                + "', '{" + array + "}'::character varying[],'" + fullFileName + "')");

        dbConnection.execute(query, sim_key);
        //boolean addConfigToDB = true;
        //if (addConfigToDB) {
        //  this.getParameters().writeToDB(dbConnection.getConnection(sim_key), sim_key, parameters);
        //}
        parameterClass.writeToDB(dbConnection.getConnection(sim_key), sim_key, parameters);
        // end of simulation control code

        TPS_Main main = new TPS_Main(file, args[2]);
        main.init();
        main.run(1);//Runtime.getRuntime().availableProcessors());
        main.finish();
        main.getPersistenceManager().close();
        TPS_Main.STATE.setFinished();

    }

}
