/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.matrixMap;

import de.dlr.ivf.tapas.runtime.util.MultilanguageSupport;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.TPS_Geometrics;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import matrixTool_v2.model.TAPASMemoryModel;
import matrixTool_v2.view.MatrixManipulatorFrame;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;


/**
 * main class for handling matrix maps and matrices in the db
 */
public class MatrixMapHandler {
    private final static int arrayChunk = 10000;

    private ResourceBundle rb;
    private JFrame controlInstance;  //  @jve:decl-index=0:visual-constraint="11,106"
    private JPanel mainPanel = null;
    private JPanel jConnectionPane = null;
    private JPanel jMatrixMapPane = null;
    private JPanel jMatrixPane = null;
    private JTextField serverText = null;
    private JTextField portText = null;
    private JTextField userText = null;
    private JPasswordField passwordField = null;
    private JButton connectButton = null;
    private JButton disconnectButton = null;
    private JButton configFileButton = null;
    private JSplitPane MatrixMapSplitPane = null;
    private JPanel MapListPane = null;
    private JPanel matrixMapButtonPane = null;
    private JPanel matrixMapDBNamePane = null;
    private JComboBox<String> matrixMapDBNameComboBox = null;
    private JPanel matrixMapActionPane = null;
    private JButton matrixMapLoadButton = null;
    private JButton matrixMapEditButton = null;
    private JButton matrixMapAddButton = null;
    private JButton matrixMapDeleteButton = null;
    private JScrollPane matrixMapListScrollPane = null;
    private JList<String> matrixMapList = null;
    private JSplitPane MatrixSplitPane = null;
    private JPanel matrixListPanel = null;
    private JPanel matrixDBNamePanel = null;
    private JScrollPane matrixListScrollPane = null;
    private JList<String> matrixList = null;
    private JPanel matrixButtonPanel = null;
    private JPanel matrixActionPanel = null;
    private JButton matrixLoadButton = null;
    private JComboBox<String> matrixDBNameComboBox = null;
    private JButton matrixEditButton = null;
    private JButton matrixEditTop3Button = null;
    private JButton matrixAddButton = null;
    private JButton matrixDeleteButton = null;
    private JButton matrixImportButton = null;
    private Connection connection = null;
    private Statement statement = null;
    private JTextField dbNameText = null;
    private JButton matrixCloneButton = null;
    private JButton matrixMapCloneButton = null;
    private JButton renameMatrixMapButton = null;
    private JButton matrixRenameButton = null;

    /**
     * main method to start
     *
     * @param args parameters
     */
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {

        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        MatrixMapHandler controlGUI = new MatrixMapHandler();
        controlGUI.createAndShowGUI();
        controlGUI.controlInstance.setVisible(true);
    }

