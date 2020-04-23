package de.dlr.ivf.tapas.tools.fileModifier;

//VERY SIMPLE!
//NO CHECKS!
//QUICK AND DIRTY!
//ALLWAYS WRITE TO A NEW FILE!
//EXPERTS ONLY!


import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

public class ExpertKnowledgeHandler {
	private JFrame controlInstance;  //  @jve:decl-index=0:visual-constraint="11,106"
	private JPanel jMainContentPane = null;
	private JPanel jTargetPanel = null;
	private JList<String> jPersonsList = null;
	private JList<String> jActionsList = null;
	private JList<String> jDistancesList = null;
	private JPanel jValuesPanel = null;
	private JTextField jMIVTextFieldF = null;
	private JTextField jMIVTextFieldS = null;
	private JTextField jPassTextFieldF = null;
	private JTextField jPassTextFieldS = null;
	private JTextField jOeVTextFieldF = null;
	private JTextField jOeVTextFieldS = null;
	private JTextField jBikeTextFieldF = null;
	private JTextField jBikeTextFieldS = null;
	private JTextField jWalkTextFieldF = null;
	private JTextField jWalkTextFieldS = null;
	private JTextField jMiscTextFieldF = null;
	private JTextField jMiscTextFieldS = null;
	private JTextField jTaxiTextFieldF = null;
	private JTextField jTaxiTextFieldS = null;


	private JPanel jOutputPanel = null;
	private JTextField jFilenameTextField = null;
	private JButton jAddButton = null;
	private HashMap<Integer,Integer> distanceConverter = new HashMap<>();  //  @jve:decl-index=0:
	private HashMap<Integer,Integer[]> actionConverter = new HashMap<>();
	private JList<String> jBBRCatList = null;
	public JFrame createAndShowGUI(){
		this.controlInstance = createGui();
		return this.controlInstance;
	}
	
