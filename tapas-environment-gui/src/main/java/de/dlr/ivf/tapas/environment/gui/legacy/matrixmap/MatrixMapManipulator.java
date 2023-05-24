/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.legacy.matrixmap;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Vector;

/**
 * Class for the "edit MatrixMap"-Dialog.
 * This modal dialog is called by the MatrixMapHandler
 *
 * @author hein_mh
 */

public class MatrixMapManipulator extends JDialog {
    /**
     * Automatic serialVErsionID for serialisation
     */
    private static final long serialVersionUID = -6979246790481072698L;
    private Statement statement = null;
    private String dbMatrixName = "";
    private String dbName = "";
    private String entryName = "";  //  @jve:decl-index=0:
    private JPanel mainPanel1 = null;
    private JSplitPane mainSplitPane = null;
    private JPanel availableMatrixPanel = null;
    private JPanel currentMatrixMapSetPanel = null;
    private JScrollPane matrixScrollPane = null;
    private JList<String> availableMatrixList;
    private JList<String> matrixMapList;
    private JScrollPane matrixMapEntriesScrollPane = null;
    private JPanel matrixMapButtonPanel = null;
    private JPanel matrixButtonPanel = null;
    private JButton matrixAddButton = null;
    private JButton changeMatrixButton = null;
    private JButton changeTimeButton = null;
    private JButton deleteButton = null;
    private String[] matrixEntries = null;
    private double[] distribution = null;
    private JButton saveButton = null;

    /**
     * Method to get an instance and initialise it
     *
     * @param parent the parent frame
     */
    MatrixMapManipulator(JFrame parent) {
        super(parent, true);
        initialize();
    }

    /**
     * static method to get a dialog
     *
     * @param parent      parent frame for modal mode
     * @param stat        the statement for the sql-db
     * @param dbMatrixMap the name of the matrixMap-table
     * @param dbMatrix    the name of the matrix-table
     * @param entry       the entry to edit in dbMatrixMap
     */
    public static void showDialog(JFrame parent, Statement stat, String dbMatrixMap, String dbMatrix, String entry) {
        MatrixMapManipulator instance = new MatrixMapManipulator(parent);
        instance.statement = stat;
        instance.dbName = dbMatrixMap;
        instance.dbMatrixName = dbMatrix;
        instance.entryName = entry;
        instance.setTitle("Edit MatrixMap " + instance.entryName);
        instance.matrixAddButton.setEnabled(false);
        instance.loadMatrixList();
        instance.changeMatrixButton.setEnabled(false);
        instance.changeTimeButton.setEnabled(false);
        instance.deleteButton.setEnabled(false);
        instance.loadMatrixMapEntries();
        instance.setVisible(true);
    }

    /**
     * method to add the selected matrix to the matrixMap
     */
    private void addMatrixToMatrixMap() {
        List<String> selection = this.availableMatrixList.getSelectedValuesList();
        for (String entry : selection) {
            boolean done;
            do {
                String s = (String) JOptionPane.showInputDialog(this, "Enter the end time for " + entry + ": ",
                        "Add " + entry + " to matrixMap", JOptionPane.PLAIN_MESSAGE, null, null, null);
                // If a string was returned, say so.
                if ((s != null) && (s.length() > 0)) {
                    double time = Double.parseDouble(s);
                    if (this.isValidTime(time)) {
                        double[] newDistribution = new double[distribution.length + 1];
                        String[] newEntries = new String[this.matrixEntries.length + 1];
                        //copy old values
                        for (int j = 0; j < this.distribution.length; ++j) {
                            newDistribution[j] = this.distribution[j];
                            newEntries[j] = this.matrixEntries[j];
                        }
                        //insert new values
                        newDistribution[this.distribution.length] = time;
                        newEntries[this.distribution.length] = entry;
                        //update and sort values
                        this.distribution = newDistribution;
                        this.matrixEntries = newEntries;
                        this.checkMatrixMap();
                        this.updateMatrixMapDisplay();
                        this.saveButton.setEnabled(true);
                        done = true;
                    } else {
                        JOptionPane.showMessageDialog(this, "Entered time allready exists: " + time);
                        done = false;
                    }
                } else {
                    done = true;
                }
            } while (!done);
        }
    }

