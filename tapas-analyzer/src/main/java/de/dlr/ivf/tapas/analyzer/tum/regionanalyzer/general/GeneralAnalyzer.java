/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;

import de.dlr.ivf.tapas.analyzer.gui.AbstractCoreProcess;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DefaultDistanceCategoryAnalyzer;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.ModeAnalyzer;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.PersonGroupAnalyzer;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.TripIntentionAnalyzer;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.Analyzer;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.AnalyzerBase;
import de.dlr.ivf.tapas.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.tools.TUM.IntegratedTUM;
import jxl.write.WriteException;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * <p>
 * This is the general logic module for the TUM export of the TAPAS Analyzer. It
 * takes a tree of {@link AnalyzerCollection}s and fills it accordingly. The
 * main loop of adding {@link TapasTrip}s is done outside this class. The method
 * {@link GeneralAnalyzer#prepare(String, TapasTrip, TPS_ParameterClass)} is expected to be called
 * very often.
 * </p>
 * <p>
 * The logic of this module is mostly handled by the {@link Analyzer}s hold (and
 * implicitly defined with the tree passed to the constructor) and the
 * exporters. This class provides an entry point to the TAPAS analyzer
 * structure.
 * </p>
 * <p>
 * As per the definition of the extended {@link AbstractCoreProcess} the method
 * {@link GeneralAnalyzer#finish()} will trigger the default export methods. An
 * excel export via {@link GeneralExcelExport} will be attempted. If the boolean
 * <code>writeToDB</code> is set on construction, a
 * {@link GeneralDatabaseSummaryExport} will be started as well.
 * </p>
 *
 * @author boec_pa
 */
@SuppressWarnings("rawtypes")
public class GeneralAnalyzer extends AbstractCoreProcess {

    private final static String sourceSeperator = ";";
    private StyledDocument console;
    private String outputPath;
    private final HashSet<String> sources = new HashSet<>();
    private final HashMap<DefaultMutableTreeNode, Analyzer> analyzers;
    private final DefaultMutableTreeNode root;
    private final boolean writeToDB;
    private final String region;
    private String description;
    private long totalTrips = 0;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public GeneralAnalyzer(DefaultMutableTreeNode root, boolean writeToDB) {
        this(root, writeToDB, "");
    }

    /**
     * @param root      must be the root of a tree of {@link AnalyzerCollection}s as
     *                  provided by {@link TUMControlGeneral}.
     * @param writeToDB if <code>true</code>, it will trigger a
     *                  {@link GeneralDatabaseSummaryExport} on finish.
     * @param region    only used for the {@link GeneralExcelExport} as a flag in the
     *                  header. Defaults to <code>""</code>.
     */

    public GeneralAnalyzer(DefaultMutableTreeNode root, boolean writeToDB, String region) {
        this.root = root;
        this.writeToDB = writeToDB;
        this.analyzers = buildAnalyzerList();
        this.region = region;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {

        this.pcs.addPropertyChangeListener(listener);
    }

    private HashMap<DefaultMutableTreeNode, Analyzer> buildAnalyzerList() {
        HashMap<DefaultMutableTreeNode, Analyzer> result = new HashMap<>();
        Enumeration en = root.depthFirstEnumeration();
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
            if (node.isLeaf()) {

                Object[] oPath = node.getUserObjectPath();
                ArrayList<AnalyzerBase> analyzerPath = new ArrayList<>();
                for (Object o : oPath) {
                    analyzerPath.addAll(Arrays.asList(((AnalyzerCollection) o).getAnalyzers()));
                }
                result.put(node, new Analyzer(analyzerPath.toArray(new AnalyzerBase[0])));
            }
        }

        if (writeToDB) {// add the calibration results analyzer
            ModeAnalyzer mo = new ModeAnalyzer();
            TripIntentionAnalyzer ti = new TripIntentionAnalyzer();
            DefaultDistanceCategoryAnalyzer dc = new DefaultDistanceCategoryAnalyzer();
            PersonGroupAnalyzer pg = new PersonGroupAnalyzer(mo, ti, dc);

            Analyzer analyzer = new Analyzer(mo, pg, ti, dc);
            result.put(null, analyzer);
        }

        return result;
    }

    @Override
    public boolean finish() throws BadLocationException {

        isProcessing = false;
        if (null != console) {
            console.insertString(console.getLength(),
                    "Region Auswertung beendet. Ergebnisse werden nach " + outputPath + " exportiert\n", null);
        }

        GeneralExcelExport excelExport = new GeneralExcelExport(this);
        boolean excelSuccess = false;
        try {
            if (!outputPath.endsWith("/")) {
                outputPath += "/";
            }
            excelExport.writeAnalysis(outputPath);
            excelSuccess = true;

        } catch (IOException | WriteException e) {
            e.printStackTrace();
        } finally {
            if (excelSuccess) {
                if (null != console) {
                    console.insertString(console.getLength(), "Excel export successful.\n", null);
                }
            } else {
                if (null != console) {
                    console.insertString(console.getLength(), "Excel export failed.\n", null);
                } else {
                    System.err.println("Excel export failed.\n");
                }
            }
        }

        if (writeToDB) {
            boolean dbSuccess = false;
            try {
                GeneralDatabaseSummaryExport dbExport = new GeneralDatabaseSummaryExport(analyzers.get(null),
                        sourceSeperator);
                dbSuccess = dbExport.writeSummary();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                if (dbSuccess) {
                    if (null != console) {
                        console.insertString(console.getLength(), "DB export successful.\n", null);
                    }
                } else {
                    if (null != console) {
                        console.insertString(console.getLength(), "DB export failed.\n", null);
                    } else {
                        System.err.println("DB export failed.\n");
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean finish(File exportfile) throws BadLocationException {

        isProcessing = false;

        GeneralExcelExport excelExport = new GeneralExcelExport(this);
        boolean excelSuccess = false;
        try {
            excelSuccess = excelExport.writeAnalysis(exportfile);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (excelSuccess) {
                if (null != console) {
                    console.insertString(console.getLength(), "Excel export successful.\n", null);
                }
            } else {
                if (null != console) {
                    console.insertString(console.getLength(), "Excel export failed.\n", null);
                } else {
                    System.err.println("Excel export failed.\n");
                }
            }
        }

        return excelSuccess;
    }

    public HashMap<DefaultMutableTreeNode, Analyzer> getAnalyzers() {
        return analyzers;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the total number of added trips.
     */
    public long getNumberTrips() {
        return totalTrips;
    }

    /**
     * @return the region of the analysis given at construction time. May be
     * empty (but not <code>null</code>).
     */
    public String getRegion() {
        return region;
    }

    public DefaultMutableTreeNode getRoot() {
        return this.root;
    }

    /**
     * @return the accumulated sources of all added trips.
     */
    public String getSource() {
        StringBuilder source = new StringBuilder();
        for (String s : sources) {
            source.append(sourceSeperator).append(s);
        }
        return source.toString().replaceFirst(sourceSeperator, "");
    }

    @Override
    public boolean init(String outputPath, StyledDocument console, boolean clearSources) throws BadLocationException {
        isProcessing = true;
        this.console = console;
        this.outputPath = outputPath;
        if (clearSources) this.sources.clear();
        if (null != console) console.insertString(console.getLength(), "Region Analysis started.\n", null);
        return true;
    }

    /**
     * Used by the {@link IntegratedTUM}
     *
     * @param clearSources
     * @return
     */
    @Override
    public boolean init(boolean clearSources) {
        isProcessing = true;
        if (clearSources) this.sources.clear();
        return true;
    }

    @Override
    public boolean prepare(String source, TapasTrip trip, TPS_ParameterClass parameterClass) {

        try {
            if (sources.add(source) && null != console) {
                console.insertString(console.getLength(), "The following source was added: " + source + "\n", null);
            }

            totalTrips++;
            for (Analyzer a : analyzers.values()) {
                a.addTrip(trip);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {

        this.pcs.removePropertyChangeListener(listener);
    }
}
