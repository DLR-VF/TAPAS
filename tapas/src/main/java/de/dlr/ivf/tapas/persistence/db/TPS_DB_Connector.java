/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides in general two services. After you built one instance you
 * have one new client connected to the database. Then you can retrieve a
 * connection corresponding to a special key value. This connection is always
 * open. The second service is to close a connection.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.CLIENT)
public class TPS_DB_Connector {

    public TPS_ParameterClass parameterClass;
    private String myUserName;
    private String myUserPwd;
    /**
     * The connection to the database. The connection to the database is
     * established in the constructor and closed when the application exits, via
     * a shutdown hook.
     */
    private final Map<Object, Connection> connections;

    /**
     * The constructor has three parts. Fist of all the database properties are
     * read from a file. Then these properties are checked if they have correct
     * values. The connections are opened in the TPS_DB_Connector#getConnection(Object) routine.
     *
     * @param password
     * @param username
     * @throws ClassNotFoundException This exception is thrown if the driver in
     *                                ParamString.DB_DRIVER was not found
     * @throws UnknownHostException   This exception is thrown if the host in ParamString.DB_HOST
     *                                was not found
     */
    public TPS_DB_Connector(String username, String password, TPS_ParameterClass parameterClass) throws UnknownHostException, ClassNotFoundException {
        this.myUserName = username;
        this.myUserPwd = password;
        this.parameterClass = parameterClass;
        checkProperties(this.parameterClass);
        this.connections = new HashMap<>();
    }

    public TPS_DB_Connector(TPS_ParameterClass parameterClass) throws UnknownHostException, ClassNotFoundException {
        this(parameterClass.getString(ParamString.DB_USER), parameterClass.getString(ParamString.DB_PASSWORD),
                parameterClass);
    }

    /**
     * This method checks all properties. It calls checkProperty(String key) for
     * every key except the password key. Furthermore some property values are
     * checked in detail, e.g. if the driver for the database exists, if the
     * host exists or if the port has a correct integer value etc.
     *
     * @throws ClassNotFoundException   This exception is thrown if the driver in
     *                                  ParamString.DB_DRIVER was not found
     * @throws UnknownHostException     This exception is thrown if the host in ParamString.DB_HOST
     *                                  was not found
     * @throws IllegalArgumentException This exception is thrown if the port is outside the value
     *                                  range of [1,65534]
     */
    private static void checkProperties(TPS_ParameterClass parameterClass) throws ClassNotFoundException, UnknownHostException {
        checkProperty(ParamString.DB_DRIVER, parameterClass);
        // check if the driver exists
        Class.forName(parameterClass.getString(ParamString.DB_DRIVER));

        checkProperty(ParamString.DB_TYPE, parameterClass);

        checkProperty(ParamString.DB_HOST, parameterClass);
        // check if host exists
        String host = parameterClass.getString(ParamString.DB_HOST);
        InetAddress.getByName(host);

        // check if port is between [0,65535]
        int port = parameterClass.paramValueClass.getIntValue(ParamValue.DB_PORT);
        if (port < 0 || port > 65535) throw new IllegalArgumentException(
                "The value for 'port' is outside the correct range [0, 65535]: " + port);

        checkProperty(ParamString.DB_DBNAME, parameterClass);
    }

    /**
     * This method checks, if the property value corresponding to the given key
     * exists and has a correct value (i.e. a length greater than 0)
     *
     * @param key property key
     * @throws NullPointerException This exception is thrown, when the property value is null or
     *                              has a length of 0
     */
    private static void checkProperty(ParamString key, TPS_ParameterClass parameterClass) {
        String value = parameterClass.getString(key);
        if (value == null || value.length() == 0) {
            throw new NullPointerException("Value to key '" + key + "' not set");
        }
    }

