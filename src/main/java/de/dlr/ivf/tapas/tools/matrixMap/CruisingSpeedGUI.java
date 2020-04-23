/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.dlr.ivf.tapas.tools.matrixMap;

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
public class CruisingSpeedGUI extends javax.swing.JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8820976812627995474L;
	private Properties properties = null;
    private boolean changes = false;
    private javax.swing.JButton bStart;
    private javax.swing.JCheckBox cbActivateValidation;
    private javax.swing.JCheckBox cbBikeDist;
    private javax.swing.JCheckBox cbBikeTT;
    private javax.swing.JCheckBox cbMIVDist;
    private javax.swing.JCheckBox cbMIVTT;
    private javax.swing.JCheckBox cbPTDist;
    private javax.swing.JCheckBox cbPTTT;
    @SuppressWarnings("rawtypes")
    private javax.swing.JComboBox cbTerrain;
    private javax.swing.JCheckBox cbTop3;
    private javax.swing.JCheckBox cbValBikeDist;
    private javax.swing.JCheckBox cbValBikeTT;
    private javax.swing.JCheckBox cbValMIVDist;
    private javax.swing.JCheckBox cbValMIVTT;
    private javax.swing.JCheckBox cbValPTDist;
    private javax.swing.JCheckBox cbValPTTT;
    private javax.swing.JCheckBox cbValWalkDist;
    private javax.swing.JCheckBox cbValWalkTT;
    private javax.swing.JCheckBox cbWalkDist;
    private javax.swing.JCheckBox cbWalkTT;
    private javax.swing.JCheckBox cbZValue;
    private javax.swing.JLabel lInfo;
    private javax.swing.JPanel pGroupValidation;
    private javax.swing.JPanel pTTPara;
    private javax.swing.JPanel pValidation;
    private javax.swing.JTextField tfBikeDist;
    private javax.swing.JTextField tfBikeTimeApproach;
    private javax.swing.JTextField tfBikeTimeDeparture;
    private javax.swing.JTextField tfMIVDist;
    private javax.swing.JTextField tfMIVTimeApproach;
    private javax.swing.JTextField tfMIVTimeDeparture;
    private javax.swing.JTextField tfMatrixTable;
    private javax.swing.JTextField tfPTDist;
    private javax.swing.JTextField tfPTTimeApproach;
    private javax.swing.JTextField tfPTTimeDeparture;
    private javax.swing.JTextField tfPath;
    private javax.swing.JTextField tfRecordName;
    private javax.swing.JTextField tfTazTable;
    private javax.swing.JTextField tfValBikeDist;
    private javax.swing.JTextField tfValBikeTT;
    private javax.swing.JTextField tfValMIVDist;
    private javax.swing.JTextField tfValMIVTT;
    private javax.swing.JTextField tfValPTDist;
    private javax.swing.JTextField tfValPTTT;
    private javax.swing.JTextField tfValWalkDist;
    private javax.swing.JTextField tfValWalkTT;
    private javax.swing.JTextField tfWalkDist;
    private javax.swing.JTextField tfWalkTimeApproach;
    private javax.swing.JTextField tfWalkTimeDeparture;
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

    /**
     * This method is called from within the constructor to initialize
     * the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bStart = new javax.swing.JButton();
        // Variables declaration - do not modify//GEN-BEGIN:variables
        javax.swing.JButton bCancel = new javax.swing.JButton();
        javax.swing.JButton bSave = new javax.swing.JButton();
        javax.swing.JTabbedPane jTabbedPane1 = new javax.swing.JTabbedPane();
        javax.swing.JPanel pGroupGenerate = new javax.swing.JPanel();
        pTTPara = new javax.swing.JPanel();
        javax.swing.JLabel lWalk = new javax.swing.JLabel();
        javax.swing.JLabel lBike = new javax.swing.JLabel();
        javax.swing.JLabel lPT = new javax.swing.JLabel();
        javax.swing.JLabel lMIV = new javax.swing.JLabel();
        javax.swing.JLabel lSpeed = new javax.swing.JLabel();
        javax.swing.JLabel lTimeAdditionApproach = new javax.swing.JLabel();
        tfWalkDist = new javax.swing.JTextField();
        tfBikeDist = new javax.swing.JTextField();
        tfPTDist = new javax.swing.JTextField();
        tfMIVDist = new javax.swing.JTextField();
        tfWalkTimeApproach = new javax.swing.JTextField();
        tfBikeTimeApproach = new javax.swing.JTextField();
        tfPTTimeApproach = new javax.swing.JTextField();
        tfMIVTimeApproach = new javax.swing.JTextField();
        javax.swing.JLabel lTimeAdditionDeparture = new javax.swing.JLabel();
        tfWalkTimeDeparture = new javax.swing.JTextField();
        tfBikeTimeDeparture = new javax.swing.JTextField();
        tfPTTimeDeparture = new javax.swing.JTextField();
        tfMIVTimeDeparture = new javax.swing.JTextField();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JPanel pInput = new javax.swing.JPanel();
        cbTerrain = new javax.swing.JComboBox<Terrain>();
        javax.swing.JLabel lTazTable = new javax.swing.JLabel();
        javax.swing.JLabel lTerrain = new javax.swing.JLabel();
        tfTazTable = new javax.swing.JTextField();
        javax.swing.JLabel lMatrixTable = new javax.swing.JLabel();
        tfMatrixTable = new javax.swing.JTextField();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        cbZValue = new javax.swing.JCheckBox();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        tfRecordName = new javax.swing.JTextField();
        javax.swing.JLabel lTop3 = new javax.swing.JLabel();
        cbTop3 = new javax.swing.JCheckBox();
        javax.swing.JPanel pMatrixCalc = new javax.swing.JPanel();
        javax.swing.JLabel lDist = new javax.swing.JLabel();
        javax.swing.JLabel lTT = new javax.swing.JLabel();
        javax.swing.JLabel lWalk1 = new javax.swing.JLabel();
        javax.swing.JLabel lBike1 = new javax.swing.JLabel();
        javax.swing.JLabel lPT1 = new javax.swing.JLabel();
        javax.swing.JLabel lMIV1 = new javax.swing.JLabel();
        cbWalkDist = new javax.swing.JCheckBox();
        cbBikeDist = new javax.swing.JCheckBox();
        cbPTDist = new javax.swing.JCheckBox();
        cbMIVDist = new javax.swing.JCheckBox();
        cbWalkTT = new javax.swing.JCheckBox();
        cbBikeTT = new javax.swing.JCheckBox();
        cbPTTT = new javax.swing.JCheckBox();
        cbMIVTT = new javax.swing.JCheckBox();
        lInfo = new javax.swing.JLabel();
        pGroupValidation = new javax.swing.JPanel();
        pValidation = new javax.swing.JPanel();
        javax.swing.JLabel lPath = new javax.swing.JLabel();
        tfPath = new javax.swing.JTextField();
        javax.swing.JButton bPath = new javax.swing.JButton();
        cbActivateValidation = new javax.swing.JCheckBox();
        javax.swing.JPanel pValDist = new javax.swing.JPanel();
        cbValWalkDist = new javax.swing.JCheckBox();
        cbValBikeDist = new javax.swing.JCheckBox();
        cbValPTDist = new javax.swing.JCheckBox();
        cbValMIVDist = new javax.swing.JCheckBox();
        javax.swing.JLabel lValDistRef = new javax.swing.JLabel();
        tfValWalkDist = new javax.swing.JTextField();
        tfValBikeDist = new javax.swing.JTextField();
        tfValPTDist = new javax.swing.JTextField();
        tfValMIVDist = new javax.swing.JTextField();
        javax.swing.JPanel pValTT = new javax.swing.JPanel();
        cbValWalkTT = new javax.swing.JCheckBox();
        cbValBikeTT = new javax.swing.JCheckBox();
        cbValPTTT = new javax.swing.JCheckBox();
        cbValMIVTT = new javax.swing.JCheckBox();
        javax.swing.JLabel lValTTRef = new javax.swing.JLabel();
        tfValWalkTT = new javax.swing.JTextField();
        tfValBikeTT = new javax.swing.JTextField();
        tfValPTTT = new javax.swing.JTextField();
        tfValMIVTT = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
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

        pTTPara.setBorder(javax.swing.BorderFactory.createTitledBorder("Reisezeiten-Parameter"));

        lWalk.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lWalk.setText("WALK");

        lBike.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lBike.setText("BIKE");

        lPT.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lPT.setText("PT");

        lMIV.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lMIV.setText("MIV");

        lSpeed.setText("Fahrgeschwindigkeit Vm [km/h]");

        lTimeAdditionApproach.setText("Zugang Zmz [min]");

        tfWalkDist.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfWalkDist.setText(properties.getProperty("speedWalk"));

        tfBikeDist.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfBikeDist.setText(properties.getProperty("speedBike"));

        tfPTDist.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfPTDist.setText(properties.getProperty("speedPT"));

        tfMIVDist.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfMIVDist.setText(properties.getProperty("speedMIV"));

        tfWalkTimeApproach.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfWalkTimeApproach.setText(properties.getProperty("timeWalkApproach"));

        tfBikeTimeApproach.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfBikeTimeApproach.setText(properties.getProperty("timeBikeApproach"));

        tfPTTimeApproach.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfPTTimeApproach.setText(properties.getProperty("timePTApproach"));

        tfMIVTimeApproach.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfMIVTimeApproach.setText(properties.getProperty("timeMIVApproach"));

        lTimeAdditionDeparture.setText("Abgang Zma [min]");

        tfWalkTimeDeparture.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfWalkTimeDeparture.setText(properties.getProperty("timeWalkDeparture"));

        tfBikeTimeDeparture.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfBikeTimeDeparture.setText(properties.getProperty("timeBikeDeparture"));

        tfPTTimeDeparture.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfPTTimeDeparture.setText(properties.getProperty("timePTDeparture"));

        tfMIVTimeDeparture.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfMIVTimeDeparture.setText(properties.getProperty("timeMIVDeparture"));

        jLabel3.setText("Zeitzuschlag Zm = Zmz + Zma [min]");

        javax.swing.GroupLayout pTTParaLayout = new javax.swing.GroupLayout(pTTPara);
        pTTPara.setLayout(pTTParaLayout);
        pTTParaLayout.setHorizontalGroup(pTTParaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                      .addGroup(pTTParaLayout.createSequentialGroup().addGroup(
                                                              pTTParaLayout.createParallelGroup(
                                                                      javax.swing.GroupLayout.Alignment.LEADING)
                                                                           .addComponent(lSpeed)
                                                                           .addComponent(lTimeAdditionApproach)
                                                                           .addComponent(lTimeAdditionDeparture))
                                                                             .addPreferredGap(
                                                                                     javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                     30, Short.MAX_VALUE).addGroup(
                                                                      pTTParaLayout.createParallelGroup(
                                                                              javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                   .addComponent(jLabel3,
                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                           204,
                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                   .addGroup(pTTParaLayout
                                                                                           .createSequentialGroup()
                                                                                           .addGroup(pTTParaLayout
                                                                                                   .createParallelGroup(
                                                                                                           javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                   .addGroup(
                                                                                                           pTTParaLayout
                                                                                                                   .createParallelGroup(
                                                                                                                           javax.swing.GroupLayout.Alignment.CENTER)
                                                                                                                   .addComponent(
                                                                                                                           tfWalkDist,
                                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                           35,
                                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                   .addComponent(
                                                                                                                           lWalk,
                                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                           29,
                                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                   .addComponent(
                                                                                                                           tfWalkTimeApproach,
                                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                           35,
                                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                   .addComponent(
                                                                                                           tfWalkTimeDeparture,
                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                           35,
                                                                                                           javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                           .addGap(18, 18, 18).addGroup(
                                                                                                   pTTParaLayout
                                                                                                           .createParallelGroup(
                                                                                                                   javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                   false)
                                                                                                           .addGroup(
                                                                                                                   pTTParaLayout
                                                                                                                           .createSequentialGroup()
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   javax.swing.GroupLayout.Alignment.CENTER)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfBikeDist,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   lBike,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   29,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   javax.swing.GroupLayout.Alignment.CENTER)
                                                                                                                                           .addComponent(
                                                                                                                                                   lPT,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   40,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfPTDist,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   javax.swing.GroupLayout.Alignment.CENTER)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfMIVDist,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   lMIV,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   27,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                                                           .addGroup(
                                                                                                                   pTTParaLayout
                                                                                                                           .createSequentialGroup()
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfBikeTimeApproach,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfBikeTimeDeparture,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfPTTimeApproach,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfPTTimeDeparture,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                                           .addGap(18,
                                                                                                                                   18,
                                                                                                                                   18)
                                                                                                                           .addGroup(
                                                                                                                                   pTTParaLayout
                                                                                                                                           .createParallelGroup(
                                                                                                                                                   javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfMIVTimeDeparture,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                                           .addComponent(
                                                                                                                                                   tfMIVTimeApproach,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                                   35,
                                                                                                                                                   javax.swing.GroupLayout.PREFERRED_SIZE))))))
                                                                             .addContainerGap(19, Short.MAX_VALUE)));

        pTTParaLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, lBike, lMIV, lPT, lWalk, tfBikeDist,
                tfBikeTimeApproach, tfBikeTimeDeparture, tfMIVDist, tfMIVTimeApproach, tfMIVTimeDeparture, tfPTDist,
                tfPTTimeApproach, tfPTTimeDeparture, tfWalkDist, tfWalkTimeApproach, tfWalkTimeDeparture);

        pTTParaLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, lSpeed, lTimeAdditionApproach,
                lTimeAdditionDeparture);

        pTTParaLayout.setVerticalGroup(pTTParaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(pTTParaLayout.createSequentialGroup().addGap(2, 2, 2)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   javax.swing.GroupLayout.Alignment.CENTER)
                                                                                                  .addComponent(lWalk)
                                                                                                  .addComponent(lBike)
                                                                                                  .addComponent(lPT)
                                                                                                  .addComponent(lMIV))
                                                                           .addPreferredGap(
                                                                                   javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                                  .addComponent(
                                                                                                          tfWalkDist,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfBikeDist,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfPTDist,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfMIVDist,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(lSpeed))
                                                                           .addPreferredGap(
                                                                                   javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                   javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                   Short.MAX_VALUE).addComponent(
                                                                    jLabel3).addPreferredGap(
                                                                    javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                                  .addComponent(
                                                                                                          tfWalkTimeApproach,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfBikeTimeApproach,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfPTTimeApproach,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          tfMIVTimeApproach,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          lTimeAdditionApproach))
                                                                           .addPreferredGap(
                                                                                   javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                           .addGroup(pTTParaLayout.createParallelGroup(
                                                                                   javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                  .addGroup(
                                                                                                          pTTParaLayout
                                                                                                                  .createParallelGroup(
                                                                                                                          javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                                                  .addComponent(
                                                                                                                          tfBikeTimeDeparture,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addComponent(
                                                                                                                          tfPTTimeDeparture,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addComponent(
                                                                                                                          tfMIVTimeDeparture,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addComponent(
                                                                                                                          lTimeAdditionDeparture))
                                                                                                  .addGroup(
                                                                                                          javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                          pTTParaLayout
                                                                                                                  .createSequentialGroup()
                                                                                                                  .addComponent(
                                                                                                                          tfWalkTimeDeparture,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                                  .addContainerGap()))));

        pTTParaLayout.linkSize(javax.swing.SwingConstants.VERTICAL, tfBikeDist, tfBikeTimeApproach, tfBikeTimeDeparture,
                tfMIVDist, tfMIVTimeApproach, tfMIVTimeDeparture, tfPTDist, tfPTTimeApproach, tfPTTimeDeparture,
                tfWalkDist, tfWalkTimeApproach, tfWalkTimeDeparture);

        pInput.setBorder(javax.swing.BorderFactory.createTitledBorder("Eingabeparameter"));

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

        javax.swing.GroupLayout pInputLayout = new javax.swing.GroupLayout(pInput);
        pInput.setLayout(pInputLayout);
        pInputLayout.setHorizontalGroup(pInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(pInputLayout.createSequentialGroup().addGroup(
                                                            pInputLayout.createParallelGroup(
                                                                    javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(lTazTable).addComponent(
                                                                    lMatrixTable).addComponent(jLabel2)
                                                                        .addComponent(lTerrain).addComponent(jLabel1))
                                                                          .addPreferredGap(
                                                                                  javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                  28, Short.MAX_VALUE)
                                                                          .addGroup(pInputLayout.createParallelGroup(
                                                                                  javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(
                                                                                                        tfTazTable,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        304,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(
                                                                                                        tfMatrixTable)
                                                                                                .addComponent(
                                                                                                        tfRecordName,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        301,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(cbTerrain,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        261,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGroup(pInputLayout
                                                                                                        .createSequentialGroup()
                                                                                                        .addComponent(
                                                                                                                cbZValue,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                60,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                        .addGap(32, 32,
                                                                                                                32)
                                                                                                        .addComponent(
                                                                                                                lTop3)
                                                                                                        .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                        .addComponent(
                                                                                                                cbTop3,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                51,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))));

        pInputLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, lMatrixTable, lTazTable, lTerrain);

        pInputLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, cbTerrain, tfMatrixTable, tfRecordName,
                tfTazTable);

        pInputLayout.setVerticalGroup(pInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                  .addGroup(pInputLayout.createSequentialGroup().addGap(2, 2, 2)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(lTazTable,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                      19,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(tfTazTable,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(
                                                                                                      lMatrixTable,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                      19,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(
                                                                                                      tfMatrixTable,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(
                                                                                                      tfRecordName,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(jLabel2))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE).addGroup(
                                                                  pInputLayout.createParallelGroup(
                                                                          javax.swing.GroupLayout.Alignment.BASELINE)
                                                                              .addComponent(cbTerrain,
                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                              .addComponent(lTerrain,
                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                      5,
                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pInputLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                              .addGroup(pInputLayout
                                                                                                      .createParallelGroup(
                                                                                                              javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                      .addComponent(
                                                                                                              cbZValue)
                                                                                                      .addComponent(
                                                                                                              lTop3)
                                                                                                      .addComponent(
                                                                                                              cbTop3))
                                                                                              .addComponent(jLabel1))));

        pInputLayout.linkSize(javax.swing.SwingConstants.VERTICAL, jLabel1, jLabel2, lMatrixTable, lTazTable, lTerrain,
                tfTazTable);

        pMatrixCalc.setBorder(javax.swing.BorderFactory.createTitledBorder("Matrixberechnung"));

        lDist.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lDist.setText("Distanzen");
        lDist.setToolTipText("");

        lTT.setText("Reisezeiten");

        lWalk1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lWalk1.setText("WALK");

        lBike1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lBike1.setText("BIKE");

        lPT1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lPT1.setText("PT");

        lMIV1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
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

        javax.swing.GroupLayout pMatrixCalcLayout = new javax.swing.GroupLayout(pMatrixCalc);
        pMatrixCalc.setLayout(pMatrixCalcLayout);
        pMatrixCalcLayout.setHorizontalGroup(
                pMatrixCalcLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                 .addGroup(pMatrixCalcLayout.createSequentialGroup().addGroup(
                                         pMatrixCalcLayout.createParallelGroup(
                                                 javax.swing.GroupLayout.Alignment.LEADING).addComponent(lDist)
                                                          .addComponent(lTT)).addPreferredGap(
                                         javax.swing.LayoutStyle.ComponentPlacement.RELATED, 132, Short.MAX_VALUE)
                                                            .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                                    javax.swing.GroupLayout.Alignment.CENTER)
                                                                                       .addComponent(lWalk1,
                                                                                               javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                               29,
                                                                                               javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                       .addComponent(cbWalkDist)
                                                                                       .addComponent(cbWalkTT)).addGap(
                                                 18, 18, 18).addGroup(pMatrixCalcLayout.createParallelGroup(
                                                 javax.swing.GroupLayout.Alignment.LEADING).addComponent(lBike1,
                                                 javax.swing.GroupLayout.PREFERRED_SIZE, 29,
                                                 javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(
                                                 pMatrixCalcLayout.createSequentialGroup().addGap(10, 10, 10)
                                                                  .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                                          javax.swing.GroupLayout.Alignment.LEADING)
                                                                                             .addComponent(cbBikeTT)
                                                                                             .addComponent(
                                                                                                     cbBikeDist))))
                                                            .addGap(18, 18, 18).addGroup(
                                                 pMatrixCalcLayout.createParallelGroup(
                                                         javax.swing.GroupLayout.Alignment.LEADING).addComponent(lPT1,
                                                         javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                         javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(
                                                         pMatrixCalcLayout.createSequentialGroup().addGap(10, 10, 10)
                                                                          .addGroup(
                                                                                  pMatrixCalcLayout.createParallelGroup(
                                                                                          javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                   .addComponent(cbPTTT)
                                                                                                   .addComponent(
                                                                                                           cbPTDist))))
                                                            .addGap(18, 18, 18)
                                                            .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                                    javax.swing.GroupLayout.Alignment.LEADING)
                                                                                       .addComponent(lMIV1,
                                                                                               javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                               27,
                                                                                               javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                       .addGroup(pMatrixCalcLayout
                                                                                               .createSequentialGroup()
                                                                                               .addGap(10, 10, 10)
                                                                                               .addGroup(
                                                                                                       pMatrixCalcLayout
                                                                                                               .createParallelGroup(
                                                                                                                       javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                               .addComponent(
                                                                                                                       cbMIVTT)
                                                                                                               .addComponent(
                                                                                                                       cbMIVDist))))
                                                            .addContainerGap()));

        pMatrixCalcLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, lBike1, lMIV1, lPT1, lWalk1);

        pMatrixCalcLayout.setVerticalGroup(
                pMatrixCalcLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                        pMatrixCalcLayout.createSequentialGroup().addGroup(pMatrixCalcLayout.createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                                pMatrixCalcLayout.createSequentialGroup().addGroup(
                                        pMatrixCalcLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                         .addComponent(lWalk1).addComponent(lBike1).addComponent(lPT1)
                                                         .addComponent(lMIV1)).addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(
                                        pMatrixCalcLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                         .addComponent(cbWalkDist,
                                                                 javax.swing.GroupLayout.Alignment.TRAILING)
                                                         .addComponent(cbBikeDist,
                                                                 javax.swing.GroupLayout.Alignment.TRAILING)
                                                         .addComponent(cbPTDist,
                                                                 javax.swing.GroupLayout.Alignment.TRAILING)
                                                         .addComponent(cbMIVDist,
                                                                 javax.swing.GroupLayout.Alignment.TRAILING))
                                                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                 .addGroup(pMatrixCalcLayout.createParallelGroup(
                                                         javax.swing.GroupLayout.Alignment.LEADING).addComponent(
                                                         cbWalkTT, javax.swing.GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(cbBikeTT,
                                                                                    javax.swing.GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(cbPTTT,
                                                                                    javax.swing.GroupLayout.Alignment.TRAILING)
                                                                            .addComponent(cbMIVTT,
                                                                                    javax.swing.GroupLayout.Alignment.TRAILING)))
                                                                                            .addGroup(pMatrixCalcLayout
                                                                                                    .createSequentialGroup()
                                                                                                    .addGap(23, 23, 23)
                                                                                                    .addComponent(lDist)
                                                                                                    .addPreferredGap(
                                                                                                            javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                    .addComponent(lTT)))
                                         .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        lInfo.setForeground(new java.awt.Color(0, 102, 102));

        javax.swing.GroupLayout pGroupGenerateLayout = new javax.swing.GroupLayout(pGroupGenerate);
        pGroupGenerate.setLayout(pGroupGenerateLayout);
        pGroupGenerateLayout.setHorizontalGroup(
                pGroupGenerateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                        pGroupGenerateLayout.createSequentialGroup().addContainerGap()
                                            .addGroup(pGroupGenerateLayout.createParallelGroup(
                                                    javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                                                    pGroupGenerateLayout.createSequentialGroup().addGap(7, 7, 7)
                                                                        .addComponent(lInfo,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)).addGroup(
                                                    pGroupGenerateLayout.createSequentialGroup().addGroup(
                                                            pGroupGenerateLayout.createParallelGroup(
                                                                    javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(pInput,
                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                        Short.MAX_VALUE)
                                                                                .addComponent(pMatrixCalc,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(pTTPara,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addGap(0, 10, Short.MAX_VALUE)))
                                            .addContainerGap()));

        pGroupGenerateLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, pInput, pMatrixCalc, pTTPara);

        pGroupGenerateLayout.setVerticalGroup(
                pGroupGenerateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                            pGroupGenerateLayout.createSequentialGroup().addContainerGap().addComponent(
                                                    pInput, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                    javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
                                                    pMatrixCalc, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                    javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
                                                    pTTPara, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                    javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(lInfo,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 16,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(24, Short.MAX_VALUE)));

        jTabbedPane1.addTab("Generierung", pGroupGenerate);

        pValidation.setBorder(javax.swing.BorderFactory.createTitledBorder("Validierung"));

        lPath.setText("Ausgabeordner:");

        tfPath.setText(properties.getProperty("path"));

        bPath.setText("...");
        bPath.addActionListener(this::bPathActionPerformed);

        cbActivateValidation.setSelected(Boolean.parseBoolean(properties.getProperty("validation")));
        cbActivateValidation.setText("Validierung aktivieren");
        cbActivateValidation.addItemListener(this::cbActivateValidationItemStateChanged);

        pValDist.setBorder(javax.swing.BorderFactory.createTitledBorder("Distanzen"));

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

        javax.swing.GroupLayout pValDistLayout = new javax.swing.GroupLayout(pValDist);
        pValDist.setLayout(pValDistLayout);
        pValDistLayout.setHorizontalGroup(pValDistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(
                                                                pValDistLayout.createSequentialGroup().addContainerGap()
                                                                              .addGroup(pValDistLayout
                                                                                      .createParallelGroup(
                                                                                              javax.swing.GroupLayout.Alignment.LEADING)
                                                                                      .addComponent(cbValWalkDist)
                                                                                      .addComponent(cbValBikeDist)
                                                                                      .addComponent(cbValPTDist)
                                                                                      .addComponent(cbValMIVDist))
                                                                              .addGap(18, 18, 18).addGroup(
                                                                        pValDistLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                false).addComponent(tfValPTDist,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                302, Short.MAX_VALUE).addComponent(
                                                                                tfValBikeDist)
                                                                                      .addComponent(tfValMIVDist)
                                                                                      .addComponent(tfValWalkDist))
                                                                              .addContainerGap(
                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                      Short.MAX_VALUE))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                pValDistLayout.createSequentialGroup().addContainerGap(
                                                                        180, Short.MAX_VALUE).addComponent(lValDistRef)
                                                                              .addGap(133, 133, 133)));
        pValDistLayout.setVerticalGroup(pValDistLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                      .addGroup(pValDistLayout.createSequentialGroup().addGroup(
                                                              pValDistLayout.createParallelGroup(
                                                                      javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGroup(pValDistLayout
                                                                                    .createSequentialGroup()
                                                                                    .addComponent(lValDistRef)
                                                                                    .addGap(0, 0, Short.MAX_VALUE))
                                                                            .addGroup(
                                                                                    javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                    pValDistLayout
                                                                                            .createSequentialGroup()
                                                                                            .addContainerGap(20,
                                                                                                    Short.MAX_VALUE)
                                                                                            .addGroup(pValDistLayout
                                                                                                    .createParallelGroup(
                                                                                                            javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                                    .addComponent(
                                                                                                            tfValWalkDist,
                                                                                                            javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                    .addComponent(
                                                                                                            cbValWalkDist))))
                                                                              .addGap(3, 3, 3).addGroup(
                                                                      pValDistLayout.createParallelGroup(
                                                                              javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                    .addComponent(tfValBikeDist,
                                                                                            javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                    .addComponent(cbValBikeDist))
                                                                              .addPreferredGap(
                                                                                      javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                              .addGroup(pValDistLayout
                                                                                      .createParallelGroup(
                                                                                              javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                      .addComponent(tfValPTDist,
                                                                                              javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                              javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                              javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                      .addComponent(cbValPTDist))
                                                                              .addGap(3, 3, 3).addGroup(
                                                                      pValDistLayout.createParallelGroup(
                                                                              javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                    .addComponent(cbValMIVDist)
                                                                                    .addComponent(tfValMIVDist,
                                                                                            javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                            javax.swing.GroupLayout.PREFERRED_SIZE))));

        pValTT.setBorder(javax.swing.BorderFactory.createTitledBorder("Reisezeiten"));

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

        javax.swing.GroupLayout pValTTLayout = new javax.swing.GroupLayout(pValTT);
        pValTT.setLayout(pValTTLayout);
        pValTTLayout.setHorizontalGroup(pValTTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(pValTTLayout.createSequentialGroup().addContainerGap()
                                                                          .addGroup(pValTTLayout.createParallelGroup(
                                                                                  javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(
                                                                                                        cbValWalkTT)
                                                                                                .addComponent(
                                                                                                        cbValBikeTT)
                                                                                                .addComponent(cbValPTTT)
                                                                                                .addComponent(
                                                                                                        cbValMIVTT))
                                                                          .addGap(18, 18, 18)
                                                                          .addGroup(pValTTLayout.createParallelGroup(
                                                                                  javax.swing.GroupLayout.Alignment.LEADING,
                                                                                  false).addComponent(tfValPTTT,
                                                                                  javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                  302, Short.MAX_VALUE).addComponent(
                                                                                  tfValBikeTT).addComponent(tfValMIVTT)
                                                                                                .addComponent(
                                                                                                        tfValWalkTT))
                                                                          .addContainerGap(
                                                                                  javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                  Short.MAX_VALUE))
                                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                            pValTTLayout.createSequentialGroup().addContainerGap(180,
                                                                    Short.MAX_VALUE).addComponent(lValTTRef)
                                                                        .addGap(133, 133, 133)));
        pValTTLayout.setVerticalGroup(pValTTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                  .addGroup(pValTTLayout.createSequentialGroup().addGroup(
                                                          pValTTLayout.createParallelGroup(
                                                                  javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                                                                  pValTTLayout.createSequentialGroup()
                                                                              .addComponent(lValTTRef)
                                                                              .addGap(0, 0, Short.MAX_VALUE)).addGroup(
                                                                  javax.swing.GroupLayout.Alignment.TRAILING,
                                                                  pValTTLayout.createSequentialGroup()
                                                                              .addContainerGap(20, Short.MAX_VALUE)
                                                                              .addGroup(
                                                                                      pValTTLayout.createParallelGroup(
                                                                                              javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                                  .addComponent(
                                                                                                          tfValWalkTT,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                          javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                          javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                  .addComponent(
                                                                                                          cbValWalkTT))))
                                                                        .addGap(3, 3, 3).addGroup(
                                                                  pValTTLayout.createParallelGroup(
                                                                          javax.swing.GroupLayout.Alignment.BASELINE)
                                                                              .addComponent(tfValBikeTT,
                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                              .addComponent(cbValBikeTT))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(pValTTLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(tfValPTTT,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                              .addComponent(cbValPTTT))
                                                                        .addGap(3, 3, 3)
                                                                        .addGroup(pValTTLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                              .addComponent(cbValMIVTT)
                                                                                              .addComponent(tfValMIVTT,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                      javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                      javax.swing.GroupLayout.PREFERRED_SIZE))));

        javax.swing.GroupLayout pValidationLayout = new javax.swing.GroupLayout(pValidation);
        pValidation.setLayout(pValidationLayout);
        pValidationLayout.setHorizontalGroup(
                pValidationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                        pValidationLayout.createSequentialGroup().addGroup(pValidationLayout.createParallelGroup(
                                javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                                pValidationLayout.createSequentialGroup().addContainerGap()
                                                 .addGroup(pValidationLayout.createParallelGroup(
                                                         javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                                                         pValidationLayout.createSequentialGroup()
                                                                          .addComponent(cbActivateValidation,
                                                                                  javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                  166,
                                                                                  javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                          .addGap(0, 0, Short.MAX_VALUE)).addGroup(
                                                         pValidationLayout.createSequentialGroup().addComponent(lPath)
                                                                          .addPreferredGap(
                                                                                  javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                          .addComponent(tfPath).addPreferredGap(
                                                                 javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                          .addComponent(bPath,
                                                                                  javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                  30,
                                                                                  javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                                                            .addComponent(pValDist,
                                                                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                    Short.MAX_VALUE)
                                                                                            .addComponent(pValTT,
                                                                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                    Short.MAX_VALUE))
                                         .addContainerGap()));
        pValidationLayout.setVerticalGroup(
                pValidationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                 .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                         pValidationLayout.createSequentialGroup().addContainerGap().addComponent(
                                                 cbActivateValidation).addPreferredGap(
                                                 javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(
                                                 pValDist, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                 javax.swing.GroupLayout.DEFAULT_SIZE,
                                                 javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                 javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(
                                                 pValTT, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                 javax.swing.GroupLayout.DEFAULT_SIZE,
                                                 javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(
                                                 javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45,
                                                 Short.MAX_VALUE).addGroup(pValidationLayout.createParallelGroup(
                                                 javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lPath)
                                                                                            .addComponent(tfPath,
                                                                                                    javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                            .addComponent(bPath))
                                                          .addContainerGap()));

        javax.swing.GroupLayout pGroupValidationLayout = new javax.swing.GroupLayout(pGroupValidation);
        pGroupValidation.setLayout(pGroupValidationLayout);
        pGroupValidationLayout.setHorizontalGroup(
                pGroupValidationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                        pGroupValidationLayout.createSequentialGroup().addContainerGap()
                                              .addComponent(pValidation, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                      javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                              .addContainerGap()));
        pGroupValidationLayout.setVerticalGroup(
                pGroupValidationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                        pGroupValidationLayout.createSequentialGroup().addContainerGap()
                                              .addComponent(pValidation, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                      javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                              .addContainerGap()));

        jTabbedPane1.addTab("Validierung", pGroupValidation);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                                                layout.createSequentialGroup().addGap(20, 20, 20).addComponent(bCancel)
                                                      .addPreferredGap(
                                                              javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                              javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                      .addComponent(bSave).addPreferredGap(
                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                      .addComponent(bStart)).addGroup(
                                                layout.createSequentialGroup().addContainerGap()
                                                      .addComponent(jTabbedPane1))).addContainerGap()));

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, bCancel, bSave, bStart);

        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                      .addGroup(layout.createSequentialGroup().addContainerGap().addComponent(
                                              jTabbedPane1).addPreferredGap(
                                              javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                      .addGroup(layout.createParallelGroup(
                                                              javax.swing.GroupLayout.Alignment.BASELINE).addComponent(
                                                              bCancel).addComponent(bSave).addComponent(bStart))
                                                      .addGap(11, 11, 11)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void start() {
        try {
            properties = PropertyReader.getProperties(CruisingSpeed.propertyFileName);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

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

    private void bCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_bCancelActionPerformed
    {//GEN-HEADEREND:event_bCancelActionPerformed
        close();
    }//GEN-LAST:event_bCancelActionPerformed

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

    private void cbTerrainItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbTerrainItemStateChanged
    {//GEN-HEADEREND:event_cbTerrainItemStateChanged
        if (!cbTerrain.getSelectedItem().toString().equals(properties.getProperty("terrain"))) changes = true;
    }//GEN-LAST:event_cbTerrainItemStateChanged

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

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing

    private void cbWalkDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbWalkDistItemStateChanged
    {//GEN-HEADEREND:event_cbWalkDistItemStateChanged
        //changes = changes | distanceSelected(Modus.WALK);
        distanceSelected(Modus.WALK);
        changes = true;
    }//GEN-LAST:event_cbWalkDistItemStateChanged

    private void cbBikeDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbBikeDistItemStateChanged
    {//GEN-HEADEREND:event_cbBikeDistItemStateChanged
        //changes = changes | distanceSelected(Modus.BIKE);
        distanceSelected(Modus.BIKE);
        changes = true;
    }//GEN-LAST:event_cbBikeDistItemStateChanged

    private void cbPTDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbPTDistItemStateChanged
    {//GEN-HEADEREND:event_cbPTDistItemStateChanged
        //changes = changes | distanceSelected(Modus.PT);
        distanceSelected(Modus.PT);
        changes = true;
    }//GEN-LAST:event_cbPTDistItemStateChanged

    private void cbMIVDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbMIVDistItemStateChanged
    {//GEN-HEADEREND:event_cbMIVDistItemStateChanged
        //changes = changes | distanceSelected(Modus.MIV);
        distanceSelected(Modus.MIV);
        changes = true;
    }//GEN-LAST:event_cbMIVDistItemStateChanged

    private void cbWalkTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbWalkTTItemStateChanged
    {//GEN-HEADEREND:event_cbWalkTTItemStateChanged
        //changes = changes | distanceSelected(Modus.WALK);
        distanceSelected(Modus.WALK);
        changes = true;
    }//GEN-LAST:event_cbWalkTTItemStateChanged

    private void cbBikeTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbBikeTTItemStateChanged
    {//GEN-HEADEREND:event_cbBikeTTItemStateChanged
        //changes = changes | distanceSelected(Modus.BIKE);
        distanceSelected(Modus.BIKE);
        changes = true;
    }//GEN-LAST:event_cbBikeTTItemStateChanged

    private void cbPTTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbPTTTItemStateChanged
    {//GEN-HEADEREND:event_cbPTTTItemStateChanged
        //changes = changes | distanceSelected(Modus.PT);
        distanceSelected(Modus.PT);
        changes = true;
    }//GEN-LAST:event_cbPTTTItemStateChanged

    private void cbMIVTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbMIVTTItemStateChanged
    {//GEN-HEADEREND:event_cbMIVTTItemStateChanged
        //changes = changes | distanceSelected(Modus.MIV);
        distanceSelected(Modus.MIV);
        changes = true;
    }//GEN-LAST:event_cbMIVTTItemStateChanged

    private void cbZValueItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbZValueItemStateChanged
    {//GEN-HEADEREND:event_cbZValueItemStateChanged
        changes = true;
    }//GEN-LAST:event_cbZValueItemStateChanged

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

    private void cbValWalkDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValWalkDistItemStateChanged
    {//GEN-HEADEREND:event_cbValWalkDistItemStateChanged
        enableCheckboxComponents(evt, tfValWalkDist);
    }//GEN-LAST:event_cbValWalkDistItemStateChanged

    private void cbValBikeDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValBikeDistItemStateChanged
    {//GEN-HEADEREND:event_cbValBikeDistItemStateChanged
        enableCheckboxComponents(evt, tfValBikeDist);
    }//GEN-LAST:event_cbValBikeDistItemStateChanged

    private void cbValPTDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValPTDistItemStateChanged
    {//GEN-HEADEREND:event_cbValPTDistItemStateChanged
        enableCheckboxComponents(evt, tfValPTDist);
    }//GEN-LAST:event_cbValPTDistItemStateChanged

    private void cbValMIVDistItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValMIVDistItemStateChanged
    {//GEN-HEADEREND:event_cbValMIVDistItemStateChanged
        enableCheckboxComponents(evt, tfValMIVDist);
    }//GEN-LAST:event_cbValMIVDistItemStateChanged

    private void cbValWalkTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValWalkTTItemStateChanged
    {//GEN-HEADEREND:event_cbValWalkTTItemStateChanged
        enableCheckboxComponents(evt, tfValWalkTT);
    }//GEN-LAST:event_cbValWalkTTItemStateChanged

    private void cbValBikeTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValBikeTTItemStateChanged
    {//GEN-HEADEREND:event_cbValBikeTTItemStateChanged
        enableCheckboxComponents(evt, tfValBikeTT);
    }//GEN-LAST:event_cbValBikeTTItemStateChanged

    private void cbValPTTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValPTTTItemStateChanged
    {//GEN-HEADEREND:event_cbValPTTTItemStateChanged
        enableCheckboxComponents(evt, tfValPTTT);
    }//GEN-LAST:event_cbValPTTTItemStateChanged

    private void cbValMIVTTItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbValMIVTTItemStateChanged
    {//GEN-HEADEREND:event_cbValMIVTTItemStateChanged
        enableCheckboxComponents(evt, tfValMIVTT);
    }//GEN-LAST:event_cbValMIVTTItemStateChanged

    private void cbTop3ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_cbTop3ItemStateChanged
    {//GEN-HEADEREND:event_cbTop3ItemStateChanged
        changes = true;
    }//GEN-LAST:event_cbTop3ItemStateChanged

    private void tfTazTableKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_tfTazTableKeyReleased
    {//GEN-HEADEREND:event_tfTazTableKeyReleased
        checkMandantoryFields();
    }//GEN-LAST:event_tfTazTableKeyReleased

    private void tfMatrixTableKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_tfMatrixTableKeyReleased
    {//GEN-HEADEREND:event_tfMatrixTableKeyReleased
        checkMandantoryFields();
    }//GEN-LAST:event_tfMatrixTableKeyReleased

    private void tfRecordNameKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_tfRecordNameKeyReleased
    {//GEN-HEADEREND:event_tfRecordNameKeyReleased
        checkMandantoryFields();
    }//GEN-LAST:event_tfRecordNameKeyReleased

    private void bStartActionPerformed(java.awt.event.ActionEvent evt) {
        if (changes) savePropertieFile();
        CruisingSpeed cs = new CruisingSpeed();
        CruisingSpeed.setProperties(properties);
        cs.start(null);
        changes = false;
        JOptionPane.showMessageDialog(this, "Die Daten wurden generiert");
    }
    // End of variables declaration//GEN-END:variables

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

    private void close() {
        int choice;
        if (changes) {
            choice = JOptionPane.showConfirmDialog(this, "Wollen Sie die Änderungen speichern?", "Speichern",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.YES_OPTION) savePropertieFile();
        }
        System.exit(0);
    }

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

    private void enableCheckboxComponents(java.awt.event.ItemEvent evt, Container container) {
        if (((JCheckBox) evt.getSource()).isSelected()) {
            enableComponents(container, true);
        } else enableComponents(container, false);

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

    private void checkMandantoryFields() {
        if (tfTazTable.getText().isEmpty() || tfMatrixTable.getText().isEmpty() || tfRecordName.getText().isEmpty())
            enableComponents(bStart, false);
        else enableComponents(bStart, true);

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

        if (!cbWalkTT.isSelected() && !cbBikeTT.isSelected() && !cbPTTT.isSelected() && !cbMIVTT.isSelected())
            enableComponents(pTTPara, false);
        else enableComponents(pTTPara, true);

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
        private JTextField textField;
        private String propertieKey;

        public TextFieldDokumentlistener(JTextField textField, String propertieKey) {
            this.textField = textField;
            this.propertieKey = propertieKey;
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

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (!textField.getText().trim().equals(properties.getProperty(propertieKey))) {
                //  System.out.println("Changes = "+changes+"\nchangedUpdate: "+textField.getText()+", "+properties.getProperty(propertieKey));
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