/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.gui;

import de.dlr.ivf.tapas.analyzer.tum.databaseConnector.DBTripReader;
import de.dlr.ivf.tapas.constants.TPS_SettlementSystem.TPS_SettlementSystemType;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.tools.TAZFilter.TAZFilter;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Eine UI-Komponente die das Auswählen von Simulationstabellen in der DB ermöglicht.
 *
 * @author boec_pa
 */
public class DBSimulationChooserPanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = 380071457799580782L;

    private final String NO_PICK = "      ";

    private final StyledDocument console;
    private final String schema = "core";
    private JList<String> lstSimulations;
    private JLabel lblPicked;
    private JComboBox<TPS_SettlementSystemType> cbConvert;
    private JComboBox<String> cbFilter;
    private final List<String[]> simdata = new ArrayList<>();
    private final List<String> simulations = new ArrayList<>();
    private final List<String> filters = new ArrayList<>();

    private StringListModel simulationDataModel;

    private TPS_DB_Connector dbCon = null;

    private TitledBorder border;

    /**
     * Create the panel.
     */
    public DBSimulationChooserPanel(String title, String initSimulation, StyledDocument console) {

        this.console = console;
        try {
            // Part 2: loading runtime file
            TPS_ParameterClass parameterClass = new TPS_ParameterClass();
            parameterClass.loadRuntimeParameters(TPS_BasicConnectionClass.getRuntimeFile());
            dbCon = new TPS_DB_Connector(parameterClass);
        } catch (IOException e) {
            // TODO handle no loginInfo file found
        } catch (ClassNotFoundException e) {
            // should not happen
        }

        createContents(title);
        cleanDB();

        if (null != initSimulation) {
            lstSimulations.setSelectedValue(initSimulation, true);
        }

    }

    /**
     * Create the panel.
     */
    public DBSimulationChooserPanel(StyledDocument console) {
        this("DB - Triptables", null, console);
    }

    public void cleanDB() {
        List<String> oldTables = getOldTables();

        if (oldTables != null) {
            // Custom button text
            Object[] options = {"Yes", "No"};
            int n = JOptionPane.showOptionDialog(this,
                    "The database contains old trip tables.\n" + "They should be deleted but could belong " +
                            "to another instance of the Tapas Analyzer.\n" + "Do you want to delete them?",
                    "Old tables found.", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options,
                    options[1]);
            if (n == 0) {// yes
                for (String t : oldTables) {
                    try {
                        dbCon.getConnection(this).createStatement().executeUpdate("DROP TABLE " + t);
                    } catch (SQLException e) {
                        System.err.println("Could not drop table " + t);
                    }
                }

            }
        }
    }

    private void createContents(String title) {
        // TODO stop Jlist from changing size on selection

        border = new TitledBorder(//
                new EtchedBorder(EtchedBorder.LOWERED, null, null),//
                title, //
                TitledBorder.LEADING,//
                TitledBorder.TOP,//
                null,//
                new Color(0, 0, 0));
        setBorder(border);

        // general layout setup
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{200, 100};
        gridBagLayout.rowHeights = new int[]{120, 30, 30};
        gridBagLayout.columnWeights = new double[]{0.8, 0.2};
        gridBagLayout.rowWeights = new double[]{0.7, 0.15, 0.15};
        setLayout(gridBagLayout);

        // adding basic components
        GridBagConstraints gbc = new GridBagConstraints();

        lblPicked = new JLabel();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(lblPicked, gbc);

        JButton btnRefresh = new JButton();
        btnRefresh.setText("Refresh");

        btnRefresh.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getDatabaseContent();
                // adding lists with content from database
                if (!getDatabaseContent()) lblPicked.setText("Error while fetching trips from database.");
                // if (!getDatabaseContent()) {
                // simulationDataModel = new StringListModel(simulations);
                // lstSimulations.setModel(simulationDataModel);
                // cbFilter.setModel(new DefaultComboBoxModel<String>());
                // updateLabel();
                // } else {
                // lblPicked
                // .setText("Error while fetching trips from database.");
                // }
            }
        });

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(btnRefresh, gbc);

        JLabel lblConvert = new JLabel("Typ der RegionCodes");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        add(lblConvert, gbc);

        cbConvert = new JComboBox<>(TPS_SettlementSystemType.values());
        cbConvert.setSelectedIndex(0);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(cbConvert, gbc);

        JLabel lblFilter = new JLabel("Filter");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        add(lblFilter, gbc);

        cbFilter = new JComboBox<>();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(cbFilter, gbc);

        lstSimulations = new JList<>();
        lstSimulations.setVisibleRowCount(6);
        lstSimulations.setSelectionModel(new ToggleSelectionModel());
        lstSimulations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lstSimulations.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                simulationDataModel.changed();
                updateLabel();
            }
        });

        JScrollPane scrollPane = new JScrollPane(lstSimulations);
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(scrollPane, gbc);

        getDatabaseContent();
    }

    /**
     * @return <code>null</code> if anything went wrong.
     */
    public DBTripReader createDBTripReader() {

        int idx = lstSimulations.getSelectedIndex();

        String simulation = simdata.get(idx)[0];
        String region = simdata.get(idx)[1];
        String hhkey = simdata.get(idx)[2];

        String schema = this.schema;// TODO choose schema from Parameters or GUI

        try {

            TPS_SettlementSystemType settlementType = (TPS_SettlementSystemType) cbConvert.getSelectedItem();

            String mapping = (String) cbFilter.getSelectedItem();
            Set<Integer> acceptedTAZs = null;
            if (!mapping.equals(NO_PICK)) {
                acceptedTAZs = TAZFilter.getTAZValues(mapping, dbCon, this);
            }

            return new DBTripReader(simulation, hhkey, schema, region, settlementType, acceptedTAZs, dbCon, console);
        } catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private boolean getDatabaseContent() {

        try {
            String q = "SELECT s.sim_key, sp.param_value as region, sp2.param_value as hhkey, sim_finished, sim_description" +
                    "    FROM simulations s join simulation_parameters sp on (s.sim_key = sp.sim_key)" +
                    "        join simulation_parameters sp2 on (s.sim_key = sp2.sim_key) " +
                    "WHERE (sim_finished or (not sim_started and sim_progress>0)) and sp.param_key = 'DB_REGION' " +
                    "and sp2.param_key = 'DB_HOUSEHOLD_AND_PERSON_KEY' ORDER BY sim_key";

            simdata.clear();
            simulations.clear();

            ResultSet rs = dbCon.executeQuery(q, this);
            while (rs.next()) {
                String simkey = rs.getString("sim_key");
                String description = rs.getString("sim_description");
                String region = rs.getString("region");
                String hhkey = rs.getString("hhkey");
                boolean finished = rs.getBoolean("sim_finished");

                if (description == null) description = "";
                else description = " " + description;
                String[] arr = {simkey, region, hhkey};
                simdata.add(arr);
                simulations.add(simkey + description + (finished ? "" : "\t(paused)"));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Could not fetch meta data. Check connection to database.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        try {
            filters.clear();
            filters.addAll(TAZFilter.getMappingNames(dbCon, this));
        } catch (SQLException e) {
            System.err.println("Could not fetch filters.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        // update content
        simulationDataModel = new StringListModel(simulations);
        lstSimulations.setModel(simulationDataModel);

        DefaultComboBoxModel<String> cbFilterModel = new DefaultComboBoxModel<>(new String[]{NO_PICK});
        for (String f : filters)
            cbFilterModel.addElement(f);
        cbFilter.setModel(cbFilterModel);
        updateLabel();

        return true;
    }

    private List<String> getOldTables() {

        ArrayList<String> result = new ArrayList<>();

        try {
            ArrayList<String> tables = new ArrayList<>();
            DatabaseMetaData metaData = dbCon.getConnection(this).getMetaData();
            ResultSet res = metaData.getTables(null, null, null, new String[]{"TABLE"});

            while (res.next()) {
                tables.add(res.getString("TABLE_NAME"));
            }

            for (String t : tables) {
                if (t.startsWith("tt_")) {
                    result.add(t);
                }
            }

        } catch (SQLException e) {
            System.err.println("Could not fetch meta data. Check connection to database.");
        }

        if (result.size() > 0) return result;
        else return null;
    }

    public String getTitle() {
        return border.getTitle();
    }

    public void setTitle(String title) {
        border.setTitle(title);
    }

    public boolean isReady() {
        return lstSimulations.getSelectedIndex() > -1;
    }

    private void updateLabel() {
        String s = lstSimulations.getSelectedValue();
        if (s == null) {
            String NOSIM = "No simulation picked yet.";
            lblPicked.setText(NOSIM);
        } else {
            lblPicked.setText(s + " is selected.");
        }

    }

    private class StringListModel implements ListModel<String> {

        private final List<ListDataListener> listeners = new ArrayList<>();
        private final List<String> list;

        public StringListModel(List<String> list) {
            super();
            this.list = list;
        }

        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        public void changed() {
            for (ListDataListener listener : listeners)
                listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, list.size() - 1));
        }

        public String getElementAt(int index) {
            return list.get(index);
        }

        public int getSize() {
            return list.size();
        }

        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }
    }

    private class ToggleSelectionModel extends DefaultListSelectionModel {
        /**
         *
         */
        private static final long serialVersionUID = -1823196873050478233L;

        public void setSelectionInterval(int index0, int index1) {
            if (isSelectedIndex(index0)) {
                super.removeSelectionInterval(index0, index1);
            } else {
                super.setSelectionInterval(index0, index1);
            }
        }
    }
}