    /**
     * method to change the selected matrix-references in the matrixmap
     */
    private void changeMatrixInMatrixMap() {
        int[] selection = this.matrixMapList.getSelectedIndices();

        //get the matrices from availableMAtrixList and put them in a string-array
        String[] availMatrices = new String[this.availableMatrixList.getModel().getSize()];
        for (int i = 0; i < this.availableMatrixList.getModel().getSize(); ++i) {
            availMatrices[i] = this.availableMatrixList.getModel().getElementAt(i);
        }
        for (int value : selection) {
            if (value < this.matrixEntries.length) {
                String s = (String) JOptionPane.showInputDialog(this,
                        "Select new value for " + this.matrixEntries[value] + " - " + this.distribution[value] + ": ",
                        "Change " + this.matrixEntries[value], JOptionPane.PLAIN_MESSAGE, null, availMatrices,
                        this.matrixEntries[value]);
                // If a string was returned, say so.
                if ((s != null) && (s.length() > 0)) {
                    this.matrixEntries[value] = s;
                    this.updateMatrixMapDisplay();
                    this.saveButton.setEnabled(true);
                }
            }
        }
    }

    /**
     * method to change the time for the selected entries
     */
    private void changeTimeInMatrixMap() {
        if (this.matrixEntries.length == 0) return;  //if no matrix entries are available, return
        int[] selection = this.matrixMapList.getSelectedIndices();
        for (int value : selection) {
            if (value < this.matrixEntries.length) {
                boolean done;
                do {
                    String s = (String) JOptionPane.showInputDialog(this,
                            "Enter the new end time for " + this.matrixEntries[value] + ": ",
                            "Change time for " + this.matrixEntries[value], JOptionPane.PLAIN_MESSAGE, null, null,
                            this.distribution[value]);
                    // If a string was returned, say so.
                    if ((s != null) && (s.length() > 0)) {
                        double time = Double.parseDouble(s);
                        if (this.isValidTime(time)) {
                            //update time and sort values
                            this.distribution[value] = time;
                            this.checkMatrixMap();
                            this.updateMatrixMapDisplay();
                            this.saveButton.setEnabled(true);
                            done = true;
                        } else {
                            JOptionPane.showMessageDialog(this, "Entered time allready exists: " + time);
                            done = false;
                        }
                    } else {
                        done = true;
                    }
                } while (!done);
            }
        }
    }

