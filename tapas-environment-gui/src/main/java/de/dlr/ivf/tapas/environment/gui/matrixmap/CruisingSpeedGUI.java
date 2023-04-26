/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.dlr.ivf.tapas.environment.gui.matrixmap;

import de.dlr.ivf.tapas.util.PropertyReader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Reyn
 */
public class CruisingSpeedGUI extends JFrame {

    /**
     *
     */
    private static final long serialVersionUID = 8820976812627995474L;
    private Properties properties = null;
    private boolean changes = false;
    private JButton bStart;
    private JCheckBox cbActivateValidation;
    private JCheckBox cbBikeDist;
    private JCheckBox cbBikeTT;
    private JCheckBox cbMIVDist;
    private JCheckBox cbMIVTT;
    private JCheckBox cbPTDist;
    private JCheckBox cbPTTT;
    @SuppressWarnings("rawtypes")
    private JComboBox cbTerrain;
    private JCheckBox cbTop3;
    private JCheckBox cbValBikeDist;
    private JCheckBox cbValBikeTT;
    private JCheckBox cbValMIVDist;
    private JCheckBox cbValMIVTT;
    private JCheckBox cbValPTDist;
    private JCheckBox cbValPTTT;
    private JCheckBox cbValWalkDist;
    private JCheckBox cbValWalkTT;
    private JCheckBox cbWalkDist;
    private JCheckBox cbWalkTT;
    private JCheckBox cbZValue;
    private JLabel lInfo;
    private JPanel pGroupValidation;
    private JPanel pTTPara;
    private JPanel pValidation;
    private JTextField tfBikeDist;
    private JTextField tfBikeTimeApproach;
    private JTextField tfBikeTimeDeparture;
    private JTextField tfMIVDist;
    private JTextField tfMIVTimeApproach;
    private JTextField tfMIVTimeDeparture;
    private JTextField tfMatrixTable;
    private JTextField tfPTDist;
    private JTextField tfPTTimeApproach;
    private JTextField tfPTTimeDeparture;
    private JTextField tfPath;
    private JTextField tfRecordName;
    private JTextField tfTazTable;
    private JTextField tfValBikeDist;
    private JTextField tfValBikeTT;
    private JTextField tfValMIVDist;
    private JTextField tfValMIVTT;
    private JTextField tfValPTDist;
    private JTextField tfValPTTT;
    private JTextField tfValWalkDist;
    private JTextField tfValWalkTT;
    private JTextField tfWalkDist;
    private JTextField tfWalkTimeApproach;
    private JTextField tfWalkTimeDeparture;

