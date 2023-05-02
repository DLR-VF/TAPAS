/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.dbImport;

import de.dlr.ivf.tapas.model.parameter.ParamString;
import de.dlr.ivf.tapas.model.parameter.ParamValue;
import de.dlr.ivf.tapas.model.parameter.TPS_ParameterClass;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import static de.dlr.ivf.tapas.tools.dbImport.DBImportController.DB_2_FILE_MAP;
import static de.dlr.ivf.tapas.tools.dbImport.DBImportTableModel.*;

public class DBImportGUI {

    public static final String ABS_PATH = "T:/TAPAS/Inputdateien/";

    private static final boolean DEBUG = false;
    JTextField lblHost;
    JTextField lblPort;
    JTextField lblUser;
    JPasswordField lblPassword;
    JTextField lblDatabase;
    private final TPS_ParameterClass parameterClass;
    private File configFile;
    private JFrame control;
    private DBImportController core;
    private JTable fileTable;
    private DBImportTableModel model;
    private JTextField regionField;
    private JTextField schemaField;
    private JButton btnStart;
    private JTextArea textPane;
    private Cursor cursor;

    DBImportGUI() {
        this.parameterClass = new TPS_ParameterClass();
    }

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        DBImportGUI control = new DBImportGUI();
        control.createAndShowGui();
        File runtimeFile = new File("T:/TAPAS/Simulationen/runtime.csv");
        if (!runtimeFile.exists()) runtimeFile = null;
        control.loadConfig(runtimeFile);
    }

    public void activateStart() {
        this.btnStart.setEnabled(true);
    }

    private File chooseConfigFile(String title, JFrame f) {
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fd.setSelectedFile(new File(ABS_PATH));
        fd.setVisible(true);

        fd.setDialogTitle(title);
        int value = fd.showOpenDialog(f);

        if ((value == JFileChooser.APPROVE_OPTION) && fd.getSelectedFile() != null && fd.getSelectedFile().isFile() &&
                fd.getSelectedFile().exists()) {
            return fd.getSelectedFile();
        } else if (value != JFileChooser.CANCEL_OPTION) {
            JOptionPane.showMessageDialog(f, "Invalid Config File: ", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void createAndShowGui() {
        this.core = new DBImportController(this);
        this.control = createGui();
        this.control.setIconImage(
                new ImageIcon(getClass().getClassLoader().getResource("icons/TAPAS-Logo.gif")).getImage());

        JPanel regionPanel = new JPanel(new GridLayout(2, 8, 5, 5));
        regionPanel.setBorder(BorderFactory.createTitledBorder("Database Configuration"));
        regionPanel.add(new JLabel("host"));
        lblHost = new JTextField();
        lblHost.setEditable(false);
        lblHost.setToolTipText("IP-Adress/URL for Database Connection");
        regionPanel.add(lblHost);
        regionPanel.add(new JLabel("port"));
        lblPort = new JTextField();
        lblPort.setEditable(false);
        lblPort.setToolTipText("Port for Database Connection");
        regionPanel.add(lblPort);
        regionPanel.add(new JLabel("user"));
        lblUser = new JTextField();
        lblUser.setEditable(false);
        lblUser.setToolTipText("Database Login Name");
        regionPanel.add(lblUser);
        regionPanel.add(new JLabel("password"));
        lblPassword = new JPasswordField();
        lblPassword.setEditable(false);
        lblPassword.setToolTipText("Password for Database");
        regionPanel.add(lblPassword);
        regionPanel.add(new JLabel("database"));
        lblDatabase = new JTextField();
        lblDatabase.setEditable(false);
        lblDatabase.setToolTipText("Database Name");
        regionPanel.add(lblDatabase);
        regionPanel.add(new JLabel("schema"));
        this.schemaField = new JTextField(10);
        this.schemaField.setToolTipText("Set tht Scheme Name");
        regionPanel.add(this.schemaField);
        regionPanel.add(new JLabel("region"));
        this.regionField = new JTextField(15);
        this.regionField.setToolTipText("Set the Region Name, relevant for Table Names");
        regionPanel.add(this.regionField);
        regionPanel.add(new JLabel());
        JButton btnLoadConfig = new JButton("Load DB Config");
        btnLoadConfig.setToolTipText("Load Database Connection Configuration File");
        regionPanel.add(btnLoadConfig);

        this.schemaField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                this.checkTable(e);
            }

            private void checkTable(DocumentEvent e) {
                try {
                    model.checkSchemaNames(e.getDocument().getText(0, e.getDocument().getLength()));
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }

            public void insertUpdate(DocumentEvent e) {
                this.checkTable(e);
            }

            public void removeUpdate(DocumentEvent e) {
                this.checkTable(e);
            }

        });

        this.regionField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                this.checkTable(e);
            }

            private void checkTable(DocumentEvent e) {
                try {
                    model.checkTableNames(e.getDocument().getText(0, e.getDocument().getLength()));
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }

            public void insertUpdate(DocumentEvent e) {
                this.checkTable(e);
            }

            public void removeUpdate(DocumentEvent e) {
                this.checkTable(e);
            }

        });

        JButton btnDefine = new JButton("Define");
        btnDefine.setMnemonic('d');
        btnDefine.setToolTipText("Load Table Names from Config File");
        JButton btnLoad = new JButton("Load Scn");
        btnLoad.setMnemonic('l');
        btnLoad.setToolTipText("Load Import Information from Config File");
        JButton btnClear = new JButton("Clear");
        btnClear.setMnemonic('c');
        btnClear.setToolTipText("Clear Table Content");
        btnStart = new JButton("Start");
        btnStart.setMnemonic('s');
        btnStart.setToolTipText("Begin with Import as defined in Table");