    /**
     * This method checks if the connection is still alive.
     *
     * @param caller Caller object, which should be checked for connectivity.
     * @return
     */
    public boolean checkConnection(Object caller) {
        try {
            return connections.get(caller) != null && !connections.get(caller).isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * This method checks the basic connectivity to the database. It builds up a
     * connection, sends a basic query and closes the connection. If no
     * exception is thrown everything is alright.
     *
     * @throws SQLException TZhis exception is thrown if a task in the connectivity check
     *                      fails
     */
    void checkConnectivity() throws SQLException {
        Connection con = this.openConnection();
        Statement s = con.createStatement();
        ResultSet set = s.executeQuery(
                "SELECT core.check_user('" + this.parameterClass.getString(ParamString.DB_USER) + "')");
        while (set.next()) {
            if (TPS_Logger.isLogging(SeverityLogLevel.INFO)) {
                TPS_Logger.log(SeverityLogLevel.INFO,
                        "Connectivity check for user " + this.parameterClass.getString(ParamString.DB_USER) +
                                " was successful");
            }
        }
        set.close();
        if (!con.getAutoCommit()) con.commit();
        s.close();
        con.close();
    }

    /**
     * Closes the database connection specified via the key
     *
     * @param key object key of the connection
     * @throws SQLException Exception is thrown in case of an error trying to close the
     *                      db connection
     */
    public void closeConnection(Object key) throws SQLException {
        Connection c = this.connections.remove(key);
        if (c != null) {
            if (!c.getAutoCommit()) c.commit();
            c.close();
        }
    }

    /**
     * If a connection to a database exists this method tries to close it.
     */
    public void closeConnections() {
        for (Object key : this.connections.keySet().toArray()) {
            try {
                this.closeConnection(key);
            } catch (SQLException e) {
                TPS_Logger.log(SeverityLogLevel.ERROR, e.getMessage(), e);
            }
        }
    }

    /**
     * This method commits the db-changes if autocommit is disabled.
     *
     * @param c the connection to commit
     * @throws SQLException any exception, e.g connection loss
     */
    public void commit(Connection c) throws SQLException {
        if (!c.getAutoCommit()) c.commit();
    }

    /**
     * This method does a sql-execute for the given string with the given calling class. Each calling class gets its own connection
     *
     * @param query  The sql-query
     * @param caller the caller Object. If this Object made a call before, Its connection is reused. Otherwise a new connection is created.
     */
    public void execute(String query, Object caller) {
        boolean finished = false;
        SQLException ex = null;
        int tries = 0;
        while (!finished && tries < 10) {
            try {
                Connection con = this.getConnection(caller);
                if (con != null) {
                    synchronized (this.getConnection(caller)) {
                        Statement s = this.getConnection(caller).createStatement();
                        s.execute(query);
                        //commit(this.getConnection(caller));
                        s.close();
                        finished = true;
                    }
                }
            } catch (SQLException e) {
                finished = !isSqlConnectionError(e);
                tries++;
                ex = e;
            } catch (Exception e) {
                TPS_Logger.log(SeverityLogLevel.ERROR, e);
                ex = new SQLException(e);
            }
        }
        if (ex != null) {
            TPS_Logger.log(SeverityLogLevel.ERROR, ex);
            TPS_Logger.log(SeverityLogLevel.ERROR, "Next exception:");
            TPS_Logger.log(SeverityLogLevel.ERROR, ex.getNextException());
            System.err.println("Error during sql-statement: " + query);
            ex.printStackTrace();
            if (ex.getNextException() != null) {
                System.err.println("Next exception:");
                ex.getNextException().printStackTrace();
            }
        }
    }

    /**
     * @param query
     * @param caller
     * @return
     */
    public ResultSet executeQuery(String query, Object caller) {
        ResultSet rs = null;
        boolean finished = false;
        SQLException ex = null;
        int tries = 0;
        while (!finished && tries < 10) {
            try {
                Connection con = this.getConnection(caller);
                if (con != null) {
                    synchronized (con) {
                        Statement s = this.getConnection(caller).createStatement();
                        rs = s.executeQuery(query);
                        commit(this.getConnection(caller));
                        finished = true;
                    }
                }
            } catch (SQLException e) {
                finished = !isSqlConnectionError(e);
                tries++;
                ex = e;
            } catch (Exception e) {
                TPS_Logger.log(SeverityLogLevel.ERROR, e);
                ex = new SQLException(e);
            }
        }
        if (ex != null) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "Error during sql-statement: " + query);
            TPS_Logger.log(SeverityLogLevel.ERROR, ex);
            TPS_Logger.log(SeverityLogLevel.ERROR, "Next exception:");
            TPS_Logger.log(SeverityLogLevel.ERROR, ex.getNextException());
            System.err.println("Error during sql-statement: " + query);
            ex.printStackTrace();
            if (ex.getNextException() != null) {
                System.err.println("Next exception:");
                ex.getNextException().printStackTrace();
            }
        }
        return rs;
    }

