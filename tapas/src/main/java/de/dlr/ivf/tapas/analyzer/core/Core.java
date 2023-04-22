/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.core;

import de.dlr.ivf.tapas.analyzer.core.CoreInputInterface.Module;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTripReader;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

/**
 * The main thread responsible for the processing of selected methods
 *
 * @author Marco
 */
public class Core extends Thread {

    private static long systemTime;
    // modules to be process
    private final Collection<CoreProcessInterface> modules = new HashSet<>();
    // true if the process shall be cancelled
    private boolean isCancelled;
    // path to the output files
    private String outputPath = "";
    private TapasTripReader tripReader;
    // information outputs
    private final JTextPane cons = new JTextPane();
    private StyledDocument console = cons.getStyledDocument();
    private final TPS_ParameterClass parameterClass;

    public Core(String name, TPS_ParameterClass parameterClass) {
        super(name);
        this.isCancelled = false;
        this.parameterClass = parameterClass;
    }

    /**
     * return runtime in milliseconds
     *
     * @return
     */
    public static String getRuntime() {
        return "(" + (System.nanoTime() - systemTime) / 1000000 + "ms)";
    }

    /**
     * Cancel all modules
     */
    public void cancel() {
        this.isCancelled = true;
        for (CoreProcessInterface cii : modules) {
            cii.cancelFinish();
        }

        super.interrupt();
    }

    public void run() {

        setStartTime(System.nanoTime());
        try {
            console.insertString(console.getLength(),
                    "Core [" + Core.currentThread().getId() + "]" + getRuntime() + ": Gestartet \n", null);
            boolean first = true;
            // Only if at least one module was selected
            if (modules.size() > 0 && tripReader.getIterator().hasNext()) {

                console.insertString(console.getLength(),
                        "Core [" + Core.currentThread().getId() + "]: Lese Tripdateien \n", null);

                // Initial Execution of Modules
                if (!isCancelled) {
                    console.insertString(console.getLength(),
                            "Core [" + Core.currentThread().getId() + "]" + getRuntime() +
                                    ": Starte it. Auswertungsmodule \n", null);

                    for (CoreProcessInterface cii : modules) {
                        cii.init(outputPath, console, first);
                    }
                }
                // Iterative Execution of Modules
                int countTrips = 0;
                while (tripReader.getIterator().hasNext() && !isCancelled) {
                    TapasTrip tt = tripReader.getIterator().next();
                    for (CoreProcessInterface cii : modules) {
                        if ((++countTrips) % 250000 == 0) {
                            console.insertString(console.getLength(),
                                    "Core [" + Core.currentThread().getId() + "]" + getRuntime() + ": " + (countTrips) +
                                            " Trips verarbeitet\n", null);
                        }
                        cii.prepare(tripReader.getSource(), tt, this.parameterClass);
                    }
                    // System.out.println("ID: "+converter.getTrip().getIdPers()+" Job: "+converter.getTrip().getJob());
                }

                // Final Execution of Modules
                if (!isCancelled) {
                    console.insertString(console.getLength(),
                            "Core [" + Core.currentThread().getId() + "]" + getRuntime() +
                                    ": Starte fin. Auswertungsmodule \n", null);
                    for (CoreProcessInterface cii : modules) {
                        Thread tii = new Thread(cii,
                                "Thread for ControlInputInterface implementation: " + cii.getClass().getSimpleName());
                        tii.start();
                    }
                }
            } else {
                console.insertString(console.getLength(),
                        "Core [" + Core.currentThread().getId() + "]" + getRuntime() + ": Beendet \n", null);
                String infoString = "";
                // TODO check error message
                if (!tripReader.getIterator().hasNext()) infoString = infoString + "Kein Tripfile angegeben.\n";
                if (modules.size() == 0) infoString = infoString + "Keine Module angegeben.\n";
                JOptionPane.showMessageDialog(new JPanel(), infoString, "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * übergibt die benötigten Inputdaten an {@link Core}. Die Methode holt sich
     * alles was es brauch aus dem {@link CoreInputInterface}.
     *
     * @param input
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void setCoreInput(CoreInputInterface input) throws IOException, ClassNotFoundException, SQLException {
        tripReader = input.getTripReader();
        outputPath = input.getOutputPath();
        console = input.getConsole();
        this.modules.clear();
        for (Module module : Module.values()) {
            if (input.isActive(module)) {
                this.modules.add(input.getInterface(module).getProcessImpl());
            }
        }

    }

    private void setStartTime(long st) {
        Core.systemTime = st;
    }

}
