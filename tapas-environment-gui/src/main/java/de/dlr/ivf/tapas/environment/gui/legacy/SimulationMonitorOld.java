/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.dlr.ivf.tapas.environment.gui.legacy;
import de.dlr.ivf.tapas.environment.gui.legacy.matrixmap.MatrixMapHandler;
import de.dlr.ivf.tapas.environment.gui.legacy.parametercomparator.SimParamComparatorController;
import de.dlr.ivf.tapas.environment.gui.legacy.tum.ITumInterface;
import de.dlr.ivf.tapas.environment.gui.legacy.tum.IntegratedTUM;
import de.dlr.ivf.tapas.environment.gui.legacy.util.MultilanguageSupport;
import de.dlr.ivf.tapas.environment.gui.legacy.util.table.ConfigurableJTable;
import de.dlr.ivf.tapas.environment.gui.legacy.util.table.ConfigurableJTable.ClassifiedTableModel;
import de.dlr.ivf.tapas.environment.gui.legacy.util.table.LongTextRenderer;
import de.dlr.ivf.tapas.environment.gui.legacy.util.table.TextPopupEditor;
import de.dlr.ivf.tapas.environment.model.SimulationData;
import de.dlr.ivf.tapas.environment.model.SimulationServerData;
import de.dlr.ivf.tapas.environment.model.SimulationServerData.ServerInfo;
import de.dlr.ivf.tapas.environment.model.SimulationServerData.HashStatus;
import de.dlr.ivf.tapas.tools.fileModifier.ExpertKnowledgeHandler;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * GUI of the simulation client.
 *
 * @author mark_ma
 */

public class SimulationMonitorOld implements TableModelListener {

    private static final int SERVER_INDEX_CORES = 5;
    private static final int SERVER_INDEX_CPU = 4;
    private static final int SERVER_INDEX_IP = 1;
    private static final int SERVER_INDEX_NAME = 0;
    private static final int SERVER_INDEX_ONLINE = 3;
    private static final int SERVER_INDEX_TIMESTAMP = 6;
    private static final int SERVER_INDEX_HASH = 2;
    /**
     * This column will be invisible
     */
    private static final int SERVER_INDEX_SHUTDOWN = 7;
    private static final int SERVER_INDICES = 8;
    /**
     * labels inside right server panel
     */
    final JLabel lblIdentifier = new JLabel();
    final JLabel lblRunSince = new JLabel();
    final JLabel lblPid = new JLabel();
    final JLabel lblWork = new JLabel();
    final JLabel lblRemote = new JLabel();
    /**
     * Button to insert a new simulation into the database
     */
    final JButton insertButton;
    final JButton bTUM, bCompare;
    final JButton bAnalyzer;
    final JButton bParameters;
    final JButton bStart;
    final JButton bPause;
    final JButton bDelete;
    final JButton bExport;
    final JButton bDeletesrv;
    final JButton bTerminatesrv;
    final JButton bStartsrv;
    final JPanel progresspanel = new JPanel(new BorderLayout());
    final JProgressBar trippb = new JProgressBar(0, 100);
    final JProgressBar regionpb = new JProgressBar(0, 100);
    private final JTextPane cons = new JTextPane();
    private final StyledDocument console = cons.getStyledDocument();
    /**
     * Field with the current run properties filename
     */
    private final JTextField runFilenameField;
    private final ServerColorRenderer blinkCellRenderer;
    /**
     * Toolbar -> Tools
     */
    JMenuItem analyzer;
    JMenuItem expertKnowHandler;
    JMenuItem matrixHandler;
    private final JPanel tuminfo = new JPanel(new BorderLayout());
    private File[] exportfiles;
    private IntegratedTUM itum = null;
    /**
     * right side next to the servers table
     */
    private final JPanel serverPanelRight;
    /**
     * Reference to the control instance of this client
     */
    private final SimulationControl control;
    /**
     * The GUI frame
     */
    private final JFrame frame;
    /**
     * Table with all server information
     */
    private final ConfigurableJTable serverTable;
    /**
     * Table with all simulation information
     */
    private ConfigurableJTable simTable;
    private final Set<String> workingSimKeys = Collections.synchronizedSet(new HashSet<>());

    /**
     * The constructor builds up the whole GUI
     *
     * @param control Reference to the control instance of this client
     */
    public SimulationMonitorOld(SimulationControl control) {

        this.control = control;
        this.frame = new JFrame();
//        this.frame.setTitle("TAPAS Simulation Control - Version: | Build: " +
//                control.getBuildnumber() + " | Build-date: " + control.getBuilddate());



        URL url = SimulationMonitorOld.class.getResource("/icons/TAPAS-Logo.gif");
        this.frame.setIconImage(
                new ImageIcon(url).getImage());
        // setup menu bar
        JMenu languageMenu = MultilanguageSupport.init(SimulationMonitorOld.class);
        JMenu tools = new JMenu(MultilanguageSupport.getString("TOOLS_MENU"));
        analyzer = new JMenuItem(MultilanguageSupport.getString("TOOLS_ANALYZER"));
        expertKnowHandler = new JMenuItem(MultilanguageSupport.getString("TOOLS_EXPERT_KNOWLEDGE_HANDLER"));
        matrixHandler = new JMenuItem(MultilanguageSupport.getString("TOOLS_MATRIX_HANDLER"));
        tools.add(analyzer);
        tools.add(expertKnowHandler);
        tools.add(matrixHandler);

        expertKnowHandler.addActionListener(e -> {
            ExpertKnowledgeHandler controlGUI = new ExpertKnowledgeHandler();
            JFrame the_frame = controlGUI.createAndShowGUI();
            the_frame.setVisible(true);
        });

        analyzer.addActionListener(e -> startAnalyzer(simTable.getSelectedRow()));

        matrixHandler.addActionListener(e -> {
            MatrixMapHandler controlGUI = new MatrixMapHandler();
            JFrame the_frame = controlGUI.createAndShowGUI();
            the_frame.setVisible(true);
        });

        JMenuBar bar = new JMenuBar();
        bar.add(languageMenu);
        bar.add(tools);
        this.frame.setJMenuBar(bar);

        // initialise strings for the state checkbox
//        TPS_SimulationState.FINISHED.setAction(MultilanguageSupport.getString("state.finished"));
//        TPS_SimulationState.INSERTED.setAction(MultilanguageSupport.getString("state.inserted"));
//        TPS_SimulationState.STOPPED.setAction(MultilanguageSupport.getString("state.stopped"));
//        TPS_SimulationState.STARTED.setAction(MultilanguageSupport.getString("state.started"));

        // setup server table panel

        this.serverTable = new ConfigurableJTable(
                new ClassifiedTableModel(new Object[0][0], new Object[]{"", "", "", "", "", "", ""})) {
            /**
             *
             */
            private static final long serialVersionUID = -6480516599385197561L;

            //  Apply background to existing renderer
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                return super.prepareRenderer(renderer, row, column);
            }
        };

        TableCellRenderer delegate = new BorderedRenderer();
        blinkCellRenderer = new ServerColorRenderer(serverTable, delegate);

//        serverTable.getModel().addTableModelListener(e -> {
//            if (e.getColumn() == SERVER_INDEX_SHUTDOWN && serverTable.getModel().getValueAt(e.getFirstRow(),
//                    SERVER_INDEX_SHUTDOWN) == ServerControlState.STOP) blinkCellRenderer.addBlinkingCell(
//                    e.getFirstRow(), SERVER_INDEX_ONLINE);
//            else blinkCellRenderer.removeBlinkingCell(e.getFirstRow(), SERVER_INDEX_ONLINE);
//        });


//        serverTable.addMouseListener(MouseListenerFactory.onClick(e -> {
//            if (serverTable.getSelectedRowCount() == 1) {
//                //String hostname = (String) serverTable.getValueAt(serverTable.getSelectedRow(), SERVER_INDEX_NAME);
//
//            }
//        }));

        //root server panel
        JPanel serverPanel = new JPanel(new BorderLayout(10, 0));

        //this panel contains information about the currently running server JVM and a set of controls to control a selected server
        serverPanelRight = new JPanel();
        serverPanelRight.setLayout(new BoxLayout(serverPanelRight, BoxLayout.Y_AXIS));

        //define server control buttons
        bDeletesrv = new JButton(MultilanguageSupport.getString("SERVER_DELETE"));
        bStartsrv = new JButton(MultilanguageSupport.getString("SERVER_START"));
        bTerminatesrv = new JButton(MultilanguageSupport.getString("SERVER_SHUTDOWN"));

        bTerminatesrv.setBackground(Color.RED);
        bStartsrv.setBackground(Color.GREEN);
        //wrap server control buttons
        JPanel serverControlButtons = new JPanel();
        serverControlButtons.setLayout(new BoxLayout(serverControlButtons, BoxLayout.X_AXIS));
        serverControlButtons.add(bDeletesrv);
        serverControlButtons.add(bTerminatesrv);
        serverControlButtons.add(bStartsrv);

