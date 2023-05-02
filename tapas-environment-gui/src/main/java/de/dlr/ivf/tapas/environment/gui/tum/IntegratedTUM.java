/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.tum;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.analyzer.tum.StandardTUM;
import de.dlr.ivf.tapas.environment.gui.util.MultilanguageSupport;
import de.dlr.ivf.tapas.parameter.TPS_ParameterClass;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.tum.databaseConnector.DBTripReader;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.AnalyzerBase;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general.AnalyzerCollection;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general.GeneralAnalyzer;
import de.dlr.ivf.tapas.util.constants.TPS_SettlementSystem;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * <p><strong>This class handles the integrated TUM-export. Additionally it extends {@link SwingWorker}</strong><p>
 * <p><strong>Input:</strong> since the constructor doesn't take any arguments this class gets its input through an {@link ITumInterface} interface that <strong>must</strong> be passed by the {@link #setInput(ITumInterface)} method.</p>
 * <strong>Work flow:</strong>
 * <ul>
 * 	<li>first a trip table will be created after {@link DBTripReader} constructor's call.</li>
 * 	<li>then you can access every {@link TapasTrip} by its implemented {@link Iterator} calling {@link DBTripReader#getIterator()}.</li>
 * 	<li>every single trip can be passed as argument to {@link GeneralAnalyzer#prepare(String, TapasTrip, TPS_ParameterClass)} for
 * 	evaluation.</li>
 * 	<li>finally an excel-export will be done by invoking {@link GeneralAnalyzer#finish(File)}
 * </ul>
 * <p><strong>Note:</strong> file handling is done outside of this class for example in a customized
 * {@link JFileChooser} like SimulationMonitor#tumFileChooser(int).</p>
 * <p><strong>Thrown exceptions</strong></p>
 *
 * @author sche_ai
 */


@SuppressWarnings("rawtypes")
public abstract class IntegratedTUM extends SwingWorker<Void, String> {

    /**
     * constant strings that influence console behavior and styles in {@link #process(List)}
     */
    private final String fail = "FAIL";
    private final String success = "SUCCESS";
    private final String abort = "ABORTED";
    private final String time = "TIME";
    private final String nextsim = "CONTINUE";
    private final String summary = "SUMMARY";
    private final String totalsummary = "TOTALSUMMARY";
    private final String header = "TUM Export started...";
    /**
     * input parameter for the {@link GeneralAnalyzer}
     */
    protected DefaultMutableTreeNode root = new DefaultMutableTreeNode(new AnalyzerCollection("ROOT"));
    /**
     * these variables will hold values to represent intermediate results
     */
    private long starttime;
    private long absstarttime; //absolute start time
    private long elapsedtime;
    private long currenttripcnt;
    private long totaltrips;

    private StyledDocument console;
    private File[] exportfiles;
    private String[] simulations;
    /**
     * instance variable for trip reader
     */
    private DBTripReader tripReader;
    /**
     * contains all Analyzers
     */
    private ArrayList<AnalyzerBase> baseAnalyzerList;
    /**
     * getting set by {@link IntegratedTUM#tripReader}'s {@link PropertyChangeListener}
     */
    private boolean triptablesuccess = false;


    /**
     * - fill a list with all available analyzers<br />
     * - build a collection containing {@link AnalyzerCollection}s and its {@link AnalyzerBase} objects
     */
    public IntegratedTUM() {
        defaultAnalyzerList();
        buildAnalyzerList();
    }

    private static String getTime() {

        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        return dateFormat.format(Calendar.getInstance().getTime());

    }

    /**
     * <p>implementing this method will allow you to set up your own {@link AnalyzerBase} combinations wrapped into {@link AnalyzerCollection}s.</p>
     * <p><strong>Note:</strong> this method doesn't take any parameters, instead {@link IntegratedTUM#root} will be filled and can be passed
     * to {@link GeneralAnalyzer#GeneralAnalyzer(DefaultMutableTreeNode, boolean, String)}</p>
     * <p>As an example check {@link StandardTUM} that extends this class.</p>
     */
    public abstract void buildAnalyzerList();

    /**
     * fills up {@link #baseAnalyzerList}
     */
    private void defaultAnalyzerList() {

        baseAnalyzerList = new ArrayList<>();

        baseAnalyzerList.add(new RegionCodeAnalyzer());
        baseAnalyzerList.add(new DefaultDistanceCategoryAnalyzer());
        baseAnalyzerList.add(new EquiFiveDistanceCategoryAnalyzer());
        baseAnalyzerList.add(new EquiTenDistanceCategoryAnalyzer());
        baseAnalyzerList.add(new ModeAnalyzer());
        baseAnalyzerList.add(new PersonGroupAnalyzer());
        baseAnalyzerList.add(new TravelTimeAnalyzer());
        baseAnalyzerList.add(new TripIntentionAnalyzer());
    }

    @Override
    protected Void doInBackground() throws Exception {

        absstarttime = System.nanoTime();

        publish(header);

        int simcount = this.simulations.length;

        for (int i = 0; i < simcount; i++) {

            this.currenttripcnt = 0;
            starttime = System.nanoTime();

            this.firePropertyChange("intermediate", null,
                    null); //reset progress bars in {@link SimulationMonitor} after one simulation is finished

            if (isCancelled()) //avoids to keep publishing after SwingWorker was cancelled because done() gets called before doInBackground() gets cancelled
                break;

            publish(time, MultilanguageSupport.getString("TUM_CONSOLE_PROCESS") + " " + (i + 1) + " " +
                    MultilanguageSupport.getString("TUM_CONSOLE_OF") + " " + simcount + ": '" + simulations[i] + "'");
            publish(time, MultilanguageSupport.getString("TUM_CONSOLE_TRIPTABLE") + " ");

            tripReader = new DBTripReader(simulations[i], null, TPS_SettlementSystem.TPS_SettlementSystemType.FORDCP, null, this.connection);

            //handle trip table changes coming from {@link DBTripReader}
            tripReader.addPropertyChangeListener(evt -> {
                if ("triptable".equals(evt.getPropertyName()))

                    if ((boolean) evt.getNewValue() && !isCancelled()) {
                        publish(success);
                        this.triptablesuccess = true;
                        this.firePropertyChange("triptable", null, true);
                    } else {
                        if (!isCancelled()) publish(fail,
                                MultilanguageSupport.getString("TUM_CONSOLE_TRIPTABLE_ERROR"));

                        this.triptablesuccess = false;
                        this.firePropertyChange("triptable", null, false);
                    }

            });

            GeneralAnalyzer ga = new GeneralAnalyzer(root, false, tripReader.region);
            ga.setDescription(tripReader.description);


            try {
                if (tripReader.getIterator().hasNext() && this.triptablesuccess) {

                    publish(time, MultilanguageSupport.getString("TUM_CONSOLE_REGIONANALYSIS") + " ");
                    ga.init(true);

                    this.totaltrips = tripReader.getTotal();

                    while (tripReader.getIterator().hasNext() && !isCancelled()) {

                        this.currenttripcnt++;
                        setProgress((int) ((float) this.currenttripcnt / this.totaltrips * 100));
                        if (!ga.prepare(simulations[i], tripReader.getIterator().next(),
                                this.connection.getParameters()))
                            break; // the very first trip that can't be evaluated will stop further evaluation on the remaining trips
                    }

                    // all trips have been evaluated -> success
                    if (this.currenttripcnt == this.totaltrips) {
                        publish(success);
                    } else {
                        if (!isCancelled()) {
                            publish(fail,
                                    MultilanguageSupport.getString("TUM_CONSOLE_TRIP_ERROR") + this.currenttripcnt +
                                            "... " + MultilanguageSupport.getString("TUM_CONSOLE_SKIPEXCEL"), summary);

                            if (i < simcount - 1) publish(nextsim);
                        }
                        tripReader.close();
                        continue; // if a problem appeared during region analysis -> continue with next simulation
                    }
                } else {
                    if (!isCancelled()) {

                        if (i < simcount - 1) publish(nextsim);

                        tripReader.close();
                        continue; // if for any reason a trip table hasn't successfully been created for a given simulation -> continue with next simulation
                    }
                }

                tripReader.close();

                if (!isCancelled()) {
                    publish(time, MultilanguageSupport.getString("TUM_CONSOLE_EXCEL") + " " +
                            exportfiles[i].getAbsolutePath() + "... ");

                    if (ga.finish(exportfiles[i])) publish(success, summary);
                    else {
                        publish(fail,
                                "Please check whether the Excel File you are trying to overwrite is currently NOT in use.",
                                summary);
                        if (i < simcount - 1) publish(nextsim);
                        continue;  // if excel export for an analyzed simulation fails -> continue with next simulation
                    }
                }
            } catch (Exception e) {

                if (!isCancelled()) {
                    publish(fail, e.toString());
                    tripReader.close();
                    publish(summary);
                }

                continue;  // any thrown exception unless this {@link IntegratedTUM} was cancelled on purpose -> continue with next simulation
            }

            Thread.sleep(
                    1000);    // due to concurrency it might happen while process() is running asynchronously on the EDT that trip counts are already reset
            // in the doInBackground() which could show wrong information. A short sleep on the Thread solves that problem.
        }

        return null;
    }

    @Override
    protected void done() {

        if (isCancelled()) {
            publish(abort, summary, totalsummary);
            tripReader.cancel();
        } else publish(totalsummary);
    }

    /**
     * @param c
     * @return {@link AnalyzerBase} object belonging to {@link Categories}
     * @throws {@link IllegalArgumentException} manually thrown in case there is no {@link AnalyzerBase} object corresponding to a {@link Categories}
     */
    protected AnalyzerBase getAnalyzer(Categories c) {

        return baseAnalyzerList.stream().filter(analyzer -> analyzer.getCategories().equals(c)).findFirst().orElseThrow(
                IllegalArgumentException::new);
    }

    /**
     * @return the current trip count that already has been processed
     */
    public long getCurrentTripCnt() {
        return this.currenttripcnt;
    }

    /**
     * @return total trip count
     */
    public long getTotalTrips() {

        return this.totaltrips;
    }

    @Override
    protected void process(List<String> chunks) {

        for (String s : chunks) {
            try {
                switch (s) {
                    case success:
                        this.console.insertString(this.console.getLength(),
                                MultilanguageSupport.getString("TUM_CONSOLE_SUCCESS"), this.console.getStyle("green"));
                        break;
                    case fail:
                        this.console.insertString(this.console.getLength(),
                                MultilanguageSupport.getString("TUM_CONSOLE_FAIL"), this.console.getStyle("redbold"));
                        break;
                    case abort:
                        this.console.insertString(this.console.getLength(),
                                MultilanguageSupport.getString("TUM_CONSOLE_ABORT"), this.console.getStyle("orange"));
                        break;
                    case time:
                        this.console.insertString(this.console.getLength(), "\n" + getTime() + " --> ", null);
                        break;
                    case nextsim:
                        this.console.insertString(this.console.getLength(),
                                "\n" + MultilanguageSupport.getString("TUM_CONSOLE_NEXTSIM"), null);
                        break;
                    case summary:
                        this.console.insertString(this.console.getLength(), summary(),
                                this.console.getStyle("summary"));
                        break;
                    case totalsummary:
                        this.console.insertString(this.console.getLength(), totalSummary(),
                                this.console.getStyle("totalsummary"));
                        break;
                    case header:
                        if (this.console.getLength() > 0) this.console.insertString(this.console.getLength(),
                                "\n\n" + s + "\n", this.console.getStyle("blackunderline"));
                        else this.console.insertString(this.console.getLength(), s + "\n",
                                this.console.getStyle("blackunderline"));
                        break;
                    default:
                        if (this.console.getText(this.console.getLength() - 4, 4).equals("FAIL"))
                            this.console.insertString(this.console.getLength(), " (" + s + ")",
                                    this.console.getStyle("blackitalicsmall"));
                        else this.console.insertString(this.console.getLength(), s, this.console.getStyle("standard"));
                        break;
                }
            } catch (BadLocationException e) {

                e.printStackTrace();
            }
        }

    }

    /**
     * @param input transfer
     */
    public boolean setInput(ITumInterface input) {

        try {
            this.exportfiles = input.getExportFiles();
            this.console = input.getConsole();
            this.connection = input.getConnection();
            this.simulations = input.getSimKeys();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * @return string containing information about elapsed time and processed trips of an analyzed simulation
     */
    private String summary() {
        elapsedtime = System.nanoTime() - this.starttime;
        return "\n" + MultilanguageSupport.getString("TUM_CONSOLE_PARTEXECTIME") + " " + String.format("%dm %ds",
                TimeUnit.NANOSECONDS.toMinutes(elapsedtime), TimeUnit.NANOSECONDS.toSeconds(elapsedtime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(elapsedtime))) + " | " +
                MultilanguageSupport.getString("TUM_CONSOLE_TRIPS") + " " + this.currenttripcnt + "/" +
                this.totaltrips + "\n";

    }

    /**
     * @return string containing information about overall elapsed time
     */
    private String totalSummary() {

        elapsedtime = System.nanoTime() - absstarttime;
        return "\n" + MultilanguageSupport.getString("TUM_CONSOLE_TOTALEXECTIME") + " " + String.format("%dm %ds",
                TimeUnit.NANOSECONDS.toMinutes(elapsedtime), TimeUnit.NANOSECONDS.toSeconds(elapsedtime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(elapsedtime)));

    }


}