//		btnStart.setToolTipText("Starts importing all applied information from the table below");
        JButton btnApply = new JButton("Apply for all"); // use prefixes
        btnApply.setToolTipText("Apply Selection to All");
        final JCheckBox boxApply = new JCheckBox();
        boxApply.setToolTipText("Selection to be Applied to All Entries");

        JPanel btnPanel1 = new JPanel();
        btnPanel1.setLayout(new FlowLayout(FlowLayout.LEFT));
        btnPanel1.add(btnDefine);
        btnPanel1.add(btnLoad);
        btnPanel1.add(btnClear);
        btnPanel1.add(btnApply);
        btnPanel1.add(boxApply);

        JPanel btnPanel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel2.add(btnStart);

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BorderLayout());
        btnPanel.add(btnPanel2, BorderLayout.EAST);
        btnPanel.add(btnPanel1, BorderLayout.WEST);

        this.fileTable = getFileTable();
        JScrollPane scrollPane = new JScrollPane(this.fileTable);

        JPanel up = new JPanel();
        up.setLayout(new BorderLayout());
        up.setBorder(BorderFactory.createTitledBorder("Database Import"));
        up.add(scrollPane, BorderLayout.CENTER);
        up.add(btnPanel, BorderLayout.NORTH);

        JPanel down = new JPanel();
        down.setLayout(new BorderLayout());
        down.setBorder(BorderFactory.createTitledBorder("Information"));
        this.textPane = new JTextArea();
        this.textPane.setEditable(false);
        JScrollPane textScrollPane = new JScrollPane(this.textPane);
        down.add(textScrollPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, up, down);
        splitPane.setOpaque(true);

        control.getContentPane().setLayout(new BorderLayout());
        control.getContentPane().add(regionPanel, BorderLayout.NORTH);
        control.getContentPane().add(splitPane, BorderLayout.CENTER);
        control.setVisible(true);

        splitPane.setDividerLocation(0.5);

        btnLoadConfig.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadConfig(chooseConfigFile("Choose database config file", control));
            }
        });

        btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    btnStart.setEnabled(false);
                    core.dbimport(fileTable);
                    if (DEBUG) print("Successfully imported all tables");
                } catch (Exception ex) {
                    DBImportGUI.this.handleException("Import", ex);
                }
                control.repaint();
            }
        });

        btnClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    model.clear();
