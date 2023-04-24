package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ViewModuleController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ColoredRangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;

public class ViewValueMarkingPanel extends JPanel implements Observer{

	private ViewModuleController viewModuleControl;
	private JPanel markingPanel;
	private JScrollPane markingScrolling;

	public ViewValueMarkingPanel(ViewModuleController viewModuleControl) {
		
		this.viewModuleControl = viewModuleControl;
		viewModuleControl.addObserver(this);
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;		
		JLabel lines = new JLabel(Localisation.getLocaleGuiTerm("MARKINGS"));
		add(lines,c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 1;
		markingPanel = new JPanel();
		markingPanel.setLayout(new GridBagLayout());
		populateMarkingPanel(viewModuleControl.getMarkingCrit(), markingPanel, "v");
		markingScrolling = new JScrollPane(markingPanel);
		markingScrolling.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		markingScrolling.setPreferredSize(new Dimension(340,73));
		add(markingScrolling,c);
	}

	private void populateMarkingPanel(final ArrayList<ColoredRangeCriteria<Double>> items,
			JPanel panel, String label) {

		panel.removeAll();
		
		GridBagConstraints c = new GridBagConstraints();
		int iCounter = 0;
		for (final ColoredRangeCriteria<Double> i : items){
			
			final AbstractCheckingDoubleField tmpMin = new CriteriaMinValueDoubleField(viewModuleControl,i,
					Localisation.doubleToString(i.getMinValue(), 
							viewModuleControl.getDisplayDecimalPlaces()));
			final JComboBox tmpMinOp = new JComboBox(viewModuleControl.getCritOps());
			setCriteriaInCombobox(tmpMinOp,i.getMinOp());
			final AbstractCheckingDoubleField tmpMax = new CriteriaMaxValueDoubleField(viewModuleControl,i,
					Localisation.doubleToString(i.getMaxValue(), 
							viewModuleControl.getDisplayDecimalPlaces()));
			final JComboBox tmpMaxOp = new JComboBox(viewModuleControl.getCritOps());
			setCriteriaInCombobox(tmpMaxOp,i.getMaxOp());
			final JButton colorButton= new JButton(" ");
			colorButton.setBackground(i.getColor());
			
			//anonyme actionlistener			
			tmpMinOp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					viewModuleControl.changeCritMinOp(i, (ICriteriaOperation)tmpMinOp.getSelectedItem());
				}
			});
			tmpMaxOp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					viewModuleControl.changeCritMaxOp(i, (ICriteriaOperation)tmpMaxOp.getSelectedItem());
				}
			});
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
			                            					viewModuleControl.changeCritMarking(i, colorChooser.getColor());
			                            				}
			                            			},  //OK button handler
			                                        null); //no CANCEL button handler
			        dialog.setVisible(true);
				}
			});
			
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = iCounter;
			panel.add(tmpMin,c);
			
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = iCounter;
			panel.add(tmpMinOp,c);
			
			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = iCounter;
			panel.add(new JLabel("  "+label+"  "),c);
			
			c = new GridBagConstraints();
			c.gridx = 3;
			c.gridy = iCounter;
			panel.add(tmpMaxOp,c);
			
			c = new GridBagConstraints();
			c.gridx = 4;
			c.gridy = iCounter;
			panel.add(tmpMax,c);
			
			c = new GridBagConstraints();
			c.gridx = 5;
			c.gridy = iCounter;
			panel.add(colorButton,c);				
			
			final JButton remItemButton = new JButton("-");
			c = new GridBagConstraints();
			c.gridx = 6;
			c.gridy = iCounter;
			panel.add(remItemButton,c);
			remItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					viewModuleControl.removeCrit(i);
				}
			});

			c = new GridBagConstraints();
			c.gridx = 7;
			c.gridy = iCounter;
			if (++iCounter == items.size()){
				final JButton addItemButton = new JButton("+");
				addItemButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {							
						viewModuleControl.addCrit(null);
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

	public void update(Observable obs, Object arg) {
		
		if (obs instanceof ViewModuleController){
			if (arg == null)
				populateMarkingPanel(viewModuleControl.getMarkingCrit(), markingPanel, "x");
		}
	}
}