	private JFrame createGui() {
		final JFrame jFrame = new JFrame();
		Toolkit jTools = Toolkit.getDefaultToolkit();
		Dimension dim = jTools.getScreenSize();
		jFrame.setSize(800,600);
		jFrame.setContentPane(getJMainContentPane());
		jFrame.setLocation(dim.width/2 - jFrame.getWidth()/2, dim.height/2 - jFrame.getHeight()/2);
		
		jFrame.setTitle("TAPAS Expert Knowledge Handler");
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		jFrame.addWindowListener(new java.awt.event.WindowAdapter() {
//			public void windowClosing(java.awt.event.WindowEvent e) {
//			}
//		});
		jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		jFrame.pack();
		return jFrame;
	}	
	
	/**
	 * This method initializes jMainContentPane	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJMainContentPane() {
		if (jMainContentPane == null) {
			jMainContentPane = new JPanel();
			jMainContentPane.setLayout(new BorderLayout());
			jMainContentPane.add(getJTargetPanel(), BorderLayout.NORTH);
			jMainContentPane.add(getJValuesPanel(), BorderLayout.EAST);
			jMainContentPane.add(getJOutputPanel(), BorderLayout.SOUTH);
		}
		return jMainContentPane;
	}

	/**
	 * This method initializes jTargetPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJTargetPanel() {
		if (jTargetPanel == null) {
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.fill = GridBagConstraints.BOTH;
			gridBagConstraints11.weighty = 0.0;
			gridBagConstraints11.weightx = 0.0;
			JLabel jSpacerLabel2b = new JLabel();
			jSpacerLabel2b.setText(" ");
			JLabel jSpacer2Label = new JLabel();
			jSpacer2Label.setText(" ");
			JLabel jSpacer1Label = new JLabel();
			jSpacer1Label.setText(" ");
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.fill = GridBagConstraints.BOTH;
			gridBagConstraints2.weighty = 0.0;
			gridBagConstraints2.weightx = 0.0;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.BOTH;
			gridBagConstraints1.weighty = 0.0;
			gridBagConstraints1.weightx = 0.0;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.weighty = 0.0;
			gridBagConstraints.weightx = 0.0;
			jTargetPanel = new JPanel();
			jTargetPanel.setLayout(new GridBagLayout());
			jTargetPanel.add(getJPersonsList(), gridBagConstraints);
			jTargetPanel.add(jSpacer1Label, new GridBagConstraints());
			jTargetPanel.add(getJActionsList(), gridBagConstraints1);
			jTargetPanel.add(jSpacer2Label, new GridBagConstraints());
			jTargetPanel.add(getJDistancesList(), gridBagConstraints2);
			jTargetPanel.add(jSpacerLabel2b, new GridBagConstraints());
			jTargetPanel.add(getJBBRCatList(), gridBagConstraints11);
		}
		return jTargetPanel;
	}

	/**
	 * This method initializes jPersonsList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList<String> getJPersonsList() {
		if (jPersonsList == null) {
			String[] data = {
					"0  Kinder unter 6",
					"1  Schüler",
					"2  Studenten",
					"3  erwerbstätig, Mann, Pkw im Haushalt, bis 24Jahre",
					"4  erwerbstätig, Mann oder Frau, kein Pkw im Haushalt, bis 24Jahre",
					"5  erwerbstätig, Frau, Pkw im Haushalt, bis 24 Jahre",
					"6  nichterwerbstätig, Mann, Pkw im Haushalt, bis 24 Jahre",
					"7  nichterwerbstätig, Mann oder Frau, kein Pkw im Haushalt, bis 24 Jahre",
					"8  nichterwerbstätig, Frau, Pkw im Haushalt, bis 24 Jahre",
					"9  erwerbstätig, Mann, Pkw im Haushalt, 25 bis 44 Jahre",
					"10 erwerbstätig, Mann, kein Pkw im Haushalt, 25 bis 44 Jahre",
					"11 erwerbstätig, Frau, Pkw im Haushalt, 25 bis 44 Jahre",
					"12 erwerbstätig, Frau, kein Pkw im Haushalt, 25 bis 44 Jahre",
					"13 nichterwerbstätig, Mann, Pkw im Haushalt, 25 bis 44 Jahre",
					"14 nichterwerbstätig, Mann, kein Pkw im Haushalt, 25 bis 44 Jahre",
					"15 nichterwerbstätig, Frau, Pkw im Haushalt, 25 bis 44 Jahre",
					"16 nichterwerbstätig, Frau, kein Pkw im Haushalt, 25 bis 44 Jahre",
					"17 erwerbstätig, Mann, Pkw im Haushalt, 45 bis 64 Jahre",
					"18 erwerbstätig, Mann, kein Pkw im Haushalt, 45 bis 64 Jahre",
					"19 erwerbstätig, Frau, Pkw im Haushalt, 45 bis 64 Jahre",
					"20 erwerbstätig, Frau, kein Pkw im Haushalt, 45 bis 64 Jahre",
					"21 nichterwerbstätig, Mann, Pkw im Haushalt, 45 bis 64 Jahre",
					"22 nichterwerbstätig, Mann, kein Pkw im Haushalt, 45 bis 64 Jahre",
					"23 nichterwerbstätig, Frau, Pkw im Haushalt, 45 bis 64 Jahre",
					"24 nichterwerbstätig, Frau, kein Pkw im Haushalt, 45 bis 64 Jahre",
					"25 Rentner, Mann, Pkw im Haushalt, 65 bis 74 Jahre",
					"26 Rentner, Mann, kein Pkw im Haushalt, 65 bis 74 Jahre",
					"27 Rentner, Frau, Pkw im Haushalt, 65 bis 74 Jahre",
					"28 Rentner, Frau, kein Pkw im Haushalt, 65 bis 74 Jahre",
					"29 Rentner, Mann, Pkw im Haushalt, ab 75 Jahre",
					"30 Rentner, Mann, kein Pkw im Haushalt, ab 75 Jahre",
					"31 Rentner, Frau, Pkw im Haushalt, ab 75 Jahre",
					"32 Rentner, Frau, kein Pkw im Haushalt, ab 75 Jahre"			};			
			jPersonsList = new JList<>(data);
		}
		return jPersonsList;
	}

	/**
	 * This method initializes jActionsList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList<String> getJActionsList() {
		if (jActionsList == null) {
			String[] data = {
					"0   Schule   ",
					"1   Studium  ",
					"2   Arbeit   ",
					"3   PriErl.  ",
					"4   Einkauf  ",
					"5   Freizeit ",
					"6   Sonstige "					
			};
			Integer[] array0 = {410};
			actionConverter.put(0, array0);
			Integer[] array1 = {410};
			actionConverter.put(1, array1);
			Integer[] array2 = {211, 522};
			actionConverter.put(2, array2);
			Integer[] array3 = {10, 12, 32, 522, 611, 631};
			actionConverter.put(3, array3);
			Integer[] array4 = {50};
			actionConverter.put(4, array4);
			Integer[] array5 = {299, 300, 640, 720, 721, 722, 724, 740, 800};
			actionConverter.put(5, array5);
			Integer[] array6 = {999};
			actionConverter.put(6, array6);
		
			/*
			Integer[] array0 = {410,412,413,414,881,53};
			actionConverter.put(0, array0);
			Integer[] array1 = {411};
			actionConverter.put(1, array1);
			Integer[] array2 = {211, 212,213,62};
			actionConverter.put(2, array2);
			Integer[] array3 = {10,12,32,522,611,631};
			actionConverter.put(3, array3);
			Integer[] array4 = {50,51,52,53};
			actionConverter.put(4, array4);
			Integer[] array5 = {231,299,511,512,531,533,700,711,800,300,640,720,721,722,723,724,740};
			actionConverter.put(5, array5);
			Integer[] array6 = {799,999};
			actionConverter.put(6, array6);

			 */
			
			jActionsList = new JList<>(data);
		}
		return jActionsList;
	}