//					parameterClass.clear();
                    regionField.setText("");
                    regionField.setEditable(true);
                    schemaField.setText("");
                    schemaField.setEditable(true);
                    lblDatabase.setText("");
                    lblHost.setText("");
                    lblPassword.setText("");
                    lblPort.setText("");
                    lblUser.setText("");

                    if (DEBUG) print("Successfully cleared");
                } catch (Exception ex) {
                    DBImportGUI.this.handleException("Clear", ex);
                }
                control.repaint();
            }
        });

        btnDefine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configFile = chooseConfigFile("Choose run config file", control);
                define(configFile);
            }
        });

        btnLoad.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configFile = chooseConfigFile("Choose run config file", control);
                load(configFile);
            }
        });

        btnApply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // -1 ist wichtig, da man sonst in eine endlosschelife läuft. Die Letzte Zeile der tabelle ist immer in
                    // Bearbeitung und somit gilt sie nicht für den apply all fall.
                    for (int i = 0; i < fileTable.getRowCount() - 1; i++) {
                        fileTable.setValueAt(boxApply.isSelected(), i, COL_ACC);
                    }

                } catch (Exception e1) {
                    print("Selection Exception");
                    e1.printStackTrace();
                }
                control.repaint();
            }
        });
    }

    JFrame createGui() {
        final JFrame jFrame = new JFrame();
        jFrame.setSize(new Dimension(1200, 800));
        jFrame.setTitle("TAPAS Database Importer");
        jFrame.setContentPane(new JPanel());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return jFrame;
    }

    private void define(File file) {
        if (file == null) return;

        this.loadConfig(file);

        try {
            this.parameterClass.loadRuntimeParameters(file);

            regionField.setText(this.parameterClass.getString(ParamString.DB_REGION));
            regionField.setEditable(false);
            schemaField.setText(this.parameterClass.getString(ParamString.DB_SCHEMA_CORE).substring(0,
                    this.parameterClass.getString(ParamString.DB_SCHEMA_CORE).length() - 1));
            schemaField.setEditable(false);
        } catch (Exception ex) {
            handleException("Read Config File", ex);
        }
        control.repaint();
    }

    private JTable getFileTable() {
        final JTable jTable = new JTable();
        this.model = new DBImportTableModel(jTable, this.getParameters());
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        jTable.setPreferredScrollableViewportSize(new Dimension(800, 400));
        jTable.getColumnModel().getColumn(COL_ACC).setPreferredWidth(20);
        jTable.getColumnModel().getColumn(COL_ACC).setMaxWidth(20);
        jTable.getColumnModel().getColumn(COL_ACC).setMinWidth(20);
        jTable.getColumnModel().getColumn(COL_ACC).setResizable(false);
        return jTable;

    }

    /**
     * returns parameter class reference
     *
     * @return parameter class reference
     */
    TPS_ParameterClass getParameters() {
        return this.parameterClass;
    }

    String getRegion() {
        return regionField.getText();
    }

    String getSchema() {
        return this.schemaField.getText();
    }

    public void handleException(String job, Exception ex) {
        JOptionPane.showMessageDialog(this.control,
                "Catched " + ex.getClass().getSimpleName() + " during job:" + job + " with message: " + ex.getMessage(),
                "Catched " + ex.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
        print("Catched " + ex.getClass().getSimpleName());
        ex.printStackTrace();
    }

    private void load(File file) {
        if (file == null) return;

        this.define(file);

        try {

            Object[] data = new Object[LENGTH];
            for (ParamString dbKey : DB_2_FILE_MAP.keySet()) {
                List<ParamString> fileKeyList = DB_2_FILE_MAP.get(dbKey);
                for (ParamString fileKey : fileKeyList) {
                    data[COL_T_KEY] = dbKey.name();
                    data[COL_T_NAME] = this.parameterClass.getString(dbKey);
                    data[COL_F_KEY] = fileKey.name();
                    if (this.parameterClass.isDefined(fileKey)) {
                        String s = this.parameterClass.getString(fileKey);
                        data[COL_F_NAME] = s;
                        if (DBImportController.DB_IDENTIFIABLE_TABLES.contains(dbKey)) data[COL_ID] = s.substring(
                                s.lastIndexOf('/') + 1, s.lastIndexOf('.'));
                        else data[COL_ID] = "";
                        data[COL_ACC] = Boolean.TRUE;
                    } else {
                        data[COL_F_NAME] = "";
                        data[COL_ID] = "";
                        data[COL_ACC] = Boolean.FALSE;
                    }
                    model.addFinalRow(data);
                }
            }
        } catch (Exception ex) {
            while (fileTable.getRowCount() > 0) {
                ((DefaultTableModel) fileTable.getModel()).removeRow(0);
            }
            handleException("Read Run File", ex);
        }
        control.repaint();
    }

    private void loadConfig(File file) {
        if (file == null) {
            file = chooseConfigFile("Choose Runtime csv-file", control);
        }

        try {
            this.parameterClass.loadSingleParameterFile(file);

            lblDatabase.setText(this.parameterClass.getString(ParamString.DB_DBNAME));
            lblHost.setText(this.parameterClass.getString(ParamString.DB_HOST));
            lblPassword.setText(this.parameterClass.getString(ParamString.DB_PASSWORD));
            lblPort.setText(this.parameterClass.getIntValue(ParamValue.DB_PORT) + "");
            lblUser.setText(this.parameterClass.getString(ParamString.DB_USER));

            print("Successfully loaded: " + file.getAbsolutePath());
        } catch (Exception ex) {
            handleException("Read Config File", ex);
        }
        control.repaint();
    }

    public void print(String message) {
        int start = this.textPane.getDocument().getLength() + 2;
        this.textPane.append("\n" + message);
        this.textPane.select(start, start + 1);
    }

    public void repaint() {
        this.control.repaint();
    }

    public void resetCursor() {
        this.control.setCursor(cursor);
    }

    public void setCursor(Cursor cursor) {
        this.cursor = this.control.getCursor();
        this.control.setCursor(cursor);
    }
}
