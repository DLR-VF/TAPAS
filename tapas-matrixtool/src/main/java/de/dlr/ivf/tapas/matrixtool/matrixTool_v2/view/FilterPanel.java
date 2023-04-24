package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.CheckableMatrixPartCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.IFilterProfile;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;

public class FilterPanel extends JPanel implements Observer{

	private FilterController filterControl;
	private JPanel lineFilterItemsPanel;
	private JScrollPane lineFilterItemsScrolling;
	private JPanel colFilterItemsPanel;
	private JScrollPane colFilterItemsScrolling;
	private JPanel partsPanel;
	private JPanel valueFilterItemsPanel;
	private JScrollPane valueFilterItemsScrolling;
	private JPanel colorPanel;
	private JPanel profilePanel;
	private JComboBox profiles;
	private JPanel activePanel;
	private JCheckBox activeCheck;
	private JButton colorButton;

	public FilterPanel(final FilterController filterControl) {
		
		this.filterControl = filterControl;
		
		filterControl.addObserver(this);
		
		setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("FILTER")));
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 4;
		lineFilterItemsPanel = new JPanel();
		populateIntegerFilterItemsPanel(filterControl.getLineIdxCrit(), lineFilterItemsPanel, "i");
		lineFilterItemsScrolling = new JScrollPane(lineFilterItemsPanel);
		lineFilterItemsScrolling.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		lineFilterItemsScrolling.setPreferredSize(new Dimension(312,72));
		lineFilterItemsScrolling.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("LINE_IDCS")));
		add(lineFilterItemsScrolling,c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 4;
		colFilterItemsPanel = new JPanel();
		populateIntegerFilterItemsPanel(filterControl.getColumnIdxCrit(), colFilterItemsPanel, "j");
		colFilterItemsScrolling = new JScrollPane(colFilterItemsPanel);
		colFilterItemsScrolling.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		colFilterItemsScrolling.setPreferredSize(new Dimension(312,72));
		colFilterItemsScrolling.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("COL_IDCS")));
		add(colFilterItemsScrolling,c);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 4;
		valueFilterItemsPanel = new JPanel();
		populateDoubleFilterItemsPanel(filterControl.getValueCrit(), valueFilterItemsPanel, "v");
		valueFilterItemsScrolling = new JScrollPane(valueFilterItemsPanel);
		valueFilterItemsScrolling.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		valueFilterItemsScrolling.setPreferredSize(new Dimension(312,72));
		valueFilterItemsScrolling.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("VAL_RANGE")));
		add(valueFilterItemsScrolling,c);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		partsPanel = new JPanel();
		partsPanel.setLayout(new GridBagLayout());
		partsPanel.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("MTX_PARTS")));
		populatePartsPanel(filterControl.getMatrixPartCrit(), partsPanel);
		add(partsPanel,c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 3;
		colorPanel = new JPanel();
		colorPanel.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("MARK_COLOR")));
		colorButton= new JButton("     ");
		colorButton.setBackground(filterControl.getMarkingColor());
		colorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JColorChooser colorChooser = new JColorChooser();
		        JDialog dialog = JColorChooser.createDialog(colorButton,
		                                        null,
		                                        true,  //modal
		                                        colorChooser,
		                                        new ActionListener() {
		                            				public void actionPerformed(ActionEvent e) {
		                            					colorButton.setBackground(colorChooser.getColor());
		                            					filterControl.setMarkingColor(colorChooser.getColor());
		                            				}
		                            			},  //OK button handler
		                                        null); //no CANCEL button handler
		        dialog.setVisible(true);
			}
		});
		colorPanel.add(colorButton);
		add(colorPanel,c);
		
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 3;
		profilePanel = new JPanel();
		profilePanel.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("FILTER_PRFL")));
		profiles = new JComboBox(filterControl.getProfiles());
		setProfileInComboBox(filterControl.getProfile());
		profiles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				filterControl.setProfile((IFilterProfile)profiles.getSelectedItem());
			}
		});
		profilePanel.add(profiles);
		add(profilePanel,c);
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 3;
		activePanel = new JPanel();
		activePanel.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("ACTIV")));
		activeCheck = new JCheckBox(Localisation.getLocaleGuiTerm("FILTER") + " " +
				Localisation.getLocaleGuiTerm("ACTIV"));
		activeCheck.setSelected(filterControl.isActive());
		activeCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				filterControl.setActive(activeCheck.isSelected());
			}
		});
		activePanel.add(activeCheck);
		add(activePanel, c);
	}
	
	
	private void populateProfilePanel() {
		
		
	}


	public FilterController getController(){
		return filterControl;
	}
	

	private void populatePartsPanel(final ArrayList<CheckableMatrixPartCriteria<Integer>> arrayList,
			JPanel panel) {
		
		panel.removeAll();
		
		GridBagConstraints c = new GridBagConstraints();
		int iCounter = 0;
		for (final CheckableMatrixPartCriteria i : arrayList){
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = iCounter;
			final JCheckBox diagCheck = new JCheckBox(i.toString(), i.isChecked());
			diagCheck.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					filterControl.setCrit(i, diagCheck.isSelected());
				}
			});
			partsPanel.add(diagCheck,c);
			iCounter++;
		}
	}
	
	
	private void setCriteriaFields(RangeCriteria i, JPanel panel, int iCounter,
			boolean isIdx) {
		
		if (isIdx){
			GridBagConstraints c = new GridBagConstraints();
			
			final AbstractIntegerValueCriteriaField tmpMin = new CriteriaMinValueIntegerField(
					filterControl,i, Localisation.integerToString(((RangeCriteria<Integer>)i).getMinValue()));
			
			final AbstractIntegerValueCriteriaField tmpMax = new CriteriaMaxValueIntegerField(
					filterControl,i, Localisation.integerToString(((RangeCriteria<Integer>)i).getMaxValue()));
			
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = iCounter;
			panel.add(tmpMin,c);
			
			c = new GridBagConstraints();
			c.gridx = 4;
			c.gridy = iCounter;
			panel.add(tmpMax,c);			
			
		} else {
			GridBagConstraints c = new GridBagConstraints();
			
			final AbstractDoubleValueCriteriaField tmpMin = new CriteriaMinValueDoubleField(
					filterControl,i, Localisation.doubleToString(((RangeCriteria<Double>)i).getMinValue(), 
							filterControl.getDisplayDecimalPlaces()));
			
			final AbstractDoubleValueCriteriaField tmpMax = new CriteriaMaxValueDoubleField(
					filterControl,i, Localisation.doubleToString(((RangeCriteria<Double>)i).getMaxValue(), 
							filterControl.getDisplayDecimalPlaces()));
			
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = iCounter;
			panel.add(tmpMin,c);
			
			c = new GridBagConstraints();
			c.gridx = 4;
			c.gridy = iCounter;
			panel.add(tmpMax,c);
		}
	}
	
	
	private void setOpfields(final RangeCriteria i, JPanel panel,
			int iCounter) {
		
		GridBagConstraints c = new GridBagConstraints();
		
		final JComboBox tmpMinOp = new JComboBox(filterControl.getCritOps());
		setCriteriaInCombobox(tmpMinOp,i.getMinOp());
		
		final JComboBox tmpMaxOp = new JComboBox(filterControl.getCritOps());
		setCriteriaInCombobox(tmpMaxOp,i.getMaxOp());
		
		//anonyme actionlistener			
		tmpMinOp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				filterControl.changeCritMinOp(i, (ICriteriaOperation)tmpMinOp.getSelectedItem());
			}
		});
		tmpMaxOp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				filterControl.changeCritMaxOp(i, (ICriteriaOperation)tmpMaxOp.getSelectedItem());
			}
		});
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = iCounter;
		panel.add(tmpMinOp,c);
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = iCounter;
		panel.add(tmpMaxOp,c);	
	}	
	
	private void populateIntegerFilterItemsPanel(final ArrayList<RangeCriteria<Integer>> arrayList, 
			JPanel panel, String label) {
		
		panel.removeAll();
		panel.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		int iCounter = 0;
		for (final RangeCriteria<Integer> i : arrayList){
			
			setCriteriaFields(i,panel,iCounter,true);
			
			setOpfields(i,panel,iCounter);
			
			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = iCounter;
			panel.add(new JLabel("  "+label+"  "),c);
			
			final JButton remItemButton = new JButton("-");
			c = new GridBagConstraints();
			c.gridx = 5;
			c.gridy = iCounter;
			panel.add(remItemButton,c);
			remItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					filterControl.removeCrit(i);
				}
			});

			c = new GridBagConstraints();
			c.gridx = 6;
			c.gridy = iCounter;
			if (++iCounter == arrayList.size()){
				final JButton addItemButton = new JButton("+");
				addItemButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {							
						filterControl.addCrit(arrayList);
					}
				});
				panel.add(addItemButton,c);
			}
		}
	}


	private void populateDoubleFilterItemsPanel(final ArrayList<RangeCriteria<Double>> arrayList, 
			JPanel panel, String label) {
		
		panel.removeAll();
		panel.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		int iCounter = 0;
		for (final RangeCriteria<Double> i : arrayList){
			
			setCriteriaFields(i,panel,iCounter,false);
			
			setOpfields(i,panel,iCounter);
			
			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = iCounter;
			panel.add(new JLabel("  "+label+"  "),c);
			
			final JButton remItemButton = new JButton("-");
			c = new GridBagConstraints();
			c.gridx = 5;
			c.gridy = iCounter;
			panel.add(remItemButton,c);
			remItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					filterControl.removeCrit(i);
				}
			});

			c = new GridBagConstraints();
			c.gridx = 6;
			c.gridy = iCounter;
			if (++iCounter == arrayList.size()){
				final JButton addItemButton = new JButton("+");
				addItemButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {							
						filterControl.addCrit(arrayList);
					}
				});
				panel.add(addItemButton,c);
			}
		}
	}


	private void setCriteriaInCombobox(JComboBox box, ICriteriaOperation c) {
		
		for (int i = 0; i < box.getModel().getSize(); i++){
			if (((ICriteriaOperation)box.getModel().getElementAt(i)).toString().equals(c.toString()))
				box.setSelectedItem(box.getModel().getElementAt(i));
		}
	}
	
	
	private void setProfileInComboBox(IFilterProfile p){
		
		for (int i = 0; i < profiles.getModel().getSize(); i++){
			if (((IFilterProfile)profiles.getModel().getElementAt(i)).toString().equals(p.toString()))
				profiles.setSelectedItem(profiles.getModel().getElementAt(i));
		}
	}


	public void update(Observable obs, Object arg) {
		
		if (obs instanceof FilterController){
			if (arg == null){
				
				populateIntegerFilterItemsPanel(filterControl.getLineIdxCrit(),lineFilterItemsPanel, "i");
				populateIntegerFilterItemsPanel(filterControl.getColumnIdxCrit(),colFilterItemsPanel, "j");
				populateDoubleFilterItemsPanel(filterControl.getValueCrit(),valueFilterItemsPanel, "x");
				populatePartsPanel(filterControl.getMatrixPartCrit(), partsPanel);
				
				activeCheck.setSelected(filterControl.isActive());
//				setProfileInComboBox(filterControl.getProfile());
				
				validate();
				repaint();
			}
		}
	}
}
