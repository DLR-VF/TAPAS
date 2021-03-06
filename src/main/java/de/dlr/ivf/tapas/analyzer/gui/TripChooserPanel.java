/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.gui;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTripReader;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class TripChooserPanel extends JPanel {

    // TODO fix minimum size

    private static final long serialVersionUID = 7301753761411077441L;

    @SuppressWarnings("unused")
    private final String title;
    private final StyledDocument console;
    // private JComboBox<String> cbChooser;
    private final DBSimulationChooserPanel dbChooser;
    private final FilesChooser filesChooser;

    public TripChooserPanel(String title, String initSimulation, StyledDocument console) {
        this.console = console;
        this.title = title;

        setLayout(new GridLayout(1, 2));

        dbChooser = new DBSimulationChooserPanel("DB - Triptables", initSimulation, console);
        add(dbChooser);
        filesChooser = new FilesChooser("Files");
        add(filesChooser);
    }

    public TapasTripReader getTripReader() {

        if (dbChooser.isReady() && filesChooser.isReady()) {
            return null;
        } else if (dbChooser.isReady()) {
            return dbChooser.createDBTripReader();
        } else if (filesChooser.isReady()) {
            return filesChooser.createFileTripReader(console);
        } else {
            return null;
        }

    }

    public boolean isReady() {
        if (dbChooser.isReady() && filesChooser.isReady()) {
            JOptionPane.showMessageDialog(this, "Please choose only one input mode.");
            return false;
        } else if (dbChooser.isReady()) {
            return true;
        } else if (filesChooser.isReady()) {
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Please provide some input.");
            return false;
        }
    }
}
