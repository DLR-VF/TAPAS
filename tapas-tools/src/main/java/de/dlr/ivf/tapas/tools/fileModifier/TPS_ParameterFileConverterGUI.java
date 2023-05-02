/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.fileModifier;

import de.dlr.ivf.tapas.model.parameter.*;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties.ClientControlPropKey;
import de.dlr.ivf.tapas.tools.fileModifier.filefilter.ExtensionFilter;
import de.dlr.ivf.tapas.tools.fileModifier.persistence.TPS_ParameterCSVDAO;
import de.dlr.ivf.tapas.tools.fileModifier.persistence.TPS_ParameterPropertiesDAO;
import de.dlr.ivf.tapas.tools.fileModifier.persistence.TPS_ParameterXLSDAO;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

public class TPS_ParameterFileConverterGUI {

    JTextField fieldPath;
    JTextField fieldDefault;
    JTextField fieldConfig;
    private final SortedMap<String, String> configMap = new TreeMap<>();
    private final SortedMap<String, String> defaultMap = new TreeMap<>();
    private final JFrame frame;
    private final String[] header = new String[]{"Key", "Default Value", "", "Config Value", ""};
    private final ConfigurationTable table;
    private String mSimKey;
    private ClientControlProperties mClientControlProperties;
    private final TPS_ParameterClass parameterClass;
    private File tapasNetworkDirectory;

    public TPS_ParameterFileConverterGUI(TPS_ParameterClass parameterClass) {
        this.parameterClass = parameterClass;
        this.frame = new JFrame();
        this.frame.setIconImage(
                new ImageIcon(getClass().getClassLoader().getResource("icons/TAPAS-Logo.gif")).getImage());
        this.table = new ConfigurationTable(new DefaultTableModel(new String[0][header.length], header));

        readClientProperties();

        this.init();

        this.frame.setSize(1116, (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - 30);
        this.frame.setResizable(false);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            System.err.println(
                    "Unable to set specific look and feel: com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            e.printStackTrace();
        }

        new TPS_ParameterFileConverterGUI(new TPS_ParameterClass()).show();
    }

    /**
     * Checks if the given ParamString has the correct db-schema format, which
     * means, it should end with a dot.
     *
     * @param param The parameter to check
     */
    private String checkSchema(Map<String, String> configMap, String param) {
        String val = configMap.get(param);
        if (val != null && !val.endsWith(".")) {
            val = val + ".";
            configMap.put(param, val);
        }
        return val;
    }

    private String chooseSimulation(Connection con) throws SQLException {
        if (mSimKey != null) {
            return mSimKey;
        }
        try (Statement statement = con.createStatement()) {
            ResultSet resultSet = statement.executeQuery(
                    "SELECT sim_key FROM " + this.parameterClass.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS));
            List<String> simKeys = new ArrayList<>();
            while (resultSet.next()) {
                // alle Configs aus Datenbank auslesen und den Nutzer auswählen
                // lassen, welche Config geladen/angezeigt werden soll
                String sim_key = resultSet.getString("sim_key");
                simKeys.add(sim_key);
            }
            return (String) JOptionPane.showInputDialog(frame, "Wähle die Simulation aus:\n", "Wähle Simulation",
                    JOptionPane.PLAIN_MESSAGE, null/* icon */, simKeys.toArray(), simKeys.get(0));
        }

    }

    private void fill(SortedMap<String, String> map) {
        EnumSet<ParamFlag> flagSet = EnumSet.allOf(ParamFlag.class);
        EnumSet<ParamString> stringSet = EnumSet.allOf(ParamString.class);
        EnumSet<ParamValue> valueSet = EnumSet.allOf(ParamValue.class);

        for (ParamFlag pf : flagSet) {
            if (this.parameterClass.isDefined(pf)) {
                map.put(pf.name(), Boolean.toString(this.parameterClass.isTrue(pf)));
            }
        }
        for (ParamString ps : stringSet) {
            if (this.parameterClass.isDefined(ps)) {
                map.put(ps.name(), this.parameterClass.getString(ps));
            }
        }
        for (ParamValue pv : valueSet) {
            if (this.parameterClass.isDefined(pv)) {
                map.put(pv.name(), Double.toString(this.parameterClass.getDoubleValue(pv)));
            }
        }
    }

