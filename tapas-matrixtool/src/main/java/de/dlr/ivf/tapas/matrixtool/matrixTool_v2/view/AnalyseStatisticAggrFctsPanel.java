package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AnalyseStatisticAggrController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AnalyseStatisticAggrController.Type;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IAggregationFunction;

public class AnalyseStatisticAggrFctsPanel extends JPanel implements Observer {

	private AnalyseStatisticAggrController aggrControl;
	private JRadioButton lineButton;
	private JRadioButton colButton;
	private JRadioButton matrixButton;
	private JComboBox functionBox;
	private JButton opButton;

	public AnalyseStatisticAggrFctsPanel(MemoryModel model,
			StatusBar statusBar, FilterController filterControl) {
	
		aggrControl = new AnalyseStatisticAggrController(model,statusBar,filterControl);
		aggrControl.addObserver(this);
		
		setLayout(new GridBagLayout());
		
		lineButton = new JRadioButton(Localisation.getLocaleGuiTerm("LINES"));
		colButton = new JRadioButton(Localisation.getLocaleGuiTerm("COLS"));
		matrixButton = new JRadioButton(Localisation.getLocaleGuiTerm("MTX"));
		
		ButtonGroup buttons = new ButtonGroup();
		buttons.add(lineButton);
		buttons.add(colButton);
		buttons.add(matrixButton);
		
		selectButton();
		
		lineButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aggrControl.setStrucType(Type.LINE);
			}
		});
		colButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aggrControl.setStrucType(Type.COL);
			}
		});
		matrixButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aggrControl.setStrucType(Type.MATRIX);
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
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridheight = 2;
		add(matrixButton,c);
		
		
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 1;
		c.gridy = 1;		
		functionBox = new JComboBox(aggrControl.getAggrFunctions());
		add(functionBox,c);
		
		selectFunction(aggrControl.getFunction());
		
		functionBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aggrControl.setFunction((IAggregationFunction<Number>)functionBox.getSelectedItem());
			}
		});
		
		opButton = new JButton(Localisation.getLocaleGuiTerm("OPERAND_CLIPBOARD")+" {|} := f()");
		opButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aggrControl.performOperation();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		add(opButton,c);
	}

	private void selectFunction(IAggregationFunction<Number> function) {

		for (int i = 0; i < functionBox.getModel().getSize(); i++){
			if (((IAggregationFunction<Number>)functionBox.getModel().getElementAt(i)).toString().equals(function.toString()))
				functionBox.setSelectedItem(functionBox.getModel().getElementAt(i));
		}
	}

	private void selectButton() {
		
		Type type = aggrControl.getStrucType();
		if (type == AnalyseStatisticAggrController.Type.LINE){
			lineButton.getModel().setSelected(true);
		} 
		if (type == AnalyseStatisticAggrController.Type.COL){
			colButton.getModel().setSelected(true);
		}
		if (type == AnalyseStatisticAggrController.Type.MATRIX){
			matrixButton.getModel().setSelected(true);
		}
	}

	public void update(Observable o, Object arg) {
		
		if (o instanceof AnalyseStatisticAggrController){
			if (arg == null){
				
				selectButton();
				
			} 
//				else if (arg instanceof BinaryOperationEvent){
//				
//				if (((BinaryOperationEvent) arg).getType() == AbstractOperationEvent.Type.ERROR){
//					JOptionPane.showMessageDialog(this,
//						    ((BinaryOperationEvent) arg).getMessage(),
//						    "Ausführungsfehler",
//						    JOptionPane.ERROR_MESSAGE);
//				}
//			}
		}
	}

}