    /**
     * This method does a sql-executeUpdate for the given string with the given calling class and returns the number
     * of updated rows. Each calling class gets its own connection.
     *
     * @param query  The sql-query
     * @param caller the caller Object. If this Object made a call before, Its connection is reused. Otherwise a new
     *               connection is created.
     * @return the number of updated rows
     */
    public int executeUpdate(String query, Object caller) {
        boolean finished = false;
        SQLException ex = null;
        int updatedRows = 0;
        int tries = 0;
        while (!finished && tries < 10) {
            try {
                Connection con = this.getConnection(caller);
                if (con != null) {
                    synchronized (this.getConnection(caller)) {
                        Statement s = this.getConnection(caller).createStatement();
                        updatedRows = s.executeUpdate(query);
                        commit(this.getConnection(caller));
                        s.close();
                        finished = true;
                    }
                }
            } catch (SQLException e) {
                finished = !isSqlConnectionError(e);
                tries++;
                ex = e;
            } catch (Exception e) {
                TPS_Logger.log(SeverityLogLevel.ERROR, e);
                ex = new SQLException(e);
            }
        }
        if (ex != null) {
            TPS_Logger.log(SeverityLogLevel.ERROR, ex);
            TPS_Logger.log(SeverityLogLevel.ERROR, "Next exception:");
            TPS_Logger.log(SeverityLogLevel.ERROR, ex.getNextException());
            System.err.println("Error during sql-statement: " + query);
            ex.printStackTrace();
            if (ex.getNextException() != null) {
                System.err.println("Next exception:");
                ex.getNextException().printStackTrace();
            }
        }
        return updatedRows;
    }

    /**
     * Returns the connection specified via its object key; if the key does not
     * reference an existing connection, a new connection with the provided
     * object key is opened
     *
     * @param key the object key of the connection
     * @return the connection referenced by the key
     * @throws SQLException This exception is thrown in case of non-existence or
     *                      non-accessibility to the server or refusing the connection by
     *                      the server (e.g. this IP has no permission to connect to the
     *                      database)
     */
    public Connection getConnection(Object key) throws SQLException {
        Connection c = connections.get(key);
        if (c != null && c.isClosed()) {
            c = null;
        }
        SQLException ex = null;
        for (int i = 0; i < 120 && c == null; ++i) {
            try {
                if (i > 0) {
                    Thread.sleep(1000);
                }
                c = this.openConnection();
                this.connections.put(key, c);
            } catch (SQLException e) {
                TPS_Logger.log(SeverityLogLevel.ERROR,
                        "Error creating db-connection. Try: " + i + "\n" + e.getMessage(), e);
                ex = e;
            } catch (InterruptedException e) {
                TPS_Logger.log(SeverityLogLevel.ERROR, e.getMessage(), e);
            }
        }
        if (c == null) {
            if (ex == null) throw new RuntimeException("Unknown error while creating connection");
            throw ex;
        }
        return c;
    }

    /**
     * returns the parameters class reference
     *
     * @return
     */
    public TPS_ParameterClass getParameters() {
        return this.parameterClass;
    }

    private boolean isSqlConnectionError(SQLException e) {
        return e.getErrorCode() == 8000 ||//CONNECTION EXCEPTION	connection_exception
                e.getErrorCode() == 8003 ||//CONNECTION DOES NOT EXIST	connection_does_not_exist
                e.getErrorCode() == 8006 ||//CONNECTION FAILURE	connection_failure
                e.getErrorCode() == 8001 ||
                //SQLCLIENT UNABLE TO ESTABLISH SQLCONNECTION	sqlclient_unable_to_establish_sqlconnection
                e.getErrorCode() == 8004 ||
                //SQLSERVER REJECTED ESTABLISHMENT OF SQLCONNECTION	 sqlserver_rejected_establishment_of_sqlconnection
                e.getErrorCode() == 8007;//TRANSACTION RESOLUTION UNKNOWN	transaction_resolution_unknown
    }