    private void generateTemporaryParametersInMap(Map<String, String> configMap2) {
        //String core = checkSchema(configMap2, "DB_SCHEMA_CORE");
        String schemeTemp = checkSchema(configMap2, "DB_SCHEMA_TEMP");
        String tmp, runID = configMap2.get("RUN_IDENTIFIER");
        if (runID != null) {
            tmp = configMap2.get("DB_TABLE_TRIPS") + "_" + runID;
            configMap2.put("DB_TABLE_TRIPS", tmp);
            if (schemeTemp != null) {
                configMap2.put("DB_TABLE_LOCATION_TMP", schemeTemp + "locations_" + runID);
                configMap2.put("DB_TABLE_HOUSEHOLD_TMP", schemeTemp + "households_" + runID);
            }
        }
    }

    private SortedMap<String, String> getDefaultParams() {
        SortedMap<String, String> params = new TreeMap<>();
        for (ParamString paramString : ParamString.values()) {
            if (this.parameterClass.paramStringClass.getPreset(paramString) != null && !this.configMap.containsKey(
                    paramString.name())) params.put(paramString.name(),
                    this.parameterClass.paramStringClass.getPreset(paramString));
        }
        for (ParamValue paramValue : ParamValue.values()) {
            if (this.parameterClass.paramValueClass.getPreset(paramValue) != null && !this.configMap.containsKey(
                    paramValue.name())) params.put(paramValue.name(),
                    this.parameterClass.paramValueClass.getPreset(paramValue).toString());
        }
        for (ParamFlag paramFlag : ParamFlag.values()) {
            if (this.parameterClass.paramFlagClass.getPreset(paramFlag) != null && !this.configMap.containsKey(
                    paramFlag.name())) params.put(paramFlag.name(),
                    this.parameterClass.paramFlagClass.getPreset(paramFlag).toString());
        }
        return params;
    }

    private String getNewSimKey() {
        Calendar c = Calendar.getInstance();
        NumberFormat f00 = new DecimalFormat("00");
        NumberFormat f000 = new DecimalFormat("000");

        int msOffset = 0;
        return c.get(Calendar.YEAR) + "y_" + f00.format(c.get(Calendar.MONTH) + 1) + "m_" + f00.format(
                c.get(Calendar.DAY_OF_MONTH)) + "d_" + f00.format(c.get(Calendar.HOUR_OF_DAY)) + "h_" + f00.format(
                c.get(Calendar.MINUTE)) + "m_" + f00.format(c.get(Calendar.SECOND)) + "s_" + f000.format(
                c.get(Calendar.MILLISECOND) + msOffset) + "ms";
    }