        //wrapper for the left side of labels ("headers")
        JPanel labelPane = new JPanel();
        labelPane.setLayout(new BoxLayout(labelPane, BoxLayout.Y_AXIS));

        //wrapper for right side of labels. These labels will be set whenever one clicks on a server
        JPanel resultPane = new JPanel();
        resultPane.setLayout(new BoxLayout(resultPane, BoxLayout.Y_AXIS));

        //wrapper for both panel with labels (left and right side)
        JPanel labelWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        //labelWrapper.setLayout(new BoxLayout(labelWrapper,BoxLayout.X_AXIS));

        //defining labeling for JVM information

        String identifier = MultilanguageSupport.getString("SERVER_CONTROL_IDENT") + " ";
        String runsince = MultilanguageSupport.getString("SERVER_CONTROL_RUNSINCE") + " ";
        String pid = MultilanguageSupport.getString("SERVER_CONTROL_PID") + " ";
        String workon = MultilanguageSupport.getString("SERVER_CONTROL_WORKON") + " ";
        String rmistatus = MultilanguageSupport.getString("SERVER_CONTROL_RMISTATUS") + " ";


        String[] labels = {identifier, runsince, pid, workon, rmistatus};

        for (String s : labels)
            labelPane.add(new JLabel(s));


        resultPane.add(this.lblIdentifier);
        resultPane.add(this.lblRunSince);
        resultPane.add(this.lblPid);
        resultPane.add(this.lblWork);
        resultPane.add(this.lblRemote);

        //wrap labels
        labelWrapper.add(labelPane);
        labelWrapper.add(resultPane);


        //wrap controls and JVM information labels
        serverPanelRight.add(serverControlButtons);
        serverPanelRight.add(labelWrapper);


        //add controls and labels to root server panel
        serverPanel.add(serverPanelRight, BorderLayout.LINE_END);

        //disable all {@link Component}s
        enableComponents(serverPanelRight, false);

        labelWrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));


        serverPanel.setBorder(BorderFactory.createTitledBorder(MultilanguageSupport.getString("SERVER_PANEL_TITLE")));

        serverPanel.add(new JScrollPane(serverTable), BorderLayout.CENTER);

        bDeletesrv.addActionListener(event -> {
            String ip = (String) serverTable.getModel().getValueAt(serverTable.getSelectedRow(), SERVER_INDEX_IP);
            String name = (String) serverTable.getModel().getValueAt(serverTable.getSelectedRow(), SERVER_INDEX_NAME);

            try {
                SimulationServerData serverData = new SimulationServerData(InetAddress.getByName(ip), name);
                removeServerData(serverData);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        bTerminatesrv.addActionListener(event -> {
            String host = (String) serverTable.getModel().getValueAt(serverTable.getSelectedRow(), SERVER_INDEX_NAME);
            this.control.shutServerDown(host);

        });
        bStartsrv.addActionListener(event -> {
            try {
                String host = (String) serverTable.getModel().getValueAt(serverTable.getSelectedRow(),
                        SERVER_INDEX_NAME);
                this.control.startServer(host);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // setup control panel
        JButton chooseButton = new JButton(MultilanguageSupport.getString("CHOOSE_SIMULATION_FILE_BUTTON"));
        chooseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File[] files = SimulationMonitorOld.this.control.chooseRunFile(frame);
                if (files != null) {
                    String fileList = "";
                    for (File singleFile : files) {
                        fileList = fileList.concat(singleFile.getAbsolutePath() + ";");
                    }
                    fileList = fileList.substring(0, fileList.length() - 1);
                    runFilenameField.setText(fileList);
                    // if (((TPS_ServerTableModel)
                    // serverTable.getModel()).areThreadsSelected() ||
                    // maximalButton.isSelected()) {
                    insertButton.setEnabled(true);
                    // }
                }
            }
        });
        this.runFilenameField = new JTextField();


        this.insertButton = new JButton(MultilanguageSupport.getString("ADD_SIMULATION_BUTTON"));
        this.insertButton.setEnabled(false);
        /*
         * (non-Javadoc)
         *
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.
         * ActionEvent)
         */
        this.insertButton.addActionListener(e -> {
//            SimulationMonitorOld.this.control.updateProperties();
            SimulationMonitorOld.this.insertAction();
        });

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(chooseButton, BorderLayout.WEST);
        controlPanel.add(runFilenameField, BorderLayout.CENTER);
        controlPanel.add(insertButton, BorderLayout.EAST);
        //controlPanel.add(statusPanel, BorderLayout.SOUTH);

        // setup simulation panel
        this.simTable = new ConfigurableJTable(
                new ClassifiedTableModel(new Object[0][0], new String[SIM_INDEX.values().length]) {
                    /**
                     * serial UID
                     */
                    private static final long serialVersionUID = 6881207952855577992L;

                    /*
                     * (non-Javadoc)
                     *
                     * @see de.dlr.de.dlr.ivf.client.runtime.tapas.ivf.SimulationMonitorOld.
                     * TPS_ItemTableModel#isCellEditable(int, int)
                     */
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return column == SIM_INDEX.ACTION.ordinal() || column == SIM_INDEX.DESCRIPTION.ordinal();
                    }

                    @Override
                    public void setValueAt(Object aValue, int row, int column) {
                        super.setValueAt(aValue, row, column);
                        if (column == SIM_INDEX.DESCRIPTION.ordinal()) {
                            String simkey = (String) getValueAt(row, SIM_INDEX.KEY.ordinal());
                            updateSimulationDescription((String) aValue, simkey);
                        }

                    }
                });

        this.simTable.addKeyListener(new KeyAdapter() {
            /*
             * (non-Javadoc)
             *
             * @see
             * java.awt.event.KeyAdapter#keyReleased(java.awt.event.KeyEvent)
             */
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 127) {
                    ArrayList<String> sim_key_to_remove = new ArrayList<>();
                    // get all sim_keys to remove. remember: the
                    // simTable-indices change after removal of a simulation, so
                    // we need to store the keys BEFORE any change!
                    for (Integer row : simTable.getSelectedRows()) {
                        sim_key_to_remove.add(simTable.getValueAt(row, SIM_INDEX.KEY.ordinal()).toString());
                    }
                    for (String key : sim_key_to_remove) {
                        SimulationMonitorOld.this.control.removeSimulation(key, true);
                    }
                }
            }
        });

        this.simTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = simTable.getSelectedRow();
                if (e.getClickCount() == 2 && selectedRow != -1) {
                    final String simKeyOld = simTable.getModel().getValueAt(selectedRow, SIM_INDEX.KEY.ordinal())
                                                     .toString();
                    new Thread(() -> {
                        String optionalNewSimKey = "";//new TPS_ParameterFileConverterGUI(control.getParameters()).showModal(frame, simKeyOld);
                        if (!simKeyOld.equals(optionalNewSimKey)) {
                            // start new simulation on the basis of the simulation key
                            // configuration data is already in the DB
                            try {
//                                control.addSimulation(optionalNewSimKey,
//                                        TPS_BasicConnectionClass.getRuntimeFile().getAbsolutePath(), false);
                            } catch (Exception e12) {
                                JOptionPane.showMessageDialog(null, e12.getMessage(), "Start Simulation Exception",
                                        JOptionPane.ERROR_MESSAGE);
                                e12.printStackTrace();
                            }
                        }
                    }).start();

                }

                super.mouseClicked(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                handleSimulationButtons(simTable.getSelectedRowCount());
            }

        });


        JPanel simPanel = new JPanel(new BorderLayout());
        simPanel.setBorder(BorderFactory.createTitledBorder(MultilanguageSupport.getString("SIM_PANEL_TITLE")));
        simPanel.add(new JScrollPane(simTable), BorderLayout.CENTER);

        // combine panels to whole layout

        JPanel tablesPanel = new JPanel();
        JPanel simControlPanel = new JPanel();
        tablesPanel.setLayout(new BoxLayout(tablesPanel, BoxLayout.Y_AXIS));
        tablesPanel.setPreferredSize(new Dimension(800, 600));
        tablesPanel.add(serverPanel);
        tablesPanel.add(simPanel);

        FlowLayout flowLayout = (FlowLayout) simControlPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        simPanel.add(simControlPanel, BorderLayout.SOUTH);


        bStart = new JButton(MultilanguageSupport.getString("SIM_CONTROL_START"));
        bStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSelected(getSelectedRows());
            }
        });
        bPause = new JButton(MultilanguageSupport.getString("SIM_CONTROL_PAUSE"));
        bPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseSelected(getSelectedRows());
            }
        });
        bDelete = new JButton(MultilanguageSupport.getString("SIM_CONTROL_DELETE"));
        bDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected(getSelectedRows());
            }
        });
        bExport = new JButton(MultilanguageSupport.getString("SIM_CONTROL_EXPORT"));
        bExport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportSelected(getSelectedRows());
            }
        });

        bParameters = new JButton(MultilanguageSupport.getString("SIM_CONTROL_PARAMETER"));
        bParameters.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                int selectedRow = simTable.getSelectedRow();

                if (selectedRow != -1) {

                    final String simKeyOld = simTable.getModel().getValueAt(selectedRow, SIM_INDEX.KEY.ordinal())
                                                     .toString();
                    new Thread(() -> {
                        String optionalNewSimKey = "";//new TPS_ParameterFileConverterGUI(control.getParameters()).showModal(frame, simKeyOld);

                        if (!simKeyOld.equals(optionalNewSimKey)) {
                            // start new simulation on the basis of the simulation key
                            // configuration data is already in the DB
                            try {
                               // control.addSimulation(optionalNewSimKey,TPS_BasicConnectionClass.getRuntimeFile().getAbsolutePath(), false);
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null, e.getMessage(), "Start Simulation Exception",
                                        JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        });

        bAnalyzer = new JButton(MultilanguageSupport.getString("SIM_CONTROL_ANALYZER"));

        bAnalyzer.addActionListener(event -> startAnalyzer(simTable.getSelectedRow()));

        analyzer.setEnabled(false);

        bAnalyzer.setEnabled(true);
        bAnalyzer.setFocusable(false);

        bCompare = new JButton(MultilanguageSupport.getString("SIM_CONTROL_COMPARE"));
        bCompare.addActionListener(e -> {
            JFrame comparator = new JFrame();
            JFXPanel fxpanel = new JFXPanel();
            int[] selected_sim_indices = simTable.getSelectedRows();
            if (selected_sim_indices.length != 2) {
                throw new IllegalArgumentException("Need exactly 2 simulation keys to compare");
            } else {
                String simkey1 = (String) simTable.getValueAt(selected_sim_indices[0], SIM_INDEX.KEY.ordinal());
                String simkey2 = (String) simTable.getValueAt(selected_sim_indices[1], SIM_INDEX.KEY.ordinal());

                if (simkey1 != null && simkey2 != null && !simkey1.equals("") && !simkey2.equals("")) {
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getClassLoader().getResource("ParamComparator.fxml"));
                            SimParamComparatorController controller = new SimParamComparatorController(simkey1, simkey2,
                                    null);
                            loader.setController(controller);
                            Parent root;
                            root = loader.load();
                            Scene scene = new Scene(root);
                            scene.getStylesheets().add(
                                    getClass().getClassLoader().getResource("ParamComparator.css").toExternalForm());
                            fxpanel.setScene(scene);
                            //ScenicView.show(scene);
                            SwingUtilities.invokeLater(() -> {
                                comparator.add(fxpanel);
                                comparator.pack();
                                comparator.setVisible(true);

                            });
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    });
                } else {
                    throw new IllegalArgumentException("One of the simkeys is either null or empty...");
                }
            }
        });


        bTUM = new JButton(MultilanguageSupport.getString("SIM_CONTROL_TUM"));
        bTUM.addActionListener(e -> {
            //a dialog box that allows you to chose the destination for the TUM export
            JFileChooser tumfc = tumFileChooser(getSelectedSimkeys().length);
            //tumfc.setCurrentDirectory(Paths.get(this.control.getProperties().get(ClientControlPropKey.LAST_TUM_EXPORT)).toFile());
            int approved = tumfc.showSaveDialog(null);

            if (approved == JFileChooser.APPROVE_OPTION) {

                this.exportfiles = tumfc.getSelectedFiles();
//                this.control.getProperties().set(ClientControlPropKey.LAST_TUM_EXPORT, tumfc.getCurrentDirectory().toString());
//                this.control.updateProperties();
                IntegratedTUM itum = null;
                //SwingWorker is done
                itum.addPropertyChangeListener(evt -> {
                    if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(
                            evt.getNewValue())) {
                        handleSimulationButtons(simTable.getSelectedRows().length);
                        this.progresspanel.setVisible(false);
                        resetProgressBars();
                    }
                    //Progress change on the General Analyzer
                    if ("progress".equals(evt.getPropertyName())) {
                        this.regionpb.setValue((int) evt.getNewValue());
                        this.regionpb.setString(itum.getCurrentTripCnt() + "/" + itum.getTotalTrips());
                    }
                    //Trip table progress
                    if ("triptable".equals(evt.getPropertyName())) if ((boolean) evt.getNewValue()) {
                        this.trippb.setIndeterminate(false);
                        this.trippb.setValue(100);
                        this.trippb.setString("trip table created");
                    } else resetProgressBars();
                    //One simulation is finished
                    if ("intermediate".equals(evt.getPropertyName())) resetProgressBars();
                });
//                if (itum.setInput(this)) {
//                    itum.execute();
//                    this.itum = itum;
//                    bTUM.setEnabled(false);
//                    this.progresspanel.setVisible(true);
//                } else JOptionPane.showMessageDialog(null,
//                        "Couldn't set the input for the TUM-export!\n\nPlease retry...", "Error passing input",
//                        JOptionPane.ERROR_MESSAGE);
            }
        });

        List<JButton> mainbtns = Arrays.asList(bCompare, bTUM, bAnalyzer, bParameters, bStart, bPause, bDelete,
                bExport);

        for (JButton btn : mainbtns) {
            simControlPanel.add(btn);
            btn.setEnabled(false);
        }


        Container container = this.frame.getContentPane();
        container.setLayout(new BorderLayout());
        // container.add(splitPane, BorderLayout.CENTER);
        container.add(tablesPanel);


        //adding bottom part of the client that contains a console as StyledDocument and a button to clear it
        //tuminfo is a container to wrap the console and its clear-button
        tuminfo.setBorder(BorderFactory.createTitledBorder("TUM Information Output"));

        cons.setBackground(new Color(250, 250, 250));
        cons.setEditable(false);

        initDocStyles(this.console);


        //setBorder doesn't work properly with JComponents other than JLabel and JPanel.
        //therefore the console will be put into a wrapper of type JPanel.
        JPanel consolewrapper = new JPanel(new BorderLayout());
        JScrollPane consolescroller = new JScrollPane(cons);
        consolescroller.setPreferredSize(new Dimension(0, 200));
        consolewrapper.add(consolescroller);
        tuminfo.add(consolewrapper);

        JPanel consolebar = new JPanel(new BorderLayout());
        JPanel consolebtns = new JPanel(new BorderLayout());

        JPanel tripprogress = new JPanel(new BorderLayout());

        JLabel stepone = new JLabel("Step 1: ");
        tripprogress.add(stepone, BorderLayout.WEST);
        tripprogress.add(trippb, BorderLayout.EAST);

        JPanel regionprogress = new JPanel(new BorderLayout());

        JLabel steptwo = new JLabel("  Step 2: ");
        regionprogress.add(steptwo, BorderLayout.WEST);
        regionprogress.add(regionpb, BorderLayout.EAST);

        progresspanel.add(tripprogress, BorderLayout.WEST);
        progresspanel.add(regionprogress, BorderLayout.EAST);
        progresspanel.setVisible(false);

        resetProgressBars();

        JButton clearconsole = new JButton("CLEAR CONSOLE");
        clearconsole.addActionListener(e -> cons.setText(null));

        JButton bCancel = new JButton("CANCEL");
        bCancel.addActionListener(e -> {
            if (!this.itum.isDone()) {
                int approve = JOptionPane.showConfirmDialog(null,
                        "Are you sure about cancelling the currently running TUM-Export?", "CANCEL",
                        JOptionPane.YES_NO_OPTION);
                if (approve == JOptionPane.YES_OPTION) this.itum.cancel(true);
            }
        });

        consolebtns.add(bCancel, BorderLayout.WEST);
        consolebtns.add(clearconsole, BorderLayout.EAST);

        consolebar.add(progresspanel, BorderLayout.WEST);
        consolebar.add(consolebtns, BorderLayout.EAST);

        tuminfo.add(consolebar, BorderLayout.SOUTH);

        container.add(controlPanel, BorderLayout.NORTH);
        container.add(tuminfo, BorderLayout.SOUTH);


        // initialise table strings
        this.initTables();

        // finalising frame
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.addWindowListener(new WindowAdapter() {
            /*
             * (non-Javadoc)
             *
             * @see
             * java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent
             * )
             */
            @Override
            public void windowClosing(WindowEvent e) {
                SimulationMonitorOld.this.control.close();
            }
        });
        this.frame.setPreferredSize(new Dimension(1200, 900));
        this.frame.pack();
        Toolkit jTools = Toolkit.getDefaultToolkit();
        Dimension dim = jTools.getScreenSize();
        this.frame.setLocation(dim.width / 2 - this.frame.getWidth() / 2, dim.height / 2 - this.frame.getHeight() / 2);
        this.frame.setVisible(true);
        this.frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (SimulationMonitorOld.this.control.configChanged()) {
                    // default icon, custom title
                    int n = JOptionPane.showConfirmDialog(frame,
                            MultilanguageSupport.getString("SAVE_CHANGES_QUESTION"), "", JOptionPane.YES_NO_OPTION);
                    if (n == JOptionPane.YES_OPTION) {
//                        SimulationMonitorOld.this.control.updateProperties();
                    }
                }
                System.exit(0);
            }
        });
    }

    /**
     * Adds server data to the simulation server table
     *
     * @param serverData simulation server data to add
     */
    public void addServerData(SimulationServerData serverData) {
        synchronized (this.serverTable) {
            int index = containsServerData(serverData);
            if (index == -1) {
                Object[] data = new Object[SERVER_INDICES];
                data[SERVER_INDEX_NAME] = serverData.getName();
                data[SERVER_INDEX_IP] = serverData.getIp().getHostAddress();
                data[SERVER_INDEX_HASH] = serverData.getHashStatus();
                data[SERVER_INDEX_ONLINE] = serverData.isOnline();
                data[SERVER_INDEX_CPU] = serverData.getCpu();
                data[SERVER_INDEX_TIMESTAMP] = serverData.getFormattedTimestamp();
                data[SERVER_INDEX_CORES] = serverData.getCores();
//                data[SERVER_INDEX_SHUTDOWN] = serverData.getServerState();

                ((DefaultTableModel) this.serverTable.getModel()).addRow(data);
            }
        }
    }

    /**
     * Adds simulation of the simulation table
     *
     * @param simulation simulation to add
     */
    public void addSimulation(SimulationData simulation) {

        Object[] data = new Object[this.simTable.getColumnCount()];
        data[SIM_INDEX.KEY.ordinal()] = simulation.getKey();
        data[SIM_INDEX.DESCRIPTION.ordinal()] =
                simulation.getDescription() == null ? simulation.getRelativeFileName() : simulation.getDescription();
        data[SIM_INDEX.FILE.ordinal()] = simulation.getRelativeFileName();
        data[SIM_INDEX.PROGRESS.ordinal()] = 0;
        data[SIM_INDEX.COUNT.ordinal()] = simulation.getProgress() + "/" + simulation.getTotal();
//        data[SIM_INDEX.STOPPED.ordinal()] = simulation.minimumState(TPS_SimulationState.STOPPED);
//        data[SIM_INDEX.STARTED.ordinal()] = simulation.minimumState(TPS_SimulationState.STARTED);
//        data[SIM_INDEX.FINISHED.ordinal()] = simulation.minimumState(TPS_SimulationState.FINISHED);
        data[SIM_INDEX.DATE_STARTED.ordinal()] = simulation.getTimestampStarted();
        data[SIM_INDEX.DATE_FINISHED.ordinal()] = simulation.getTimestampFinished();
        data[SIM_INDEX.ELAPSED.ordinal()] = 0;
        data[SIM_INDEX.ESTIMATED.ordinal()] = 0;
        data[SIM_INDEX.ACTION.ordinal()] = "";
        ((DefaultTableModel) this.simTable.getModel()).addRow(data);
        //((DefaultTableModel) this.simTable.getModel()).fireTableDataChanged();

    }

    private void addWorkingKey(String... keys) {
        if (keys.length == 1) workingSimKeys.add(keys[0]);
        else if (keys.length > 1) workingSimKeys.addAll(Arrays.asList(keys));

        updateTable();

    }

    /**
     * @param serverData simulation server data to search
     * @return row number of the corresponding entry in the simulation server
     * table, /1 if not found
     */
    private int containsServerData(SimulationServerData serverData) {
        synchronized (this.serverTable) {
            for (int i = 0; i < this.serverTable.getRowCount(); i++) {
                if (serverTable.getModel().getValueAt(i, SERVER_INDEX_IP).equals(serverData.getIp().getHostAddress())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * @param simulation simulation data to search
     * @return row number of the corresponding entry in the simulation table, -1
     * if not found
     */
    private int containsSimulation(SimulationData simulation) {
        synchronized (this.simTable) {
            for (int i = 0; i < this.simTable.getRowCount(); i++) {
                if (simTable.getModel().getValueAt(i, SIM_INDEX.KEY.ordinal()).equals(simulation.getKey())) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected void deleteSelected(Object[][] rows) {

        if (rows.length > 0) {

            StringBuilder toDelete = new StringBuilder();

            for (Object[] row : rows) {
                if (toDelete.length() > 0) toDelete.append(",\n");
                toDelete.append((String) row[SIM_INDEX.KEY.ordinal()]);
            }

            if (rows.length > 0 && JOptionPane.showConfirmDialog(frame,
                    "Folgende Simulationen löschen:\n\n" + toDelete + "?") == JOptionPane.YES_OPTION) {
                for (Object[] row : rows) {
                    final String simKey = (String) row[SIM_INDEX.KEY.ordinal()];
                    new Thread(() -> {
                        addWorkingKey(simKey);
                        control.removeSimulation(simKey, false);
                        removeWorkingKey(simKey);
                    }).start();
                }
                handleSimulationButtons(0);
            }
        }
    }

    /**
     * This method either enables or disables all {@link Component}s inside a specific {@link Container}
     *
     * @param container a {@link Container} where all its components should be either enabled or disabled
     * @param enable    flag specifying whether all components should be enabled (TRUE) or disabled (FALSE)
     */
    public void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component c : components) {
            c.setEnabled(enable);
            if (c instanceof Container) {
                enableComponents((Container) c, enable);
            }
        }
    }

    protected void exportSelected(Object[][] rows) {
        Connection connection = null;
        if (connection != null) {

            final JDialog jDialog = new JDialog((Dialog) null, true);
            jDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            jDialog.setTitle("Export");

            final List<String> export = new ArrayList<>();

            jDialog.setSize(400, 100);
            Toolkit jTools = Toolkit.getDefaultToolkit();
            Dimension dim = jTools.getScreenSize();
            jDialog.setLocation(dim.width / 2 - jDialog.getWidth() / 2, dim.height / 2 - jDialog.getHeight() / 2);

            ExecutorService threadPool = Executors.newCachedThreadPool();

            @SuppressWarnings("rawtypes") final List<Future> futures = new ArrayList<>();

            for (Object[] row : rows) {

                String params = ((String[]) row[SIM_INDEX.PARAMS.ordinal()])[0];
                final String key = (String) row[SIM_INDEX.KEY.ordinal()];

                if ((Boolean) row[SIM_INDEX.FINISHED.ordinal()]) {
                    export.add(key);
                   // futures.add(threadPool.submit(new ExportThread(connection, params, key)));
                }
            }

            if (futures.size() > 0) {

                StringBuilder sbuilder = new StringBuilder();

                for (String stringValue : export) {
                    if (sbuilder.length() > 1) sbuilder.append("<br/>");
                    sbuilder.append(stringValue);
                }

                jDialog.getContentPane().add(
                        new JLabel("<html><b>Folgende Simulationen werden exportiert:</b><br/>" + sbuilder));

                new Thread(() -> {
                    for (@SuppressWarnings("rawtypes") Future future : futures) {
                        try {
                            future.get();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    jDialog.setVisible(false);
                    jDialog.dispose();
                }).start();

                jDialog.setVisible(true);
            }
        }

    }


    public StyledDocument getConsole() {

        return this.console;
    }

    /**
     * Implementation of the {@link ITumInterface}
     */

    public File[] getExportFiles() {

        return this.exportfiles;
    }

    private Object[][] getSelectedRows() {

        ArrayList<Object[]> arrayList = new ArrayList<>();

        final TableModel model = simTable.getModel();
        int columnCount = model.getColumnCount();

        for (int r : simTable.getSelectedRows()) {
            Object[] row = new Object[columnCount];
            for (int j = 0; j < row.length; j++) {
                row[j] = model.getValueAt(r, j);
            }
            arrayList.add(row);
        }
        return arrayList.toArray(new Object[0][]);

    }

    private String[] getSelectedSimkeys() {
        ArrayList<String> simulations = new ArrayList<>();
        for (int sim : simTable.getSelectedRows()) {
            simulations.add((String) simTable.getModel().getValueAt(sim, 0));
        }

        return simulations.toArray(new String[0]);
    }

    public String[] getSimKeys() {

        return getSelectedSimkeys();
    }

    /**
     * Handles the enable status for all buttons below the simulations panel for mouse events occurring in that panel
     * While the {@link IntegratedTUM} is also being considered the bTUM button gets handled separately
     *
     * @param cntsim number of selected simulations in the simulation panel
     */
    private void handleSimulationButtons(int cntsim) {

        //bTUM button gets handled separately because it should be disabled during an ongoing TUM export
        List<JComponent> multiselecbtns = Arrays.asList(bCompare, bStart, bPause, bDelete, bExport, bAnalyzer,
                bParameters);
        List<JComponent> singleselecbtns = Arrays.asList(bAnalyzer, bParameters, analyzer);
        Consumer<JComponent> enablebuttons = btn -> btn.setEnabled(true);
        Consumer<JComponent> disablebuttons = btn -> btn.setEnabled(false);

        multiselecbtns.forEach(disablebuttons);
        singleselecbtns.forEach(disablebuttons);
        bTUM.setEnabled(false);

        if (cntsim != 0) {

            //Predicate<JButton> bCompareFilter = cntsim > 1 ? cntsim == 2 ? btn -> btn != bAnalyzer && btn != bParameters : btn -> btn != bCompare &&  btn != bAnalyzer && btn != bParameters : btn -> btn != bCompare;

            Predicate<JComponent> bCompareFilter = cntsim == 2 ? btn -> true : btn -> btn != bCompare;

            if (cntsim > 1) {
                multiselecbtns.stream().filter(bCompareFilter).forEach(enablebuttons);
                singleselecbtns.forEach(disablebuttons);
            } else {
                multiselecbtns.stream().filter(bCompareFilter).forEach(enablebuttons);
                singleselecbtns.forEach(enablebuttons);
            }

            if (itum == null) bTUM.setEnabled(true);
            else bTUM.setEnabled(itum.isDone());
        }
    }

    /**
     * sets up styles used in the console
     */
    private void initDocStyles(StyledDocument console) {


        Style blackunderline = console.addStyle("blackunderline", null);
        Style green = console.addStyle("green", null);
        Style redbold = console.addStyle("redbold", null);
        Style orange = console.addStyle("orange", null);
        Style blackitalicsmall = console.addStyle("blackitalicsmall", null);
        Style standard = console.addStyle("standard", null);
        Style summary = console.addStyle("summary", null);
        Style totalsummary = console.addStyle("totalsummary", null);


        StyleConstants.setForeground(blackunderline, Color.BLACK);
        StyleConstants.setUnderline(blackunderline, true);
        StyleConstants.setBold(blackunderline, true);

        StyleConstants.setForeground(blackitalicsmall, Color.BLACK);
        StyleConstants.setItalic(blackitalicsmall, true);
        StyleConstants.setFontSize(blackitalicsmall, 10);

        StyleConstants.setForeground(standard, Color.DARK_GRAY);

        StyleConstants.setForeground(summary, Color.DARK_GRAY);
        StyleConstants.setBold(summary, true);

        StyleConstants.setForeground(totalsummary, Color.BLACK);
        StyleConstants.setBold(totalsummary, true);

        StyleConstants.setBold(redbold, true);
        StyleConstants.setForeground(redbold, Color.RED);

        StyleConstants.setBold(green, true);
        StyleConstants.setForeground(green, new Color(34, 177, 76));

        StyleConstants.setBold(orange, true);
        StyleConstants.setForeground(orange, Color.ORANGE);
    }

    /**
     * Sets all strings and widths of the tables
     */
    private void initTables() {
        String[] serverHeader = {MultilanguageSupport.getString("SERVER_HEADER_NAME"), MultilanguageSupport.getString(
                "SERVER_HEADER_IP"), MultilanguageSupport.getString(
                "SERVER_HEADER_HASH"), MultilanguageSupport.getString(
                "SERVER_HEADER_ONLINE"), MultilanguageSupport.getString(
                "SERVER_HEADER_CPU"), MultilanguageSupport.getString(
                "SERVER_HEADER_CORES"), MultilanguageSupport.getString("SERVER_HEADER_TIMESTAMP"), "Shutting down"};
        ((DefaultTableModel) this.serverTable.getModel()).setColumnIdentifiers(serverHeader);

        String[] simHeader = {MultilanguageSupport.getString("SIM_HEADER_KEY"), MultilanguageSupport.getString(
                "SIM_HEADER_DESCRIPTION"), MultilanguageSupport.getString(
                "SIM_HEADER_FILE"), MultilanguageSupport.getString("SIM_HEADER_READY"), MultilanguageSupport.getString(
                "SIM_HEADER_STARTED"), MultilanguageSupport.getString(
                "SIM_HEADER_FINISHED"), MultilanguageSupport.getString(
                "SIM_HEADER_PROGRESS"), MultilanguageSupport.getString(
                "SIM_HEADER_COUNT"), MultilanguageSupport.getString(
                "SIM_HEADER_ELAPSED"), MultilanguageSupport.getString(
                "SIM_HEADER_ESTIMATED"), MultilanguageSupport.getString(
                "SIM_HEADER_DATE_STARTED"), MultilanguageSupport.getString(
                "SIM_HEADER_DATE_FINISHED"), MultilanguageSupport.getString("SIM_HEADER_ACTION")};
        // MultilanguageSupport
        ((DefaultTableModel) this.simTable.getModel()).setColumnIdentifiers(simHeader);

        this.serverTable.initTableColumn(SERVER_INDEX_CORES, 50, new AlignmentRenderer(JLabel.CENTER, false), null);
        this.serverTable.initTableColumn(SERVER_INDEX_NAME, 100, new AlignmentRenderer(JLabel.LEFT, false), null);
        this.serverTable.initTableColumn(SERVER_INDEX_TIMESTAMP, 120, new AlignmentRenderer(JLabel.RIGHT, false), null);
        this.serverTable.initTableColumn(SERVER_INDEX_HASH, 50, new HashRenderer(true), null);
        this.serverTable.initTableColumn(SERVER_INDEX_ONLINE, 50, blinkCellRenderer, null);
        this.serverTable.initTableColumn(SERVER_INDEX_CPU, 0, new ProgressRenderer(false), null);
        this.serverTable.initTableColumn(SERVER_INDEX_IP, 111, new AlignmentRenderer(JLabel.LEFT, false), null);
        this.serverTable.initTableColumn(SERVER_INDEX_SHUTDOWN, 0, null, null);

        TableColumn tc = this.serverTable.getColumn(this.serverTable.getColumnName(SERVER_INDEX_SHUTDOWN));
        this.serverTable.getColumnModel().removeColumn(tc);

        this.serverTable.getModel().addTableModelListener(this);


        // this.simTable.initTableColumn(SIM_INDEX_CHECKBOX_SELECTED, 50,
        // new CheckboxRenderer(true), new DefaultCellEditor(
        // new JCheckBox()));

        this.simTable.initTableColumn(SIM_INDEX.PROGRESS.ordinal(), 100, new ProgressRenderer(true), null);
        this.simTable.initTableColumn(SIM_INDEX.COUNT.ordinal(), 125, new AlignmentRenderer(JLabel.RIGHT, true), null);
        this.simTable.initTableColumn(SIM_INDEX.KEY.ordinal(), 0, new AlignmentRenderer(JLabel.LEFT, true), null);
        this.simTable.initTableColumn(SIM_INDEX.FINISHED.ordinal(), 21, new ColorRenderer(true), null);
        this.simTable.initTableColumn(SIM_INDEX.DESCRIPTION.ordinal(), 0, new LongTextRenderer(), new TextPopupEditor());
        this.simTable.initTableColumn(SIM_INDEX.FILE.ordinal(), 0, new LongTextRenderer(), null);
        this.simTable.initTableColumn(SIM_INDEX.STOPPED.ordinal(), 21, new ColorRenderer(true), null);
        this.simTable.initTableColumn(SIM_INDEX.STARTED.ordinal(), 21, new ColorRenderer(true), null);
        this.simTable.initTableColumn(SIM_INDEX.ESTIMATED.ordinal(), 75, new AlignmentRenderer(JLabel.RIGHT, true),
                null);
        this.simTable.initTableColumn(SIM_INDEX.ELAPSED.ordinal(), 75, new AlignmentRenderer(JLabel.RIGHT, true), null);
        this.simTable.initTableColumn(SIM_INDEX.DATE_FINISHED.ordinal(), 111, new AlignmentRenderer(JLabel.RIGHT, true),
                null);
        this.simTable.initTableColumn(SIM_INDEX.DATE_STARTED.ordinal(), 111, new AlignmentRenderer(JLabel.RIGHT, true),
                null);
        this.simTable.initTableColumn(SIM_INDEX.ACTION.ordinal(), 75, new ButtonRenderer(true),
                new SimTableButtonEditor());

        this.simTable.setDragEnabled(true);
        this.simTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.simTable.setDropMode(DropMode.USE_SELECTION);

        this.simTable.setTransferHandler(new TransferHandler("simkey") {
            /**
             *
             */
            private static final long serialVersionUID = -6516312699459718238L;

            @Override
            public boolean canImport(TransferSupport support) {
                return true;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                JTable table = (JTable) c;
                String s = (String) table.getValueAt(table.getSelectedRow(), SIM_INDEX.KEY.ordinal());
                return new StringSelection(s);
            }

            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.COPY;
            }
        });
    }

    /**
     * This method leads the control to insert a new simulation into the
     * database
     */
    private void insertAction() {
        Calendar c = Calendar.getInstance();
        NumberFormat f00 = new DecimalFormat("00");
        NumberFormat f000 = new DecimalFormat("000");
        String sim_key;

        try {
            StringTokenizer st = new StringTokenizer(runFilenameField.getText(), ";");
            int msOffset = 0;
            while (st.hasMoreTokens()) {
                sim_key = c.get(Calendar.YEAR) + "y_" + f00.format(c.get(Calendar.MONTH) + 1) + "m_" + f00.format(
                        c.get(Calendar.DAY_OF_MONTH)) + "d_" + f00.format(c.get(Calendar.HOUR_OF_DAY)) + "h_" +
                        f00.format(c.get(Calendar.MINUTE)) + "m_" + f00.format(c.get(Calendar.SECOND)) + "s_" +
                        f000.format(c.get(Calendar.MILLISECOND) + msOffset) + "ms";
                SimulationMonitorOld.this.control.addSimulation(sim_key, st.nextToken());
                msOffset++;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Start Simulation Exception",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    protected void pauseSelected(Object[][] rows) {

        StringBuilder notPaused = new StringBuilder();

        for (Object[] row : rows) {

            final String simKey = (String) row[SIM_INDEX.KEY.ordinal()];

            boolean r = (Boolean) row[SIM_INDEX.STOPPED.ordinal()];
            boolean s = (Boolean) row[SIM_INDEX.STARTED.ordinal()];

            if (r && s) {
                new Thread(() -> {
                    addWorkingKey(simKey);
                    control.changeSimulationDataState(simKey);
                    removeWorkingKey(simKey);
                }).start();
            } else {
                if (notPaused.length() > 0) notPaused.append(",\n");
                notPaused.append(simKey);
            }
        }

        if (notPaused.length() > 0) JOptionPane.showMessageDialog(frame,
                "Folgende Simulationen nicht pausiert:\n\n" + notPaused);
    }

    /**
     * Removes server data from the simulation server table
     *
     * @param serverData simulation server data to remove
     * @throws SQLException
     */
    public void removeServerData(SimulationServerData serverData) throws SQLException {
        synchronized (this.serverTable) {
            int index = containsServerData(serverData);
            if (index > -1) {
                control.removeServer(serverData.getIp());
                ((DefaultTableModel) this.serverTable.getModel()).removeRow(index);
                ((DefaultTableModel) this.serverTable.getModel()).fireTableDataChanged();
            }
        }
    }

    /**
     * Removes simulation of the simulation table
     *
     * @param simulation simulation to remove
     */
    public void removeSimulation(SimulationData simulation) {
        synchronized (this.simTable) {
            int row = this.containsSimulation(simulation);
            if (row > -1) {
                ((DefaultTableModel) this.simTable.getModel()).removeRow(row);
                ((DefaultTableModel) this.simTable.getModel()).fireTableDataChanged();
            }
        }
    }

    private void removeWorkingKey(String... keys) {
        if (keys.length == 1) workingSimKeys.remove(keys[0]);
        else if (keys.length > 1) workingSimKeys.removeAll(Arrays.asList(keys));

        updateTable();
    }

    private void resetProgressBars() {

        this.trippb.setIndeterminate(true);
        this.trippb.setValue(0);
        this.trippb.setString("trip table in progress");
        this.trippb.setStringPainted(true);

        this.regionpb.setValue(0);
        this.regionpb.setString("number of trips analyzed");
        this.regionpb.setStringPainted(true);
    }

    public void setBackGroundColorForWorking(String simKey, Component component, boolean selected) {

        if (workingSimKeys.contains(simKey)) component.setBackground(Color.LIGHT_GRAY);
        else if (!selected) component.setBackground(Color.WHITE);
    }

    private void startAnalyzer(int selectedRow) {
        final String simkey = (String) simTable.getModel().getValueAt(selectedRow, SIM_INDEX.KEY.ordinal());

        //new Thread(() -> new Control(simkey, this.control.getParameters())).start();

    }

    protected void startSelected(Object[][] rows) {

        StringBuilder notStarted = new StringBuilder();

        for (Object[] row : rows) {

            final String simKey = (String) row[SIM_INDEX.KEY.ordinal()];

            boolean r = (Boolean) row[SIM_INDEX.STOPPED.ordinal()];
            boolean s = (Boolean) row[SIM_INDEX.STARTED.ordinal()];
            boolean f = (Boolean) row[SIM_INDEX.FINISHED.ordinal()];

            if ((!r && !s && !f) || (r && !s && !f)) {
                new Thread(() -> {
                    addWorkingKey(simKey);
                    control.changeSimulationDataState(simKey);
                    removeWorkingKey(simKey);
                }).start();
            } else {
                if (notStarted.length() > 0) notStarted.append(",\n");
                notStarted.append(simKey);
            }
        }

        JOptionPane.showMessageDialog(frame, "Folgende Simulationen nicht gestartet:\n\n" + notStarted);
    }

    /**
     * performs update if server data changed
     */
    @Override
    public void tableChanged(TableModelEvent e) {

//		if (TableModelEvent.UPDATE == e.getType()
//				&& SERVER_INDEX_NAME == e.getColumn()) {
//
//			try {
//				InetAddress ip = InetAddress.getByName((String) serverTable
//						.getValueAt(e.getFirstRow(), SERVER_INDEX_IP));
//				SimulationServerData sd = new SimulationServerData(ip,
//						(String) serverTable.getValueAt(e.getFirstRow(),
//								SERVER_INDEX_NAME));
//				sd.setName((String) serverTable.getValueAt(e.getFirstRow(),
//						SERVER_INDEX_NAME), control.getConnection()
//						.getConnection(this));
//			} catch (UnknownHostException e1) {
//				e1.printStackTrace();
//			} catch (SQLException e1) {
//				e1.printStackTrace();
//			}
//		}
    }

    /**
     * @param simcount number of simulations selected in the simulation panel
     * @return a customized {@link JFileChooser} for the {@link IntegratedTUM} export
     */

    private JFileChooser tumFileChooser(int simcount) {

        JFileChooser tumfc = new JFileChooser() {

            private static final long serialVersionUID = -1927767994559130743L;
            private final ArrayList<File> filestopass = new ArrayList<>();

            @Override
            public void approveSelection() {

                String filename;
                ArrayList<File> existingfiles = new ArrayList<>();

                //if multiple simulations have to be exported, filenames will be of form "filename_1.xls" to "filename_n.xls"
                if (!(simcount == 1)) {

                    for (int i = 1; i <= simcount; i++) {

                        filename = getSelectedFile().getAbsolutePath();

                        if (!filename.toLowerCase().endsWith(".xls")) filename += "_" + i + ".xls";
                        else {
                            filename = new StringBuilder(filename).insert(filename.length() - 4, "_" + i).toString();
                        }

                        File f = new File(filename);
                        this.filestopass.add(f);

                        if (f.exists() && getDialogType() == SAVE_DIALOG) existingfiles.add(f);
                    }
                } else {

                    filename = getSelectedFile().getAbsolutePath();

                    if (!filename.toLowerCase().endsWith(".xls")) filename += ".xls";

                    File f = new File(filename);
                    this.filestopass.add(f);

                    if (f.exists() && getDialogType() == SAVE_DIALOG) existingfiles.add(f);
                }

                //treat case where at least one file at a specified location does already exist
                if (!existingfiles.isEmpty()) {

                    ArrayList<File> protectedfiles = new ArrayList<>();

                    StringBuilder failfiles = new StringBuilder("<html><strong>");
                    StringBuilder protfiles = new StringBuilder("<html><strong>");

                    //treat case whether existing files can be overwritten
                    for (File f : existingfiles) {
                        if (!f.canWrite()) {
                            protectedfiles.add(f);
                            protfiles.append("- ").append(f.getName()).append("<br />");
                        } else failfiles.append("- ").append(f.getName()).append("<br />");
                    }
                    failfiles.append("</strong>");
                    protfiles.append("</strong>");

                    int result;

                    if (protectedfiles.size() > 0) {
                        JOptionPane.showConfirmDialog(this,
                                "<html>The following files already exist and are <strong>protected to overwrite</strong>:\n" +
                                        protfiles + "\nPlease choose a different filename.", "Protected Files",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                        result = JOptionPane.NO_OPTION;
                    } else {
                        result = JOptionPane.showConfirmDialog(this,
                                "The following files already exist:\n" + failfiles +
                                        "\nWould you like to overwrite them?", "Existing files",
                                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    }

                    switch (result) {

                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                        default:
                            this.filestopass.clear();
                            return;
                    }
                }
                super.approveSelection();
            }

            /**
             * return computed file objects that get passed to {@link IntegratedTUM} through {@link ITumInterface}
             * @return
             */
            @Override
            public File[] getSelectedFiles() {

                File[] result = new File[this.filestopass.size()];
                for (int i = 0; i < this.filestopass.size(); i++) {

                    result[i] = filestopass.get(i);
                }

                return result;
            }


        };

        tumfc.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(".xls files", "xls");
        tumfc.setFileFilter(filter);

        return tumfc;
    }

    /**
     * This method updates the server control panel on the right side next to the servers table. It handles the presentation of additional server information
     * and the enable status of the control buttons
     *
     * @param serverDataMap a map containing hostname as key and {@link SimulationServerData} as value
     */
    public void updateServerControl(Map<String, SimulationServerData> serverDataMap) {


        int count = this.serverTable.getSelectedRowCount();

        if (count == 1) {

            String host = (String) this.serverTable.getModel().getValueAt(this.serverTable.getSelectedRow(),
                    SERVER_INDEX_NAME);
            SimulationServerData ssd = serverDataMap.get(host);
            if (ssd != null) {
                Map<ServerInfo, String> serverinfo = ssd.getServerProcessInfo();
                lblIdentifier.setText(serverinfo.get(ServerInfo.IDENTIFIER));
                lblRunSince.setText(serverinfo.get(ServerInfo.RUNSINCE));
                lblPid.setText(serverinfo.get(ServerInfo.PID));
                lblWork.setText(serverinfo.get(ServerInfo.WORKON));
                lblRemote.setText(serverinfo.get(ServerInfo.SHUTDOWN));

                bStartsrv.setEnabled(ssd.isRemotelyControllable() && !ssd.isOnline());
                bTerminatesrv.setEnabled(ssd.isRemotelyControllable() && ssd.isOnline());

                //enableComponents(this.serverPanelRight,true);
            } else {
                lblIdentifier.setText("NA");
                lblRunSince.setText("NA");
                lblPid.setText("NA");
                lblWork.setText("NA");
                bTerminatesrv.setEnabled(false);
                bStartsrv.setEnabled(false);
            }
            bDeletesrv.setEnabled(true);
        } else enableComponents(this.serverPanelRight, false);
    }

    /**
     * Updates server data of the simulation server table
     *
     * @param serverData simulation server data to update
     */
    public void updateServerData(SimulationServerData serverData) {


        synchronized (this.serverTable) {
            int index = containsServerData(serverData);
            if (index == -1) {
                this.addServerData(serverData);
            } else {
                this.serverTable.setValueAt(serverData.getName(), index, SERVER_INDEX_NAME);
                this.serverTable.setValueAt(serverData.getIp().getHostAddress(), index, SERVER_INDEX_IP);
                this.serverTable.setValueAt(serverData.getHashStatus(), index, SERVER_INDEX_HASH);
                this.serverTable.setValueAt(serverData.isOnline(), index, SERVER_INDEX_ONLINE);
                this.serverTable.setValueAt(serverData.getCpu(), index, SERVER_INDEX_CPU);
                this.serverTable.setValueAt(serverData.getCores(), index, SERVER_INDEX_CORES);
                this.serverTable.setValueAt(serverData.getFormattedTimestamp(), index, SERVER_INDEX_TIMESTAMP);
                //this.serverTable.getModel().setValueAt(serverData.getServerState(), index, SERVER_INDEX_SHUTDOWN);
            }
        }
    }

    /**
     * Updates simulation of the simulation table
     *
     * @param simulation simulation to update
     */
    public void updateSimulationData(SimulationData simulation) {

        int row = this.containsSimulation(simulation);
        if (row == -1) {
            this.addSimulation(simulation);
        } else {
            simTable.setValueAt((double) simulation.getProgress() / simulation.getTotal(), row,
                    SIM_INDEX.PROGRESS.ordinal());
            simTable.setValueAt(simulation.getProgress() + "/" + simulation.getTotal(), row, SIM_INDEX.COUNT.ordinal());
//            simTable.setValueAt(simulation.minimumState(TPS_SimulationState.STOPPED), row, SIM_INDEX.STOPPED.ordinal());
//            simTable.setValueAt(simulation.minimumState(TPS_SimulationState.STARTED), row, SIM_INDEX.STARTED.ordinal());
//            simTable.setValueAt(simulation.minimumState(TPS_SimulationState.FINISHED), row,
//                    SIM_INDEX.FINISHED.ordinal());
            simTable.setValueAt(simulation.getTimestampStarted(), row, SIM_INDEX.DATE_STARTED.ordinal());
            simTable.setValueAt(simulation.getTimestampFinished(), row, SIM_INDEX.DATE_FINISHED.ordinal());
            simTable.setValueAt(simulation.getElapsedTime(control.getCurrentDatabaseTimestamp()), row,
                    SIM_INDEX.ELAPSED.ordinal());
            simTable.setValueAt(simulation.getEstimatedTime(control.getCurrentDatabaseTimestamp()), row,
                    SIM_INDEX.ESTIMATED.ordinal());
//            simTable.setValueAt(simulation.getState().getAction(), row, SIM_INDEX.ACTION.ordinal());
        }

    }

    /**
     * Updates the description text of the given simulation in the database.
     *
     * @param description
     * @param simkey
     */
    private void updateSimulationDescription(String description, String simkey) {
//        TPS_DB_Connector connection = control.getConnection();
//        String query =
//                "UPDATE simulations SET sim_description = '" + description + "' WHERE sim_key = '" + simkey + "'";
//        connection.executeUpdate(query, control);
    }

    private void updateTable() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                simTable.updateUI();
            }
        });
    }


    enum SIM_INDEX {
        KEY, DESCRIPTION, FILE, STOPPED, STARTED, FINISHED, PROGRESS, COUNT, ELAPSED, ESTIMATED, DATE_STARTED, DATE_FINISHED, ACTION, PARAMS
    }

    /**
     * This class provides a DefaultTableCellRenderer which set the alignment of
     * the component inside the cell.
     *
     * @author mark_ma
     */
    private class AlignmentRenderer extends DefaultTableCellRenderer {

        /**
         * serial UID
         */
        private static final long serialVersionUID = -6707632542192433108L;
        private final boolean checkWorkingRows;
        /**
         * the alignment value
         */
        private final int alignment;

        /**
         * The constructor sets the alignment value
         *
         * @param alignment        One of the following constants defined in
         *                         <code>SwingConstants</code>: <code>LEFT</code>,
         *                         <code>CENTER</code> (the default for image-only labels),
         *                         <code>RIGHT</code>, <code>LEADING</code> (the default for
         *                         text-only labels) or <code>TRAILING</code>.
         * @param checkWorkingRows
         * @see SwingConstants
         */
        public AlignmentRenderer(int alignment, boolean checkWorkingRows) {
            this.alignment = alignment;
            this.checkWorkingRows = checkWorkingRows;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent
         * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            label.setText(value != null ? value.toString() : "");
            label.setHorizontalAlignment(alignment);
            if (checkWorkingRows) setBackGroundColorForWorking(
                    table.getValueAt(row, SIM_INDEX.KEY.ordinal()).toString(), label, isSelected);
            return label;
        }

    }

    /**
     * This class provides a Table Cell Renderer which creates an empty border
     * around the component inside the cell.
     *
     * @author mark_ma
     */
    private class BorderedRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final boolean checkWorkingRows;
        /**
         * The component inside the cell
         */
        private JComponent component;
        /**
         * The border when the line in the table is selected
         */
        private Border selectedBorder = null;
        /**
         * The border when the line in the table is not selected
         */
        private Border unselectedBorder = null;

        /**
         * The constructor builds the borders for the selected and the non
         * selected case
         *
         * @param component        the component which is inside the cell
         * @param checkWorkingRows flag to indicate if ths row should be checked, if
         *                         background work is performed
         */
        public BorderedRenderer(JComponent component, boolean checkWorkingRows) {
            this.component = component;
            this.checkWorkingRows = checkWorkingRows;
            this.component.setOpaque(true); // MUST do this for background to
            // show up.
        }

        public BorderedRenderer() {
            this.component = new JLabel();
            this.checkWorkingRows = false;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * javax.swing.table.TableCellRenderer#getTableCellRendererComponent
         * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (this.component == null) this.component = (JComponent) super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            this.component.setOpaque(true); // MUST do this for background to show up
            if (this.selectedBorder == null) {
                this.selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5, table.getSelectionBackground());
                this.unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5, table.getBackground());
            }
            // sets the correct border for the selected or non-selected case
            this.component.setBorder(isSelected ? this.selectedBorder : this.unselectedBorder);
            if (checkWorkingRows) setBackGroundColorForWorking(
                    table.getValueAt(row, SIM_INDEX.KEY.ordinal()).toString(), component, isSelected);
            return this.component;
        }
    }

    /**
     * This class represents a renderer for a bordered progress bar inside a
     * cell of a JTable. The cell component has to be a ProgressItem.
     *
     * @author mark_ma
     */
    private class ButtonRenderer extends BorderedRenderer implements TableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -3512025783684345842L;

        public ButtonRenderer(boolean checkWorkingRows) {
            super(new JButton(""), checkWorkingRows);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
         * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
         * boolean, boolean, int, int)
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JButton btn = (JButton) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            btn.setText(value.toString());
            return btn;
        }
    }

    /**
     * This class represents a renderer for a bordered progress bar inside a
     * cell of a JTable. The cell component has to be a ProgressItem.
     *
     * @author mark_ma
     */
    private class StringArrayRenderer extends BorderedRenderer implements TableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = 561448092814896118L;

        /**
         * Calls super constructor
         *
         * @param checkWorkingRows flag to check if this entry is working in background
         */
        public StringArrayRenderer(boolean checkWorkingRows) {
            super(new JLabel(), checkWorkingRows);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
         * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
         * boolean, boolean, int, int)
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            StringBuilder sbuilder = new StringBuilder();

            sbuilder.append("[");
            for (String stringValue : (String[]) value) {
                if (sbuilder.length() > 1) sbuilder.append(", ");
                sbuilder.append(stringValue);
            }
            sbuilder.append("]");

            lbl.setText(sbuilder.toString());
            return lbl;
        }
    }

    /**
     * This class renders cells in a JTable with ColorItem objects inside. The
     * cells have an empty border and show a box with a color between red and
     * green.
     *
     * @author mark_ma
     */
    private class ColorRenderer extends BorderedRenderer implements TableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = 8626017776312229467L;

        /**
         * Calls super constructor
         *
         * @param checkWorkingRows flag to check if this entry is working in background
         */
        public ColorRenderer(boolean checkWorkingRows) {
            super(new JLabel(), checkWorkingRows);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
         * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
         * boolean, boolean, int, int)
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // get bordered component
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // determine color
            double v;
            if (value instanceof Boolean) {
                Boolean newValue = (Boolean) value;
                v = newValue ? 1 : 0;
            } else {
                Number newValue = (Number) value;
                v = newValue.doubleValue();
            }
            int red = (int) (255 * (1 - v));
            int green = (int) (255 * v);
            Color newColor = new Color(red, green, 0);
            // set color
            comp.setBackground(newColor);

            return comp;
        }
    }

    private class HashRenderer extends BorderedRenderer implements TableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -7640945626507083454L;
        private final Color transparent = new Color(0, 0, 0, 0);

        public HashRenderer(boolean checkWorkingRows) {
            super(new JLabel(), checkWorkingRows);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasfocus, int row, int column) {

            JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasfocus, row,
                    column);

            if (value instanceof HashStatus) {

                switch ((HashStatus) value) {
                    case UNKNOWN:
                        comp.setBackground(transparent);
                        comp.setToolTipText(MultilanguageSupport.getString("HASH_MESSAGE_UNKNOWN"));
                        break;
                    case INVALID:
                        comp.setBackground(Color.RED);
                        comp.setToolTipText(MultilanguageSupport.getString("HASH_MESSAGE_ERROR"));
                        break;
                    case NON_DOMINANT:
                        comp.setBackground(Color.GRAY);
                        comp.setToolTipText("<html><strong>" + table.getValueAt(row, SERVER_INDEX_NAME) + "</strong> " +
                                MultilanguageSupport.getString("HASH_MESSAGE_INFIX") + "<br />- " +
                                control.getServerHash((String) table.getValueAt(row, SERVER_INDEX_NAME)) + "</html>");
                        break;
                    case DOMINANT:
                        comp.setBackground(Color.GREEN);
                        comp.setToolTipText("<html><strong>" + table.getValueAt(row, SERVER_INDEX_NAME) + "</strong> " +
                                MultilanguageSupport.getString("HASH_MESSAGE_INFIX") + "<br />- " +
                                control.getServerHash((String) table.getValueAt(row, SERVER_INDEX_NAME)) + "</html>");
                        break;
                    case DEBUG:
                        comp.setBackground(Color.ORANGE);
                        comp.setToolTipText(table.getValueAt(row, SERVER_INDEX_NAME) + " " +
                                MultilanguageSupport.getString("HASH_MESSAGE_DEBUG"));
                        break;
                }
            } else comp.setBackground(transparent);
            //String tooltip = control.getServerHash((String)table.getValueAt(row, SERVER_INDEX_NAME));
            //comp.setToolTipText(tooltip);

            return comp;
        }
    }

    /**
     * End of {@link ITumInterface} implementation
     */

    private class ServerColorRenderer extends BorderedRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -8525790773829893978L;
        private final TableCellRenderer delegate;
        private final Set<Cell> blinkingCells = new HashSet<>();
        private boolean blinkingState = true;

        ServerColorRenderer(final JTable table, TableCellRenderer delegate) {

            this.delegate = delegate;


//            Timer blinkingTimer = new Timer(250, e -> {
//                blinkingState = !blinkingState;
//                table.repaint();
//            });
//            blinkingTimer.setInitialDelay(0);
//            blinkingTimer.start();
        }

        void addBlinkingCell(int r, int c) {
            blinkingCells.add(new Cell(r, c));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            Cell cell = new Cell(row, column);

            if (blinkingCells.contains(cell)) component.setBackground(blinkingState ? Color.RED : Color.GREEN);
            else component.setBackground(
                    (Boolean) table.getModel().getValueAt(row, SERVER_INDEX_ONLINE) ? Color.GREEN : Color.RED);

            return component;
        }

        void removeBlinkingCell(int r, int c) {
            blinkingCells.remove(new Cell(r, c));
        }

        class Cell {
            final int row;
            final int col;

            Cell(int row, int col) {
                this.row = row;
                this.col = col;
            }

            @Override
            public boolean equals(Object object) {
                if (object instanceof Cell) {
                    Cell cell = (Cell) object;
                    return row == cell.row && col == cell.col;
                }
                return false;
            }

            @Override
            public int hashCode() {
                return col + row;
            }
        }
    }

    /**
     * This class represents a renderer for a bordered progress bar inside a
     * cell of a JTable. The cell component has to be a ProgressItem.
     *
     * @author mark_ma
     */
    private class ProgressRenderer extends BorderedRenderer implements TableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -2219856734598874179L;

        /**
         * Calls super constructor
         *
         * @param checkWorkingRows flag to check if this entry is working in background
         */
        public ProgressRenderer(boolean checkWorkingRows) {
            super(new JProgressBar(0, 1000), checkWorkingRows);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
         * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
         * boolean, boolean, int, int)
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JProgressBar pBar = (JProgressBar) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            int pValue = (int) (((Number) value).doubleValue() * 1000);
            pBar.setValue(pValue);
            pBar.setString(pValue / 10.0 + "%");
            pBar.setStringPainted(true);
            return pBar;
        }
    }

    /**
     * This class provides an editor for the simulation data table. The editor
     * contains a checkbox, which can change the state of the corresponding
     * simulation in this row of the simulation data table.
     *
     * @author mark_ma
     */
    private class SimTableButtonEditor extends AbstractCellEditor implements TableCellEditor {
        /**
         * serial UID
         */
        private static final long serialVersionUID = 7571850003681799808L;

        /**
         * checkbox inside the cell
         */
        JButton button;

        /**
         * The constructor builds the checkbox and adds the ActionListener which
         * changes the state of the simulation
         */
        public SimTableButtonEditor() {
            this.button = new JButton();
            this.button.addActionListener(new ActionListener() {
                /*
                 * (non-Javadoc)
                 *
                 * @see
                 * java.awt.event.ActionListener#actionPerformed(java.awt.event
                 * .ActionEvent)
                 */
                public void actionPerformed(ActionEvent e) {
                    new Thread(() -> {
                        int row = simTable.getEditingRow();
                        if (row >= 0) {
                            String key = simTable.getValueAt(row, SIM_INDEX.KEY.ordinal()).toString();
                            addWorkingKey(key);
                            SimulationMonitorOld.this.control.changeSimulationDataState(key);
                            stopCellEditing();
                            removeWorkingKey(key);
                        }
                    }).start();
                }
            });
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.CellEditor#getCellEditorValue()
         */
        public Object getCellEditorValue() {
            return button.getText();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax
         * .swing.JTable, java.lang.Object, boolean, int, int)
         */
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText(value.toString());
            return button;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * javax.swing.AbstractCellEditor#isCellEditable(java.util.EventObject)
         */
        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                return ((MouseEvent) anEvent).getClickCount() > 0;
            }
            return false;
        }
    }


}
