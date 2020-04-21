package de.dlr.ivf.tapas.tools.persitence.db.dbImport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class DBImportV2GUI {

	private JFrame frame;
	private DBImportV2 control = new DBImportV2();
	
	JTextField lblRegion;
	
	JTextField lblLoginFile; //done
	JTextField lblAct2LocFile; //done
	JTextField lblCFN4File; //done
	JTextField lblCFN4IndFile; //done
	JTextField lblHouseholdFile; //done
	JTextField lblLocationsFile; //done
	JTextField lblMatrixFile; //done
	JTextField lblPersonFile; //done
	JTextField lblTAZFile; //done
	JTextField lblTAZFeeFile; //done
	JTextField lblTAZIntraMITFile; //done
	JTextField lblTAZIntraPTFile; //done
	JTextField lblTAZScoresFile; //done
	JTextField lblBlockFile; //done
	JTextField lblBlockNextPTStopFile; //done 
	JTextField lblBlockScoresFile; //done
	JTextField lblModeChoiceFile; //done

	JLabel lblLoginResult;
	JLabel lblAct2LocResult;
	JLabel lblBlockResult;
	JLabel lblBlockNextPTStopResult;
	JLabel lblBlockScoresResult;
	JLabel lblCFN4Result;
	JLabel lblCFN4IndResult;
	JLabel lblHouseholdResult;
	JLabel lblLocationsResult;
	JLabel lblMatrixResult;
	JLabel lblPersonResult;
	JLabel lblTAZResult;
	JLabel lblTAZFeeResult;
	JLabel lblTAZIntraMITResult;
	JLabel lblTAZIntraPTResult;
	JLabel lblTAZScoresResult;
	JLabel lblModeChoiceResult;

	
	File pathCacheForFiles = null;
	List<JComponent> elementsToActivate = new LinkedList<>();
	
	String fileNameSeparator = "*"; //forbidden character in Windows, Unix and Linux are "special"

		
	private String chooseFile(String title, JFrame f, boolean multi) {
		
		JFileChooser fd = new JFileChooser();
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setVisible(true);
		fd.setMultiSelectionEnabled(multi);
		if(null!=pathCacheForFiles){
			fd.setCurrentDirectory(pathCacheForFiles);
		}
		fd.setDialogTitle(title);
		int value = fd.showOpenDialog(f);

		if ((value == JFileChooser.APPROVE_OPTION) && fd.getSelectedFile() != null && fd.getSelectedFile().isFile()
				&& fd.getSelectedFile().exists()) {
			pathCacheForFiles = fd.getSelectedFile().getParentFile();
			if(multi){
				File [] files = fd.getSelectedFiles();
				StringBuilder rVal= new StringBuilder();
				if(files.length>0){
					rVal = new StringBuilder(files[0].getAbsolutePath());
					for (int i = 1; i<files.length; ++i){
						rVal.append(fileNameSeparator).append(files[i].getAbsoluteFile());
					}
				}				
				return rVal.toString();
			}
			else{
				return fd.getSelectedFile().getAbsolutePath();
			}
		} else if (value != JFileChooser.CANCEL_OPTION) {
			JOptionPane.showMessageDialog(f, "Invalid File: ", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return "";
	}

	
	
	private void activateElements(boolean val){
		for(JComponent comp:this.elementsToActivate){
			comp.setEnabled(val);
		}
	}
	
	private void createAndShowGui(){
		
		//build the main frame
		frame = new JFrame();
		frame.setTitle("TAPAS Database Importer V2");
		frame.setContentPane(new JPanel());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("icons/TAPAS-Logo.gif")).getImage());

		
		JPanel regionPanel = new JPanel(); // 
		regionPanel.setBorder(BorderFactory.createTitledBorder("Region"));
		regionPanel.add(new JLabel("Region:"));
		lblRegion = new JTextField("Braunschweig");
		regionPanel.add(lblRegion);
		JButton btnCreateRegion = new JButton("Create region");
		btnCreateRegion.setToolTipText("Create region based tables");
		regionPanel.add(btnCreateRegion);
		elementsToActivate.add(btnCreateRegion);
		
		btnCreateRegion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.createRegion(lblRegion.getText());
			}
		});
		
		
		JPanel actionPanel = new JPanel(new GridLayout(17, 5, 5, 5)); // five entries per item: Label, Text, File picker, processor button, result
		actionPanel.setBorder(BorderFactory.createTitledBorder("Files"));
		
		//Connection
		actionPanel.add(new JLabel("DB-Connection file:"));
		lblLoginFile = new JTextField();
		lblLoginFile.setToolTipText("File with login information");
		actionPanel.add(lblLoginFile);
		JButton btnLoadLogin = new JButton("Load DB Config");
		btnLoadLogin.setToolTipText("Load Database Connection Configuration File");
		actionPanel.add(btnLoadLogin);
		JButton btnExecConfig = new JButton("Login");
		btnExecConfig.setToolTipText("Login to Database");
		actionPanel.add(btnExecConfig);
		lblLoginResult = new JLabel("disconnected");
		actionPanel.add(lblLoginResult);

		btnLoadLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lblLoginFile.setText(chooseFile("Choose database config file", frame, false));
				lblLoginResult.setText("Disconnected");
				frame.repaint();
			}
		});

		btnExecConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				File file = new File(lblLoginFile.getText());
				if(file.exists()){
					if(control.dbCon!=null){
						control.dbCon.closeConnections();
						control.dbCon=null;
						lblLoginResult.setText("Disconnected");
						frame.repaint();
					}
					if(control.login(file)){
						lblLoginResult.setText("Connected");
						activateElements(true);
						frame.repaint();
					}
				}
			}
		});
		
		//TAZ
		actionPanel.add(new JLabel("TAZ file:"));
		lblTAZFile = new JTextField();
		lblTAZFile.setToolTipText("File with TAZ information");
		elementsToActivate.add(lblTAZFile);
		actionPanel.add(lblTAZFile);
		JButton btnLoadTAZ = new JButton("Load TAZ file");
		elementsToActivate.add(btnLoadTAZ);
		btnLoadTAZ.setToolTipText("Load file containing the TAZ information");
		actionPanel.add(btnLoadTAZ);
		JButton btnExecTAZ = new JButton("Import");
		btnExecTAZ.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecTAZ);
		actionPanel.add(btnExecTAZ);
		lblTAZResult = new JLabel("");
		actionPanel.add(lblTAZResult);

		btnLoadTAZ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file = chooseFile("Choose taz file", frame,false);
				if(!"".equals(file)){
					lblTAZFile.setText(file);
					lblTAZResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecTAZ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				File file = new File(lblTAZFile.getText());
				if(file.exists()){
					activateElements(false);
					String[] options = {"With TAZ numbers", "With TAZ IDs"};
					Object result = JOptionPane.showInputDialog(frame, "Select the format of the TAZ", "Import TAZ", JOptionPane.QUESTION_MESSAGE, null,
							options,
							options[0]);
					if(options[0].equals(result)){
						if(control.importTAZ(file, lblRegion.getText().toLowerCase(), false))
							lblTAZResult.setText("Imported");						
						else
							lblTAZResult.setText("Import failed");
					}
					else if(options[1].equals(result)){
						if(control.importTAZ(file, lblRegion.getText().toLowerCase(), true))
							lblTAZResult.setText("Imported");
						else
							lblTAZResult.setText("Import failed");
					}
					else{
						lblTAZResult.setText("Cancel pressed");
					}
					activateElements(true);
					frame.repaint();				
				}
			}
		});
		
		//TAZ fees
		actionPanel.add(new JLabel("TAZ-Fee file:"));
		lblTAZFeeFile = new JTextField();
		lblTAZFeeFile.setToolTipText("File with TAZ fee information");
		elementsToActivate.add(lblTAZFeeFile);
		actionPanel.add(lblTAZFeeFile);
		JButton btnLoadTAZFee = new JButton("Load TAZ fee file");
		btnLoadTAZFee.setToolTipText("Load file containing the TAZ fee information");
		elementsToActivate.add(btnLoadTAZFee);
		actionPanel.add(btnLoadTAZFee);
		JButton btnExecTAZFee = new JButton("Import");
		btnExecTAZFee.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecTAZFee);
		actionPanel.add(btnExecTAZFee);
		lblTAZFeeResult = new JLabel("");
		actionPanel.add(lblTAZFeeResult);

		btnLoadTAZFee.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose taz fee file", frame, false);
				if(!"".equals(file)){
					lblTAZFeeFile.setText(file);
					lblTAZFeeResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecTAZFee.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				File file = new File(lblTAZFeeFile.getText());
				if(file.exists()){
					activateElements(false);
					String name = file.getName();
					name = name.substring(0, name.lastIndexOf("."));
					name = JOptionPane.showInputDialog(frame, "Please enter the name of this fee", name);

					String[] options = {"With TAZ numbers and BBR", "WithTAZ IDs and BBR", "With TAZ numbers w/o BBR", "With TAZ IDs w/o BBR"};
					Object result = JOptionPane.showInputDialog(frame, "Select the format of the TAZ fees", "Import TAZ fees", JOptionPane.QUESTION_MESSAGE, null,
							options,
							options[0]);
					if(options[0].equals(result)){
						if(control.importTazFee(file, lblRegion.getText().toLowerCase(), name, false, true))
							lblTAZFeeResult.setText("Imported");
						else
							lblTAZFeeResult.setText("Import failed");
					}
					else if(options[1].equals(result)){
						if(control.importTazFee(file, lblRegion.getText().toLowerCase(), name, true, true))
							lblTAZFeeResult.setText("Imported");
						else
							lblTAZFeeResult.setText("Import failed");
					}
					else if(options[2].equals(result)){
						if(control.importTazFee(file, lblRegion.getText().toLowerCase(), name, true, false))
							lblTAZFeeResult.setText("Imported");
						else
							lblTAZFeeResult.setText("Import failed");
					}
					else if(options[3].equals(result)){
						if(control.importTazFee(file, lblRegion.getText().toLowerCase(), name, true, false))
							lblTAZFeeResult.setText("Imported");
						else
							lblTAZFeeResult.setText("Import failed");
					}
					else{
						lblTAZFeeResult.setText("Cancel pressed");
					}
					activateElements(true);
					frame.repaint();					
				}
			}
		});
		
		//locations
		actionPanel.add(new JLabel("Locations file:"));
		lblLocationsFile = new JTextField();
		lblLocationsFile.setToolTipText("File with locations");
		elementsToActivate.add(lblLocationsFile);
		actionPanel.add(lblLocationsFile);
		JButton btnLoadLocations = new JButton("Load locations file");
		btnLoadLocations.setToolTipText("Load file containing the locations");
		elementsToActivate.add(btnLoadLocations);
		actionPanel.add(btnLoadLocations);
		JButton btnExecLocations = new JButton("Import");
		btnExecLocations.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecLocations);
		actionPanel.add(btnExecLocations);
		lblLocationsResult = new JLabel("");
		actionPanel.add(lblLocationsResult);

		btnLoadLocations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose location file", frame, false);
				if(!"".equals(file)){
					lblLocationsFile.setText(file);
					lblLocationsResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecLocations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				File file = new File(lblLocationsFile.getText());
				if(file.exists()){
					activateElements(false);
					String[] options = {"With TAZ numbers", "With TAZ IDs"};
					Object result = JOptionPane.showInputDialog(frame, "Select the format of the locations", "Import locations", JOptionPane.QUESTION_MESSAGE, null,
							options,
							options[0]);
					
					if(options[0].equals(result)){
						if(control.importLocations(file, lblRegion.getText().toLowerCase(), false))
							lblLocationsResult.setText("Imported");
						else
							lblLocationsResult.setText("Import failed");
					}
					else if(options[1].equals(result)){
						if(control.importLocations(file, lblRegion.getText().toLowerCase(), true))
							lblLocationsResult.setText("Imported");
						else
							lblLocationsResult.setText("Import failed");
					}
					else{
						lblLocationsResult.setText("Cancel pressed");
					}
					activateElements(true);
					frame.repaint();					
				}
			}
		});

		//matrices
		actionPanel.add(new JLabel("Matrix file:"));
		lblMatrixFile = new JTextField();
		lblMatrixFile.setToolTipText("File with matrix information");
		elementsToActivate.add(lblMatrixFile);
		actionPanel.add(lblMatrixFile);
		JButton btnLoadMatrix = new JButton("Load matrix file");
		btnLoadMatrix.setToolTipText("Load file containing the matrix information");
		elementsToActivate.add(btnLoadMatrix);
		actionPanel.add(btnLoadMatrix);
		JButton btnExecMatrix = new JButton("Import");
		btnExecMatrix.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecMatrix);
		actionPanel.add(btnExecMatrix);
		lblMatrixResult = new JLabel("");
		actionPanel.add(lblMatrixResult);

		btnLoadMatrix.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose matrix file", frame, true);
				if(!"".equals(file)){
					lblMatrixFile.setText(file);
					lblMatrixResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecMatrix.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				String[] tok = lblMatrixFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);

						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this matrix", name);
						
						if(control.importMatrix(file, lblRegion.getText().toLowerCase(), name))
							lblMatrixResult.setText("Imported");
						else
							lblMatrixResult.setText("Import failed");
						activateElements(true);
						frame.repaint();
						
					}
				}
			}
		});	
		
		//Households
		actionPanel.add(new JLabel("Household file:"));
		lblHouseholdFile = new JTextField();
		lblHouseholdFile.setToolTipText("File with household information");
		elementsToActivate.add(lblHouseholdFile);
		actionPanel.add(lblHouseholdFile);
		JButton btnLoadHH = new JButton("Load household file");
		elementsToActivate.add(btnLoadHH);
		btnLoadHH.setToolTipText("Load file containing the household information");
		actionPanel.add(btnLoadHH);
		JButton btnExecHH = new JButton("Import");
		btnExecHH.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecHH);
		actionPanel.add(btnExecHH);
		lblHouseholdResult = new JLabel("");
		actionPanel.add(lblHouseholdResult);

		btnLoadHH.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				 file = chooseFile("Choose household file", frame,true);
				if(!"".equals(file)){
					lblHouseholdFile.setText(file);
					lblHouseholdResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecHH.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblHouseholdFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this household set", name);

						String[] options = {"With TAZ numbers", "With TAZ IDs"};
						Object result = JOptionPane.showInputDialog(frame, "Select the format of the household", "Import households", JOptionPane.QUESTION_MESSAGE, null,
								options,
								options[0]);
						
						if(options[0].equals(result)){
							if(control.importHH(file, lblRegion.getText().toLowerCase(), name, false))
								lblHouseholdResult.setText("Imported");
							else
								lblHouseholdResult.setText("Import failed");
						}
						else if(options[1].equals(result)){
							if(control.importHH(file, lblRegion.getText().toLowerCase(), name, true))
								lblHouseholdResult.setText("Imported");
							else
								lblHouseholdResult.setText("Import failed");
						}
						else{
							lblHouseholdResult.setText("Cancel pressed");
						}
						activateElements(true);
						frame.repaint();					
					}
				}
			}
		});
		
		//Persons
		actionPanel.add(new JLabel("Persons file:"));
		lblPersonFile = new JTextField();
		lblPersonFile.setToolTipText("File with person information");
		elementsToActivate.add(lblPersonFile);
		actionPanel.add(lblPersonFile);
		JButton btnLoadPerson = new JButton("Load person file");
		elementsToActivate.add(btnLoadPerson);
		btnLoadPerson.setToolTipText("Load file containing the person information");
		actionPanel.add(btnLoadPerson);
		JButton btnExecPerson = new JButton("Import");
		btnExecPerson.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecPerson);
		actionPanel.add(btnExecPerson);
		lblPersonResult = new JLabel("");
		actionPanel.add(lblPersonResult);

		btnLoadPerson.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose person file", frame,true);
				if(!"".equals(file)){
					lblPersonFile.setText(file);
					lblPersonResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecPerson.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblPersonFile.getText().split("\\"+fileNameSeparator);
					for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this person set", name);
								
						if(control.importPersons(file, lblRegion.getText().toLowerCase(), name) )
							lblPersonResult.setText("Imported");
						else
							lblPersonResult.setText("Import failed");
						
						activateElements(true);			
						frame.repaint();					
					}
				}
			}
		});
		
		// Activity 2 Locations
		actionPanel.add(new JLabel("Act 2 Loc file:"));
		lblAct2LocFile = new JTextField();
		lblAct2LocFile.setToolTipText("File with activity to location mapping");
		elementsToActivate.add(lblAct2LocFile);
		actionPanel.add(lblAct2LocFile);
		JButton btnLoadAct2Loc = new JButton("Load act2loc file");
		elementsToActivate.add(btnLoadAct2Loc);
		btnLoadAct2Loc.setToolTipText("Load file containing the activity to location");
		actionPanel.add(btnLoadAct2Loc);
		JButton btnExecAct2Loc = new JButton("Import");
		btnExecAct2Loc.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecAct2Loc);
		actionPanel.add(btnExecAct2Loc);
		lblAct2LocResult = new JLabel("");
		actionPanel.add(lblAct2LocResult);

		btnLoadAct2Loc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose activity to location file", frame, false);
				if (!"".equals(file)) {
					lblAct2LocFile.setText(file);
					lblAct2LocResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecAct2Loc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File file = new File(lblAct2LocFile.getText());
				if (file.exists()) {
					activateElements(false);

					if (control.importAct2Loc(file, lblRegion.getText().toLowerCase()))
						lblAct2LocResult.setText("Imported");
					else
						lblAct2LocResult.setText("Import failed");

					activateElements(true);
					frame.repaint();
				}
			}
		});
		
		// CFN4
		actionPanel.add(new JLabel("CFN4 file:"));
		lblCFN4File = new JTextField();
		lblCFN4File.setToolTipText("File with CFN4 values");
		elementsToActivate.add(lblCFN4File);
		actionPanel.add(lblCFN4File);
		JButton btnLoadCFN4 = new JButton("Load CFN4 file");
		elementsToActivate.add(btnLoadCFN4);
		btnLoadCFN4.setToolTipText("Load file containing the CFN4 values");
		actionPanel.add(btnLoadCFN4);
		JButton btnExecCFN4 = new JButton("Import");
		btnExecCFN4.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecCFN4);
		actionPanel.add(btnExecCFN4);
		lblCFN4Result = new JLabel("");
		actionPanel.add(lblCFN4Result);

		btnLoadCFN4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose CFN4 file", frame, false);
				if (!"".equals(file)) {
					lblCFN4File.setText(file);
					lblCFN4Result.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecCFN4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File file = new File(lblCFN4File.getText());
				if (file.exists()) {
					activateElements(false);

					if (control.importCFN4(file, lblRegion.getText().toLowerCase()))
						lblCFN4Result.setText("Imported");
					else
						lblCFN4Result.setText("Import failed");

					activateElements(true);
					frame.repaint();
				}
			}
		});	
		
		// CFN4 index
		actionPanel.add(new JLabel("CFN4 Index file:"));
		lblCFN4IndFile = new JTextField();
		lblCFN4IndFile.setToolTipText("File with CFN4 index values");
		elementsToActivate.add(lblCFN4IndFile);
		actionPanel.add(lblCFN4IndFile);
		JButton btnLoadCFN4Ind = new JButton("Load CFN4 index file");
		elementsToActivate.add(btnLoadCFN4Ind);
		btnLoadCFN4Ind.setToolTipText("Load file containing the CFN4 index values");
		actionPanel.add(btnLoadCFN4Ind);
		JButton btnExecCFN4ind = new JButton("Import");
		btnExecCFN4ind.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecCFN4ind);
		actionPanel.add(btnExecCFN4ind);
		lblCFN4IndResult = new JLabel("");
		actionPanel.add(lblCFN4IndResult);

		btnLoadCFN4Ind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose CFN4 index file", frame, false);
				if (!"".equals(file)) {
					lblCFN4IndFile.setText(file);
					lblCFN4IndResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecCFN4ind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File file = new File(lblCFN4IndFile.getText());
				if (file.exists()) {
					activateElements(false);

					if (control.importCFN4Ind(file, lblRegion.getText().toLowerCase()))
						lblCFN4IndResult.setText("Imported");
					else
						lblCFN4IndResult.setText("Import failed");

					activateElements(true);
					frame.repaint();
				}
			}
		});	
		
		// Intra MIT
		actionPanel.add(new JLabel("Intra MIT file:"));
		lblTAZIntraMITFile = new JTextField();
		lblTAZIntraMITFile.setToolTipText("File with intra MIT values");
		elementsToActivate.add(lblTAZIntraMITFile);
		actionPanel.add(lblTAZIntraMITFile);
		JButton btnLoadIntraMIT = new JButton("Load intra MIT file");
		elementsToActivate.add(btnLoadIntraMIT);
		btnLoadIntraMIT.setToolTipText("Load file containing the intra MIT values");
		actionPanel.add(btnLoadIntraMIT);
		JButton btnExecIntraMIT = new JButton("Import");
		btnExecIntraMIT.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecIntraMIT);
		actionPanel.add(btnExecIntraMIT);
		lblTAZIntraMITResult = new JLabel("");
		actionPanel.add(lblTAZIntraMITResult);

		btnLoadIntraMIT.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose intra MIT file", frame, true);
				if (!"".equals(file)) {
					lblTAZIntraMITFile.setText(file);
					lblTAZIntraMITResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecIntraMIT.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblTAZIntraMITFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this intra MIT info set", name);

						String[] options = {"With TAZ numbers", "With TAZ IDs"};
						Object result = JOptionPane.showInputDialog(frame, "Select the format of the intra MIT info set", "Import intra MIT info set", JOptionPane.QUESTION_MESSAGE, null,
								options,
								options[0]);
						
						if(options[0].equals(result)){
							if(control.importIntraMIT(file, lblRegion.getText().toLowerCase(), name, false))
								lblTAZIntraMITResult.setText("Imported");
							else
								lblTAZIntraMITResult.setText("Import failed");
						}
						else if(options[1].equals(result)){
							if(control.importIntraMIT(file, lblRegion.getText().toLowerCase(), name, true))
								lblTAZIntraMITResult.setText("Imported");
							else
								lblTAZIntraMITResult.setText("Import failed");
						}
						else{
							lblTAZIntraMITResult.setText("Cancel pressed");
						}
						
						activateElements(true);
						frame.repaint();
					}
				}
			}
		});	
		
		// Intra PT
		actionPanel.add(new JLabel("Intra PT file:"));
		lblTAZIntraPTFile = new JTextField();
		lblTAZIntraPTFile.setToolTipText("File with intra PT values");
		elementsToActivate.add(lblTAZIntraPTFile);
		actionPanel.add(lblTAZIntraPTFile);
		JButton btnLoadIntraPT = new JButton("Load intra PT file");
		elementsToActivate.add(btnLoadIntraPT);
		btnLoadIntraPT.setToolTipText("Load file containing the intra PT values");
		actionPanel.add(btnLoadIntraPT);
		JButton btnExecIntraPT = new JButton("Import");
		btnExecIntraPT.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecIntraPT);
		actionPanel.add(btnExecIntraPT);
		lblTAZIntraPTResult = new JLabel("");
		actionPanel.add(lblTAZIntraPTResult);

		btnLoadIntraPT.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose intra PT file", frame, true);
				if (!"".equals(file)) {
					lblTAZIntraPTFile.setText(file);
					lblTAZIntraPTResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecIntraPT.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblTAZIntraPTFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this intra PT Info set", name);

						String[] options = {"With TAZ numbers", "With TAZ IDs"};
						Object result = JOptionPane.showInputDialog(frame, "Select the format of the intra PT info set", "Import intra PT info set", JOptionPane.QUESTION_MESSAGE, null,
								options,
								options[0]);
						
						if(options[0].equals(result)){
							if(control.importIntraPT(file, lblRegion.getText().toLowerCase(), name, false))
								lblTAZIntraPTResult.setText("Imported");
							else
								lblTAZIntraPTResult.setText("Import failed");
						}
						else if(options[1].equals(result)){
							if(control.importIntraPT(file, lblRegion.getText().toLowerCase(), name, true))
								lblTAZIntraPTResult.setText("Imported");
							else
								lblTAZIntraPTResult.setText("Import failed");
						}
						else{
							lblTAZIntraPTResult.setText("Cancel pressed");
						}
						
						activateElements(true);
						frame.repaint();
					}
				}
			}
		});		
		
		// TAZ scores
		actionPanel.add(new JLabel("TAZ scores file:"));
		lblTAZScoresFile = new JTextField();
		lblTAZScoresFile.setToolTipText("File with TAZ scores values");
		elementsToActivate.add(lblTAZScoresFile);
		actionPanel.add(lblTAZScoresFile);
		JButton btnLoadScores = new JButton("Load TAZ scores file");
		elementsToActivate.add(btnLoadScores);
		btnLoadScores.setToolTipText("Load file containing the TAZ scores values");
		actionPanel.add(btnLoadScores);
		JButton btnExecScores = new JButton("Import");
		btnExecScores.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecScores);
		actionPanel.add(btnExecScores);
		lblTAZScoresResult = new JLabel("");
		actionPanel.add(lblTAZScoresResult);

		btnLoadScores.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose TAZ scores file", frame, true);
				if (!"".equals(file)) {
					lblTAZScoresFile.setText(file);
					lblTAZScoresResult.setText("File specified");
				}
				frame.repaint();
			}
		});

		btnExecScores.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblTAZScoresFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this TAZ score set", name);

						String[] options = {"With TAZ numbers", "With TAZ IDs"};
						Object result = JOptionPane.showInputDialog(frame, "Select the format of the TAZ score set", "Import TAZ score set", JOptionPane.QUESTION_MESSAGE, null,
								options,
								options[0]);
						
						if(options[0].equals(result)){
							if(control.importScores(file, lblRegion.getText().toLowerCase(), "taz", name, false))
								lblTAZScoresResult.setText("Imported");
							else
								lblTAZScoresResult.setText("Import failed");
						}
						else if(options[1].equals(result)){
							if(control.importScores(file, lblRegion.getText().toLowerCase(), "taz", name, true))
								lblTAZScoresResult.setText("Imported");
							else
								lblTAZScoresResult.setText("Import failed");
						}
						else{
							lblTAZScoresResult.setText("Cancel pressed");
						}
						
						activateElements(true);
						frame.repaint();
					}
				}
			}
		});	
		
		// Block
		actionPanel.add(new JLabel("Block file:"));
		lblBlockFile = new JTextField();
		lblBlockFile.setToolTipText("File with Block information");
		elementsToActivate.add(lblBlockFile);
		actionPanel.add(lblBlockFile);
		JButton btnLoadBlock = new JButton("Load Block file");
		elementsToActivate.add(btnLoadBlock);
		btnLoadBlock.setToolTipText("Load file containing the Block information");
		actionPanel.add(btnLoadBlock);
		JButton btnExecBlock = new JButton("Import");
		btnExecBlock.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecBlock);
		actionPanel.add(btnExecBlock);
		lblBlockResult = new JLabel("");
		actionPanel.add(lblBlockResult);

		btnLoadBlock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file = chooseFile("Choose Block file", frame, false);
				if (!"".equals(file)) {
					lblBlockFile.setText(file);
					lblBlockResult.setText("File specified");
				}
				frame.repaint();
			}
		});
		
		btnExecBlock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				File file = new File(lblBlockFile.getText());
				if (file.exists()) {
					activateElements(false);
					String[] options = {"With Block numbers", "With Block IDs"};
					Object result = JOptionPane.showInputDialog(frame, "Select the format of the blocks", "Import Block",
							JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					if (options[0].equals(result)) {
						if (control.importBlock(file, lblRegion.getText().toLowerCase(), false))
							lblBlockResult.setText("Imported");
						else
							lblBlockResult.setText("Import failed");
					} else if (options[1].equals(result)) {
						if (control.importBlock(file, lblRegion.getText().toLowerCase(), true))
							lblBlockResult.setText("Imported");
						else
							lblBlockResult.setText("Import failed");
					} else {
						lblBlockResult.setText("Cancel pressed");
					}
					activateElements(true);
					frame.repaint();

				}
			}
		});		
		
		// Block scores
		actionPanel.add(new JLabel("Block scores file:"));
		lblBlockScoresFile = new JTextField();
		lblBlockScoresFile.setToolTipText("File with Block scores values");
		elementsToActivate.add(lblBlockScoresFile);
		actionPanel.add(lblBlockScoresFile);
		JButton btnLoadBlockScores = new JButton("Load Block scores file");
		elementsToActivate.add(btnLoadBlockScores);
		btnLoadBlockScores.setToolTipText("Load file containing the Block scores values");
		actionPanel.add(btnLoadBlockScores);
		JButton btnExecBlockScores = new JButton("Import");
		btnExecBlockScores.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecBlockScores);
		actionPanel.add(btnExecBlockScores);
		lblBlockScoresResult = new JLabel("");
		actionPanel.add(lblBlockScoresResult);

		btnLoadBlockScores.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose Block scores file", frame, true);
				if (!"".equals(file)) {
					lblBlockScoresFile.setText(file);
					lblBlockScoresResult.setText("File specified");
				}
				frame.repaint();
			}
		});
				
		btnExecBlockScores.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblBlockScoresFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this block score set", name);
	
	
						if(control.importScores(file, lblRegion.getText().toLowerCase(), "block", name, false))
							lblBlockScoresResult.setText("Imported");
						else
							lblBlockScoresResult.setText("Import failed");
							
						activateElements(true);
						frame.repaint();
					}
				}
			}
		});		
		
		// BlockNextPTStop
		actionPanel.add(new JLabel("Block next pt stop file:"));
		lblBlockNextPTStopFile = new JTextField();
		lblBlockNextPTStopFile.setToolTipText("File with Block next pt stop values");
		elementsToActivate.add(lblBlockNextPTStopFile);
		actionPanel.add(lblBlockNextPTStopFile);
		JButton btnLoadBlockNextPTStop = new JButton("Load Block next pt stop file");
		elementsToActivate.add(btnLoadBlockNextPTStop);
		btnLoadBlockNextPTStop.setToolTipText("Load file containing the Block next pt stop values");
		actionPanel.add(btnLoadBlockNextPTStop);
		JButton btnExecBlockNextPTStop = new JButton("Import");
		btnExecBlockNextPTStop.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecBlockNextPTStop);
		actionPanel.add(btnExecBlockNextPTStop);
		lblBlockNextPTStopResult = new JLabel("");
		actionPanel.add(lblBlockNextPTStopResult);

		btnLoadBlockNextPTStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose Block next pt stop file", frame, true);
				if (!"".equals(file)) {
					lblBlockNextPTStopFile.setText(file);
					lblBlockNextPTStopResult.setText("File specified");
				}
				frame.repaint();
			}
		});	
		
		btnExecBlockNextPTStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblBlockNextPTStopFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this block next pt stop set", name);
	
						if (control.importBlockNextPTStop(file, lblRegion.getText().toLowerCase(), name))
							lblBlockNextPTStopResult.setText("Imported");
						else
							lblBlockNextPTStopResult.setText("Import failed");
	
						activateElements(true);
						frame.repaint();
					}
				}
			}
		});	
		
		// ModeChoice
		actionPanel.add(new JLabel("Mode choice file:"));
		lblModeChoiceFile = new JTextField();
		lblModeChoiceFile.setToolTipText("File with mode choice values");
		elementsToActivate.add(lblModeChoiceFile);
		actionPanel.add(lblModeChoiceFile);
		JButton btnLoadModeChoice = new JButton("Load mode choice file");
		elementsToActivate.add(btnLoadModeChoice);
		btnLoadModeChoice.setToolTipText("Load file containing the mode choice values");
		actionPanel.add(btnLoadModeChoice);
		JButton btnExecModeChoice = new JButton("Import");
		btnExecModeChoice.setToolTipText("Import to Database");
		elementsToActivate.add(btnExecModeChoice);
		actionPanel.add(btnExecModeChoice);
		lblModeChoiceResult = new JLabel("");
		actionPanel.add(lblModeChoiceResult);

		btnLoadModeChoice.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String file;
				file = chooseFile("Choose mode choice file", frame, true);
				if (!"".equals(file)) {
					lblModeChoiceFile.setText(file);
					lblModeChoiceResult.setText("File specified");
				}
				frame.repaint();
			}
		});	
		
		btnExecModeChoice.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] tok = lblModeChoiceFile.getText().split("\\"+fileNameSeparator);
				for(String txt: tok){
					File file = new File(txt);
					if(file.exists()){
						activateElements(false);
	
						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						name = JOptionPane.showInputDialog(frame, "Please enter the name of this mode choice set", name);
	
						if (control.importModeChoice(file, name))
							lblModeChoiceResult.setText("Imported");
						else
							lblModeChoiceResult.setText("Import failed");
	
						activateElements(true);
						frame.repaint();
					}
				}
			}
		});	
		// set modal buttons/fields to disable
		this.activateElements(false);
		
		//add everything
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(regionPanel, BorderLayout.NORTH);
		frame.getContentPane().add(actionPanel, BorderLayout.CENTER);
		frame.pack();
		
		//center
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final int x = (screenSize.width - frame.getWidth()) / 2;
		final int y = (screenSize.height - frame.getHeight()) / 2;
		frame.setLocation(x,y);
		frame.setVisible(true);
		
	}
	
	/**
	 * @param args
	 * @throws UnsupportedLookAndFeelException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		DBImportV2GUI control = new DBImportV2GUI();
		control.createAndShowGui();
		if(args.length>0){
			control.lblLoginFile.setText(args[0]);
			
		}

	}


}