    /**
     * Creates new form CruisingSpeedGUI
     */
    public CruisingSpeedGUI() {
        try {
            this.properties = PropertyReader.getProperties(CruisingSpeed.propertyFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        initComponents();

        // monitoring of change-status

        tfTazTable.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfTazTable, "tazTable"));
        tfMatrixTable.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfMatrixTable, "matricesTable"));
        tfRecordName.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfRecordName, "record"));

        /* ************* velocity ************* */
        tfWalkDist.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfWalkDist, "speedWalk"));
        tfBikeDist.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfBikeDist, "speedBike"));
        tfPTDist.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfPTDist, "speedPT"));
        tfMIVDist.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfMIVDist, "speedMIV"));

        /* ************* travel time modyfier ************* */
        tfWalkTimeApproach.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfWalkTimeApproach, "timeWalkApproach"));
        tfBikeTimeApproach.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfBikeTimeApproach, "timeBikeApproach"));
        tfPTTimeApproach.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfPTTimeApproach, "timePTApproach"));
        tfMIVTimeApproach.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfMIVTimeApproach, "timeMIVApproach"));

        tfWalkTimeDeparture.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfWalkTimeDeparture, "timeWalkDeparture"));
        tfBikeTimeDeparture.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfBikeTimeDeparture, "timeBikeDeparture"));
        tfPTTimeDeparture.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfPTTimeDeparture, "timePTDeparture"));
        tfMIVTimeDeparture.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfMIVTimeDeparture, "timeMIVDeparture"));

        /* ************* validation ************* */
        tfValWalkDist.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfValWalkDist, "validationWalkDist"));
        tfValBikeDist.getDocument().addDocumentListener(
                new TextFieldDokumentlistener(tfValBikeDist, "validationBikeDist"));
        tfValPTDist.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfValPTDist, "referencePTDist"));
        tfValMIVDist.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfValMIVDist, "referenceMIVDist"));

        tfValWalkTT.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfValWalkTT, "referenceWalkTT"));
        tfValBikeTT.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfValBikeTT, "referenceBikeTT"));
        tfValPTTT.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfValPTTT, "referencePTTT"));
        tfValMIVTT.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfValMIVTT, "referenceMIVTT"));

        tfPath.getDocument().addDocumentListener(new TextFieldDokumentlistener(tfPath, "path"));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus." + "NimbusLookAndFeel");
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            ex.getMessage();
        }
        CruisingSpeedGUI gui = new CruisingSpeedGUI();
        gui.start();
        gui.setLocationRelativeTo(null);
        gui.setVisible(true);
    }

    private void bCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_bCancelActionPerformed
    {//GEN-HEADEREND:event_bCancelActionPerformed
        close();
    }//GEN-LAST:event_bCancelActionPerformed

    private void bPathActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_bPathActionPerformed
    {//GEN-HEADEREND:event_bPathActionPerformed
        String titel = "Vaildierungspfad";
        JFileChooser fileChooser = new JFileChooser(titel);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        fileChooser.setFileFilter(new FolderFileFilter());

        // u.a notwendig, damit das ausgewählte Direktory als
        // Ordnername angezeigt wird
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        //showDialog liefert einen int-Wert
        int abbrechen = fileChooser.showDialog(null, "Auswählen");
        if (abbrechen != JFileChooser.CANCEL_OPTION) {
            File file = fileChooser.getSelectedFile();
            tfPath.setText(file.getAbsolutePath());
            changes = true;
        }
    }//GEN-LAST:event_bPathActionPerformed

    private void bSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_bSaveActionPerformed
    {//GEN-HEADEREND:event_bSaveActionPerformed
        if (changes) {
            savePropertieFile();
            changes = false;
            JOptionPane.showMessageDialog(this, "Die Daten wurden gespeichert.");
        } else {
            JOptionPane.showMessageDialog(this, "Die Daten wurden nicht verändert, speichern nicht notwengig.");
        }

    }//GEN-LAST:event_bSaveActionPerformed

    private void bStartActionPerformed(java.awt.event.ActionEvent evt) {
        if (changes) savePropertieFile();
        CruisingSpeed cs = new CruisingSpeed();
        CruisingSpeed.setProperties(properties);
        cs.start(null);
        changes = false;
        JOptionPane.showMessageDialog(this, "Die Daten wurden generiert");
    }

    private void cbActivateValidationItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbActivateValidationItemStateChanged
    {//GEN-HEADEREND:event_cbActivateValidationItemStateChanged
        JCheckBox source = (JCheckBox) evt.getSource();
        enableCheckboxComponents(evt, pGroupValidation);
        source.setEnabled(true);

        // if activated  enable/disable dependend checkboxes and textfields
        if (source.isSelected()) {
            enableCheckboxes(cbValWalkDist, tfValWalkDist);
            enableCheckboxes(cbValBikeDist, tfValBikeDist);
            enableCheckboxes(cbValPTDist, tfValPTDist);
            enableCheckboxes(cbValMIVDist, tfValMIVDist);

            // traveltime (TT)
            enableCheckboxes(cbValWalkTT, tfValWalkTT);
            enableCheckboxes(cbValBikeTT, tfValBikeTT);
            enableCheckboxes(cbValPTTT, tfValPTTT);
            enableCheckboxes(cbValMIVTT, tfValMIVTT);
        }

    }//GEN-LAST:event_cbActivateValidationItemStateChanged

    private void cbBikeDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbBikeDistItemStateChanged
    {//GEN-HEADEREND:event_cbBikeDistItemStateChanged
        //changes = changes | distanceSelected(Modus.BIKE);
        distanceSelected(Modus.BIKE);
        changes = true;
    }//GEN-LAST:event_cbBikeDistItemStateChanged

    private void cbBikeTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbBikeTTItemStateChanged
    {//GEN-HEADEREND:event_cbBikeTTItemStateChanged
        //changes = changes | distanceSelected(Modus.BIKE);
        distanceSelected(Modus.BIKE);
        changes = true;
    }//GEN-LAST:event_cbBikeTTItemStateChanged

    private void cbMIVDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbMIVDistItemStateChanged
    {//GEN-HEADEREND:event_cbMIVDistItemStateChanged
        //changes = changes | distanceSelected(Modus.MIV);
        distanceSelected(Modus.MIV);
        changes = true;
    }//GEN-LAST:event_cbMIVDistItemStateChanged

    private void cbMIVTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbMIVTTItemStateChanged
    {//GEN-HEADEREND:event_cbMIVTTItemStateChanged
        //changes = changes | distanceSelected(Modus.MIV);
        distanceSelected(Modus.MIV);
        changes = true;
    }//GEN-LAST:event_cbMIVTTItemStateChanged

    private void cbPTDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbPTDistItemStateChanged
    {//GEN-HEADEREND:event_cbPTDistItemStateChanged
        //changes = changes | distanceSelected(Modus.PT);
        distanceSelected(Modus.PT);
        changes = true;
    }//GEN-LAST:event_cbPTDistItemStateChanged

    private void cbPTTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbPTTTItemStateChanged
    {//GEN-HEADEREND:event_cbPTTTItemStateChanged
        //changes = changes | distanceSelected(Modus.PT);
        distanceSelected(Modus.PT);
        changes = true;
    }//GEN-LAST:event_cbPTTTItemStateChanged

    private void cbTerrainItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbTerrainItemStateChanged
    {//GEN-HEADEREND:event_cbTerrainItemStateChanged
        if (!cbTerrain.getSelectedItem().toString().equals(properties.getProperty("terrain"))) changes = true;
    }//GEN-LAST:event_cbTerrainItemStateChanged

    private void cbTop3ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbTop3ItemStateChanged
    {//GEN-HEADEREND:event_cbTop3ItemStateChanged
        changes = true;
    }//GEN-LAST:event_cbTop3ItemStateChanged

    private void cbValBikeDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValBikeDistItemStateChanged
    {//GEN-HEADEREND:event_cbValBikeDistItemStateChanged
        enableCheckboxComponents(evt, tfValBikeDist);
    }//GEN-LAST:event_cbValBikeDistItemStateChanged

    private void cbValBikeTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValBikeTTItemStateChanged
    {//GEN-HEADEREND:event_cbValBikeTTItemStateChanged
        enableCheckboxComponents(evt, tfValBikeTT);
    }//GEN-LAST:event_cbValBikeTTItemStateChanged

    private void cbValMIVDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValMIVDistItemStateChanged
    {//GEN-HEADEREND:event_cbValMIVDistItemStateChanged
        enableCheckboxComponents(evt, tfValMIVDist);
    }//GEN-LAST:event_cbValMIVDistItemStateChanged

    private void cbValMIVTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValMIVTTItemStateChanged
    {//GEN-HEADEREND:event_cbValMIVTTItemStateChanged
        enableCheckboxComponents(evt, tfValMIVTT);
    }//GEN-LAST:event_cbValMIVTTItemStateChanged

    private void cbValPTDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValPTDistItemStateChanged
    {//GEN-HEADEREND:event_cbValPTDistItemStateChanged
        enableCheckboxComponents(evt, tfValPTDist);
    }//GEN-LAST:event_cbValPTDistItemStateChanged

    private void cbValPTTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValPTTTItemStateChanged
    {//GEN-HEADEREND:event_cbValPTTTItemStateChanged
        enableCheckboxComponents(evt, tfValPTTT);
    }//GEN-LAST:event_cbValPTTTItemStateChanged

    private void cbValWalkDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValWalkDistItemStateChanged
    {//GEN-HEADEREND:event_cbValWalkDistItemStateChanged
        enableCheckboxComponents(evt, tfValWalkDist);
    }//GEN-LAST:event_cbValWalkDistItemStateChanged

    private void cbValWalkTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValWalkTTItemStateChanged
    {//GEN-HEADEREND:event_cbValWalkTTItemStateChanged
        enableCheckboxComponents(evt, tfValWalkTT);
    }//GEN-LAST:event_cbValWalkTTItemStateChanged

    private void cbWalkDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbWalkDistItemStateChanged
    {//GEN-HEADEREND:event_cbWalkDistItemStateChanged
        //changes = changes | distanceSelected(Modus.WALK);
        distanceSelected(Modus.WALK);
        changes = true;
    }//GEN-LAST:event_cbWalkDistItemStateChanged

    private void cbWalkTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbWalkTTItemStateChanged
    {//GEN-HEADEREND:event_cbWalkTTItemStateChanged
        //changes = changes | distanceSelected(Modus.WALK);
        distanceSelected(Modus.WALK);
        changes = true;
    }//GEN-LAST:event_cbWalkTTItemStateChanged

    private void cbZValueItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbZValueItemStateChanged
    {//GEN-HEADEREND:event_cbZValueItemStateChanged
        changes = true;
    }//GEN-LAST:event_cbZValueItemStateChanged

    private void checkMandantoryFields() {
        enableComponents(bStart, !tfTazTable.getText().isEmpty() && !tfMatrixTable.getText().isEmpty() &&
                !tfRecordName.getText().isEmpty());

    }

    private void close() {
        int choice;
        if (changes) {
            choice = JOptionPane.showConfirmDialog(this, "Wollen Sie die Änderungen speichern?", "Speichern",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.YES_OPTION) savePropertieFile();
        }
        System.exit(0);
    }

    private boolean distanceSelected(Modus modus) {
        switch (modus) {
            case WALK:
                if (!cbWalkDist.isSelected() && cbWalkTT.isSelected()) {
                    cbWalkDist.setSelected(true);
                    lInfo.setText("Die Distanz wird für die Reisezeitberechnung benötigt.");
                    return false;
                }

            case BIKE:
                if (!cbBikeDist.isSelected() && cbBikeTT.isSelected()) {
                    cbBikeDist.setSelected(true);
                    lInfo.setText("Die Distanz wird für die Reisezeitberechnung benötigt.");
                    return false;
                }

            case PT:
                if (!cbPTDist.isSelected() && cbPTTT.isSelected()) {
                    cbPTDist.setSelected(true);
                    lInfo.setText("Die Distanz wird für die Reisezeitberechnung benötigt.");
                    return false;
                }

            case MIV:
                if (!cbMIVDist.isSelected() && cbMIVTT.isSelected()) {
                    cbMIVDist.setSelected(true);
                    lInfo.setText("Die Distanz wird für die Reisezeitberechnung benötigt.");
                    return false;
                }
        }

        enableComponents(pTTPara,
                cbWalkTT.isSelected() || cbBikeTT.isSelected() || cbPTTT.isSelected() || cbMIVTT.isSelected());

        if (!cbWalkDist.isSelected() && !cbBikeDist.isSelected() && !cbPTDist.isSelected() && !cbMIVDist.isSelected()) {
            enableComponents(bStart, false);
            enableComponents(pGroupValidation, false);
        } else {
            enableComponents(bStart, true);
            cbActivateValidation.setEnabled(true);
            if (cbActivateValidation.isSelected()) {
                cbValWalkDist.setEnabled(true);
                cbValBikeDist.setEnabled(true);
                cbValPTDist.setEnabled(true);
                cbValMIVDist.setEnabled(true);

                cbValWalkTT.setEnabled(true);
                cbValBikeTT.setEnabled(true);
                cbValPTTT.setEnabled(true);
                cbValMIVTT.setEnabled(true);

                if (cbValWalkDist.isSelected()) tfValWalkDist.setEnabled(true);
                if (cbValBikeDist.isSelected()) tfValBikeDist.setEnabled(true);
                if (cbValPTDist.isSelected()) tfValPTDist.setEnabled(true);
                if (cbValMIVDist.isSelected()) tfValMIVDist.setEnabled(true);

                if (cbValWalkTT.isSelected()) tfValWalkTT.setEnabled(true);
                if (cbValBikeTT.isSelected()) tfValBikeTT.setEnabled(true);
                if (cbValPTTT.isSelected()) tfValPTTT.setEnabled(true);
                if (cbValMIVTT.isSelected()) tfValMIVTT.setEnabled(true);
            }
            bStart.setEnabled(true);
        }

        lInfo.setText("");
        return true;
    }

    private void enableCheckboxComponents(java.awt.event.ItemEvent evt, Container container) {
        enableComponents(container, ((JCheckBox) evt.getSource()).isSelected());

        changes = true;
    }

    private void enableCheckboxes(JCheckBox caller, JTextField textfield) {
        enableComponents(textfield, caller.isSelected());
        caller.setEnabled(true);
    }

    private void enableComponentOnStart(Container caller, Container container, boolean enable) {
        enableComponents(container, enable);
        caller.setEnabled(true);
    }

    private void enableComponentOnStart(Container caller, Container container, String property) {
        enableComponentOnStart(caller, container, Boolean.parseBoolean(properties.getProperty(property)));
    }
    // End of variables declaration//GEN-END:variables

    private void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();

        if (components.length == 0) container.setEnabled(enable);

        for (Component component : components) {
            component.setEnabled(enable);

            if (component instanceof Container) {
                enableComponents((Container) component, enable);
            }
        }
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing

    /**
     * This method is called from within the constructor to initialize
     * the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bStart = new JButton();
        // Variables declaration - do not modify//GEN-BEGIN:variables
        JButton bCancel = new JButton();
        JButton bSave = new JButton();
        JTabbedPane jTabbedPane1 = new JTabbedPane();
        JPanel pGroupGenerate = new JPanel();
        pTTPara = new JPanel();
        JLabel lWalk = new JLabel();
        JLabel lBike = new JLabel();
        JLabel lPT = new JLabel();
        JLabel lMIV = new JLabel();
        JLabel lSpeed = new JLabel();
        JLabel lTimeAdditionApproach = new JLabel();
        tfWalkDist = new JTextField();
        tfBikeDist = new JTextField();
        tfPTDist = new JTextField();
        tfMIVDist = new JTextField();
        tfWalkTimeApproach = new JTextField();
        tfBikeTimeApproach = new JTextField();
        tfPTTimeApproach = new JTextField();
        tfMIVTimeApproach = new JTextField();
        JLabel lTimeAdditionDeparture = new JLabel();
        tfWalkTimeDeparture = new JTextField();
        tfBikeTimeDeparture = new JTextField();
        tfPTTimeDeparture = new JTextField();
        tfMIVTimeDeparture = new JTextField();
        JLabel jLabel3 = new JLabel();
        JPanel pInput = new JPanel();
        cbTerrain = new JComboBox<Terrain>();
        JLabel lTazTable = new JLabel();
        JLabel lTerrain = new JLabel();
        tfTazTable = new JTextField();
        JLabel lMatrixTable = new JLabel();
        tfMatrixTable = new JTextField();
        JLabel jLabel1 = new JLabel();
        cbZValue = new JCheckBox();
        JLabel jLabel2 = new JLabel();
        tfRecordName = new JTextField();
        JLabel lTop3 = new JLabel();
        cbTop3 = new JCheckBox();
        JPanel pMatrixCalc = new JPanel();
        JLabel lDist = new JLabel();
        JLabel lTT = new JLabel();
        JLabel lWalk1 = new JLabel();
        JLabel lBike1 = new JLabel();
        JLabel lPT1 = new JLabel();
        JLabel lMIV1 = new JLabel();
        cbWalkDist = new JCheckBox();
        cbBikeDist = new JCheckBox();
        cbPTDist = new JCheckBox();
        cbMIVDist = new JCheckBox();
        cbWalkTT = new JCheckBox();
        cbBikeTT = new JCheckBox();
        cbPTTT = new JCheckBox();
        cbMIVTT = new JCheckBox();
        lInfo = new JLabel();
        pGroupValidation = new JPanel();
        pValidation = new JPanel();
        JLabel lPath = new JLabel();
        tfPath = new JTextField();
        JButton bPath = new JButton();
        cbActivateValidation = new JCheckBox();
        JPanel pValDist = new JPanel();
        cbValWalkDist = new JCheckBox();
        cbValBikeDist = new JCheckBox();
        cbValPTDist = new JCheckBox();
        cbValMIVDist = new JCheckBox();
        JLabel lValDistRef = new JLabel();
        tfValWalkDist = new JTextField();
        tfValBikeDist = new JTextField();
        tfValPTDist = new JTextField();
        tfValMIVDist = new JTextField();
        JPanel pValTT = new JPanel();
        cbValWalkTT = new JCheckBox();
        cbValBikeTT = new JCheckBox();
        cbValPTTT = new JCheckBox();
        cbValMIVTT = new JCheckBox();
        JLabel lValTTRef = new JLabel();
        tfValWalkTT = new JTextField();
        tfValBikeTT = new JTextField();
        tfValPTTT = new JTextField();
        tfValMIVTT = new JTextField();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("CruisingSpeed");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        bStart.setText("Starten");
        bStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bStartActionPerformed(evt);
            }
        });
        bCancel.setText("Abbrechen");
        bCancel.addActionListener(this::bCancelActionPerformed);

        bSave.setText("Speichern");
        bSave.addActionListener(this::bSaveActionPerformed);

        jTabbedPane1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTabbedPane1FocusGained(evt);
            }
        });

        pTTPara.setBorder(BorderFactory.createTitledBorder("Reisezeiten-Parameter"));

        lWalk.setHorizontalAlignment(SwingConstants.CENTER);
        lWalk.setText("WALK");

        lBike.setHorizontalAlignment(SwingConstants.CENTER);
        lBike.setText("BIKE");

        lPT.setHorizontalAlignment(SwingConstants.CENTER);
        lPT.setText("PT");

        lMIV.setHorizontalAlignment(SwingConstants.CENTER);
        lMIV.setText("MIV");

        lSpeed.setText("Fahrgeschwindigkeit Vm [km/h]");

        lTimeAdditionApproach.setText("Zugang Zmz [min]");

        tfWalkDist.setHorizontalAlignment(JTextField.RIGHT);
        tfWalkDist.setText(properties.getProperty("speedWalk"));

        tfBikeDist.setHorizontalAlignment(JTextField.RIGHT);
        tfBikeDist.setText(properties.getProperty("speedBike"));

        tfPTDist.setHorizontalAlignment(JTextField.RIGHT);
        tfPTDist.setText(properties.getProperty("speedPT"));

        tfMIVDist.setHorizontalAlignment(JTextField.RIGHT);
        tfMIVDist.setText(properties.getProperty("speedMIV"));

        tfWalkTimeApproach.setHorizontalAlignment(JTextField.RIGHT);
        tfWalkTimeApproach.setText(properties.getProperty("timeWalkApproach"));

        tfBikeTimeApproach.setHorizontalAlignment(JTextField.RIGHT);
        tfBikeTimeApproach.setText(properties.getProperty("timeBikeApproach"));

        tfPTTimeApproach.setHorizontalAlignment(JTextField.RIGHT);
        tfPTTimeApproach.setText(properties.getProperty("timePTApproach"));

        tfMIVTimeApproach.setHorizontalAlignment(JTextField.RIGHT);
        tfMIVTimeApproach.setText(properties.getProperty("timeMIVApproach"));

        lTimeAdditionDeparture.setText("Abgang Zma [min]");

        tfWalkTimeDeparture.setHorizontalAlignment(JTextField.RIGHT);
        tfWalkTimeDeparture.setText(properties.getProperty("timeWalkDeparture"));

        tfBikeTimeDeparture.setHorizontalAlignment(JTextField.RIGHT);
        tfBikeTimeDeparture.setText(properties.getProperty("timeBikeDeparture"));

        tfPTTimeDeparture.setHorizontalAlignment(JTextField.RIGHT);
        tfPTTimeDeparture.setText(properties.getProperty("timePTDeparture"));

        tfMIVTimeDeparture.setHorizontalAlignment(JTextField.RIGHT);
        tfMIVTimeDeparture.setText(properties.getProperty("timeMIVDeparture"));

        jLabel3.setText("Zeitzuschlag Zm = Zmz + Zma [min]");

        GroupLayout pTTParaLayout = new GroupLayout(pTTPara);
        pTTPara.setLayout(pTTParaLayout);
        pTTParaLayout.setHorizontalGroup(pTTParaLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                      .addGroup(pTTParaLayout.createSequentialGroup().addGroup(
                                                              pTTParaLayout.createParallelGroup(
                                                                      GroupLayout.Alignment.LEADING)
                                                                           .addComponent(lSpeed)
                                                                           .addComponent(lTimeAdditionApproach)
                                                                           .addComponent(lTimeAdditionDeparture))
                                                                             .addPreferredGap(
                                                                                     LayoutStyle.ComponentPlacement.RELATED,
                                                                                     30, Short.MAX_VALUE).addGroup(
                                                                      pTTParaLayout.createParallelGroup(
                                                                              GroupLayout.Alignment.TRAILING)
                                                                                   .addComponent(jLabel3,
                                                                                           GroupLayout.PREFERRED_SIZE,
                                                                                           204,
                                                                                           GroupLayout.PREFERRED_SIZE)
                                                                                   .addGroup(pTTParaLayout
                                                                                           .createSequentialGroup()
                                                                                           .addGroup(pTTParaLayout
                                                                                                   .createParallelGroup(
                                                                                                           GroupLayout.Alignment.TRAILING)
                                                                                                   .addGroup(
                                                                                                           pTTParaLayout
                                                                                                                   .createParallelGroup(
                                                                                                                           GroupLayout.Alignment.CENTER)
                                                                                                                   .addComponent(
                                                                                                                           tfWalkDist,
                                                                                                                           GroupLayout.PREFERRED_SIZE,
                                                                                                                           35,
                                                                                                                           GroupLayout.PREFERRED_SIZE)
                                                                                                                   .addComponent(
                                                                                                                           lWalk,
                                                                                                                           GroupLayout.PREFERRED_SIZE,
                                                                                                                           29,
                                                                                                                           GroupLayout.PREFERRED_SIZE)
                                                                                                                   .addComponent(
                                                                                                                           tfWalkTimeApproach,
                                                                                                                           GroupLayout.PREFERRED_SIZE,
                                                                                                                           35,
                                                                                                                           GroupLayout.PREFERRED_SIZE))
                                                                                                   .addComponent(
                                                                                                           tfWalkTimeDeparture,
                                                                                                           GroupLayout.PREFERRED_SIZE,
                                                                                                           35,
                                                                                                           GroupLayout.PREFERRED_SIZE))
                                                                                           .addGap(18, 18, 18).addGroup(
                                                                                                   pTTParaLayout
                                                                                                           .createParallelGroup(
                                                                                                                   GroupLayout.Alignment.LEADING,
                                                                                                                   false)
                                                                                                           .addGroup(
                                                                                                                   pTTParaLayout
                                                                                                                           .createSequentialGroup()
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   GroupLayout.Alignment.CENTER)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfBikeDist,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   lBike,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   29,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   GroupLayout.Alignment.CENTER)
                                                                                                                                           .addComponent(
                                                                                                                                                   lPT,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   40,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfPTDist,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   GroupLayout.Alignment.CENTER)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfMIVDist,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   lMIV,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   27,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE)))
                                                                                                           .addGroup(
                                                                                                                   pTTParaLayout
                                                                                                                           .createSequentialGroup()
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   GroupLayout.Alignment.LEADING)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfBikeTimeApproach,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfBikeTimeDeparture,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   GroupLayout.Alignment.LEADING)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfPTTimeApproach,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfPTTimeDeparture,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   GroupLayout.Alignment.LEADING)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfMIVTimeDeparture,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfMIVTimeApproach,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   GroupLayout.PREFERRED_SIZE))))))
                                                                             .addContainerGap(19, Short.MAX_VALUE)));

        pTTParaLayout.linkSize(SwingConstants.HORIZONTAL, lBike, lMIV, lPT, lWalk, tfBikeDist,
                tfBikeTimeApproach, tfBikeTimeDeparture, tfMIVDist, tfMIVTimeApproach, tfMIVTimeDeparture, tfPTDist,
                tfPTTimeApproach, tfPTTimeDeparture, tfWalkDist, tfWalkTimeApproach, tfWalkTimeDeparture);

        pTTParaLayout.linkSize(SwingConstants.HORIZONTAL, lSpeed, lTimeAdditionApproach,
                lTimeAdditionDeparture);

        pTTParaLayout.setVerticalGroup(pTTParaLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addGroup(pTTParaLayout.createSequentialGroup().addGap(2, 2, 2)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   GroupLayout.Alignment.CENTER)
                                                                                                  .addComponent(lWalk)
                                                                                                  .addComponent(lBike)
                                                                                                  .addComponent(lPT)
                                                                                                  .addComponent(lMIV))
                                                                           .addPreferredGap(
                                                                                   LayoutStyle.ComponentPlacement.RELATED)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   GroupLayout.Alignment.BASELINE)
                                                                                                  .addComponent(
                                                                                                          tfWalkDist,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfBikeDist,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfPTDist,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfMIVDist,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(lSpeed))
                                                                           .addPreferredGap(
                                                                                   LayoutStyle.ComponentPlacement.RELATED,
                                                                                   GroupLayout.DEFAULT_SIZE,
                                                                                   Short.MAX_VALUE).addComponent(
                                                                    jLabel3).addPreferredGap(
                                                                    LayoutStyle.ComponentPlacement.RELATED)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   GroupLayout.Alignment.BASELINE)
                                                                                                  .addComponent(
                                                                                                          tfWalkTimeApproach,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfBikeTimeApproach,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfPTTimeApproach,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfMIVTimeApproach,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          lTimeAdditionApproach))
                                                                           .addPreferredGap(
                                                                                   LayoutStyle.ComponentPlacement.RELATED)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   GroupLayout.Alignment.LEADING)
                                                                                                  .addGroup(
                                                                                                          pTTParaLayout
                                                                                                                  .createParallelGroup(
                                                                                                                          GroupLayout.Alignment.BASELINE)
                                                                                                                  .addComponent(
                                                                                                                          tfBikeTimeDeparture,
                                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addComponent(
                                                                                                                          tfPTTimeDeparture,
                                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addComponent(
                                                                                                                          tfMIVTimeDeparture,
                                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addComponent(
                                                                                                                          lTimeAdditionDeparture))
                                                                                                  .addGroup(
                                                                                                          GroupLayout.Alignment.TRAILING,
                                                                                                          pTTParaLayout
                                                                                                                  .createSequentialGroup()
                                                                                                                  .addComponent(
                                                                                                                          tfWalkTimeDeparture,
                                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addContainerGap()))));

        pTTParaLayout.linkSize(SwingConstants.VERTICAL, tfBikeDist, tfBikeTimeApproach, tfBikeTimeDeparture,
                tfMIVDist, tfMIVTimeApproach, tfMIVTimeDeparture, tfPTDist, tfPTTimeApproach, tfPTTimeDeparture,
                tfWalkDist, tfWalkTimeApproach, tfWalkTimeDeparture);

        pInput.setBorder(BorderFactory.createTitledBorder("Eingabeparameter"));

        cbTerrain.setModel(new DefaultComboBoxModel<>(Terrain.values()));
        if (properties.getProperty("terrain").isEmpty()) cbTerrain.setSelectedItem(0);
        else cbTerrain.setSelectedItem(Terrain.valueOf(properties.getProperty("terrain")));
        cbTerrain.setToolTipText("");
        cbTerrain.addItemListener(this::cbTerrainItemStateChanged);

        lTazTable.setText("TAZ-Tabelle:");

        lTerrain.setText("Terrain:");

        tfTazTable.setText(properties.getProperty("tazTable"));
        tfTazTable.setDragEnabled(true);
        tfTazTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tfTazTableKeyReleased(evt);
            }
        });

        lMatrixTable.setText("Matrix-Tabelle:");

        tfMatrixTable.setText(properties.getProperty("matricesTable"));
        tfMatrixTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tfMatrixTableKeyReleased(evt);
            }
        });

        jLabel1.setText("z - Werte:");
        jLabel1.setToolTipText("");

        cbZValue.setSelected(Boolean.parseBoolean(properties.getProperty("zValues")));
        cbZValue.addItemListener(this::cbZValueItemStateChanged);

        jLabel2.setText("Datensatzname:");

        tfRecordName.setText(properties.getProperty("record"));
        tfRecordName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tfRecordNameKeyReleased(evt);
            }
        });

        lTop3.setText("top3:");

        cbTop3.setSelected(Boolean.parseBoolean(properties.getProperty("top3")));
        cbTop3.addItemListener(this::cbTop3ItemStateChanged);

        GroupLayout pInputLayout = new GroupLayout(pInput);
        pInput.setLayout(pInputLayout);
        pInputLayout.setHorizontalGroup(pInputLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addGroup(pInputLayout.createSequentialGroup().addGroup(
                                                            pInputLayout.createParallelGroup(
                                                                    GroupLayout.Alignment.LEADING)
                                                                        .addComponent(lTazTable).addComponent(
                                                                    lMatrixTable).addComponent(jLabel2)
                                                                        .addComponent(lTerrain).addComponent(jLabel1))
                                                                          .addPreferredGap(
                                                                                  LayoutStyle.ComponentPlacement.RELATED,
                                                                                  28, Short.MAX_VALUE)
                                                                          .addGroup(pInputLayout.createParallelGroup(
                                                                                  GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(
                                                                                                        tfTazTable,
                                                                                                        GroupLayout.PREFERRED_SIZE,
                                                                                                        304,
                                                                                                        GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(
                                                                                                        tfMatrixTable)
                                                                                                .addComponent(
                                                                                                        tfRecordName,
                                                                                                        GroupLayout.PREFERRED_SIZE,
                                                                                                        301,
                                                                                                        GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(cbTerrain,
                                                                                                        GroupLayout.PREFERRED_SIZE,
                                                                                                        261,
                                                                                                        GroupLayout.PREFERRED_SIZE)
                                                                                                .addGroup(pInputLayout
                                                                                                        .createSequentialGroup()
                                                                                                        .addComponent(
                                                                                                                cbZValue,
                                                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                                                60,
                                                                                                                GroupLayout.PREFERRED_SIZE)
                                                                                                        .addGap(32, 32,
                                                                                                                32)
                                                                                                        .addComponent(
                                                                                                                lTop3)
                                                                                                        .addPreferredGap(
                                                                                                                LayoutStyle.ComponentPlacement.RELATED)
                                                                                                        .addComponent(
                                                                                                                cbTop3,
                                                                                                                GroupLayout.PREFERRED_SIZE,
                                                                                                                51,
                                                                                                                GroupLayout.PREFERRED_SIZE)))));

        pInputLayout.linkSize(SwingConstants.HORIZONTAL, lMatrixTable, lTazTable, lTerrain);

        pInputLayout.linkSize(SwingConstants.HORIZONTAL, cbTerrain, tfMatrixTable, tfRecordName,
                tfTazTable);

        pInputLayout.setVerticalGroup(pInputLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                  .addGroup(pInputLayout.createSequentialGroup().addGap(2, 2, 2)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(lTazTable,
                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                      19,
                                                                                                      GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(tfTazTable,
                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                                      GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(
                                                                                                      lMatrixTable,
                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                      19,
                                                                                                      GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(
                                                                                                      tfMatrixTable,
                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                                      GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(
                                                                                                      tfRecordName,
                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                                      GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(jLabel2))
                                                                        .addPreferredGap(
                                                                                LayoutStyle.ComponentPlacement.RELATED,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE).addGroup(
                                                                  pInputLayout.createParallelGroup(
                                                                          GroupLayout.Alignment.BASELINE)
                                                                              .addComponent(cbTerrain,
                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                      GroupLayout.PREFERRED_SIZE)
                                                                              .addComponent(lTerrain,
                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                      5,
                                                                                      GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                GroupLayout.Alignment.LEADING)
                                                                                              .addGroup(pInputLayout
                                                                                                      .createParallelGroup(
                                                                                                              GroupLayout.Alignment.TRAILING)
                                                                                                      .addComponent(
                                                                                                              cbZValue)
                                                                                                      .addComponent(
                                                                                                              lTop3)
                                                                                                      .addComponent(
                                                                                                              cbTop3))
                                                                                              .addComponent(jLabel1))));

        pInputLayout.linkSize(SwingConstants.VERTICAL, jLabel1, jLabel2, lMatrixTable, lTazTable, lTerrain,
                tfTazTable);

        pMatrixCalc.setBorder(BorderFactory.createTitledBorder("Matrixberechnung"));

        lDist.setHorizontalAlignment(SwingConstants.CENTER);
        lDist.setText("Distanzen");
        lDist.setToolTipText("");

        lTT.setText("Reisezeiten");

        lWalk1.setHorizontalAlignment(SwingConstants.CENTER);
        lWalk1.setText("WALK");

        lBike1.setHorizontalAlignment(SwingConstants.CENTER);
        lBike1.setText("BIKE");

        lPT1.setHorizontalAlignment(SwingConstants.CENTER);
        lPT1.setText("PT");

        lMIV1.setHorizontalAlignment(SwingConstants.CENTER);
        lMIV1.setText("MIV");

        cbWalkDist.setSelected(Boolean.parseBoolean(properties.getProperty("distWalkCalc")));
        cbWalkDist.setToolTipText("Berechnung und Anlegen der Distanztabelle für den Modus WALK");
        cbWalkDist.addItemListener(this::cbWalkDistItemStateChanged);

        cbBikeDist.setSelected(Boolean.parseBoolean(properties.getProperty("distBikeCalc")));
        cbBikeDist.setToolTipText("Berechnung und Anlegen der Distanztabelle für den Modus BIKE");
        cbBikeDist.addItemListener(this::cbBikeDistItemStateChanged);

        cbPTDist.setSelected(Boolean.parseBoolean(properties.getProperty("distPTCalc")));
        cbPTDist.setToolTipText("Berechnung und Anlegen der Distanztabelle für den Modus PT");
        cbPTDist.addItemListener(this::cbPTDistItemStateChanged);

        cbMIVDist.setSelected(Boolean.parseBoolean(properties.getProperty("distMIVCalc")));
        cbMIVDist.setToolTipText("Berechnung und Anlegen der Distanztabelle für den Modus MIV");
        cbMIVDist.addItemListener(this::cbMIVDistItemStateChanged);

        cbWalkTT.setSelected(Boolean.parseBoolean(properties.getProperty("timeWalkCalc")));
        cbWalkTT.setToolTipText("Berechnung und Anlegen der Reisezeitentabelle für den Modus WALK");
        cbWalkTT.addItemListener(this::cbWalkTTItemStateChanged);

        cbBikeTT.setSelected(Boolean.parseBoolean(properties.getProperty("timeBikeCalc")));
        cbBikeTT.setToolTipText("Berechnung und Anlegen der Reisezeitentabelle für den Modus BIKE");
        cbBikeTT.addItemListener(this::cbBikeTTItemStateChanged);

        cbPTTT.setSelected(Boolean.parseBoolean(properties.getProperty("timePTCalc")));
        cbPTTT.setToolTipText("Berechnung und Anlegen der Reisezeitentabelle für den Modus PT");
        cbPTTT.addItemListener(this::cbPTTTItemStateChanged);

        cbMIVTT.setSelected(Boolean.parseBoolean(properties.getProperty("timeMIVCalc")));
        cbMIVTT.setToolTipText("Berechnung und Anlegen der Reisezeitentabelle für den Modus MIV");
        cbMIVTT.addItemListener(this::cbMIVTTItemStateChanged);

        GroupLayout pMatrixCalcLayout = new GroupLayout(pMatrixCalc);
        pMatrixCalc.setLayout(pMatrixCalcLayout);
        pMatrixCalcLayout.setHorizontalGroup(
                pMatrixCalcLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                 .addGroup(pMatrixCalcLayout.createSequentialGroup().addGroup(
                                         pMatrixCalcLayout.createParallelGroup(
                                                 GroupLayout.Alignment.LEADING).addComponent(lDist)
                                                          .addComponent(lTT)).addPreferredGap(
                                         LayoutStyle.ComponentPlacement.RELATED, 132, Short.MAX_VALUE)
                                                            .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                                    GroupLayout.Alignment.CENTER)
                                                                                       .addComponent(lWalk1,
                                                                                               GroupLayout.PREFERRED_SIZE,
                                                                                               29,
                                                                                               GroupLayout.PREFERRED_SIZE)
                                                                                       .addComponent(cbWalkDist)
                                                                                       .addComponent(cbWalkTT)).addGap(
                                                 18, 18, 18).addGroup(pMatrixCalcLayout.createParallelGroup(
                                                 GroupLayout.Alignment.LEADING).addComponent(lBike1,
                                                 GroupLayout.PREFERRED_SIZE, 29,
                                                 GroupLayout.PREFERRED_SIZE).addGroup(
                                                 pMatrixCalcLayout.createSequentialGroup().addGap(10, 10, 10)
                                                                  .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                                          GroupLayout.Alignment.LEADING)
                                                                                             .addComponent(cbBikeTT)
                                                                                             .addComponent(
                                                                                                     cbBikeDist))))
                                                            .addGap(18, 18, 18).addGroup(
                                                 pMatrixCalcLayout.createParallelGroup(
                                                         GroupLayout.Alignment.LEADING).addComponent(lPT1,
                                                         GroupLayout.PREFERRED_SIZE, 40,
                                                         GroupLayout.PREFERRED_SIZE).addGroup(
                                                         pMatrixCalcLayout.createSequentialGroup().addGap(10, 10, 10)
                                                                          .addGroup(
                                                                                  pMatrixCalcLayout.createParallelGroup(
                                                                                          GroupLayout.Alignment.LEADING)
                                                                                                   .addComponent(cbPTTT)
                                                                                                   .addComponent(
                                                                                                           cbPTDist))))
                                                            .addGap(18, 18, 18)
                                                            .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                                    GroupLayout.Alignment.LEADING)
                                                                                       .addComponent(lMIV1,
                                                                                               GroupLayout.PREFERRED_SIZE,
                                                                                               27,
                                                                                               GroupLayout.PREFERRED_SIZE)
                                                                                       .addGroup(pMatrixCalcLayout
                                                                                               .createSequentialGroup()
                                                                                               .addGap(10, 10, 10)
                                                                                               .addGroup(
                                                                                                       pMatrixCalcLayout
                                                                                                               .createParallelGroup(
                                                                                                                       GroupLayout.Alignment.LEADING)
                                                                                                               .addComponent(
                                                                                                                       cbMIVTT)
                                                                                                               .addComponent(
                                                                                                                       cbMIVDist))))
                                                            .addContainerGap()));

        pMatrixCalcLayout.linkSize(SwingConstants.HORIZONTAL, lBike1, lMIV1, lPT1, lWalk1);

        pMatrixCalcLayout.setVerticalGroup(
                pMatrixCalcLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                        pMatrixCalcLayout.createSequentialGroup().addGroup(pMatrixCalcLayout.createParallelGroup(
                                GroupLayout.Alignment.LEADING).addGroup(
                                pMatrixCalcLayout.createSequentialGroup().addGroup(
                                        pMatrixCalcLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                         .addComponent(lWalk1).addComponent(lBike1).addComponent(lPT1)
                                                         .addComponent(lMIV1)).addPreferredGap(
                                        LayoutStyle.ComponentPlacement.RELATED).addGroup(
                                        pMatrixCalcLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                         .addComponent(cbWalkDist,
                                                                 GroupLayout.Alignment.TRAILING)
                                                         .addComponent(cbBikeDist,
                                                                 GroupLayout.Alignment.TRAILING)
                                                         .addComponent(cbPTDist,
                                                                 GroupLayout.Alignment.TRAILING)
                                                         .addComponent(cbMIVDist,
                                                                 GroupLayout.Alignment.TRAILING))
                                                 .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                 .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                         GroupLayout.Alignment.LEADING).addComponent(
                                                         cbWalkTT, GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(cbBikeTT,
                                                                                    GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(cbPTTT,
                                                                                    GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(cbMIVTT,
                                                                                    GroupLayout.Alignment.TRAILING)))
                                                                                            .addGroup(pMatrixCalcLayout
                                                                                                    .createSequentialGroup()
                                                                                                    .addGap(23, 23, 23)
                                                                                                    .addComponent(lDist)
                                                                                                    .addPreferredGap(
                                                                                                            LayoutStyle.ComponentPlacement.RELATED)
                                                                                                    .addComponent(lTT)))
                                         .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        lInfo.setForeground(new Color(0, 102, 102));

        GroupLayout pGroupGenerateLayout = new GroupLayout(pGroupGenerate);
        pGroupGenerate.setLayout(pGroupGenerateLayout);
        pGroupGenerateLayout.setHorizontalGroup(
                pGroupGenerateLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                        pGroupGenerateLayout.createSequentialGroup().addContainerGap()
                                            .addGroup(pGroupGenerateLayout.createParallelGroup(
                                                    GroupLayout.Alignment.LEADING).addGroup(
                                                    pGroupGenerateLayout.createSequentialGroup().addGap(7, 7, 7)
                                                                        .addComponent(lInfo,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)).addGroup(
                                                    pGroupGenerateLayout.createSequentialGroup().addGroup(
                                                            pGroupGenerateLayout.createParallelGroup(
                                                                    GroupLayout.Alignment.LEADING)
                                                                                .addComponent(pInput,
                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                        Short.MAX_VALUE)
                                                                                .addComponent(pMatrixCalc,
                                                                                        GroupLayout.PREFERRED_SIZE,
                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                        GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(pTTPara,
                                                                                        GroupLayout.PREFERRED_SIZE,
                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                        GroupLayout.PREFERRED_SIZE))
                                                                        .addGap(0, 10, Short.MAX_VALUE)))
                                            .addContainerGap()));

        pGroupGenerateLayout.linkSize(SwingConstants.HORIZONTAL, pInput, pMatrixCalc, pTTPara);

        pGroupGenerateLayout.setVerticalGroup(
                pGroupGenerateLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(GroupLayout.Alignment.TRAILING,
                                            pGroupGenerateLayout.createSequentialGroup().addContainerGap().addComponent(
                                                    pInput, GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                    LayoutStyle.ComponentPlacement.RELATED).addComponent(
                                                    pMatrixCalc, GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                    LayoutStyle.ComponentPlacement.RELATED).addComponent(
                                                    pTTPara, GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                    LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(lInfo,
                                                                        GroupLayout.PREFERRED_SIZE, 16,
                                                                        GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(24, Short.MAX_VALUE)));

        jTabbedPane1.addTab("Generierung", pGroupGenerate);

        pValidation.setBorder(BorderFactory.createTitledBorder("Validierung"));

        lPath.setText("Ausgabeordner:");

        tfPath.setText(properties.getProperty("path"));

        bPath.setText("...");
        bPath.addActionListener(this::bPathActionPerformed);

        cbActivateValidation.setSelected(Boolean.parseBoolean(properties.getProperty("validation")));
        cbActivateValidation.setText("Validierung aktivieren");
        cbActivateValidation.addItemListener(this::cbActivateValidationItemStateChanged);

        pValDist.setBorder(BorderFactory.createTitledBorder("Distanzen"));

        cbValWalkDist.setSelected(Boolean.parseBoolean(properties.getProperty("validationWalkDist")));
        cbValWalkDist.setText("WALK");
        cbValWalkDist.addItemListener(this::cbValWalkDistItemStateChanged);

        cbValBikeDist.setSelected(Boolean.parseBoolean(properties.getProperty("validationBikeDist")));
        cbValBikeDist.setText("BIKE");
        cbValBikeDist.addItemListener(this::cbValBikeDistItemStateChanged);

        cbValPTDist.setSelected(Boolean.parseBoolean(properties.getProperty("validationPTDist")));
        cbValPTDist.setText("PT");
        cbValPTDist.addItemListener(this::cbValPTDistItemStateChanged);

        cbValMIVDist.setSelected(Boolean.parseBoolean(properties.getProperty("validationMIVDist")));
        cbValMIVDist.setText("MIV ");
        cbValMIVDist.addItemListener(this::cbValMIVDistItemStateChanged);

        lValDistRef.setText("Referenz-Tabelle:");

        tfValWalkDist.setText(properties.getProperty("referenceWalkDist"));

        tfValBikeDist.setText(properties.getProperty("referenceBikeDist"));

        tfValPTDist.setText(properties.getProperty("referencePTDist"));

        tfValMIVDist.setText(properties.getProperty("referenceMIVDist"));

        GroupLayout pValDistLayout = new GroupLayout(pValDist);
        pValDist.setLayout(pValDistLayout);
        pValDistLayout.setHorizontalGroup(pValDistLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addGroup(
                                                                pValDistLayout.createSequentialGroup().addContainerGap()
                                                                              .addGroup(pValDistLayout
                                                                                      .createParallelGroup(
                                                                                              GroupLayout.Alignment.LEADING)
                                                                                      .addComponent(cbValWalkDist)
                                                                                      .addComponent(cbValBikeDist)
                                                                                      .addComponent(cbValPTDist)
                                                                                      .addComponent(cbValMIVDist))
                                                                              .addGap(18, 18, 18).addGroup(
                                                                        pValDistLayout.createParallelGroup(
                                                                                GroupLayout.Alignment.LEADING,
                                                                                false).addComponent(tfValPTDist,
                                                                                GroupLayout.DEFAULT_SIZE,
                                                                                302, Short.MAX_VALUE).addComponent(
                                                                                tfValBikeDist)
                                                                                      .addComponent(tfValMIVDist)
                                                                                      .addComponent(tfValWalkDist))
                                                                              .addContainerGap(
                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                      Short.MAX_VALUE))
                                                        .addGroup(GroupLayout.Alignment.TRAILING,
                                                                pValDistLayout.createSequentialGroup().addContainerGap(
                                                                        180, Short.MAX_VALUE).addComponent(lValDistRef)
                                                                              .addGap(133, 133, 133)));
        pValDistLayout.setVerticalGroup(pValDistLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                      .addGroup(pValDistLayout.createSequentialGroup().addGroup(
                                                              pValDistLayout.createParallelGroup(
                                                                      GroupLayout.Alignment.LEADING)
                                                                            .addGroup(pValDistLayout
                                                                                    .createSequentialGroup()
                                                                                    .addComponent(lValDistRef)
                                                                                    .addGap(0, 0, Short.MAX_VALUE))
                                                                            .addGroup(
                                                                                    GroupLayout.Alignment.TRAILING,
                                                                                    pValDistLayout
                                                                                            .createSequentialGroup()
                                                                                            .addContainerGap(20,
                                                                                                    Short.MAX_VALUE)
                                                                                            .addGroup(pValDistLayout
                                                                                                    .createParallelGroup(
                                                                                                            GroupLayout.Alignment.BASELINE)
                                                                                                    .addComponent(
                                                                                                            tfValWalkDist,
                                                                                                            GroupLayout.PREFERRED_SIZE,
                                                                                                            GroupLayout.DEFAULT_SIZE,
                                                                                                            GroupLayout.PREFERRED_SIZE)
                                                                                                    .addComponent(
                                                                                                            cbValWalkDist))))
                                                                              .addGap(3, 3, 3).addGroup(
                                                                      pValDistLayout.createParallelGroup(
                                                                              GroupLayout.Alignment.BASELINE)
                                                                                    .addComponent(tfValBikeDist,
                                                                                            GroupLayout.PREFERRED_SIZE,
                                                                                            GroupLayout.DEFAULT_SIZE,
                                                                                            GroupLayout.PREFERRED_SIZE)
                                                                                    .addComponent(cbValBikeDist))
                                                                              .addPreferredGap(
                                                                                      LayoutStyle.ComponentPlacement.RELATED)
                                                                              .addGroup(pValDistLayout
                                                                                      .createParallelGroup(
                                                                                              GroupLayout.Alignment.BASELINE)
                                                                                      .addComponent(tfValPTDist,
                                                                                              GroupLayout.PREFERRED_SIZE,
                                                                                              GroupLayout.DEFAULT_SIZE,
                                                                                              GroupLayout.PREFERRED_SIZE)
                                                                                      .addComponent(cbValPTDist))
                                                                              .addGap(3, 3, 3).addGroup(
                                                                      pValDistLayout.createParallelGroup(
                                                                              GroupLayout.Alignment.BASELINE)
                                                                                    .addComponent(cbValMIVDist)
                                                                                    .addComponent(tfValMIVDist,
                                                                                            GroupLayout.PREFERRED_SIZE,
                                                                                            GroupLayout.DEFAULT_SIZE,
                                                                                            GroupLayout.PREFERRED_SIZE))));

        pValTT.setBorder(BorderFactory.createTitledBorder("Reisezeiten"));

        cbValWalkTT.setSelected(Boolean.parseBoolean(properties.getProperty("validationWalkTT")));
        cbValWalkTT.setText("WALK");
        cbValWalkTT.addItemListener(this::cbValWalkTTItemStateChanged);

        cbValBikeTT.setSelected(Boolean.parseBoolean(properties.getProperty("validationBikeTT")));
        cbValBikeTT.setText("BIKE");
        cbValBikeTT.addItemListener(this::cbValBikeTTItemStateChanged);

        cbValPTTT.setSelected(Boolean.parseBoolean(properties.getProperty("validationPTTT")));
        cbValPTTT.setText("PT");
        cbValPTTT.addItemListener(this::cbValPTTTItemStateChanged);

        cbValMIVTT.setSelected(Boolean.parseBoolean(properties.getProperty("validationMIVTT")));
        cbValMIVTT.setText("MIV ");
        cbValMIVTT.addItemListener(this::cbValMIVTTItemStateChanged);

        lValTTRef.setText("Referenz-Tabelle:");

        tfValWalkTT.setText(properties.getProperty("referenceWalkTT"));

        tfValBikeTT.setText(properties.getProperty("referenceBikeTT"));

        tfValPTTT.setText(properties.getProperty("referencePTTT"));

        tfValMIVTT.setText(properties.getProperty("referenceMIVTT"));

        GroupLayout pValTTLayout = new GroupLayout(pValTT);
        pValTT.setLayout(pValTTLayout);
        pValTTLayout.setHorizontalGroup(pValTTLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addGroup(pValTTLayout.createSequentialGroup().addContainerGap()
                                                                          .addGroup(pValTTLayout.createParallelGroup(
                                                                                  GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(
                                                                                                        cbValWalkTT)
                                                                                                .addComponent(
                                                                                                        cbValBikeTT)
                                                                                                .addComponent(cbValPTTT)
                                                                                                .addComponent(
                                                                                                        cbValMIVTT))
                                                                          .addGap(18, 18, 18)
                                                                          .addGroup(pValTTLayout.createParallelGroup(
                                                                                  GroupLayout.Alignment.LEADING,
                                                                                  false).addComponent(tfValPTTT,
                                                                                  GroupLayout.DEFAULT_SIZE,
                                                                                  302, Short.MAX_VALUE).addComponent(
                                                                                  tfValBikeTT).addComponent(tfValMIVTT)
                                                                                                .addComponent(
                                                                                                        tfValWalkTT))
                                                                          .addContainerGap(
                                                                                  GroupLayout.DEFAULT_SIZE,
                                                                                  Short.MAX_VALUE))
                                                    .addGroup(GroupLayout.Alignment.TRAILING,
                                                            pValTTLayout.createSequentialGroup().addContainerGap(180,
                                                                    Short.MAX_VALUE).addComponent(lValTTRef)
                                                                        .addGap(133, 133, 133)));
        pValTTLayout.setVerticalGroup(pValTTLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                  .addGroup(pValTTLayout.createSequentialGroup().addGroup(
                                                          pValTTLayout.createParallelGroup(
                                                                  GroupLayout.Alignment.LEADING).addGroup(
                                                                  pValTTLayout.createSequentialGroup()
                                                                              .addComponent(lValTTRef)
                                                                              .addGap(0, 0, Short.MAX_VALUE)).addGroup(
                                                                  GroupLayout.Alignment.TRAILING,
                                                                  pValTTLayout.createSequentialGroup()
                                                                              .addContainerGap(20, Short.MAX_VALUE)
                                                                              .addGroup(
                                                                                      pValTTLayout.createParallelGroup(
                                                                                              GroupLayout.Alignment.BASELINE)
                                                                                                  .addComponent(
                                                                                                          tfValWalkTT,
                                                                                                          GroupLayout.PREFERRED_SIZE,
                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                          GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          cbValWalkTT))))
                                                                        .addGap(3, 3, 3).addGroup(
                                                                  pValTTLayout.createParallelGroup(
                                                                          GroupLayout.Alignment.BASELINE)
                                                                              .addComponent(tfValBikeTT,
                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                      GroupLayout.PREFERRED_SIZE)
                                                                              .addComponent(cbValBikeTT))
                                                                        .addPreferredGap(
                                                                                LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pValTTLayout.createParallelGroup(
                                                                                GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(tfValPTTT,
                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                                      GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(cbValPTTT))
                                                                        .addGap(3, 3, 3)
                                                                        .addGroup(pValTTLayout.createParallelGroup(
                                                                                GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(cbValMIVTT)
                                                                                              .addComponent(tfValMIVTT,
                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                                      GroupLayout.PREFERRED_SIZE))));

        GroupLayout pValidationLayout = new GroupLayout(pValidation);
        pValidation.setLayout(pValidationLayout);
        pValidationLayout.setHorizontalGroup(
                pValidationLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                        pValidationLayout.createSequentialGroup().addGroup(pValidationLayout.createParallelGroup(
                                GroupLayout.Alignment.LEADING).addGroup(
                                pValidationLayout.createSequentialGroup().addContainerGap()
                                                 .addGroup(pValidationLayout.createParallelGroup(
                                                         GroupLayout.Alignment.LEADING).addGroup(
                                                         pValidationLayout.createSequentialGroup()
                                                                          .addComponent(cbActivateValidation,
                                                                                  GroupLayout.PREFERRED_SIZE,
                                                                                  166,
                                                                                  GroupLayout.PREFERRED_SIZE)
                                                                          .addGap(0, 0, Short.MAX_VALUE)).addGroup(
                                                         pValidationLayout.createSequentialGroup().addComponent(lPath)
                                                                          .addPreferredGap(
                                                                                  LayoutStyle.ComponentPlacement.RELATED)
                                                                          .addComponent(tfPath).addPreferredGap(
                                                                 LayoutStyle.ComponentPlacement.RELATED)
                                                                          .addComponent(bPath,
                                                                                  GroupLayout.PREFERRED_SIZE,
                                                                                  30,
                                                                                  GroupLayout.PREFERRED_SIZE))))
                                                                                            .addComponent(pValDist,
                                                                                                    GroupLayout.DEFAULT_SIZE,
                                                                                                    GroupLayout.DEFAULT_SIZE,
                                                                                                    Short.MAX_VALUE)
                                                                                            .addComponent(pValTT,
                                                                                                    GroupLayout.DEFAULT_SIZE,
                                                                                                    GroupLayout.DEFAULT_SIZE,
                                                                                                    Short.MAX_VALUE))
                                         .addContainerGap()));
        pValidationLayout.setVerticalGroup(
                pValidationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                 .addGroup(GroupLayout.Alignment.TRAILING,
                                         pValidationLayout.createSequentialGroup().addContainerGap().addComponent(
                                                 cbActivateValidation).addPreferredGap(
                                                 LayoutStyle.ComponentPlacement.UNRELATED).addComponent(
                                                 pValDist, GroupLayout.PREFERRED_SIZE,
                                                 GroupLayout.DEFAULT_SIZE,
                                                 GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                 LayoutStyle.ComponentPlacement.UNRELATED).addComponent(
                                                 pValTT, GroupLayout.PREFERRED_SIZE,
                                                 GroupLayout.DEFAULT_SIZE,
                                                 GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                 LayoutStyle.ComponentPlacement.RELATED, 45,
                                                 Short.MAX_VALUE).addGroup(pValidationLayout.createParallelGroup(
                                                 GroupLayout.Alignment.BASELINE).addComponent(lPath)
                                                                                            .addComponent(tfPath,
                                                                                                    GroupLayout.PREFERRED_SIZE,
                                                                                                    GroupLayout.DEFAULT_SIZE,
                                                                                                    GroupLayout.PREFERRED_SIZE)
                                                                                            .addComponent(bPath))
                                                          .addContainerGap()));

        GroupLayout pGroupValidationLayout = new GroupLayout(pGroupValidation);
        pGroupValidation.setLayout(pGroupValidationLayout);
        pGroupValidationLayout.setHorizontalGroup(
                pGroupValidationLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                        pGroupValidationLayout.createSequentialGroup().addContainerGap()
                                              .addComponent(pValidation, GroupLayout.DEFAULT_SIZE,
                                                      GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                              .addContainerGap()));
        pGroupValidationLayout.setVerticalGroup(
                pGroupValidationLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                        pGroupValidationLayout.createSequentialGroup().addContainerGap()
                                              .addComponent(pValidation, GroupLayout.DEFAULT_SIZE,
                                                      GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                              .addContainerGap()));

        jTabbedPane1.addTab("Validierung", pGroupValidation);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(
                                                GroupLayout.Alignment.LEADING).addGroup(
                                                layout.createSequentialGroup().addGap(20, 20, 20).addComponent(bCancel)
                                                      .addPreferredGap(
                                                              LayoutStyle.ComponentPlacement.RELATED,
                                                              GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                      .addComponent(bSave).addPreferredGap(
                                                        LayoutStyle.ComponentPlacement.RELATED)
                                                      .addComponent(bStart)).addGroup(
                                                layout.createSequentialGroup().addContainerGap()
                                                      .addComponent(jTabbedPane1))).addContainerGap()));

        layout.linkSize(SwingConstants.HORIZONTAL, bCancel, bSave, bStart);

        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                      .addGroup(layout.createSequentialGroup().addContainerGap().addComponent(
                                              jTabbedPane1).addPreferredGap(
                                              LayoutStyle.ComponentPlacement.RELATED)
                                                      .addGroup(layout.createParallelGroup(
                                                              GroupLayout.Alignment.BASELINE).addComponent(
                                                              bCancel).addComponent(bSave).addComponent(bStart))
                                                      .addGap(11, 11, 11)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTabbedPane1FocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_jTabbedPane1FocusGained
    {//GEN-HEADEREND:event_jTabbedPane1FocusGained
        // dist
        enableComponentOnStart(cbValWalkDist, tfValWalkDist, "validationWalkDist");
        enableComponentOnStart(cbValBikeDist, tfValBikeDist, "validationBikeDist");
        enableComponentOnStart(cbValPTDist, tfValPTDist, "validationPTDist");
        enableComponentOnStart(cbValMIVDist, tfValMIVDist, "validationMIVDist");

        // traveltime (TT)
        enableComponentOnStart(cbValWalkTT, tfValWalkTT, "validationWalkTT");
        enableComponentOnStart(cbValBikeTT, tfValBikeTT, "validationBikeTT");
        enableComponentOnStart(cbValPTTT, tfValPTTT, "validationPTTT");
        enableComponentOnStart(cbValMIVTT, tfValMIVTT, "validationMIVTT");

        // validationPanel
        enableComponentOnStart(cbActivateValidation, pValidation, "validation");

        for (Modus modus : Modus.values())
            distanceSelected(modus);

        changes = false;

    }//GEN-LAST:event_jTabbedPane1FocusGained

    private void savePropertieFile() {
        try {
        	/*#############################################
            #               basic properties              #
            #############################################*/
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "tazTable", tfTazTable.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "matricesTable", tfMatrixTable.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "record", tfRecordName.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "terrain",
                    cbTerrain.getSelectedItem().toString());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "top3", cbTop3.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "indirectWayFactor", "true");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "zValues",
                    cbZValue.isSelected() ? "true" : "false");

            /*#############################################
            #             Matrix-Calculation            #
            #############################################*/
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "distWalkCalc",
                    cbWalkDist.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "distBikeCalc",
                    cbBikeDist.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "distPTCalc",
                    cbPTDist.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "distMIVCalc",
                    cbMIVDist.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeWalkCalc",
                    cbWalkTT.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeBikeCalc",
                    cbBikeTT.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timePTCalc",
                    cbPTTT.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeMIVCalc",
                    cbMIVTT.isSelected() ? "true" : "false");

            /*#############################################
            #             Traveltime-Parameter          #
            #############################################*/
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "speedWalk", tfWalkDist.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "speedBike", tfBikeDist.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "speedPT", tfPTDist.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "speedMIV", tfMIVDist.getText());

            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeWalkApproach",
                    tfWalkTimeApproach.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeBikeApproach",
                    tfBikeTimeApproach.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timePTApproach", tfPTTimeApproach.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeMIVApproach", tfMIVTimeApproach.getText());

            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeWalkDeparture",
                    tfWalkTimeDeparture.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeBikeDeparture",
                    tfBikeTimeDeparture.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timePTDeparture", tfPTTimeDeparture.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "timeMIVDeparture",
                    tfMIVTimeDeparture.getText());

            /*#############################################
            #         properties for VALIDATION           #
            #############################################*/
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validation",
                    cbActivateValidation.isSelected() ? "true" : "false");

            //checkboxes
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationWalkDist",
                    cbValWalkDist.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationBikeDist",
                    cbValBikeDist.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationPTDist",
                    cbValPTDist.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationMIVDist",
                    cbValMIVDist.isSelected() ? "true" : "false");

            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationWalkTT",
                    cbValWalkTT.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationBikeTT",
                    cbValBikeTT.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationPTTT",
                    cbValPTTT.isSelected() ? "true" : "false");
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "validationMIVTT",
                    cbValMIVTT.isSelected() ? "true" : "false");

            // textfields
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referenceWalkDist", tfValWalkDist.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referenceBikeDist", tfValBikeDist.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referencePTDist", tfValPTDist.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referenceMIVDist", tfValMIVDist.getText());

            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referenceWalkTT", tfValWalkTT.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referenceBikeTT", tfValBikeTT.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referencePTTT", tfValPTTT.getText());
            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "referenceMIVTT", tfValMIVTT.getText());

            PropertyReader.setProperty(CruisingSpeed.propertyFileName, "path", tfPath.getText());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Speichern");
    }

    public void start() {
        try {
            properties = PropertyReader.getProperties(CruisingSpeed.propertyFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void tfMatrixTableKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_tfMatrixTableKeyReleased
    {//GEN-HEADEREND:event_tfMatrixTableKeyReleased
        checkMandantoryFields();
    }//GEN-LAST:event_tfMatrixTableKeyReleased

    private void tfRecordNameKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_tfRecordNameKeyReleased
    {//GEN-HEADEREND:event_tfRecordNameKeyReleased
        checkMandantoryFields();
    }//GEN-LAST:event_tfRecordNameKeyReleased

    private void tfTazTableKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_tfTazTableKeyReleased
    {//GEN-HEADEREND:event_tfTazTableKeyReleased
        checkMandantoryFields();
    }//GEN-LAST:event_tfTazTableKeyReleased

    public static class FolderFileFilter extends FileFilter {
        /**
         * Die Methode legt fest ob ein File-Objekt zu denen
         * gehört, die angezeigt werden sollen oder nicht.
         *
         * @param file das File Objekt
         * @return true, wenn das File-Objekt ein Direktory ist, sonst
         * false
         */
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }

        @Override
        public String getDescription() {
            return "Ordner";
        }
    } // Ende der Inner-Class FolderFileFilter

    class TextFieldDokumentlistener implements DocumentListener {
        private final JTextField textField;
        private final String propertieKey;

        public TextFieldDokumentlistener(JTextField textField, String propertieKey) {
            this.textField = textField;
            this.propertieKey = propertieKey;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (!textField.getText().trim().equals(properties.getProperty(propertieKey))) {
                //  System.out.println("Changes = "+changes+"\nchangedUpdate: "+textField.getText()+", "+properties.getProperty(propertieKey));
                changes = true;
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {

            if (!textField.getText().trim().equals(properties.getProperty(propertieKey))) {
                //  System.out.println("Changes = "+changes+"\ninsertUpdate: "+textField.getText()+", "+properties.getProperty(propertieKey));
                changes = true;
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (!textField.getText().trim().equals(properties.getProperty(propertieKey))) {
                // System.out.println("Changes = "+changes+"\nremoveUpdate: "+textField.getText()+", "+properties.getProperty(propertieKey));
                changes = true;
            }
        }
    }

    class CheckBoxChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            AbstractButton abstractButton = (AbstractButton) changeEvent.getSource();
            ButtonModel buttonModel = abstractButton.getModel();
            @SuppressWarnings("unused") boolean armed = buttonModel.isArmed();
            boolean pressed = buttonModel.isPressed();
            @SuppressWarnings("unused") boolean selected = buttonModel.isSelected();

            if (pressed) changes = true;
        }
    }
}