package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController.StrucType;

public class InteractionStructureAddPanel extends JPanel implements Observer {

	private ManipModuleStructureController manipModuleControl;
	private JRadioButton lineButton;
	private JRadioButton colButton;
	private JRadioButton fileButton;
	private JRadioButton updateButton;
	private JRadioButton nonupdateButton;
	private JPanel idPanel;
	private JScrollPane idScrolling;
	private IndexField locField;
	private JButton addButton;
	private JButton resetButton;

	public InteractionStructureAddPanel(final ManipModuleStructureController manipModuleControl) {
		
		this.manipModuleControl = manipModuleControl;
		manipModuleControl.addObserver(this);
		
		setLayout(new GridBagLayout());
				
		lineButton = new JRadioButton(Localisation.getLocaleGuiTerm("LINE_IDS"));
		colButton = new JRadioButton(Localisation.getLocaleGuiTerm("COL_IDS"));
//		fileButton = new JRadioButton("Aus Datei");
		
		ButtonGroup strucButtons = new ButtonGroup();
		strucButtons.add(lineButton);
		strucButtons.add(colButton);
//		strucButtons.add(fileButton);
		
		selectStrucButton();
		
		lineButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manipModuleControl.setStrucAddType(StrucType.LINE);
			}
		});
		colButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manipModuleControl.setStrucAddType(StrucType.COL);
			}
		});
//		fileButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				manipModuleControl.setStrucAddType(StrucType.FILE);
//			}
//		});
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.NORTHWEST;
		add(lineButton,c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NORTHWEST;
		add(colButton,c);
//		c = new GridBagConstraints();
//		c.gridx = 0;
//		c.gridy = 2;
//		c.fill = GridBagConstraints.NORTHWEST;
//		c.gridheight = 2;
//		add(fileButton,c);
				
//		updateButton = new JRadioButton("Objekt-Werte überschreiben");
//		nonupdateButton = new JRadioButton("Objekt-Werte nicht überschreiben");
//		
//		ButtonGroup fileMethodButtons = new ButtonGroup();
//		fileMethodButtons.add(updateButton);
//		fileMethodButtons.add(nonupdateButton);
//		
//		selectFileMethodButton();
//		
//		updateButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				manipModuleControl.setFileMethodType(FileMethodType.UPDATE);
//			}
//		});
//		nonupdateButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				manipModuleControl.setFileMethodType(FileMethodType.NONUPDATE);
//			}
//		});
//		
//		c = new GridBagConstraints();
//		c.gridx = 1;
//		c.gridy = 2;
//		c.fill = GridBagConstraints.NORTHWEST;
//		add(updateButton,c);
//		c = new GridBagConstraints();
//		c.gridx = 1;
//		c.gridy = 3;
//		c.fill = GridBagConstraints.NORTHWEST;
//		add(nonupdateButton,c);
		
		idPanel = new JPanel();
		idPanel.setLayout(new GridBagLayout());
		populateIdPanel(manipModuleControl.getIdsToAdd());
		idScrolling = new JScrollPane(idPanel);
		idScrolling.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		idScrolling.setPreferredSize(new Dimension(150,73));
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.NORTHWEST;
		add(idScrolling,c);
		
		
		JLabel locIdxLbl = new JLabel("  "+Localisation.getLocaleGuiTerm("INSERT_IDX")+"   ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 4;
		c.fill = GridBagConstraints.NORTHWEST;
		add(locIdxLbl,c);
		
		locField = new IndexField("0", manipModuleControl);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 4;
		c.fill = GridBagConstraints.NORTHWEST;
		add(locField,c);
		
		
		addButton = new JButton("+");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manipModuleControl.performAdd();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 5;
		c.fill = GridBagConstraints.BOTH;
		add(addButton,c);
		
//		resetButton = new JButton("Reset");
//		resetButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				manipModuleControl.initAdd();
//			}
//		});
//		c = new GridBagConstraints();
//		c.gridx = 1;
//		c.gridy = 5;
//		c.fill = GridBagConstraints.BOTH;
//		add(resetButton,c);
	}

	private void populateIdPanel(ArrayList<String> idsToAdd) {
		
		idPanel.removeAll();
		
		GridBagConstraints c = new GridBagConstraints();
		int counter = 0;
		for (final String id : idsToAdd){
			
			final IDField idField = new IDField(id,manipModuleControl);

			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = counter;
			idPanel.add(idField,c);
			
			final JButton remItemButton = new JButton("-");
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = counter;
			idPanel.add(remItemButton,c);
			remItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					idField.actionPerformed(e);
					manipModuleControl.removeIdToAdd(id);
				}
			});

			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = counter;
			if (++counter == idsToAdd.size()){
				final JButton addItemButton = new JButton("+");
				addItemButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {							
						idField.actionPerformed(e);
						manipModuleControl.addIdToAdd();
					}
				});
				idPanel.add(addItemButton,c);
			}
		}
	}

//	private void selectFileMethodButton() {
//		
//		FileMethodType type = manipModuleControl.getFileMethodType();
//		if (type == FileMethodType.UPDATE)
//			updateButton.getModel().setSelected(true);
//		if (type == FileMethodType.NONUPDATE)
//			nonupdateButton.getModel().setSelected(true);		
//	}

	private void selectStrucButton() {
		
		StrucType type = manipModuleControl.getStrucAddType();
		if (type == StrucType.LINE)
			lineButton.getModel().setSelected(true);
		if (type == StrucType.COL)
			colButton.getModel().setSelected(true);
//		if (type == StrucType.FILE)
//			fileButton.getModel().setSelected(true);
	}
	

	private void setLocationIndex() {
		
		locField.setText(manipModuleControl.getLocationIdx()+"");
	}

	public void update(Observable o, Object arg) {
		
		if (o instanceof ManipModuleStructureController){
			
			if (arg == null){
				selectStrucButton();
//				selectFileMethodButton();
				setLocationIndex();
				populateIdPanel(manipModuleControl.getIdsToAdd());

//				revalidate();
//				repaint();
			}
		}
	}	
}
