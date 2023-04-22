/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.gui;

import de.dlr.ivf.tapas.analyzer.core.Core;
import de.dlr.ivf.tapas.analyzer.core.CoreInputInterface;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTripReader;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general.TUMControlGeneral;
import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;
import org.apache.log4j.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

/**
 * Einstiegsklasse des TapasAnalyzer. Zeitgleich ist {@link Control} ein
 * {@link CoreInputInterface}, dadurch können andere Objekte die Zugriff auf
 * diese Instanz haben unter anderem die selektierten Module abfragen.
 *
 * @author Marco
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.ALL)
public class Control implements CoreInputInterface, ActionListener {

    // private static Logger logger = Logger.getRootLogger();

    /**
     * Logging object
     */
    private static final Logger LOG = LogManager.getLogger(Control.class);
    private static String initOutputPath;
    private static Control instance;
    final JTextPane cons = new JTextPane();
    final StyledDocument console = cons.getStyledDocument();
    final JPanel panelStatus = new JPanel();
    final JLabel lblStatus = new JLabel();
    final JScrollPane scrollStatus = new JScrollPane(cons);
    // Path of the tripFiles
    final List<File> persFiles = new LinkedList<>();
    final JButton startApp = new JButton(
            new ImageIcon(getClass().getClassLoader().getResource("icons/rechner_sml.jpg")));
    final JButton stopApp = new JButton(new ImageIcon(getClass().getClassLoader().getResource("icons/cancel_sml.jpg")));
    // Attributes
    JFileChooser choose = new JFileChooser();
    Map<Module, ControlInputInterface> col;
    int countStarts = 0;
    // Define Labels
    JLabel groupOne = new JLabel("");
    // JCheckBox checkSlices = new JCheckBox();
    boolean isStarted = false;
    JLabel lblPath = new JLabel("Ausgabepfad: ");
    JTextField path;
    // Define Run-Buttons
    JButton searchPath = new JButton(new ImageIcon(getClass().getClassLoader().getResource("icons/folder_sml.jpg")));
    // Properties TOOD_MATRIXProperties = new Properties();
    JLabel tripOne = new JLabel("");
    private String initSimulation;
    // Iterator for TripFiles
    private TripChooserPanel tripChooser;
    private TapasTripReader tripReader;
    // boolean TOOD_MATRIXactivated;
    private final TPS_ParameterClass parameterClass;
    // boolean TUMactivated;
    // Properties TUMProperties = new Properties();
    // boolean VISEVAactivated;
    // Properties VISEVAProperties = new Properties();
    private final List<Core> threadList = new LinkedList<>();
    private FilesChooser tripFilesChooser;
    private final String version = "0.9.0.0";

    /**
     * @param parameterClass parameter container class
     */
    public Control(TPS_ParameterClass parameterClass) {
        this.parameterClass = parameterClass;
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%5p %d - %m%n")));
        Logger.getRootLogger().setLevel(Level.ALL);
        // logger.info("Starting Program");
        LOG.info("Starting Control");

        // show styles
        // Object a[] = UIManager.getInstalledLookAndFeels();
        // for (int i = 0; i < a.length; i++)
        // System.out.println(a[i]);
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            System.out.println("Cant get laf");
        }

        // Auswerte-Module werden hinzugefügt
        this.col = new HashMap<>();
        this.col.put(Module.TUM, new TUMControlGeneral());


        // Propertiefiles
        // TUMProperties.setProperty("regionalDifferenziert", "inactive");
        // TOOD_MATRIXProperties.setProperty("sampleSize", "0");
        // VISEVAProperties.setProperty("rentnerDifferenziert", "inactive");
        // TUMactivated = false;
        // TOOD_MATRIXactivated = false;
        // VISEVAactivated = false;

        DefaultCaret caret = (DefaultCaret) cons.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // load File Paths
        try {
            // System.out.println(System.getProperties());
            initOutputPath = FileChooserHistoryManager.getLastDirectory("outputPath");
            // System.out.println("Read Properties" + initOutputPath);

        } catch (Exception e) { // if propertiesFile could not be found
            // System.out.println("Invalid Format: Path-Properties File");
            initOutputPath = System.getProperty("user.dir");
            FileChooserHistoryManager.updateLastDirectory("outputPath", initOutputPath);
        }
        path = new JTextField(initOutputPath, 30);

        instance = this;

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                instance.createAndShowGUI(instance.col);
            }

        });

    }

    /**
     * @param initialSimulation is the simulation that is selected on startup (if it is found in the database)
     * @param parameterClass    parameter container class
     */
    public Control(String initialSimulation, TPS_ParameterClass parameterClass) {
        this(parameterClass);
        initSimulation = initialSimulation;
    }

    public static Control getInstance() {
        return instance;
    }

    /**
     * The timer check for progress
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        boolean isProcessing = false;
        for (Core c : threadList) {
            if (c.isAlive()) {
                isProcessing = true;
                break;
            }
        }
        if (isProcessing && tripReader != null) {
            int progress = tripReader.getProgress();
            if (progress > 0) {
                lblStatus.setText("Trips processed " + progress + "%");
                return;
            }
        }
        lblStatus.setText("");
    }

    private void addProgressBar() {
        Timer progressCheck = new Timer(300, this);
        progressCheck.setRepeats(true);
        progressCheck.start();
    }

    // Method to choose outputpath
    private File chooseDirectory(String type, JFrame f) {
        JFileChooser fd = new JFileChooser(initOutputPath);
        fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fd.setVisible(true);

        fd.setDialogTitle("Bitte wählen Sie einen Ausgabepfad");
        int value = fd.showOpenDialog(f);

        if ((value == JFileChooser.APPROVE_OPTION)) {
            if (fd.getSelectedFile().isDirectory()) {
                // Outputpath
                File pathCacheForOutputPath = fd.getSelectedFile();
                initOutputPath = pathCacheForOutputPath.toString();
                path.setText(initOutputPath);
                FileChooserHistoryManager.updateLastDirectory("outputPath", initOutputPath);

                return fd.getSelectedFile();
            }
            // System.out.println("5");
            JOptionPane.showMessageDialog(f, "Ungültiger Ausgabepfad: " + fd.getSelectedFile().getPath(), "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        }
        return new File("initOutPutPath");
    }

    private void createAndShowGUI(Map<Module, ControlInputInterface> col) {
        this.col = col;
        // Create and set up the window
        JFrame.setDefaultLookAndFeelDecorated(true);
        // make frame
        final JFrame frame = new JFrame("TAPAS-Analyzer  Ver.:" + this.version);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //removed and switched to a DISPOSE_ON_CLOSE, see above line
        //frame.addWindowListener(new WindowAdapter() {
        //	public void windowClosing(WindowEvent winEvt) {
        //		if (tripReader != null) {
        //			tripReader.close();
        //		}
        //		System.exit(0);
        //	}
        //});

        frame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("icons/TAPAS-Logo.gif")).getImage());


        // Panel for all labels etc
        JPanel view = new JPanel();
        view.setLayout(new BorderLayout());
        // // label with Buttons
        JPanel topPane = new JPanel();
        topPane.setLayout(new BorderLayout());
        //console
        cons.setPreferredSize(new Dimension(1000, 300));
        cons.setMaximumSize(new Dimension(0, 300));
        JScrollPane sp = new JScrollPane(cons);
        sp.setMaximumSize(new Dimension(0, 200));
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        path.setEditable(false);
        startApp.setToolTipText("Ausgabedateien erzeugen");
        stopApp.setToolTipText("Berechnungen abbrechen");
        searchPath.setToolTipText("Ausgabepfad ändern");

        // TripChooser

        tripChooser = new TripChooserPanel("Trip Chooser", initSimulation, console);
        topPane.add(tripChooser);

        JPanel outputPathPanel = new JPanel();
        outputPathPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        outputPathPanel.add(lblPath);
        outputPathPanel.add(path);
        searchPath.addActionListener(new ActionListener() {

            /**
             * set the outputpath
             */

            public void actionPerformed(ActionEvent arg0) {
                try {
                    path.setText(chooseDirectory("", frame).getAbsolutePath());
                } catch (Exception e) {
                    // Ordnerauswahl abgebrochen
                    // e.printStackTrace();
                }
            }
        });
        outputPathPanel.add(searchPath);
        topPane.add(outputPathPanel, BorderLayout.SOUTH);

        final JPanel modulePanel = new JPanel();

        // Panel for Status display
        panelStatus.setLayout(new BorderLayout());
        panelStatus.add(lblStatus, BorderLayout.SOUTH);

        JPanel appButtons = new JPanel();
        appButtons.setLayout(new GridLayout());

        // start button listener

        startApp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    console.insertString(0, "", null);
                } catch (BadLocationException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                if (tripChooser.isReady()) { // TODO proper check on Trip Reader
                    countStarts++;
                    Core core = new Core("Core Thread " + countStarts, instance.parameterClass);
                    try {
                        core.setCoreInput(Control.this);
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    threadList.add(core);
                    core.start();
                } else {
                    // JOptionPane.showMessageDialog(modulePanel,
                    // "Fehlende Eingabedateien", "Fehler",
                    // JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // cancel button listener
        stopApp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                // Beende die gestarteten Auswertungen
                int numOfThreads = countStarts;

                for (Core t : threadList) {

                    t.cancel();
                    --countStarts;

                }
                threadList.clear();
                tripReader.close();

                if (numOfThreads > 0) {
                    JOptionPane.showMessageDialog(modulePanel, "Abbruch durch Benutzer", "Info",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        appButtons.add(startApp);
        appButtons.add(stopApp);
        // JSeparator separateTwo = new JSeparator();

        int i = 0;
        // Sortiere die Auswerte-Module nach Priorität, so hat die UI einen
        // festen Aufbau
        List<ControlInputInterface> values = new ArrayList<>(col.values());
        values.sort(Comparator.comparingInt(ControlInputInterface::getIndex));

        // Füge die Auswerte-Module der UI hinzu
        for (ControlInputInterface coi : values) {
            GridBagConstraints gbcM = new GridBagConstraints();
            gbcM.fill = GridBagConstraints.BOTH;
            gbcM.gridx = 0;
            gbcM.gridy = i++;
            modulePanel.add(coi.getComponent(), gbcM);
        }
        modulePanel.setLayout(new BoxLayout(modulePanel, BoxLayout.Y_AXIS));

        JSeparator separateFour = new JSeparator();
        modulePanel.add(separateFour);

        modulePanel.doLayout();

        JPanel appPanel = new JPanel();
        appPanel.setLayout(new BorderLayout());
        appPanel.add(panelStatus, BorderLayout.SOUTH);
        appPanel.add(appButtons, BorderLayout.NORTH);

        JPanel main = new JPanel();

        main.setLayout(new BorderLayout());
        JScrollPane contentScrollPane = new JScrollPane(modulePanel);
        contentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        main.add(contentScrollPane, BorderLayout.CENTER);
        main.add(appPanel, BorderLayout.SOUTH);

        JPanel infooutput = new JPanel();
        infooutput.setLayout(new BorderLayout());
        infooutput.add(sp, BorderLayout.NORTH);


        view.add(topPane, BorderLayout.NORTH);
        view.add(main, BorderLayout.CENTER);
        view.add(infooutput, BorderLayout.SOUTH);

        frame.setContentPane(view);
        frame.pack();
        frame.setVisible(true);
        addProgressBar();
    }

    public StyledDocument getConsole() {
        return console;
    }

    public ControlInputInterface getInterface(Module module) {
        return this.col.get(module);
    }

    public String getOutputPath() {
        return (path.getText());
    }

    public JPanel getPanel() {
        return new JPanel();
    }

    public List<File> getTripFiles() {
        return tripFilesChooser.getFiles();
    }

    @Override
    public TapasTripReader getTripReader() {
        tripReader = tripChooser.getTripReader();
        return tripReader;
    }

    public boolean isActive(Module module) {
        return this.getInterface(module).isActive();
    }

//	private void close() {
//		if (tripReader != null) {
//			tripReader.close();
//		}
//	}

//	@Override
//	protected void finalize() throws Throwable {
//		try {
//			close();
//		} catch (Throwable t) {
//			throw t;
//		} finally {
//			super.finalize();
//		}
//	}

}