    /**
     * This method opens a connection to a database if there doesn't already
     * exist one. All parameters for the connection are determined by the
     * properties file. If there is no password set, the user gets a dialog
     * where is asked for the password.
     *
     * @throws SQLException This exception is thrown in case of non-existence or
     *                      non-accessibility to the server or refuting the connection by
     *                      the server (e.g. this IP has no permission to connect to the
     *                      database)
     */
    private Connection openConnection() throws SQLException {
        String url = "jdbc:" + this.parameterClass.getString(ParamString.DB_TYPE) + "://" +
                this.parameterClass.getString(ParamString.DB_HOST) + ":" + this.parameterClass.getIntValue(
                ParamValue.DB_PORT) + "/" + this.parameterClass.getString(ParamString.DB_DBNAME);


        if (this.myUserName == null) {
            if (this.parameterClass.isDefined(ParamString.DB_USER)) {
                this.myUserName = this.parameterClass.getString(ParamString.DB_USER);
            } else {
                JLabel label = new JLabel("Please enter database user:");
                JTextField jtf = new JTextField();
                JOptionPane.showConfirmDialog(null, new Object[]{label, jtf}, "Password:",
                        JOptionPane.OK_CANCEL_OPTION);
                this.myUserName = jtf.getText();
            }
        }

        if (this.myUserPwd == null) {
            if (this.parameterClass.isDefined(ParamString.DB_PASSWORD)) {
                this.myUserPwd = this.parameterClass.getString(ParamString.DB_PASSWORD);
            } else {
                JLabel label = new JLabel("Please enter password for database user '" + this.myUserName + "':");
                JPasswordField jpf = new JPasswordField();
                JOptionPane.showConfirmDialog(null, new Object[]{label, jpf}, "Password:",
                        JOptionPane.OK_CANCEL_OPTION);
                this.myUserPwd = new String(jpf.getPassword());
            }
        }

        Connection con = DriverManager.getConnection(url, this.myUserName, this.myUserPwd);
        con.setAutoCommit(true);
        con.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);

        return con;
    }



    /**
     * Method to return a parameter value for a given simulation key and parameter key. Returns null, if simulation or
     * parameter key is not present
     *
     * @param simKey   The simulation to look for
     * @param paramKey The parameter to look for
     * @param caller   The caller for connection hygiene
     * @return A String containing the parameter value
     */
    public String readParameter(String simKey, String paramKey, Object caller) {
        String query = "";
        String result = null;
        try {
            query = "SELECT param_value from simulation_parameters WHERE sim_key = '" + simKey + "' and param_key = '" +
                    paramKey + "'";
            ResultSet rs = this.executeQuery(query, caller);
            if (rs.next()) {
                result = rs.getString("param_value");
            }
            rs.close();
        } catch (SQLException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, "Error in SQL-query: " + query, e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Method to read simulation parameters from the DB for a given key.
     *
     * @param simKey
     * @return
     */
    public void readRuntimeParametersFromDB(String simKey) {
        String query = "";
        try {

            query = "SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS) +
                    " WHERE sim_key = '" + simKey + "'";
            ResultSet rs = this.executeQuery(query, this);
            this.parameterClass.readRuntimeParametersFromDB(rs);
            rs.close();
            this.parameterClass.checkParameters();
        } catch (SQLException e) {
            System.err.println("Error in sql-statement: " + query);
            e.printStackTrace();
        }
    }

    /**
     * Method to read a single simulation parameter from the DB.
     *
     * @param sim_key   the simulation-key
     * @param param_key the parameter key
     * @return a string containing the vale or null if no matching key is found.
     */
    public String readSingleParameter(String sim_key, String param_key) {
        String returnVal = null;
        String query = "";
        try {
            query = "SELECT param_value from simulation_parameters where sim_key = '" + sim_key +
                    "' AND param_key = '" + param_key + "'";
            ResultSet rs = this.executeQuery(query, this);
            if (rs.next()) {
                returnVal = rs.getString("param_value");
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error in sql-statement: " + query);
            e.printStackTrace();
        }
        return returnVal;
    }

    /**
     * Method to update a single simulation parameter for a given simulation in the DB. Be careful, if you have the rights to do so!
     *
     * @param sim_key    The simmulation
     * @param param_key  The parameter
     * @param paramValue The new value
     * @return 1 for success, 0 for no update
     */

    public int updateSingleParameter(String sim_key, String param_key, String paramValue) {
        String query =
                "UPDATE simulation_parameters set param_value ='" + paramValue + "' where sim_key = '" + sim_key +
                        "' AND " + "param_key = '" + param_key + "'";
        return this.executeUpdate(query, this); // affected rows
    }

    /**
     * User types for the database
     *
     * @author mark_ma
     */
    public enum DBUser {
        /**
         * Application user
         */
        APP,
        /**
         * Administrator user
         */
        ADMIN
    }

    public Map<Integer,Integer> readCarSharingData(String car_sharing_data_table, String key){

        Map<Integer,Integer> result = new HashMap<>();

        String query = "SELECT ft_taz_id, car_sharing_capacity FROM "+car_sharing_data_table+" WHERE ft_name = '"+key+"'";

        try(ResultSet rs = this.executeQuery(query,this)) {

            while (rs.next()) {
                result.put(rs.getInt("ft_taz_id"),rs.getInt("car_sharing_capacity"));
            }
        }catch (Exception e){
            TPS_Logger.log(HierarchyLogLevel.THREAD, SeverityLogLevel.SEVERE,"Error reading car sharing data.");
        }

        return result;

    }

}