    private void init() {
        JLabel lblPath = new JLabel("Path: ");
        JLabel lblDefault = new JLabel("Default File: ");
        JLabel lblConfig = new JLabel("Config File: ");
        fieldPath = new JTextField();
        fieldPath.setEditable(false);
        fieldPath.setBackground(Color.WHITE);
        fieldDefault = new JTextField();
        fieldDefault.setEditable(false);
        fieldDefault.setBackground(Color.WHITE);
        fieldConfig = new JTextField();

        JPanel panelPath = new JPanel(new BorderLayout());
        panelPath.add(lblPath, BorderLayout.WEST);
        panelPath.add(fieldPath, BorderLayout.CENTER);
        JPanel panelConfig = new JPanel(new BorderLayout());
        panelConfig.add(lblConfig, BorderLayout.WEST);
        panelConfig.add(fieldConfig, BorderLayout.CENTER);
        JPanel panelDefault = new JPanel(new BorderLayout());
        panelDefault.add(lblDefault, BorderLayout.WEST);
        panelDefault.add(fieldDefault, BorderLayout.CENTER);

        JPanel panelFile = new JPanel(new GridLayout(1, 2, 30, 5));
        panelFile.add(panelDefault);
        panelFile.add(panelConfig);

        JPanel panelNorth = new JPanel(new GridLayout(2, 1, 5, 5));
        panelNorth.add(panelPath);
        panelNorth.add(panelFile);
        panelNorth.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        EnumSet<ParamFlag> flagSet = EnumSet.allOf(ParamFlag.class);
        EnumSet<ParamString> stringSet = EnumSet.allOf(ParamString.class);
        EnumSet<ParamValue> valueSet = EnumSet.allOf(ParamValue.class);

        int row = 0;
        for (ParamFlag pf : flagSet) {
            row = insert(pf.name(), this.parameterClass.getType(pf), row);
        }
        for (ParamString ps : stringSet) {
            row = insert(ps.name(), this.parameterClass.getType(ps), row);
        }
        for (ParamValue pv : valueSet) {
            row = insert(pv.name(), this.parameterClass.getType(pv), row);
        }

        table.getTableHeader().setReorderingAllowed(false);
        new DropTarget(table, new FileTargetListener());
        table.initTableColum(0, 350, new ConfigurationTableRenderer(), null);
        table.initTableColum(1, 350, new ConfigurationTableRenderer(), null);
        table.initTableColum(3, 350, new ConfigurationTableRenderer(), null);
        table.initTableColum(2, 20, new JRadioButtonRenderer(), new JRadioButtonEditor());
        table.initTableColum(4, 20, new JRadioButtonRenderer(), new JRadioButtonEditor());
        table.setAutoCreateRowSorter(true); //make the table sortable
        table.getRowSorter().toggleSortOrder(0); //set default sort to column 0
        JScrollPane pane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        Container con = this.frame.getContentPane();
        con.setLayout(new BorderLayout());
        con.add(panelNorth, BorderLayout.NORTH);
        con.add(pane, BorderLayout.CENTER);

        // MENU
        JMenuItem itemDefault = new JMenuItem("Load default file...");
        itemDefault.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    File file = TPS_ParameterFileConverter.chooseFile(new File("T:/TAPAS"), "Load default file",
                            JFileChooser.FILES_ONLY);
                    load(file, defaultMap, 1, false);
                    setFilename(file, fieldDefault, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "Loading default file failed!",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JMenuItem itemDefaultWithParents = new JMenuItem("Load default file with parents...");
        itemDefaultWithParents.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    File file = TPS_ParameterFileConverter.chooseFile(new File("T:/TAPAS"), "Load default file",
                            JFileChooser.FILES_ONLY);
                    load(file, defaultMap, 1, true);
                    setFilename(file, fieldDefault, false);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "Loading default file failed!",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JMenuItem itemConfig = new JMenuItem("Load config file...");
        itemConfig.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    File file = TPS_ParameterFileConverter.chooseFile(new File("T:/TAPAS"), "Load config file",
                            JFileChooser.FILES_ONLY);
                    load(file, configMap, 3, false);
                    setFilename(file, fieldConfig, true);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "Loading config file failed!",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JMenuItem itemConfigWithParents = new JMenuItem("Load config file with parents...");
        itemConfigWithParents.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    File file = TPS_ParameterFileConverter.chooseFile(new File("T:/TAPAS"), "Load config file",
                            JFileChooser.FILES_ONLY);
                    load(file, configMap, 3, false);
                    setFilename(file, fieldConfig, true);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "Loading config file failed!",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JMenuItem itemConfigFromDatabase = new JMenuItem("Load config file from database...");
        itemConfigFromDatabase.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                loadConfigFromDatabase();
            }
        });

        JMenuItem itemSaveInDB = new JMenuItem("Save config file in database...");
        itemSaveInDB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveConfigInDatabase();
            }
        });

        JMenuItem itemSave = new JMenuItem("Save config file");
        itemSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//				parameterClass.clear();
                updateConfigParamsInEnum();
                File file = null;

                // Wenn eines der beiden Textfelder leer ist (normalerweise sind dann beide leer), wurde die Config aus der Datenbank geladen
                // Der Nutzer muss nun einen Speicherort wählen
                if (fieldPath.getText().length() > 0 && fieldConfig.getText().length() > 0) {
                    File folder = new File(fieldPath.getText(),
                            fieldConfig.getText().replace(".xls", "").replace(".csv", ""));
                    if (!folder.exists()) folder.mkdir();
                    file = new File(folder, fieldConfig.getText());
                } else {
                    file = TPS_ParameterFileConverter.chooseFileToSave(
                            new File(fieldPath.getText(), fieldConfig.getText()), "Save config file",
                            JFileChooser.FILES_ONLY, new ExtensionFilter("CSV-File", ".csv"),
                            new ExtensionFilter("Excel-File", ".xls"));
                }

                if (file != null) {
                    fieldPath.setText(file.getParent());
                    fieldConfig.setText(file.getName());

                    // Prüfe ob es in eine Excel- oder CSV-Datei geschrieben werden soll
                    if (file.getAbsolutePath().endsWith(".xls")) {
                        String parent = "./" + fieldDefault.getText();
                        TPS_ParameterXLSDAO converter = new TPS_ParameterXLSDAO();
                        converter.setFile(file);
                        converter.addAdditionalParameter(ParamString.FILE_PARENT_PROPERTIES.name(), parent,
                                "link to default file");
                        converter.writeParameter(parameterClass);
                    } else {
                        String parent = "./" + fieldDefault.getText();
                        TPS_ParameterCSVDAO converter = new TPS_ParameterCSVDAO();
                        converter.setFile(file);
                        converter.addAdditionalParameter(ParamString.FILE_PARENT_PROPERTIES.name(), parent,
                                "link to default file");
                        converter.writeParameter(parameterClass);
                    }
                }
            }
        });
        JMenuItem itemFullSave = new JMenuItem("Save config file without dependencies");
        itemFullSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//				parameterClass.clear();
                Object value = null;
                for (int i = 0; i < table.getRowCount(); i++) {
                    value = table.getValueAt(i, 3);
                    if (value != null && value.toString().length() > 0) parameterClass.setValue(
                            table.getValueAt(i, 0).toString(), value.toString());
                    else {
                        value = table.getValueAt(i, 1);
                        if (value != null && value.toString().length() > 0) parameterClass.setValue(
                                table.getValueAt(i, 0).toString(), value.toString());

                    }
                }
                File file = null;
                if (fieldPath.getText().length() > 0 && fieldConfig.getText().length() > 0) {
                    File folder = new File(fieldPath.getText(),
                            fieldConfig.getText().replace(".xls", "").replace(".csv", ""));
                    if (!folder.exists()) folder.mkdir();
                    file = new File(folder, fieldConfig.getText());
                } else {
                    // Wenn eines der beiden Textfelder leer ist (normalerweise sind dann beide leer), wurde die Config aus der Datenbank geladen
                    // Der Nutzer muss nun einen Speicherort wählen
                    file = TPS_ParameterFileConverter.chooseFileToSave(new File("T:/TAPAS"), "Save config file",
                            JFileChooser.FILES_ONLY, new ExtensionFilter("CSV-File", ".csv"),
                            new ExtensionFilter("Excel-File", ".xls"));
                    if (file != null) {
                        fieldPath.setText(file.getParent());
                        fieldConfig.setText(file.getName());
                    }
                }
                if (file != null) {
                    if (file.getAbsolutePath().endsWith(".xls")) {
                        TPS_ParameterXLSDAO converter = new TPS_ParameterXLSDAO();
                        converter.setFile(file);
                        converter.writeParameter(parameterClass);
                    } else {
                        TPS_ParameterCSVDAO converter = new TPS_ParameterCSVDAO();
                        converter.setFile(file);
                        converter.writeParameter(parameterClass);
                    }
                }
            }
        });

        JMenu menu = new JMenu("File");
        menu.add(itemDefault);
        menu.add(itemDefaultWithParents);
        menu.add(itemConfig);
        menu.add(itemConfigWithParents);
        menu.add(itemConfigFromDatabase);
        menu.add(new JSeparator());
        menu.add(itemSave);
        menu.add(itemFullSave);
        menu.add(itemSaveInDB);
        JMenuBar bar = new JMenuBar();
        bar.add(menu);
        this.frame.setJMenuBar(bar);
    }

    private int insert(String key, ParamType type, int row) {
        if (type.equals(ParamType.EXEC) || type.equals(ParamType.TMP)) return row;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Object[] entry = new Object[header.length];
        entry[0] = key;
        entry[1] = "";
        entry[2] = false;
        entry[3] = "";
        entry[4] = false;
        model.addRow(entry);
        return row + 1;
    }

    private boolean isSimulationStartedAlready(Connection con) throws SQLException {
        Statement statement = con.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT sim_started, sim_finished FROM " +
                this.parameterClass.getString(ParamString.DB_TABLE_SIMULATIONS) + " WHERE sim_key = '" + mSimKey + "'");
        while (resultSet.next()) {
            // iteriere über alle Simulationen mit diesem simKey. Im Regelfall
            // ist dies nur eine Zeile.
            boolean simStarted = resultSet.getBoolean("sim_started");
            boolean simFinished = resultSet.getBoolean("sim_finished");
            // Sobald die Simulation bereits gestartet oder beendet ist, wird
            // true zurückgegeben
            if (simStarted || simFinished) {
                resultSet.close();
                return true;
            }
        }
        resultSet.close();
        return false;
    }

    private void load(File file, SortedMap<String, String> map, int column, boolean loadParents) throws IOException {
        if (file.getName().endsWith(".txt")) {
            TPS_ParameterPropertiesDAO converter = new TPS_ParameterPropertiesDAO();
            map.clear();
//			this.parameterClass.clear();
            converter.setFile(file);
            converter.readParameter(this.parameterClass);
            fill(map);
            updateTable(map, column);
        } else if (file.getName().endsWith(".csv")) {
            map.clear();
//			this.parameterClass.clear();
            if (loadParents) this.parameterClass.loadRuntimeParameters(file);
            else this.parameterClass.loadSingleParameterFile(file);
            fill(map);
            updateTable(map, column);
        } else {
            throw new IOException("File type incorrect - *.csv expected -> " + file);
        }
    }

    private void loadConfigFromDatabase() {

        Connection con = null;
        Statement statement = null;
        try {
            TPS_DB_Connector db = new TPS_DB_Connector(this.parameterClass);
            con = db.getConnection(this);

            ResultSet resultSet;
            String simKey = chooseSimulation(con);
            if (simKey != null) {
                mSimKey = simKey;

                // anhand des selektierten sim_keys die Config laden und in TPS_Parameters einlesen
                statement = con.createStatement();
                resultSet = statement.executeQuery(
                        "SELECT * FROM " + this.parameterClass.getString(ParamString.DB_TABLE_SIMULATION_PARAMETERS) +
                                " WHERE sim_key = '" + mSimKey + "' order by param_key");
                configMap.clear();
                this.parameterClass.setString(ParamString.RUN_IDENTIFIER, simKey);
                this.parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tapasNetworkDirectory.getPath());
                //TPS_Parameters.readRuntimeParametersFromDB(resultSet);
                String key, value;
                while (resultSet.next()) {
                    key = resultSet.getString(2);
                    value = resultSet.getString(3);
                    configMap.put(key, value);
                }
                resultSet.close();
                generateTemporaryParametersInMap(configMap);
                updateTable(configMap, 3);
                SortedMap<String, String> defaults = getDefaultParams();
                updateTable(defaults, 1);

            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Loading config file failed!",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            if (con != null) try {
                con.close();
            } catch (SQLException e) {
            }

            if (statement != null) try {
                statement.close();
            } catch (SQLException e) {
            }
        }
    }

    private void readClientProperties() {
        mClientControlProperties = new ClientControlProperties(new File("converter.properties"));
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            tapasNetworkDirectory = new File(mClientControlProperties.get(ClientControlPropKey.TAPAS_DIR_WIN));
        } else {
            tapasNetworkDirectory = new File(mClientControlProperties.get(ClientControlPropKey.TAPAS_DIR_LINUX));
        }
        this.parameterClass.setString(ParamString.FILE_WORKING_DIRECTORY, tapasNetworkDirectory.getPath());

        if (!parameterClass.isDefined(ParamString.DB_USER) || !parameterClass.isDefined(ParamString.DB_PASSWORD)) {
            String loginFile = "runtime.csv";

            if (!this.mClientControlProperties.get(ClientControlPropKey.LOGIN_CONFIG).isEmpty()) {
                loginFile = this.mClientControlProperties.get(ClientControlPropKey.LOGIN_CONFIG);
            } else {
                this.mClientControlProperties.set(ClientControlPropKey.LOGIN_CONFIG, loginFile);
            }

            File runtimeFile = new File(new File(tapasNetworkDirectory, this.parameterClass.SIM_DIR), loginFile);
            try {
                this.parameterClass.loadRuntimeParameters(runtimeFile);

            } catch (Exception e) {
                System.err.println("Exception thrown during reading " + loginFile);
                e.printStackTrace();
            }
        }
    }

    private void revertTemporaryParametersInMap(Map<String, String> configMap2) {
        String tmp, suffix="_trips";
        configMap2.remove("DB_TABLE_LOCATION_TMP");
        configMap2.remove("DB_TABLE_HOUSEHOLD_TMP");
        tmp = configMap2.get("DB_TABLE_TRIPS");
        tmp = tmp.substring(0, tmp.lastIndexOf(suffix)+suffix.length()); // forget everything after the suffix
        configMap2.put("DB_TABLE_TRIPS", tmp);
    }

    private void saveConfigInDatabase() {

        Connection con = null;
        try {
            TPS_DB_Connector db = new TPS_DB_Connector(this.parameterClass);
            con = db.getConnection(this);
            updateConfigParamsInEnum();
            //TPS_Parameters.revertTemporaryParameters();
            //SortedMap<String, String> params = getConfigParams();
            revertTemporaryParametersInMap(this.configMap);
            if (isSimulationStartedAlready(con)) {
                String simKey = getNewSimKey();

                this.parameterClass.writeToDB(con, simKey, this.configMap);
                mSimKey = simKey;
            } else {
                this.parameterClass.updateInDB(con, mSimKey, this.configMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Saving config file failed!",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            if (con != null) try {
                con.close();
            } catch (SQLException e) {
            }

        }
    }

    private void setFilename(File file, JTextField field, boolean xls) {
        String name = file.getName();
        if (xls) {
            name = name.substring(0, name.length() - 4) + ".xls";
        } else {
            fieldPath.setText(file.getParent());
        }
        field.setText(name);
    }

    public void show() {
        this.frame.setVisible(true);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Zeigt ein Fenster zum Anzeigen der Config für den übergebenen Simulationskey. Zurückgegeben wird der Simulationskey, der zuletzt bearbeitet wurde, so dass diese Config in dem Parent-Frame
     * angezeigt werden kann. Die Änderungen die an der Config vorgenommen wurden sind in den Enums {@link ParamFlag}, {@link ParamValue} und {@link ParamString} gespeichert. Sollte ein Simulationskey
     * zurückgegeben werden, der vorher noch nicht vorhanden war, sollte daraus eine neue Simulation gestartet werden.
     *
     * @param parent
     * @param simKey
     * @return
     */
    public String showModal(final JFrame parent, String simKey) {
        this.mSimKey = simKey;
        if (parent != null) {
            parent.setVisible(false);
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    synchronized (parent) {
                        parent.notifyAll();
                    }
                }
            });
            loadConfigFromDatabase();
            frame.setVisible(true);
            try {
                synchronized (parent) {
                    parent.wait();
                }
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            parent.setVisible(true);
        }
        return mSimKey;
    }

    private void updateConfigParamsInEnum() {
        Object value = null;
        String key;
        for (int i = 0; i < table.getRowCount(); i++) {
            value = table.getValueAt(i, 3);
            key = table.getValueAt(i, 0).toString();
            if (value != null && value.toString().length() > 0) this.configMap.put(key, value.toString());
        }
    }

    private void updateTable(Map<String, String> map, int column) {
        for (int i = 0; i < table.getRowCount(); i++) {
            Object key = table.getValueAt(i, 0);
            table.setValueAt(map.getOrDefault(key, ""), i, column);
        }
    }

    private class ConfigurationTableRenderer extends DefaultTableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -5820831318161515261L;

        Color red = new Color(255, 200, 200);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            if (table.isCellSelected(row, column)) return label;

            if (column == 0) {
                label.setFont(table.getFont().deriveFont(Font.BOLD));
                label.setBackground(Color.WHITE);
            } else if (column == 1 || column == 3) {
                if (value == null || value.toString().length() == 0) {
                    label.setFont(table.getFont());
                    Object text = table.getValueAt(row, (column + 2) % 4);
                    if (text == null || text.toString().length() == 0) {
                        label.setBackground(red);
                    } else {
                        label.setBackground(Color.WHITE);
                    }
                } else {
                    if (Boolean.TRUE.equals(table.getValueAt(row, column + 1))) {
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    } else {
                        label.setFont(table.getFont());
                    }
                    label.setBackground(Color.WHITE);
                }
            }
            return label;
        }
    }

    private class ConfigurationTable extends JTable {
        /**
         *
         */
        private static final long serialVersionUID = -4259885616852930910L;

        public ConfigurationTable(TableModel tableModel) {
            super(tableModel);
        }

        /**
         * This method initialises the column by index with a width and a renderer and an editor.
         *
         * @param column   column index
         * @param width    column's width, only has effect when value is greater 0
         * @param renderer column's renderer, can be null
         * @param editor   column's editor, can be null
         */
        private void initTableColum(int column, int width, TableCellRenderer renderer, TableCellEditor editor) {
            TableColumn tc = table.getColumnModel().getColumn(column);
            if (width > 0) {
                tc.setMaxWidth(width);
                tc.setMinWidth(width);
            }
            if (renderer != null) tc.setCellRenderer(renderer);
            if (editor != null) tc.setCellEditor(editor);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column < 2) return false;
            return super.isCellEditable(row, column);
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            super.setValueAt(aValue, row, column);
            if ((column == 2 || column == 4) && Boolean.TRUE.equals(aValue)) {
                column = (column % 4) + 2;
                setValueAt(false, row, column);
            } else if ((column == 1 || column == 3)) {
                setValueAt(aValue != null && aValue.toString().length() != 0, row, column + 1);
            }
            this.repaint();
        }
    }

    private class FileTargetListener extends DropTargetAdapter {

        @SuppressWarnings("unchecked")
        public void drop(DropTargetDropEvent dtde) {
            Transferable tr = dtde.getTransferable();
            if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                try {
                    Point p = dtde.getLocation();
                    JTable table = (JTable) ((DropTarget) dtde.getSource()).getComponent();
                    int column = table.columnAtPoint(p);
                    List<File> files = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                    File file = files.get(0);
                    if (column == 1) {
                        load(file, defaultMap, column, false);
                        setFilename(file, fieldDefault, false);
                    } else if (column == 3) {
                        load(file, configMap, column, false);
                        setFilename(file, fieldConfig, true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "Error importing file",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private class JRadioButtonEditor extends AbstractCellEditor implements TableCellEditor {
        /**
         * serial UID
         */
        private static final long serialVersionUID = 7571850003681799808L;

        /**
         * button inside the cell
         */
        private final JRadioButton button;

        /**
         * The constructor builds the button and adds the {@link ActionListener} which changes the state of the
         * simulation
         */
        public JRadioButtonEditor() {
            this.button = new JRadioButton();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.CellEditor#getCellEditorValue()
         */
        public Object getCellEditorValue() {
            return button.isSelected();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object,
         * boolean, int, int)
         */
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.button.setSelected(Boolean.parseBoolean(value.toString()));
            return button;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.AbstractCellEditor#isCellEditable(java.util.EventObject)
         */
        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                return ((MouseEvent) anEvent).getClickCount() > 0;
            }
            return false;
        }
    }

    private class JRadioButtonRenderer implements TableCellRenderer {

        private final JRadioButton button;

        public JRadioButtonRenderer() {
            this.button = new JRadioButton();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table.isCellSelected(row, column)) {
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setBackground(Color.WHITE);
            }
            this.button.setSelected((Boolean) value);
            return button;
        }
    }
}