	/**
	 * This method initializes jDistancesList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList<String> getJDistancesList() {
		if (jDistancesList == null) {
			String[] data = {"200", "500", "1000", "2000", "5000", "7000", "10000", "15000", "20000", "30000", "50000"};	
			distanceConverter.put(0, 200);
			distanceConverter.put(1, 500);
			distanceConverter.put(2, 1000);
			distanceConverter.put(3, 2000);
			distanceConverter.put(4, 5000);
			distanceConverter.put(5, 7000);
			distanceConverter.put(6, 10000);
			distanceConverter.put(7, 15000);
			distanceConverter.put(8, 20000);
			distanceConverter.put(9, 30000);
			distanceConverter.put(10, 50000);
			jDistancesList = new JList<>(data);
		}
		return jDistancesList;
	}

	/**
	 * This method initializes jValuesPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJValuesPanel() {
		if (jValuesPanel == null) {
			JLabel jTaxiLabelF = new JLabel();
			jTaxiLabelF.setText("Taxi F");
			JLabel jTaxiLabelS = new JLabel();
			jTaxiLabelS.setText("Taxi S");
			JLabel jMIscLabelF = new JLabel();
			jMIscLabelF.setText("Sonst. F");
			JLabel jMIscLabelS = new JLabel();
			jMIscLabelS.setText("Sonst. S");
			JLabel jWalkLabelF = new JLabel();
			jWalkLabelF.setText("Walk F");
			JLabel jWalkLabelS = new JLabel();
			jWalkLabelS.setText("Walk S");
			JLabel jBikeLabelF = new JLabel();
			jBikeLabelF.setText("Bike F");
			JLabel jBikeLabelS = new JLabel();
			jBikeLabelS.setText("Bike S");
			JLabel jOeVLabelF = new JLabel();
			jOeVLabelF.setText("ÖV F");
			JLabel jOeVLabelS = new JLabel();
			jOeVLabelS.setText("ÖV S");
			JLabel jPassLabelF = new JLabel();
			jPassLabelF.setText("Passagier F");
			JLabel jPassLabelS = new JLabel();
			jPassLabelS.setText("Passagier S");
			JLabel jMIVLabelF = new JLabel();
			jMIVLabelF.setText("MIV F");
			JLabel jMIVLabelS = new JLabel();
			jMIVLabelS.setText("MIV S");
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints4.gridy = 1;
			gridBagConstraints4.weightx = 0.0;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints3.weightx = 0.0;
			gridBagConstraints3.gridy = 0;
			
			gridBagConstraints3.ipadx = 25;
			gridBagConstraints4.ipadx = 25;
			jValuesPanel = new JPanel();
			jValuesPanel.setLayout(new GridBagLayout());
			jValuesPanel.add(jMIVLabelF, gridBagConstraints3);
			jValuesPanel.add(getJMIVTextFieldF(), gridBagConstraints3);
			jValuesPanel.add(jMIVLabelS, gridBagConstraints4);
			jValuesPanel.add(getJMIVTextFieldS(), gridBagConstraints4);
			jValuesPanel.add(jPassLabelF, gridBagConstraints3);
			jValuesPanel.add(getJPassTextFieldF(), gridBagConstraints3);
			jValuesPanel.add(jPassLabelS, gridBagConstraints4);
			jValuesPanel.add(getJPassTextFieldS(), gridBagConstraints4);
			jValuesPanel.add(jOeVLabelF, gridBagConstraints3);
			jValuesPanel.add(getJOeVTextFieldF(), gridBagConstraints3);
			jValuesPanel.add(jOeVLabelS, gridBagConstraints4);
			jValuesPanel.add(getJOeVTextFieldS(), gridBagConstraints4);
			jValuesPanel.add(jBikeLabelF, gridBagConstraints3);
			jValuesPanel.add(getJBikeTextFieldF(), gridBagConstraints3);
			jValuesPanel.add(jBikeLabelS, gridBagConstraints4);
			jValuesPanel.add(getJBikeTextFieldS(), gridBagConstraints4);
			jValuesPanel.add(jWalkLabelF, gridBagConstraints3);
			jValuesPanel.add(getJWalkTextFieldF(), gridBagConstraints3);
			jValuesPanel.add(jWalkLabelS, gridBagConstraints4);
			jValuesPanel.add(getJWalkTextFieldS(), gridBagConstraints4);
			jValuesPanel.add(jMIscLabelF, gridBagConstraints3);
			jValuesPanel.add(getJMiscTextFieldF(), gridBagConstraints3);
			jValuesPanel.add(jMIscLabelS, gridBagConstraints4);
			jValuesPanel.add(getJMiscTextFieldS(), gridBagConstraints4);
			jValuesPanel.add(jTaxiLabelF, gridBagConstraints3);
			jValuesPanel.add(getJTaxiTextFieldF(), gridBagConstraints3);
			jValuesPanel.add(jTaxiLabelS, gridBagConstraints4);
			jValuesPanel.add(getJTaxiTextFieldS(), gridBagConstraints4);
		}
		return jValuesPanel;
	}


	/**
	 * This method initializes jMIVTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJMIVTextFieldF() {
		if (jMIVTextFieldF == null) {
			jMIVTextFieldF = new JTextField("1.00");
		}
		return jMIVTextFieldF;
	}

	/**
	 * This method initializes jMIVTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJMIVTextFieldS() {
		if (jMIVTextFieldS == null) {
			jMIVTextFieldS = new JTextField("0.00");
		}
		return jMIVTextFieldS;
	}

	/**
	 * This method initializes jPassTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJPassTextFieldF() {
		if (jPassTextFieldF == null) {
			jPassTextFieldF = new JTextField("1.00");
		}
		return jPassTextFieldF;
	}

	/**
	 * This method initializes jPassTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJPassTextFieldS() {
		if (jPassTextFieldS == null) {
			jPassTextFieldS = new JTextField("0.00");
		}
		return jPassTextFieldS;
	}

	/**
	 * This method initializes jOeVTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJOeVTextFieldF() {
		if (jOeVTextFieldF == null) {
			jOeVTextFieldF = new JTextField("1.00");
		}
		return jOeVTextFieldF;
	}

	/**
	 * This method initializes jOeVTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJOeVTextFieldS() {
		if (jOeVTextFieldS == null) {
			jOeVTextFieldS = new JTextField("0.00");
		}
		return jOeVTextFieldS;
	}

	/**
	 * This method initializes jBikeTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJBikeTextFieldF() {
		if (jBikeTextFieldF == null) {
			jBikeTextFieldF = new JTextField("1.00");
		}
		return jBikeTextFieldF;
	}

	/**
	 * This method initializes jBikeTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJBikeTextFieldS() {
		if (jBikeTextFieldS == null) {
			jBikeTextFieldS = new JTextField("0.00");
		}
		return jBikeTextFieldS;
	}

	/**
	 * This method initializes jWalkTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJWalkTextFieldF() {
		if (jWalkTextFieldF == null) {
			jWalkTextFieldF = new JTextField("1.00");
		}
		return jWalkTextFieldF;
	}

	/**
	 * This method initializes jWalkTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJWalkTextFieldS() {
		if (jWalkTextFieldS == null) {
			jWalkTextFieldS = new JTextField("0.00");
		}
		return jWalkTextFieldS;
	}

	/**
	 * This method initializes jMiscTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJMiscTextFieldF() {
		if (jMiscTextFieldF == null) {
			jMiscTextFieldF = new JTextField("1.00");
		}
		return jMiscTextFieldF;
	}

	/**
	 * This method initializes jMiscTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJMiscTextFieldS() {
		if (jMiscTextFieldS == null) {
			jMiscTextFieldS = new JTextField("0.00");
		}
		return jMiscTextFieldS;
	}

	/**
	 * This method initializes jTaxiTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTaxiTextFieldF() {
		if (jTaxiTextFieldF == null) {
			jTaxiTextFieldF = new JTextField("1.00");
		}
		return jTaxiTextFieldF;
	}

	/**
	 * This method initializes jTaxiTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTaxiTextFieldS() {
		if (jTaxiTextFieldS == null) {
			jTaxiTextFieldS = new JTextField("0.00");
		}
		return jTaxiTextFieldS;
	}

	/**
	 * This method initializes jOutputPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJOutputPanel() {
		if (jOutputPanel == null) {
			JLabel jFileNameLabel = new JLabel();
			jFileNameLabel.setText("File:" );
			GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
			gridBagConstraints10.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints10.weightx = 0.0;
			
			gridBagConstraints10.ipadx = 200;
			jOutputPanel = new JPanel();
			jOutputPanel.setLayout(new GridBagLayout());
			jOutputPanel.add(jFileNameLabel, new GridBagConstraints());
			jOutputPanel.add(getJFilenameTextField(), gridBagConstraints10);
			jOutputPanel.add(getJAddButton(), new GridBagConstraints());
		}
		return jOutputPanel;
	}

	/**
	 * This method initializes jFilenameTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJFilenameTextField() {
		if (jFilenameTextField == null) {
			jFilenameTextField = new JTextField("C:\\temp\\test.csv");
		}
		return jFilenameTextField;
	}

	/**
	 * This method initializes jAddButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJAddButton() {
		if (jAddButton == null) {
			jAddButton = new JButton("Add rule");
			jAddButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					addRules();
				}
			});
		}
		return jAddButton;
	}

    int[] rootNode = {0, 0, 0};
	int actNode =0;
	
	public void addRules(){
		try{
			boolean writeHeader = !(new File(this.jFilenameTextField.getText()).exists());
			FileOutputStream outStream = new FileOutputStream(this.jFilenameTextField.getText(), true); //append
			PrintWriter outWrite = new PrintWriter(outStream);
			
			if(writeHeader){
				outWrite.println("personengruppe;wegzweck;distanzkategorie;bbrCat;isFactor;miv;miv_pass;oev;bike;walk;misc;taxi");
			}
			
					
			
			String personCode;
			String actionCode;
			String distanceCode;
			String bbrCode;
			String output;
			int[] persons = this.jPersonsList.getSelectedIndices();
			int[] actions = this.jActionsList.getSelectedIndices();
			int[] distances = this.jDistancesList.getSelectedIndices();
			int[] bbrCats = this.jBBRCatList.getSelectedIndices();

			for (int person : persons) {
				personCode = person + ";";
				for (int action : actions) {
					//get list of actions
					Integer[] actionList = this.actionConverter.get(action);
					for (Integer integer : actionList) {
						actionCode = integer + ";";
						for (int distance : distances) {
							distanceCode = this.distanceConverter.get(distance) + ";";
							for (int bbrCat : bbrCats) {
								bbrCode = (bbrCat + 1) + ";";
								output = personCode + actionCode + distanceCode + bbrCode + "0;" +
										this.jMIVTextFieldS.getText() + ";" + this.jPassTextFieldS.getText() + ";" +
										this.jOeVTextFieldS.getText() + ";" + this.jBikeTextFieldS.getText() + ";" +
										this.jWalkTextFieldS.getText() + ";" + this.jMiscTextFieldS.getText() + ";" +
										this.jTaxiTextFieldS.getText();
								outWrite.println(output);
								output = personCode + actionCode + distanceCode + bbrCode + "1;" +
										this.jMIVTextFieldF.getText() + ";" + this.jPassTextFieldF.getText() + ";" +
										this.jOeVTextFieldF.getText() + ";" + this.jBikeTextFieldF.getText() + ";" +
										this.jWalkTextFieldF.getText() + ";" + this.jMiscTextFieldF.getText() + ";" +
										this.jTaxiTextFieldF.getText();
								outWrite.println(output);
							}
						}
					}
				}
			}
			outWrite.close();
			
			//node tree
			String nodeFileName = this.jFilenameTextField.getText().substring(0, this.jFilenameTextField.getText().lastIndexOf("."))+"_node.csv";
			writeHeader = !(new File(nodeFileName).exists());
			outStream = new FileOutputStream(nodeFileName, true); //append
			outWrite = new PrintWriter(outStream);
			String setName = this.jFilenameTextField.getText().substring(this.jFilenameTextField.getText().lastIndexOf("\\")+1, this.jFilenameTextField.getText().lastIndexOf("."));
			String summandArray = 
					"{"+
					this.jWalkTextFieldS.getText()+","+
					this.jBikeTextFieldS.getText()+","+
					this.jMIVTextFieldS.getText()+","+
					this.jPassTextFieldS.getText()+","+
					this.jTaxiTextFieldS.getText()+","+
					this.jOeVTextFieldS.getText()+","+
					this.jMiscTextFieldS.getText()+
					"}";
			String factorArray = 
					"{"+
					this.jWalkTextFieldF.getText()+","+
					this.jBikeTextFieldF.getText()+","+
					this.jMIVTextFieldF.getText()+","+
					this.jPassTextFieldF.getText()+","+
					this.jTaxiTextFieldF.getText()+","+
					this.jOeVTextFieldF.getText()+","+
					this.jMiscTextFieldF.getText()+
					"}";
			
			
			if(writeHeader){
				outWrite.println("name;node_id;parent_node_id;attribute_values;spilt_variable;summand;factor");
				//write root node
				outWrite.println(setName+";0;-1;{1};PERSON_AGE_CLASS_CODE_PERSON_GROUP;{0,0,0,0,0,0,0};{1,1,1,1,1,1,1}");			
			}
			rootNode[0]=0;
			for (int person : persons) {
				personCode = Integer.toString(person);
				actNode++;
				if (actions.length > 0) {
					outWrite.println(setName + ";" + actNode + ";" + rootNode[0] + ";{" + personCode +
							"};CURRENT_EPISODE_ACTIVITY_CODE_MCT;{0,0,0,0,0,0,0};{1,1,1,1,1,1,1}");
				} else {
					outWrite.println(
							setName + ";" + actNode + ";" + rootNode[0] + ";{" + personCode + "};;" + summandArray +
									";" + factorArray);
				}
				rootNode[1] = actNode;
				for (int action : actions) {
					//get list of actions
					Integer[] actionList = this.actionConverter.get(action);
					for (Integer integer : actionList) {
						actionCode = Integer.toString(integer);
						actNode++;
						if (distances.length > 0) {
							outWrite.println(setName + ";" + actNode + ";" + rootNode[1] + ";{" + actionCode +
									"};CURRENT_DISTANCE_CLASS_CODE_MCT;{0,0,0,0,0,0,0};{1,1,1,1,1,1,1}");
						} else {
							outWrite.println(setName + ";" + actNode + ";" + rootNode[1] + ";{" + actionCode + "};;" +
									summandArray + ";" + factorArray);
						}
						rootNode[2] = actNode;
						for (int distance : distances) {
							distanceCode = Integer.toString(this.distanceConverter.get(distance));
							actNode++;
							outWrite.println(setName + ";" + actNode + ";" + rootNode[2] + ";{" + distanceCode + "};;" +
									summandArray + ";" + factorArray);
						}
					}
				}
			}
			outWrite.close();
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	/**
	 * This method initializes jBBRCatList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList<String> getJBBRCatList() {
		if (jBBRCatList == null) {

			String[] data = {"BBR 1-8: Agglomeration - R 1", "BBR 9-10: Verstädtert - R 2", "BBR 11: mittl. Verst. - R 3", "BBR 12-15: Ländlich - R 4", "BBR 16-17: sehr Ländl. - R 5"};	

			jBBRCatList = new JList<>(data);
		}
		return jBBRCatList;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		ExpertKnowledgeHandler controlGUI = new ExpertKnowledgeHandler();
		controlGUI.createAndShowGUI();
		controlGUI.controlInstance.setVisible(true);
	}

}