    public static GridBagConstraints newGridBag(int gridX, int padX, int gridY, int padY) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridX;
        gridBagConstraints.gridy = gridY;
        gridBagConstraints.ipadx = padX;
        gridBagConstraints.ipady = padY;
        return gridBagConstraints;
    }

    /**
     * method to add a new entry in the matrix table
     */
    private void addMatrix() {
        try {
            String s;
            boolean done = false;
            do {
                s = (String) JOptionPane.showInputDialog(controlInstance,
                        rb.getString("ADD_MATRIX_ENTER_NEW_NAME") + ": ", rb.getString("ADD_MATRIX_TITLE"),
                        JOptionPane.PLAIN_MESSAGE, null, null, null);
                // If a string was returned, say so.
                if ((s != null) && (s.length() > 0)) {
                    // check if name is already in use
                    StringBuilder query = new StringBuilder("SELECT \"matrix_name\" FROM core." +
                            this.matrixDBNameComboBox.getSelectedItem().toString() + " WHERE \"matrix_name\" = '" + s +
                            "'");
                    ResultSet rs = statement.executeQuery(query.toString());
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(controlInstance, s + " " + rb.getString("NAME_ALREADY_EXISTS"));
                    } else {
                        String sizeString = (String) JOptionPane.showInputDialog(controlInstance,
                                rb.getString("ADD_MATRIX_ENTER_SIZE") + ": ", rb.getString("ADD_MATRIX_TITLE"),
                                JOptionPane.PLAIN_MESSAGE, null, null, null);
                        int size = Integer.parseInt(sizeString);
                        size = Math.max(0, size); // must be zero or positive!
                        size *= size; //build quadratic matrix
                        // build empty entry
                        query = new StringBuilder(
                                "INSERT INTO core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                                        " (\"matrix_name\",\"matrix_values\")" + " VALUES('" + s + "','{");
                        StringBuilder buffer;
                        int numOfChunks;
                        if (size % arrayChunk == 0) {
                            numOfChunks = size / arrayChunk;
                        } else {
                            numOfChunks = 1 + (size / arrayChunk);
                        }
                        for (int j = 0; j < numOfChunks; ++j) {
                            buffer = new StringBuilder();
                            for (int i = j * arrayChunk; i < Math.min((j + 1) * arrayChunk, size); ++i) {
                                if (i == size - 1) buffer.append("0");
                                else buffer.append("0,");
                            }
                            query.append(buffer);
                        }
                        query.append("}')");
                        statement.execute(query.toString());
                        done = true;
                    }
                }
            } while (!done);
            loadMatrixList();
            int answer = JOptionPane.showConfirmDialog(controlInstance, rb.getString("MATRIX_SUCCESSFULLY_ADDED"),
                    rb.getString("EDIT_NEW_MATRIX_TITLE"), JOptionPane.YES_NO_OPTION);
            if (JOptionPane.YES_OPTION == answer) {
                this.matrixList.setSelectedValue(s, true);
                editMatrix();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to add a new entry in the matrixMap table
     */
    private void addMatrixMap() {
        try {
            String s;
            boolean done = false;
            do {
                s = (String) JOptionPane.showInputDialog(controlInstance,
                        rb.getString("ADD_MATRIX_ENTER_NEW_NAME") + ": ", rb.getString("ADD_MATRIX_MAP_TITLE"),
                        JOptionPane.PLAIN_MESSAGE, null, null, null);
                // If a string was returned, say so.
                if ((s != null) && (s.length() > 0)) {
                    // check if name is already in use
                    String query = "SELECT \"matrixMap_name\" FROM core." +
                            this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                            " WHERE \"matrixMap_name\" = '" + s + "'";
                    ResultSet rs = statement.executeQuery(query);
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(controlInstance, s + rb.getString("NAME_ALREADY_EXISTS"));
                    } else {
                        // build empty entry
                        query = "INSERT INTO core." + this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                                " (\"matrixMap_name\",\"matrixMap_num\",\"matrixMap_matrixNames\",\"matrixMap_distribution\")" +
                                " VALUES('" + s + "',0,'{}','{}')";
                        statement.execute(query);
                        done = true;
                    }
                }
            } while (!done);
            loadMatrixMapList();
            int answer = JOptionPane.showConfirmDialog(controlInstance, rb.getString("MATRIX_MAP_SUCCESSFULLY_ADDED"),
                    rb.getString("EDIT_NEW_MATRIX_MAP_TITLE"), JOptionPane.YES_NO_OPTION);
            if (JOptionPane.YES_OPTION == answer) {
                this.matrixMapList.setSelectedValue(s, true);
                editMatrixMap();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to clone db entries in the matrix table
     */
    private void cloneSelectedMatrices() {
        if (-1 == this.matrixList.getSelectedIndex()) {
            JOptionPane.showMessageDialog(controlInstance, rb.getString("NO_MATRIX_MAP_SELECTED"));
            return;
        }
        try {
            List<String> listEntries = this.matrixList.getSelectedValuesList();
            for (String entry : listEntries) {
                String s = entry + "_1";
                boolean done = false;
                do {
                    s = (String) JOptionPane.showInputDialog(controlInstance, rb.getString("ADD_MATRIX_ENTER_NEW_NAME"),
                            rb.getString("CLONE_MATRIX_TITLE"), JOptionPane.PLAIN_MESSAGE, null, null, s);

                    // If a string was returned, say so.
                    if ((s != null) && (s.length() > 0)) {

                        if (isMatrixExisting(s)) {
                            JOptionPane.showMessageDialog(controlInstance, s + rb.getString("NAME_ALREADY_EXISTS"));
                        } else {
                            // get father
                            StringBuilder query = new StringBuilder(
                                    "SELECT * FROM core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                                            " WHERE \"matrix_name\" = '" + entry + "'");
                            ResultSet rs = statement.executeQuery(query.toString());
                            if (rs.next()) {
                                // build entry
                                query = new StringBuilder(
                                        "INSERT INTO core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                                                " (\"matrix_name\",\"matrix_values\")" + " VALUES('" + s + "','{");
                                // build array of matrix integers
                                Object array = rs.getArray(2).getArray();
                                if (array instanceof Integer[]) {
                                    Integer[] matrixVal = (Integer[]) array;
                                    //subdivision of this array into chunks for performance reasons!
                                    StringBuilder subValueStrings;
                                    int numOfChunks;
                                    if (matrixVal.length % arrayChunk == 0) {
                                        // TODO are these chunks necessary with StringBuilder
                                        numOfChunks = matrixVal.length / arrayChunk;
                                    } else {
                                        numOfChunks = 1 + (matrixVal.length / arrayChunk);
                                    }
                                    for (int k = 0; k < numOfChunks; ++k) {
                                        subValueStrings = new StringBuilder();
                                        for (int j = k * arrayChunk; j < Math.min((k + 1) * arrayChunk,
                                                matrixVal.length); ++j) {
                                            if (j < matrixVal.length - 1) subValueStrings.append(
                                                    matrixVal[j].toString()).append(",");
                                            else subValueStrings.append(matrixVal[j].toString());
                                        }
                                        query.append(subValueStrings);
                                    }
                                } else {
                                    throw new SQLException("Cannot cast to Integer array");
                                }
                                query.append("}')");

                                statement.execute(query.toString());
                            } else {
                                JOptionPane.showMessageDialog(controlInstance,
                                        rb.getString("ERROR_MESSAGE_PARENT_DISAPPEARED"));
                            }
                            done = true;
                        }
                    } else {
                        done = true;
                    }
                } while (!done);
            }
            loadMatrixList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to clone db entries in the matrixMap table
     */
    private void cloneSelectedMatrixMaps() {
        if (-1 == this.matrixMapList.getSelectedIndex()) {
            JOptionPane.showMessageDialog(controlInstance, rb.getString("NO_MATRIX_MAP_SELECTED"));
            return;
        }
        try {
            List<String> listEntries = this.matrixMapList.getSelectedValuesList();
            for (String entry : listEntries) {
                String s = entry + "_1";
                boolean done = false;
                do {
                    s = (String) JOptionPane.showInputDialog(controlInstance, rb.getString("ADD_MATRIX_ENTER_NEW_NAME"),
                            rb.getString("CLONE_MATRIX_MAP_TITLE"), JOptionPane.PLAIN_MESSAGE, null, null, s);

                    // If a string was returned, say so.
                    if ((s != null) && (s.length() > 0)) {
                        if (isMatrixMapExisting(s)) {
                            JOptionPane.showMessageDialog(controlInstance, s + rb.getString("NAME_ALREADY_EXISTS"));
                        } else {
                            // get father
                            StringBuilder query = new StringBuilder(
                                    "SELECT * FROM core." + this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                                            " WHERE \"matrixMap_name\" = '" + entry + "'");
                            ResultSet rs = statement.executeQuery(query.toString());
                            if (rs.next()) {
                                // build entry
                                query = new StringBuilder("INSERT INTO core." +
                                        this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                                        " (\"matrixMap_name\",\"matrixMap_num\",\"matrixMap_matrixNames\",\"matrixMap_distribution\")" +
                                        " VALUES('" + s + "'," + rs.getInt(2) + ",'{");
                                // build array of matrix names
                                Object array = rs.getArray(3).getArray();
                                if (array instanceof String[]) {
                                    String[] matrixNames = (String[]) array;
                                    for (int j = 0; j < matrixNames.length; ++j) {
                                        if (j < matrixNames.length - 1) query.append("\"").append(matrixNames[j])
                                                                             .append("\",");
                                        else query.append("\"").append(matrixNames[j]).append("\"");
                                    }

                                } else {
                                    throw new SQLException("Cannot cast to string array");
                                }
                                query.append("}','{");

                                // build distribution
                                array = rs.getArray(4).getArray();
                                if (array instanceof Double[]) {
                                    Double[] distribution = (Double[]) array;
                                    for (int j = 0; j < distribution.length; ++j) {
                                        if (j < distribution.length - 1) query.append(distribution[j].toString())
                                                                              .append(",");
                                        else query.append(distribution[j].toString());
                                    }

                                } else {
                                    throw new SQLException("Cannot cast to double array");
                                }
                                query.append("}')");
                                statement.execute(query.toString());
                            } else {
                                JOptionPane.showMessageDialog(controlInstance,
                                        rb.getString("ERROR_MESSAGE_PARENT_DISAPPEARED"));
                            }
                            done = true;
                        }
                    } else {
                        done = true;
                    }
                } while (!done);
            }
            loadMatrixMapList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to close the db connection and statement
     */
    private void closeConnection() {
        try {
            if (statement != null) {
                statement.close();
                statement = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function generates the gui and fills it with default values
     *
     * @return jFrame gui element
     */
    public JFrame createAndShowGUI() {
        rb = ResourceBundle.getBundle("MatrixMapHandlerLabels", MultilanguageSupport.getLocale());
        this.controlInstance = createGui();
        this.fillLoginData(false);
        this.controlInstance.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        return this.controlInstance;

    }

    /**
     * method to initialise the gui elements and returns the main panel
     *
     * @return jFrame gui element
     */
    private JFrame createGui() {
        final JFrame jFrame = new JFrame();
        Toolkit jTools = Toolkit.getDefaultToolkit();
        Dimension dim = jTools.getScreenSize();
        jFrame.setSize(1024, 768);
        jFrame.setContentPane(getMainPanel());
        jFrame.setLocation(dim.width / 2 - jFrame.getWidth() / 2, dim.height / 2 - jFrame.getHeight() / 2);

        jFrame.setTitle(rb.getString("TITLE"));
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeConnection();
            }
        });
        return jFrame;
    }

    /**
     * method to clear the lists and combo boxes
     */
    private void deleteLists() {
        this.matrixDBNameComboBox.removeAllItems();
        this.matrixMapDBNameComboBox.removeAllItems();
        deleteMatrixList();
        deleteMatrixMapList();
    }

    /**
     * method to clear the matrix map list
     */
    private void deleteMatrixList() {
        this.matrixListScrollPane.getViewport().setView(null);
        if (this.matrixList != null) {
            this.matrixList.removeAll();
            this.matrixList = null;
        }
        set2ndLevelMatrixButtonEnabled(false);
    }

    /**
     * method to clear the matrix map list
     */
    private void deleteMatrixMapList() {
        this.matrixMapListScrollPane.getViewport().setView(null);
        if (this.matrixMapList != null) {
            this.matrixMapList.removeAll();
            this.matrixMapList = null;
        }
        set2ndLevelMatrixMapEnabled(false);
    }

    /**
     * method to delete the selected Matrices from the db, each entry must be confirmed!
     */
    private void deleteSelectedMatrices() {
        if (-1 == this.matrixList.getSelectedIndex()) {
            JOptionPane.showMessageDialog(controlInstance, rb.getString("NO_MATRIX_SELECTED"));
            return;
        }
        try {
            List<String> listEntries = this.matrixList.getSelectedValuesList();
            for (String entry : listEntries) {
                int s = JOptionPane.showConfirmDialog(controlInstance, rb.getString("DELETE_MATRIX_DIALOG") + entry,
                        rb.getString("DELETE_MATRIX_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION);

                if (JOptionPane.YES_OPTION == s) {// delete if Yes
                    String query = "DELETE FROM core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                            " WHERE \"matrix_name\" = '" + entry + "'";
                    statement.execute(query);
                } else if (JOptionPane.CANCEL_OPTION == s) { //aboard whole action if cancel
                    break;
                }
            }
            loadMatrixList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to delete the selected matrix maps from the db, each entry must be confirmed!
     */
    private void deleteSelectedMatrixMaps() {
        if (-1 == this.matrixMapList.getSelectedIndex()) {
            JOptionPane.showMessageDialog(controlInstance, rb.getString("NO_MATRIX_MAP_SELECTED"));
            return;
        }
        try {
            List<String> listEntries = this.matrixMapList.getSelectedValuesList();
            for (String entry : listEntries) {
                int s = JOptionPane.showConfirmDialog(controlInstance, rb.getString("DELETE_MATRIX_MAP_DIALOG") + entry,
                        rb.getString("DELETE_MATRIX_MAP_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION);

                if (JOptionPane.YES_OPTION == s) {// delete if Yes
                    String query = "DELETE FROM core." + this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                            " WHERE \"matrixMap_name\" = '" + entry + "'";
                    statement.execute(query);
                } else if (JOptionPane.CANCEL_OPTION == s) { //aboard whole action if cancel
                    break;
                }
            }
            loadMatrixMapList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to edit a matrix entry
     */
    private void editMatrix() {
        //check valid input
        if (this.matrixList.getSelectedIndex() < 0) {
            JOptionPane.showMessageDialog(this.controlInstance, rb.getString("EDIT_MATRIX_SELECT_FIRST_DIALOG"));
            return;
        }
        try {
            List<String> selection = this.matrixList.getSelectedValuesList();
            for (String entry : selection) {
                //load the data from the db
                StringBuilder query = new StringBuilder(
                        "SELECT * FROM core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                                " WHERE \"matrix_name\" = '" + entry + "'");
                ResultSet rs = statement.executeQuery(query.toString());
                if (rs.next()) {
                    Object array = rs.getArray(2).getArray();
                    if (array instanceof Integer[]) {
                        //parse data to memory model
                        Integer[] matrixVal = (Integer[]) array;
                        double[] values = new double[matrixVal.length];
                        for (int j = 0; j < matrixVal.length; ++j) values[j] = (double) matrixVal[j];
                        TAPASMemoryModel matrix = new TAPASMemoryModel(values);
                        //show frame
                        MatrixManipulatorFrame.showDialog(this.controlInstance, matrix,
                                rb.getString("EDIT_BUTTON") + " " + entry);
                        int answer = JOptionPane.showConfirmDialog(this.controlInstance,
                                rb.getString("SAVE_TO_DATABASE_DIALOG"), rb.getString("SAVE_CHANGES"),
                                JOptionPane.YES_NO_OPTION);
                        if (JOptionPane.YES_OPTION == answer) {
                            //save data back
                            query = new StringBuilder(
                                    "UPDATE core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                                            " SET \"matrix_values\" = '{");
                            StringBuilder buffer;
                            for (int j = 0; j < matrix.getMatrix().getActualYSize(); ++j) {
                                buffer = new StringBuilder();
                                for (int k = 0; k < matrix.getMatrix().getActualXSize(); ++k) {
                                    if (k == matrix.getMatrix().getActualXSize() - 1 &&
                                            j == matrix.getMatrix().getActualYSize() - 1) buffer.append(
                                            Math.round(matrix.getMatrix().getValue(j, k)));
                                    else buffer.append(Math.round(matrix.getMatrix().getValue(j, k))).append(",");
                                }
                                query.append(buffer);
                            }
                            query.append("}' WHERE \"matrix_name\" = '").append(entry).append("'");
                            statement.execute(query.toString());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to edit a matrixMap entry
     */
    private void editMatrixMap() {
        if (-1 == this.matrixMapList.getSelectedIndex()) {
            JOptionPane.showMessageDialog(controlInstance, rb.getString("NO_MATRIX_MAP_SELECTED"));
            return;
        }
        if (this.matrixList == null) {
            //no matrixList loaded!
            String[] matrixNames = new String[this.matrixDBNameComboBox.getItemCount()];
            for (int i = 0; i < this.matrixDBNameComboBox.getItemCount(); ++i) {
                matrixNames[i] = this.matrixDBNameComboBox.getItemAt(i);
            }
            String s = (String) JOptionPane.showInputDialog(controlInstance,
                    rb.getString("LOAD_MATRIX_SPECIFY_NAME_DIALOG"), rb.getString("LOAD_MATRIX_TABLE_TITLE"),
                    JOptionPane.PLAIN_MESSAGE, null, matrixNames,
                    matrixNames[this.matrixDBNameComboBox.getSelectedIndex()]);
            if ((s != null) && (s.length() > 0)) {
                matrixDBNameComboBox.setSelectedItem(s);
                loadMatrixList();
            } else {
                return;
            }
        }
        List<String> listEntries = this.matrixMapList.getSelectedValuesList();
        for (String entry : listEntries) {
            MatrixMapManipulator.showDialog(controlInstance, statement,
                    this.matrixMapDBNameComboBox.getSelectedItem().toString(),
                    this.matrixDBNameComboBox.getSelectedItem().toString(), entry);
        }
    }

    /**
     * method to fill the combo boxes
     */
    private void fillLists() {
        try {
            String query = "select relname from pg_stat_user_tables WHERE schemaname='core' AND relname LIKE '%matrices%' ORDER BY relname";
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                this.matrixDBNameComboBox.addItem(rs.getString(1));
            }
            query = "select relname from pg_stat_user_tables WHERE schemaname='core' AND relname LIKE '%matrixmap%' ORDER BY relname";
            rs = statement.executeQuery(query);
            while (rs.next()) {
                this.matrixMapDBNameComboBox.addItem(rs.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void fillLoginData(boolean forceNewFile) {
        this.closeConnection();
        this.setConnectionEnabled(false);

        File configFile = TPS_BasicConnectionClass.getRuntimeFile(forceNewFile);
        if (configFile.exists()) {
            try {
                TPS_ParameterClass parameterClass = new TPS_ParameterClass();
                parameterClass.loadRuntimeParameters(configFile);
                this.serverText.setText(parameterClass.getString(ParamString.DB_HOST));
                this.portText.setText(Integer.toString(parameterClass.getIntValue(ParamValue.DB_PORT)));
                this.dbNameText.setText(parameterClass.getString(ParamString.DB_DBNAME));
                this.userText.setText(parameterClass.getString(ParamString.DB_USER));
                this.passwordField.setText(parameterClass.getString(ParamString.DB_PASSWORD));
            } catch (IOException e) {
                this.serverText.setText("129.247.221.173");
                this.portText.setText("5433");
                this.dbNameText.setText("tapas");
                this.userText.setText("postgres");
                this.passwordField.setText("postgres");
            }
        } else {
            this.serverText.setText("129.247.221.173");
            this.portText.setText("5433");
            this.dbNameText.setText("tapas");
            this.userText.setText("postgres");
            this.passwordField.setText("postgres");
        }

    }

    /**
     * This method initializes configFileButton
     *
     * @return javax.swing.JButton
     */
    private JButton getConfigFileButton() {
        if (configFileButton == null) {
            configFileButton = new JButton();
            configFileButton.setText(rb.getString("LOAD_CONFIG_FILE_BUTTON"));
            configFileButton.addActionListener(e -> fillLoginData(true));
        }
        return configFileButton;
    }

    /**
     * This method initializes connectButton
     *
     * @return javax.swing.JButton
     */
    private JButton getConnectButton() {
        if (connectButton == null) {
            connectButton = new JButton();
            connectButton.setText(rb.getString("CONNECT_BUTTON"));
            connectButton.addActionListener(e -> {
                openConnection();
                if (connection != null) {
                    setConnectionEnabled(true);
                    fillLists();
                }
            });
        }
        return connectButton;
    }

    /**
     * This method initializes dbNameText
     *
     * @return javax.swing.JTextField
     */
    private JTextField getDbNameText() {
        if (dbNameText == null) {
            dbNameText = new JTextField();
        }
        return dbNameText;
    }

    /**
     * This method initializes disconnectButton
     *
     * @return javax.swing.JButton
     */
    private JButton getDisconnectButton() {
        if (disconnectButton == null) {
            disconnectButton = new JButton();
            disconnectButton.setText(rb.getString("DISCONNECT_BUTTON"));
            disconnectButton.addActionListener(e -> {
                closeConnection();
                if (connection == null) {
                    setConnectionEnabled(false);
                    deleteLists();
                }
            });
        }
        return disconnectButton;
    }

    /**
     * This method initializes jConnectionPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJConnectionPane() {
        if (jConnectionPane == null) {
            JLabel dbNameLabel = new JLabel();
            dbNameLabel.setText(rb.getString("DATABASE") + ":");
            JLabel jLabelSpacer2 = new JLabel();
            jLabelSpacer2.setText(" ");
            JLabel jLabelSpacer1 = new JLabel();
            jLabelSpacer1.setText(" ");
            GridBagConstraints gridBagConstraints17 = newGridBag(14, 50, 0, 0);
            GridBagConstraints gridBagConstraints16 = newGridBag(13, 0, 0, 0);
            GridBagConstraints gridBagConstraints15 = newGridBag(12, 0, 0, 0);
            GridBagConstraints gridBagConstraints14 = newGridBag(11, 0, 0, 0);
            GridBagConstraints gridBagConstraints13 = newGridBag(10, 0, 0, 0);
            GridBagConstraints gridBagConstraints12 = newGridBag(9, 125, 0, 0);
            GridBagConstraints gridBagConstraints11 = newGridBag(8, 10, 0, 0);
            GridBagConstraints gridBagConstraints10 = newGridBag(7, 100, 0, 0);
            gridBagConstraints10.weightx = 1.0;
            GridBagConstraints gridBagConstraints9 = newGridBag(6, 10, 0, 0);
            GridBagConstraints gridBagConstraints8 = newGridBag(5, 75, 0, 0);
            gridBagConstraints8.weightx = 1.0;
            GridBagConstraints gridBagConstraints7 = newGridBag(4, 10, 0, 0);
            GridBagConstraints gridBagConstraints6 = newGridBag(3, 30, 0, 0);
            gridBagConstraints6.weightx = 1.0;
            GridBagConstraints gridBagConstraints5 = newGridBag(2, 10, 0, 0);
            GridBagConstraints gridBagConstraints4 = newGridBag(1, 100, 0, 0);
            gridBagConstraints4.weightx = 1.0;
            GridBagConstraints gridBagConstraints3 = newGridBag(0, 10, 0, 0);
            JLabel userLabel = new JLabel();
            userLabel.setText(rb.getString("USER"));
            JLabel passwordLabel = new JLabel();
            passwordLabel.setText(rb.getString("PASSWORD"));
            JLabel portLabel = new JLabel();
            portLabel.setText(rb.getString("PORT"));
            JLabel serverLabel = new JLabel();
            serverLabel.setText(rb.getString("SERVER_IP"));
            jConnectionPane = new JPanel();
            jConnectionPane.setLayout(new GridBagLayout());
            jConnectionPane.add(serverLabel, gridBagConstraints3);
            jConnectionPane.add(getServerText(), gridBagConstraints4);
            jConnectionPane.add(portLabel, gridBagConstraints5);
            jConnectionPane.add(getPortText(), gridBagConstraints6);
            jConnectionPane.add(dbNameLabel, gridBagConstraints7);
            jConnectionPane.add(getDbNameText(), gridBagConstraints8);
            jConnectionPane.add(userLabel, gridBagConstraints9);
            jConnectionPane.add(getUserText(), gridBagConstraints10);
            jConnectionPane.add(passwordLabel, gridBagConstraints11);
            jConnectionPane.add(getPasswordField(), gridBagConstraints12);
            jConnectionPane.add(jLabelSpacer1, gridBagConstraints13);
            jConnectionPane.add(getConnectButton(), gridBagConstraints14);
            jConnectionPane.add(getDisconnectButton(), gridBagConstraints15);
            jConnectionPane.add(getConfigFileButton(), gridBagConstraints16);
            jConnectionPane.add(jLabelSpacer2, gridBagConstraints17);
        }
        return jConnectionPane;
    }

    /**
     * This method initializes jMatrixMapPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJMatrixMapPane() {
        if (jMatrixMapPane == null) {
            jMatrixMapPane = new JPanel();
            jMatrixMapPane.setLayout(new FlowLayout());
            jMatrixMapPane.add(getMatrixMapSplitPane(), null);
        }
        return jMatrixMapPane;
    }

    /**
     * This method initializes jMatrixPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJMatrixPane() {
        if (jMatrixPane == null) {
            jMatrixPane = new JPanel();
            jMatrixPane.setLayout(new FlowLayout());
            jMatrixPane.add(getMatrixSplitPane(), null);
        }
        return jMatrixPane;
    }

    /**
     * This method initializes mainPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMainPanel() {
        if (mainPanel == null) {
            GridBagConstraints gridBagConstraints2 = newGridBag(0, 0, 2, 0);
            GridBagConstraints gridBagConstraints1 = newGridBag(0, 0, 1, 0);
            GridBagConstraints gridBagConstraints = newGridBag(0, 0, 0, 0);
            mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            mainPanel.add(getJConnectionPane(), gridBagConstraints);
            mainPanel.add(getJMatrixMapPane(), gridBagConstraints1);
            mainPanel.add(getJMatrixPane(), gridBagConstraints2);
        }
        return mainPanel;
    }

    /**
     * This method initializes MapListPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMapListPane() {
        if (MapListPane == null) {
            GridBagConstraints gridBagConstraints16 = newGridBag(0, 900, 1, 300);
            gridBagConstraints16.fill = GridBagConstraints.BOTH;
            gridBagConstraints16.weightx = 1.0;
            gridBagConstraints16.weighty = 1.0;
            GridBagConstraints gridBagConstraints15 = newGridBag(0, 0, 0, 0);
            MapListPane = new JPanel();
            MapListPane.setLayout(new GridBagLayout());
            MapListPane.add(getMatrixMapDBNamePane(), gridBagConstraints15);
            MapListPane.add(getMatrixMapList(), gridBagConstraints16);
        }
        return MapListPane;
    }

    /**
     * This method initializes matrixActionPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixActionPanel() {
        if (matrixActionPanel == null) {
            GridBagConstraints gridBagConstraints34 = newGridBag(0, 20, 7, 0);
            GridBagConstraints gridBagConstraints33 = newGridBag(0, 8, 6, 0);
            GridBagConstraints gridBagConstraints32 = newGridBag(0, 8, 5, 0);
            GridBagConstraints gridBagConstraints31 = newGridBag(0, 12, 4, 0);
            GridBagConstraints gridBagConstraints30 = newGridBag(0, 20, 3, 0);
            GridBagConstraints gridBagConstraints29 = newGridBag(0, 20, 2, 0);
            GridBagConstraints gridBagConstraints28 = newGridBag(0, 0, 1, 0);
            GridBagConstraints gridBagConstraints27 = newGridBag(0, 16, 0, 0);
            matrixActionPanel = new JPanel();
            matrixActionPanel.setLayout(new GridBagLayout());
            matrixActionPanel.add(getMatrixLoadButton(), gridBagConstraints27);
            matrixActionPanel.add(getMatrixRenameButton(), gridBagConstraints28);
            matrixActionPanel.add(getMatrixEditButton(), gridBagConstraints29);
            matrixActionPanel.add(getMatrixAddButton(), gridBagConstraints30);
            matrixActionPanel.add(getMatrixCloneButton(), gridBagConstraints31);
            matrixActionPanel.add(getMatrixDeleteButton(), gridBagConstraints32);
            matrixActionPanel.add(getMatrixImportButton(), gridBagConstraints33);
            matrixActionPanel.add(matrixEditTop3Button(), gridBagConstraints34);
        }
        return matrixActionPanel;
    }

    /**
     * This method initializes matrixAddButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixAddButton() {
        if (matrixAddButton == null) {
            matrixAddButton = new JButton();
            matrixAddButton.setText(rb.getString("ADD_BUTTON"));
            matrixAddButton.addActionListener(e -> addMatrix());
        }
        return matrixAddButton;
    }

    /**
     * This method initializes matrixButtonPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixButtonPanel() {
        if (matrixButtonPanel == null) {
            matrixButtonPanel = new JPanel();
            matrixButtonPanel.setLayout(new BorderLayout());
            matrixButtonPanel.add(getMatrixActionPanel(), BorderLayout.NORTH);
        }
        return matrixButtonPanel;
    }

    /**
     * This method initializes matrixCloneButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixCloneButton() {
        if (matrixCloneButton == null) {
            matrixCloneButton = new JButton();
            matrixCloneButton.setText(rb.getString("CLONE_BUTTON"));
            matrixCloneButton.addActionListener(e -> cloneSelectedMatrices());
        }
        return matrixCloneButton;
    }

    /**
     * This method initializes matrixDBNameComboBox
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<String> getMatrixDBNameComboBox() {
        if (matrixDBNameComboBox == null) {
            matrixDBNameComboBox = new JComboBox<>();
            matrixDBNameComboBox.setBounds(new Rectangle(0, 0, 0, 0));
            matrixDBNameComboBox.addItemListener(e -> deleteMatrixList());
            matrixDBNameComboBox.setPrototypeDisplayValue("item prototype layout is a log string for the combo box");
        }
        return matrixDBNameComboBox;
    }

    /**
     * This method initializes matrixDBNamePanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixDBNamePanel() {
        if (matrixDBNamePanel == null) {
            GridBagConstraints gridBagConstraints26 = newGridBag(1, 510, 0, 0);
            GridBagConstraints gridBagConstraints25 = newGridBag(0, 20, 0, 0);
            JLabel matrixDBNameLabel = new JLabel();
            matrixDBNameLabel.setText(rb.getString("MATRIX_DB_NAME"));
            matrixDBNamePanel = new JPanel();
            matrixDBNamePanel.setLayout(new GridBagLayout());
            matrixDBNamePanel.add(matrixDBNameLabel, gridBagConstraints25);
            matrixDBNamePanel.add(getMatrixDBNameComboBox(), gridBagConstraints26);
        }
        return matrixDBNamePanel;
    }

    /**
     * This method initializes matrixDeleteButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixDeleteButton() {
        if (matrixDeleteButton == null) {
            matrixDeleteButton = new JButton();
            matrixDeleteButton.setText(rb.getString("DELETE_BUTTON"));
            matrixDeleteButton.addActionListener(e -> deleteSelectedMatrices());
        }
        return matrixDeleteButton;
    }

    /**
     * This method initializes matrixEditButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixEditButton() {
        if (matrixEditButton == null) {
            matrixEditButton = new JButton();
            matrixEditButton.setText(rb.getString("EDIT_BUTTON"));
            matrixEditButton.addActionListener(e -> editMatrix());
        }
        return matrixEditButton;
    }

    /**
     * This method initializes matrixImportButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixImportButton() {
        if (matrixImportButton == null) {
            matrixImportButton = new JButton();
            matrixImportButton.setText(rb.getString("IMPORT_BUTTON"));
            matrixImportButton.addActionListener(e -> {
                //todo
            });
        }
        return matrixImportButton;
    }

    /**
     * This method initializes matrixList
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getMatrixList() {
        if (matrixListScrollPane == null) {
            matrixListScrollPane = new JScrollPane();
        }
        return matrixListScrollPane;
    }

    /**
     * This method initializes matrixListPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixListPanel() {
        if (matrixListPanel == null) {
            GridBagConstraints gridBagConstraints30 = newGridBag(0, 900, 1, 300);
            gridBagConstraints30.fill = GridBagConstraints.BOTH;
            gridBagConstraints30.weightx = 1.0;
            gridBagConstraints30.weighty = 1.0;
            GridBagConstraints gridBagConstraints29 = newGridBag(0, 0, 0, 0);
            matrixListPanel = new JPanel();
            matrixListPanel.setLayout(new GridBagLayout());
            matrixListPanel.add(getMatrixDBNamePanel(), gridBagConstraints29);
            matrixListPanel.add(getMatrixList(), gridBagConstraints30);
        }
        return matrixListPanel;
    }

    /**
     * This method initializes matrixLoadButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixLoadButton() {
        if (matrixLoadButton == null) {
            matrixLoadButton = new JButton();
            matrixLoadButton.setText(rb.getString("LOAD_BUTTON"));
            matrixLoadButton.addActionListener(e -> loadMatrixList());
        }
        return matrixLoadButton;
    }

    /**
     * This method initializes matrixMapActionPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixMapActionPane() {
        if (matrixMapActionPane == null) {
            GridBagConstraints gridBagConstraints25 = newGridBag(0, 8, 5, 0);
            GridBagConstraints gridBagConstraints24 = newGridBag(0, 12, 4, 0);
            GridBagConstraints gridBagConstraints23 = newGridBag(0, 20, 3, 0);
            GridBagConstraints gridBagConstraints22 = newGridBag(0, 20, 2, 0);
            GridBagConstraints gridBagConstraints21 = newGridBag(0, 0, 1, 0);
            GridBagConstraints gridBagConstraints20 = newGridBag(0, 16, 0, 0);
            matrixMapActionPane = new JPanel();
            matrixMapActionPane.setLayout(new GridBagLayout());
            matrixMapActionPane.add(getMatrixMapLoadButton(), gridBagConstraints20);
            matrixMapActionPane.add(getRenameMatrixMapButton(), gridBagConstraints21);
            matrixMapActionPane.add(getMatrixMapEditButton(), gridBagConstraints22);
            matrixMapActionPane.add(getMatrixMapAddButton(), gridBagConstraints23);
            matrixMapActionPane.add(getMatrixMapCloneButton(), gridBagConstraints24);
            matrixMapActionPane.add(getMatrixMapDeleteButton(), gridBagConstraints25);
        }
        return matrixMapActionPane;
    }

    /**
     * This method initializes matrixMapAddButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixMapAddButton() {
        if (matrixMapAddButton == null) {
            matrixMapAddButton = new JButton();
            matrixMapAddButton.setText(rb.getString("ADD_BUTTON"));
            matrixMapAddButton.addActionListener(e -> addMatrixMap());
        }
        return matrixMapAddButton;
    }

    /**
     * This method initializes matrixMapButtonPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixMapButtonPane() {
        if (matrixMapButtonPane == null) {
            matrixMapButtonPane = new JPanel();
            matrixMapButtonPane.setLayout(new BorderLayout());
            matrixMapButtonPane.add(getMatrixMapActionPane(), BorderLayout.NORTH);
        }
        return matrixMapButtonPane;
    }

    /**
     * This method initializes matrixMapCloneButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixMapCloneButton() {
        if (matrixMapCloneButton == null) {
            matrixMapCloneButton = new JButton();
            matrixMapCloneButton.setText(rb.getString("CLONE_BUTTON"));
            matrixMapCloneButton.addActionListener(e -> cloneSelectedMatrixMaps());
        }
        return matrixMapCloneButton;
    }

    /**
     * This method initializes matrixMapDBNameComboBox
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<String> getMatrixMapDBNameComboBox() {
        if (matrixMapDBNameComboBox == null) {
            matrixMapDBNameComboBox = new JComboBox<>();
            matrixMapDBNameComboBox.setBounds(new Rectangle(0, 0, 0, 0));
            matrixMapDBNameComboBox.addItemListener(e -> deleteMatrixMapList());
            matrixMapDBNameComboBox.setPrototypeDisplayValue("item prototype layout is a log string for the combo box");
        }
        return matrixMapDBNameComboBox;
    }

    /**
     * This method initializes matrixMapDBNamePane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixMapDBNamePane() {
        if (matrixMapDBNamePane == null) {
            GridBagConstraints gridBagConstraints18 = newGridBag(1, 510, 0, 0);
            GridBagConstraints gridBagConstraints17 = newGridBag(0, 0, 0, 0);
            JLabel matrixMapDBLabel = new JLabel();
            matrixMapDBLabel.setText(rb.getString("MATRIX_MAP_DB_NAME"));
            matrixMapDBNamePane = new JPanel();
            matrixMapDBNamePane.setLayout(new GridBagLayout());
            matrixMapDBNamePane.add(matrixMapDBLabel, gridBagConstraints17);
            matrixMapDBNamePane.add(getMatrixMapDBNameComboBox(), gridBagConstraints18);
        }
        return matrixMapDBNamePane;
    }

    /**
     * This method initializes matrixMapDeleteButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixMapDeleteButton() {
        if (matrixMapDeleteButton == null) {
            matrixMapDeleteButton = new JButton();
            matrixMapDeleteButton.setText(rb.getString("DELETE_BUTTON"));
            matrixMapDeleteButton.addActionListener(e -> deleteSelectedMatrixMaps());
        }
        return matrixMapDeleteButton;
    }

    /**
     * This method initializes matrixMapEditButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixMapEditButton() {
        if (matrixMapEditButton == null) {
            matrixMapEditButton = new JButton();
            matrixMapEditButton.setText(rb.getString("EDIT_BUTTON"));
            matrixMapEditButton.addActionListener(e -> editMatrixMap());
        }
        return matrixMapEditButton;
    }

    /**
     * This method initializes matrixMapList
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getMatrixMapList() {
        if (matrixMapListScrollPane == null) {
            matrixMapListScrollPane = new JScrollPane();
        }
        return matrixMapListScrollPane;
    }

    /**
     * This method initializes matrixMapLoadButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixMapLoadButton() {
        if (matrixMapLoadButton == null) {
            matrixMapLoadButton = new JButton();
            matrixMapLoadButton.setText(rb.getString("LOAD_BUTTON"));
            matrixMapLoadButton.addActionListener(e -> loadMatrixMapList());
        }
        return matrixMapLoadButton;
    }

    /**
     * This method initializes MatrixMapSplitPane
     *
     * @return javax.swing.JSplitPane
     */
    private JSplitPane getMatrixMapSplitPane() {
        if (MatrixMapSplitPane == null) {
            MatrixMapSplitPane = new JSplitPane();
            MatrixMapSplitPane.setBounds(new Rectangle(0, 0, 0, 0));
            MatrixMapSplitPane.setRightComponent(getMatrixMapButtonPane());
            MatrixMapSplitPane.setLeftComponent(getMapListPane());
        }
        return MatrixMapSplitPane;
    }

    /**
     * This method initializes matrixRenameButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixRenameButton() {
        if (matrixRenameButton == null) {
            matrixRenameButton = new JButton();
            matrixRenameButton.setText(rb.getString("RENAME_BUTTON"));
            matrixRenameButton.addActionListener(e -> renameMatrices());
        }
        return matrixRenameButton;
    }

    /**
     * This method initializes MatrixSplitPane
     *
     * @return javax.swing.JSplitPane
     */
    private JSplitPane getMatrixSplitPane() {
        if (MatrixSplitPane == null) {
            MatrixSplitPane = new JSplitPane();
            MatrixSplitPane.setLeftComponent(getMatrixListPanel());
            MatrixSplitPane.setRightComponent(getMatrixButtonPanel());
        }
        return MatrixSplitPane;
    }

    /**
     * This method initializes passwordField
     *
     * @return javax.swing.JPasswordField
     */
    private JPasswordField getPasswordField() {
        if (passwordField == null) {
            passwordField = new JPasswordField();
            passwordField.setText("");
        }
        return passwordField;
    }

    /**
     * This method initializes portText
     *
     * @return javax.swing.JTextField
     */
    private JTextField getPortText() {
        if (portText == null) {
            portText = new JTextField();
            portText.setText("       ");
        }
        return portText;
    }

    /**
     * This method initializes renameMatrixMapButton
     *
     * @return javax.swing.JButton
     */
    private JButton getRenameMatrixMapButton() {
        if (renameMatrixMapButton == null) {
            renameMatrixMapButton = new JButton();
            renameMatrixMapButton.setText(rb.getString("RENAME_BUTTON"));
            renameMatrixMapButton.addActionListener(e -> renameMatrixMaps());
        }
        return renameMatrixMapButton;
    }

    /**
     * This method initializes serverText
     *
     * @return javax.swing.JTextField
     */
    private JTextField getServerText() {
        if (serverText == null) {
            serverText = new JTextField();
            serverText.setText("                      ");
        }
        return serverText;
    }

    /**
     * This method initializes userText
     *
     * @return javax.swing.JTextField
     */
    private JTextField getUserText() {
        if (userText == null) {
            userText = new JTextField();
            userText.setText("");
        }
        return userText;
    }

    /**
     * method to check if a given matrix name already exists
     *
     * @return true if name exists
     */

    private boolean isMatrixExisting(String s) throws SQLException {
        // check if name is already in use
        String query = "SELECT \"matrix_name\" FROM core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                " WHERE \"matrix_name\" = '" + s + "'";
        ResultSet rs = statement.executeQuery(query);
        return rs.next();
    }

    /**
     * method to check if a given MatrixMapName is existing in the database
     *
     * @return true if name exists
     */
    private boolean isMatrixMapExisting(String s) throws SQLException {
        // check if name is already in use
        String query =
                "SELECT \"matrixMap_name\" FROM core." + this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                        " WHERE \"matrixMap_name\" = '" + s + "'";
        ResultSet rs = statement.executeQuery(query);

        return rs.next();
    }

    /**
     * method to fill the matrix list with the entries from the db
     */
    private void loadMatrixList() {
        if (-1 == this.matrixDBNameComboBox.getSelectedIndex()) return;
        try {
            String query = "select matrix_name from core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                    " ORDER BY \"matrix_name\" ASC";
            ResultSet rs = statement.executeQuery(query);
            Vector<String> entries = new Vector<>();
            while (rs.next()) {
                entries.add(rs.getString(1));
            }
            this.matrixList = new JList<>(entries);
            this.matrixListScrollPane.getViewport().setView(matrixList);
            set2ndLevelMatrixButtonEnabled(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to fill the matrix map list with the entries from the db
     */
    private void loadMatrixMapList() {
        if (-1 == this.matrixMapDBNameComboBox.getSelectedIndex()) return;
        try {
            String query =
                    "select \"matrixMap_name\" from core." + this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                            " ORDER BY \"matrixMap_name\" ASC";
            ResultSet rs = statement.executeQuery(query);
            Vector<String> entries = new Vector<>();
            while (rs.next()) {
                entries.add(rs.getString(1));
            }
            this.matrixMapList = new JList<>(entries);
            this.matrixMapListScrollPane.getViewport().setView(matrixMapList);
            set2ndLevelMatrixMapEnabled(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to manipulate a matrix entry
     */
    private void manipulateTop3() {
        //check valid input
        if (this.matrixList.getSelectedIndex() < 0) {
            JOptionPane.showMessageDialog(this.controlInstance, rb.getString("MANIPULATE_MATRIX_SELECT_FIRST_DIALOG"));
            return;
        }
        try {
            List<String> selection = this.matrixList.getSelectedValuesList();

            for (String entry : selection) {
                //load the data from the db
                String query = "SELECT * FROM core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                        " WHERE \"matrix_name\" = '" + entry + "'";
                ResultSet rs = statement.executeQuery(query);
                if (rs.next()) {
                    Object array = rs.getArray(2).getArray();
                    if (array instanceof Integer[]) {
                        //parse data to memory model
                        Integer[] matrixVal = (Integer[]) array;
                        int size = (int) Math.sqrt(matrixVal.length);
                        if (size * size != matrixVal.length) {
                            JOptionPane.showMessageDialog(this.controlInstance, rb.getString("MATRIX_NOT_SQUARE"));
                        } else {
                            double[][] values = TPS_BasicConnectionClass.array1Dto2D(matrixVal);

                            TPS_Geometrics.calcTop3(values);

                            String name = entry;
                            name = JOptionPane.showInputDialog(this.controlInstance,
                                    rb.getString("ENTER_MATRIX_NAME_DIALOG"), name);
                            int overWrite = JOptionPane.NO_OPTION;
                            if (name.equals(entry)) {
                                overWrite = JOptionPane.showConfirmDialog(this.controlInstance,
                                        rb.getString("OVERWRITE_MATRIX_DIALOG"), rb.getString("OVERWRITE_MATRIX_TITLE"),
                                        JOptionPane.YES_NO_OPTION);
                                if (JOptionPane.NO_OPTION == overWrite) {
                                    return;
                                }
                            }


                            int answer = JOptionPane.showConfirmDialog(this.controlInstance,
                                    rb.getString("SAVE_TO_DATABASE_DIALOG"), rb.getString("SAVE_CHANGES"),
                                    JOptionPane.YES_NO_OPTION);
                            if (JOptionPane.YES_OPTION == answer) {
                                if (overWrite == JOptionPane.YES_OPTION) {
                                    //save data back
                                    query = "UPDATE core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                                            " SET matrix_values = " + TPS_BasicConnectionClass.matrixToSQLArray(values,
                                            0) + " WHERE \"matrix_name\" = '" + entry + "'";
                                } else {
                                    query = "INSERT INTO core." +
                                            this.matrixDBNameComboBox.getSelectedItem().toString() + " VALUES ('" +
                                            name + "'," + TPS_BasicConnectionClass.matrixToSQLArray(values, 0) + ")";
                                }

                                statement.execute(query);
                                loadMatrixList();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method initializes matrixEditTop3Button
     *
     * @return javax.swing.JButton
     */
    private JButton matrixEditTop3Button() {
        if (matrixEditTop3Button == null) {
            matrixEditTop3Button = new JButton();
            matrixEditTop3Button.setText(rb.getString("TOP3_BUTTON"));
            matrixEditTop3Button.addActionListener(e -> manipulateTop3());
        }
        return matrixEditTop3Button;
    }

    /**
     * method to open the db connection and to create the sqlStatement
     */
    private void openConnection() {
        String url =
                "jdbc:postgresql://" + serverText.getText() + ":" + portText.getText() + "/" + dbNameText.getText();
        String user = userText.getText();
        char[] passwordC = passwordField.getPassword();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < passwordC.length; ++i) {
            password.append(passwordC[i]);
            passwordC[i] = 0;
        }
        try {
            connection = java.sql.DriverManager.getConnection(url, user, password.toString());
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to change a matrix name in the database
     */

    protected void renameMatrices() {
        if (-1 == this.matrixList.getSelectedIndex()) {
            JOptionPane.showMessageDialog(controlInstance, rb.getString("NO_MATRIX_SELECTED"));
            return;
        }
        try {
            List<String> listEntries = this.matrixList.getSelectedValuesList();
            for (String entry : listEntries) {
                String s = entry;
                boolean done = false;
                do {
                    s = (String) JOptionPane.showInputDialog(controlInstance,
                            rb.getString("RENAME_ENTER_NEW_NAME") + entry + ": ", rb.getString("RENAME_MATRIX_TITLE"),
                            JOptionPane.PLAIN_MESSAGE, null, null, s);

                    // If a string was returned, say so.
                    if ((s != null) && (s.length() > 0)) {
                        if (isMatrixExisting(s)) {
                            JOptionPane.showMessageDialog(controlInstance, s + rb.getString("NAME_ALREADY_EXISTS"));
                        } else {
                            // update name
                            String query = "UPDATE core." + this.matrixDBNameComboBox.getSelectedItem().toString() +
                                    " SET \"matrix_name\"= '" + s + "'" + " WHERE \"matrix_name\" = '" + entry + "'";
                            statement.execute(query);
                            done = true;
                        }
                    } else {
                        done = true;
                    }
                } while (!done);
                loadMatrixList();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * method to rename the selected matrix maps
     */
    private void renameMatrixMaps() {
        if (-1 == this.matrixMapList.getSelectedIndex()) {
            JOptionPane.showMessageDialog(controlInstance, rb.getString("NO_MATRIX_MAP_SELECTED"));
            return;
        }
        try {
            List<String> listEntries = this.matrixMapList.getSelectedValuesList();
            for (String entry : listEntries) {
                String s = entry;
                boolean done = false;
                do {
                    s = (String) JOptionPane.showInputDialog(controlInstance,
                            rb.getString("RENAME_ENTER_NEW_NAME") + " " + entry + ": ",
                            rb.getString("RENAME_MATRIX_MAP_TITLE"), JOptionPane.PLAIN_MESSAGE, null, null, s);

                    // If a string was returned, say so.
                    if ((s != null) && (s.length() > 0)) {
                        if (isMatrixMapExisting(s)) {
                            JOptionPane.showMessageDialog(controlInstance,
                                    s + " " + rb.getString("NAME_ALREADY_EXISTS"));
                        } else {
                            // update name
                            String query = "UPDATE core." + this.matrixMapDBNameComboBox.getSelectedItem().toString() +
                                    " SET \"matrixMap_name\"= '" + s + "'" + " WHERE \"matrixMap_name\" = '" + entry +
                                    "'";
                            statement.execute(query);
                            done = true;
                        }
                    } else {
                        done = true;
                    }
                } while (!done);
                loadMatrixMapList();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to enable and disable 2nd level matrix actions
     */
    private void set2ndLevelMatrixButtonEnabled(boolean val) {
        this.matrixRenameButton.setEnabled(val);
        this.matrixAddButton.setEnabled(val);
        this.matrixEditButton.setEnabled(val);
        this.matrixEditTop3Button.setEnabled(val);
        this.matrixDeleteButton.setEnabled(val);
        this.matrixCloneButton.setEnabled(val);
        this.matrixImportButton.setEnabled(val);
    }

    /**
     * method to enable and disable 2nd level matrix map actions
     */
    private void set2ndLevelMatrixMapEnabled(boolean val) {
        this.matrixMapAddButton.setEnabled(val);
        this.matrixMapEditButton.setEnabled(val);
        this.renameMatrixMapButton.setEnabled(val);
        this.matrixMapDeleteButton.setEnabled(val);
        this.matrixMapCloneButton.setEnabled(val);
    }

    /**
     * This function enables and disables gui-elements according to the connection status
     *
     * @param connected indicates if db connection exists or not
     */
    private void setConnectionEnabled(boolean connected) {
        //connection control
        this.serverText.setEnabled(!connected);
        this.portText.setEnabled(!connected);
        this.userText.setEnabled(!connected);
        this.passwordField.setEnabled(!connected);
        this.connectButton.setEnabled(!connected);
        this.dbNameText.setEnabled(!connected);
        this.disconnectButton.setEnabled(connected);
        //matrixMap control
        this.matrixMapLoadButton.setEnabled(connected);
        this.matrixMapDBNameComboBox.setEnabled(connected);
        this.matrixMapListScrollPane.setEnabled(connected);
        //these buttons can only be disabled, enable is done though load-button
        if (!connected) {
            set2ndLevelMatrixMapEnabled(connected);
        }
        //matrix control
        this.matrixLoadButton.setEnabled(connected);
        this.matrixListScrollPane.setEnabled(connected);
        this.matrixDBNameComboBox.setEnabled(connected);
        //these buttons can only be disabled, enable is done though load-button
        if (!connected) {
            set2ndLevelMatrixButtonEnabled(connected);
        }
    }
}
