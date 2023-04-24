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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController.StrucType;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;

public class InteractionStructureRemovePanel extends JPanel implements Observer {

	private ManipModuleStructureController manipModuleControl;
	private JRadioButton lineButton;
	private JRadioButton colButton;
	private JButton removeButton;
	private JButton resetButton;
	private JPanel critPanel;
	private JScrollPane critScrolling;

	public InteractionStructureRemovePanel(final ManipModuleStructureController manipModuleControl) {
		
		this.manipModuleControl = manipModuleControl;
		manipModuleControl.addObserver(this);
		
		setLayout(new GridBagLayout());
				
		lineButton = new JRadioButton(Localisation.getLocaleGuiTerm("LINE_IDCS"));
		colButton = new JRadioButton(Localisation.getLocaleGuiTerm("COL_IDCS"));
		
		ButtonGroup strucButtons = new ButtonGroup();
		strucButtons.add(lineButton);
		strucButtons.add(colButton);
		
		selectStrucButton();
		
		lineButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manipModuleControl.setStrucRemoveType(StrucType.LINE);
			}
		});
		colButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manipModuleControl.setStrucRemoveType(StrucType.COL);
			}
		});
		
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
		
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		critPanel = new JPanel();
		critPanel.setLayout(new GridBagLayout());
		populateCriteriaPanel(manipModuleControl.getIdxToRemove());
		critScrolling = new JScrollPane(critPanel);
		critScrolling.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		critScrolling.setPreferredSize(new Dimension(340,73));
		add(critScrolling,c);
		
		
		removeButton = new JButton("-");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manipModuleControl.performRemove();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.fill = GridBagConstraints.BOTH;
		add(removeButton,c);
		
//		resetButton = new JButton("Reset");
//		resetButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				manipModuleControl.initRemove();
//			}
//		});
//		c = new GridBagConstraints();
//		c.gridx = 1;
//		c.gridy = 3;
//		c.fill = GridBagConstraints.BOTH;
//		add(resetButton,c);
	}
	
	private void populateCriteriaPanel(ArrayList<RangeCriteria<Integer>> idxToRemove) {
		
		critPanel.removeAll();
		
		GridBagConstraints c = new GridBagConstraints();
		int counter = 0;
		for (final RangeCriteria<Integer> i : idxToRemove){
			
			final AbstractCheckingIntegerField tmpMin = new CriteriaMinValueIntegerField(manipModuleControl,i,
					Localisation.integerToString(i.getMinValue()));
			final JComboBox tmpMinOp = new JComboBox(manipModuleControl.getCritOps());
			setCriteriaInCombobox(tmpMinOp,i.getMinOp());
			final AbstractCheckingIntegerField tmpMax = new CriteriaMaxValueIntegerField(manipModuleControl,i,
					Localisation.integerToString(i.getMaxValue()));
			final JComboBox tmpMaxOp = new JComboBox(manipModuleControl.getCritOps());
			setCriteriaInCombobox(tmpMaxOp,i.getMaxOp());
			
			//anonyme actionlistener			
			tmpMinOp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					manipModuleControl.changeCritMinOp(i, (ICriteriaOperation)tmpMinOp.getSelectedItem());
				}
			});
			tmpMaxOp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					manipModuleControl.changeCritMaxOp(i, (ICriteriaOperation)tmpMaxOp.getSelectedItem());
				}
			});
			
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = counter;
			critPanel.add(tmpMin,c);
			
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = counter;
			critPanel.add(tmpMinOp,c);
			
			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = counter;
			critPanel.add(new JLabel("  "+Localisation.getLocaleGuiTerm("IDX")+"  "),c);
			
			c = new GridBagConstraints();
			c.gridx = 3;
			c.gridy = counter;
			critPanel.add(tmpMaxOp,c);
			
			c = new GridBagConstraints();
			c.gridx = 4;
			c.gridy = counter;
			critPanel.add(tmpMax,c);				
			
			final JButton remItemButton = new JButton("-");
			c = new GridBagConstraints();
			c.gridx = 5;
			c.gridy = counter;
			critPanel.add(remItemButton,c);
			remItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					manipModuleControl.removeCrit(i);
				}
			});

			c = new GridBagConstraints();
			c.gridx = 6;
			c.gridy = counter;
			if (++counter == idxToRemove.size()){
				final JButton addItemButton = new JButton("+");
				addItemButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {							
						manipModuleControl.addCrit(null);
					}
				});
				critPanel.add(addItemButton,c);
			}
		}
	}	
	
	private void setCriteriaInCombobox(JComboBox box, ICriteriaOperation c) {
		
		for (int i = 0; i < box.getModel().getSize(); i++){
			if (((ICriteriaOperation)box.getModel().getElementAt(i)).toString().equals(c.toString()))
				box.setSelectedItem(box.getModel().getElementAt(i));
		}
	}

	private void selectStrucButton() {
		
		StrucType type = manipModuleControl.getStrucRemoveType();
		if (type == StrucType.LINE)
			lineButton.getModel().setSelected(true);
		if (type == StrucType.COL)
			colButton.getModel().setSelected(true);
	}

	public void update(Observable o, Object arg) {
		
		if (o instanceof ManipModuleStructureController){
			
			if (arg == null){
				selectStrucButton();
				populateCriteriaPanel(manipModuleControl.getIdxToRemove());
			}
		}
	}
}