    /**
     * method to check, if the entries are valid and save them
     *
     * @return sucessfull check and save
     */
    private boolean checkAndSaveMatrixMap() {
        if (checkMatrixMap()) {
            // build entry
            boolean success = true;
            StringBuilder query = new StringBuilder(
                    "UPDATE core." + this.dbName + " SET \"matrixMap_num\" = " + this.distribution.length +
                            ", \"matrixMap_matrixNames\" = '{");

            // build array of matrixnames
            for (int j = 0; j < this.matrixEntries.length; ++j) {
                if (j < matrixEntries.length - 1) query.append("\"").append(matrixEntries[j]).append("\",");
                else query.append("\"").append(matrixEntries[j]).append("\"");
            }

            query.append("}', \"matrixMap_distribution\" = '{");
            // build distribution
            for (int j = 0; j < distribution.length; ++j) {
                if (j < distribution.length - 1) query.append(distribution[j]).append(",");
                else query.append(distribution[j]);
            }

            // set matrixmap-entry
            query.append("}' WHERE \"matrixMap_name\" = '").append(this.entryName).append("'");

            try {
                statement.execute(query.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                success = false;
            }

            this.saveButton.setEnabled(false);
            return success;
        } else {
            JOptionPane.showMessageDialog(this, "Multiple time-entries found. Please correct them!");
            return false;
        }
    }

    /**
     * method to check, if the entries are valid
     *
     * @return sucessfull check
     */
    private boolean checkMatrixMap() {
        boolean result = true, changed = true;
        double tmpD;
        String tmpS;
        //sort entries: use bubble sort
        for (int j = 0; j < this.matrixEntries.length && changed; ++j) {
            changed = false;
            for (int i = this.matrixEntries.length - 1; i > j; --i) {
                if (this.distribution[i] < this.distribution[i - 1]) {
                    //swap the entries in distribution and matrixEntries
                    changed = true;
                    tmpD = this.distribution[i];
                    this.distribution[i] = this.distribution[i - 1];
                    this.distribution[i - 1] = tmpD;
                    tmpS = this.matrixEntries[i];
                    this.matrixEntries[i] = this.matrixEntries[i - 1];
                    this.matrixEntries[i - 1] = tmpS;
                } else if (this.distribution[i] == this.distribution[i - 1]) { // perform check for same time entries
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * method to check if saving is needed and asks to save or not
     *
     * @return success of this operation
     */
    private boolean closeAction() {
        if (this.saveButton.isEnabled()) {
            int answer = JOptionPane.showConfirmDialog(this, "Changes not saved. Save now?", "Save changes?",
                    JOptionPane.YES_NO_OPTION);
            if (JOptionPane.YES_OPTION == answer) {
                return checkAndSaveMatrixMap();
            }
        }
        return true;
    }

    /**
     * method to delete selected matrix entries from matrixMap
     */
    private void deleteMatrixFromMatrixMap() {
        int[] selection = this.matrixMapList.getSelectedIndices();
        if (selection.length == 0) {
            return;
        }
        for (int value : selection) {
            // user selected an empty entry, this is a bug in java: inform and abort
            if (value >= this.matrixEntries.length) {
                JOptionPane.showMessageDialog(this, "Empty cells selected! Please unselect them and retry");
                return;
            }
        }

        int answer = JOptionPane.showConfirmDialog(this, "Are you sure to delete the selected items?",
                "Delete " + "matrices from MatrixMap", JOptionPane.YES_NO_OPTION);
        if (JOptionPane.NO_OPTION == answer) {
            return;
        }

        String[] newMatrixEntries = new String[this.matrixEntries.length - selection.length];
        double[] newDistribution = new double[this.matrixEntries.length - selection.length];
        int newIndex = 0, selectionIndex = 0;
        for (int i = 0; i < matrixEntries.length; ++i) {
            if (selectionIndex < selection.length && i == selection[selectionIndex]) {
                ++selectionIndex;
            } else {
                newMatrixEntries[newIndex] = this.matrixEntries[i];
                newDistribution[newIndex] = this.distribution[i];
                newIndex++;
            }
        }

        this.distribution = newDistribution;
        this.matrixEntries = newMatrixEntries;
        this.updateMatrixMapDisplay();
        this.saveButton.setEnabled(true);
    }

    /**
     * This method initializes availableMatrixPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getAvailableMatrixPanel() {
        if (availableMatrixPanel == null) {
            GridBagConstraints gridBagConstraints2 = MatrixMapHandler.newGridBag(0, 0, 1, 0);
            GridBagConstraints gridBagConstraints1 = MatrixMapHandler.newGridBag(0, 500, 0, 600);
            gridBagConstraints1.fill = GridBagConstraints.BOTH;

            availableMatrixPanel = new JPanel();
            availableMatrixPanel.setLayout(new GridBagLayout());
            availableMatrixPanel.add(getMatrixScrollPane(), gridBagConstraints1);
            availableMatrixPanel.add(getMatrixButtonPanel(), gridBagConstraints2);
        }
        return availableMatrixPanel;
    }

    /**
     * This method initializes changeMatrixButton
     *
     * @return javax.swing.JButton
     */
    private JButton getChangeMatrixButton() {
        if (changeMatrixButton == null) {
            changeMatrixButton = new JButton();
            changeMatrixButton.setText("Change matrix");
            changeMatrixButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    changeMatrixInMatrixMap();
                }
            });
        }
        return changeMatrixButton;
    }

    /**
     * This method initializes changeTimeButton
     *
     * @return javax.swing.JButton
     */
    private JButton getChangeTimeButton() {
        if (changeTimeButton == null) {
            changeTimeButton = new JButton();
            changeTimeButton.setText("Change time");
            changeTimeButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    changeTimeInMatrixMap();
                }
            });
        }
        return changeTimeButton;
    }

    /**
     * This method initializes currentMatrixMapSetPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getCurrentMatrixMapSetPanel() {
        if (currentMatrixMapSetPanel == null) {
            GridBagConstraints gridBagConstraints5 = MatrixMapHandler.newGridBag(0, 0, 1, 0);
            GridBagConstraints gridBagConstraints4 = MatrixMapHandler.newGridBag(0, 500, 0, 600);
            gridBagConstraints4.fill = GridBagConstraints.BOTH;
            currentMatrixMapSetPanel = new JPanel();
            currentMatrixMapSetPanel.setLayout(new GridBagLayout());
            currentMatrixMapSetPanel.add(getMatrixMapEntriesScrollPane(), gridBagConstraints4);
            currentMatrixMapSetPanel.add(getMatrixMapButtonPanel(), gridBagConstraints5);
        }
        return currentMatrixMapSetPanel;
    }

    /**
     * This method initializes deleteButton
     *
     * @return javax.swing.JButton
     */
    private JButton getDeleteButton() {
        if (deleteButton == null) {
            deleteButton = new JButton();
            deleteButton.setText("Delete");
            deleteButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    deleteMatrixFromMatrixMap();
                }
            });
        }
        return deleteButton;
    }

    /**
     * This method initializes mainPanel1
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMainPanel1() {
        if (mainPanel1 == null) {
            GridBagConstraints gridBagConstraints1 = MatrixMapHandler.newGridBag(0, 0, 1, 0);
            GridBagConstraints gridBagConstraints = MatrixMapHandler.newGridBag(0, 0, 0, 0);
            mainPanel1 = new JPanel();
            mainPanel1.setLayout(new GridBagLayout());
            mainPanel1.add(getMainSplitPane(), gridBagConstraints);
            mainPanel1.add(getSaveButton(), gridBagConstraints1);
        }
        return mainPanel1;
    }

    /**
     * This method initializes mainSplitPane
     *
     * @return javax.swing.JSplitPane
     */
    private JSplitPane getMainSplitPane() {
        if (mainSplitPane == null) {
            mainSplitPane = new JSplitPane();
            mainSplitPane.setLeftComponent(getAvailableMatrixPanel());
            mainSplitPane.setRightComponent(getCurrentMatrixMapSetPanel());
        }
        return mainSplitPane;
    }

    /**
     * This method initializes matrixAddButton
     *
     * @return javax.swing.JButton
     */
    private JButton getMatrixAddButton() {
        if (matrixAddButton == null) {
            matrixAddButton = new JButton();
            matrixAddButton.setText("Add");
            matrixAddButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    addMatrixToMatrixMap();
                }
            });
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
            matrixButtonPanel.setLayout(new GridBagLayout());
            matrixButtonPanel.add(getMatrixAddButton(), new GridBagConstraints());
        }
        return matrixButtonPanel;
    }

    /**
     * This method initializes matrixMapButtonPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getMatrixMapButtonPanel() {
        if (matrixMapButtonPanel == null) {
            matrixMapButtonPanel = new JPanel();
            matrixMapButtonPanel.setLayout(new GridBagLayout());
            matrixMapButtonPanel.add(getChangeMatrixButton(), new GridBagConstraints());
            matrixMapButtonPanel.add(getChangeTimeButton(), new GridBagConstraints());
            matrixMapButtonPanel.add(getDeleteButton(), new GridBagConstraints());
        }
        return matrixMapButtonPanel;
    }

    /**
     * This method initializes matrixMapEntriesScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getMatrixMapEntriesScrollPane() {
        if (matrixMapEntriesScrollPane == null) {
            matrixMapEntriesScrollPane = new JScrollPane();
        }
        return matrixMapEntriesScrollPane;
    }

    /**
     * This method initializes matrixScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getMatrixScrollPane() {
        if (matrixScrollPane == null) {
            matrixScrollPane = new JScrollPane();
        }
        return matrixScrollPane;
    }

    /**
     * This method initializes saveButton
     *
     * @return javax.swing.JButton
     */
    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton();
            saveButton.setText("Save");
            saveButton.setEnabled(false);
            saveButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    checkAndSaveMatrixMap();
                }
            });
        }
        return saveButton;
    }

    /**
     * This method initialises this instance
     */
    private void initialize() {
        Toolkit jTools = Toolkit.getDefaultToolkit();
        Dimension dim = jTools.getScreenSize();
        this.setSize(1024, 710);
        this.setLocation(dim.width / 2 - this.getWidth() / 2, dim.height / 2 - this.getHeight() / 2);

        this.setTitle("Edit MatrixMap");
        this.setContentPane(getMainPanel1());
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (closeAction()) {
                    dispose();
                }
            }
        });

    }

    /**
     * method to check if this new timeslot is valid
     *
     * @return true if valid
     */
    private boolean isValidTime(double time) {
        boolean result = true;

        for (int i = 0; i < this.distribution.length && result; ++i) {
            if (this.distribution[i] > time) {
                break;
            } else if (this.distribution[i] == time) {
                result = false;
            }
        }
        return result;
    }

    /**
     * method to fill the matrixlist with the entries from the db
     */
    private void loadMatrixList() {
        if (null == this.dbMatrixName) return;
        try {
            String querry = "SELECT matrix_name FROM core." + this.dbMatrixName + " ORDER BY \"matrix_name\" ASC";
            ResultSet rs = statement.executeQuery(querry);
            Vector<String> entries = new Vector<>();
            while (rs.next()) {
                entries.add(rs.getString(1));
            }
            this.availableMatrixList = new JList<>(entries);
            this.availableMatrixList.setPrototypeCellValue("long name for matrix entries in db and to display");
            this.matrixScrollPane.getViewport().setView(availableMatrixList);
            this.matrixAddButton.setEnabled(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * method to fill the matrixlist with the entries from the db
     */
    private void loadMatrixMapEntries() {
        if (null == this.dbName || null == this.entryName) return;
        try {
            // get the entries
            String querry =
                    "SELECT * FROM core." + this.dbName + " WHERE \"matrixMap_name\" = '" + this.entryName + "'";
            ResultSet rs = statement.executeQuery(querry);
            if (rs.next()) {
                if (rs.getInt(2) > 0) {
                    Object array = rs.getArray(3).getArray();
                    if (array instanceof String[]) {
                        this.matrixEntries = (String[]) array;
                    } else {
                        throw new SQLException("Cannot cast to String array");
                    }
                    this.distribution = new double[rs.getInt(2)];

                    array = rs.getArray(4).getArray();
                    if (array instanceof Double[]) {
                        Double[] dist = (Double[]) array;
                        for (int j = 0; j < dist.length; ++j) {
                            this.distribution[j] = Double.parseDouble(dist[j].toString());
                        }
                    } else {
                        throw new SQLException("Cannot cast to Double array");
                    }
                } else {
                    this.matrixEntries = new String[0];
                    this.distribution = new double[0];
                }
            }
            if (this.checkMatrixMap()) {
                updateMatrixMapDisplay();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.changeMatrixButton.setEnabled(true);
        this.changeTimeButton.setEnabled(true);
        this.deleteButton.setEnabled(true);
    }

    /**
     * method to update the matrixmap-related fields
     */
    private void updateMatrixMapDisplay() {
        String[] displayNames = new String[this.entryName.length()];
        for (int i = 0; i < this.matrixEntries.length; ++i) {
            displayNames[i] = this.matrixEntries[i] + " - time: " + this.distribution[i];
        }
        this.matrixMapList = new JList<>(displayNames);
        this.matrixMapList.setPrototypeCellValue("long name for matrix entries in db and to display - time xx.x");
        this.matrixMapEntriesScrollPane.getViewport().setView(matrixMapList);
    }
}
