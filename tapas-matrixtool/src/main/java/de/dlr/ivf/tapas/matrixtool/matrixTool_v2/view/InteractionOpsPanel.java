package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleOpsController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IManipulationOperand;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IManipulationOperation;

public class InteractionOpsPanel extends JPanel implements Observer{

	private JComboBox firstOpBox;
	private ManipModuleOpsController control;
	private JComboBox opBox;
	private JComboBox secOpBox;
	private JButton opButton;
	private JButton resetButton;
	private ScalarField scalarOp;

	public InteractionOpsPanel(ManipModuleOpsController opsControl) {
		
		this.control = opsControl;
		opsControl.addObserver(this);

		setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("OPS")));
		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;		
		firstOpBox = new JComboBox(opsControl.getManipOperands());
		firstOpBox.setMaximumSize(new Dimension(100, firstOpBox.getHeight()));
		add(firstOpBox,c);
		
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 1;
		c.gridy = 0;		
		opBox = new JComboBox(opsControl.getManipOperations());
		add(opBox,c);
		
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 2;
		c.gridy = 0;		
		secOpBox = new JComboBox(opsControl.getManipOperands());
		add(secOpBox,c);
		
		selectFirstOperand(control.getSinkOperand());
		selectSecondOperand(control.getSourceOperand());
		selectOperation(control.getOperation());
		
		firstOpBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.changeSinkOp((IManipulationOperand)firstOpBox.getSelectedItem());
			}
		});
		
		opBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.changeOp((IManipulationOperation)opBox.getSelectedItem());
			}
		});
		
		secOpBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.changeSourceOp((IManipulationOperand)secOpBox.getSelectedItem());
			}
		});
		
		
		opButton = new JButton("f()");
		opButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.performOperation();
			}
		});
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		add(opButton,c);
		
		JLabel scalarLabel = new JLabel();
		scalarLabel.setText("  Scalar:");
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		add(scalarLabel,c);
		
		scalarOp = new ScalarField("scalar", this.control);
		scalarOp.setText("");
		scalarOp.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				scalarOp.setText(""+Localisation.doubleToString(control.getScalar(),4));
			}

			public void focusLost(FocusEvent e) {
				scalarOp.actionPerformed(null);
			}
		});
		c = new GridBagConstraints();
		c.gridx = 2;
		c.ipadx = 50;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		add(scalarOp,c);
		
//		resetButton = new JButton("Reset");
//		resetButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				control.init();
//			}
//		});
//		c = new GridBagConstraints();
//		c.gridx = 1;
//		c.gridy = 1;
//		c.fill = GridBagConstraints.BOTH;
//		add(resetButton,c);
	}
	
	private void selectOperation(IManipulationOperation o) {	
		for (int i = 0; i < opBox.getModel().getSize(); i++){
			if (((IManipulationOperation)opBox.getModel().getElementAt(i)).toString().equals(o.toString()))
				opBox.setSelectedItem(opBox.getModel().getElementAt(i));
		}
	}

	private void selectSecondOperand(IManipulationOperand<Number> o) {
		for (int i = 0; i < secOpBox.getModel().getSize(); i++){
			if (((IManipulationOperand)secOpBox.getModel().getElementAt(i)).toString().equals(o.toString()))
				secOpBox.setSelectedItem(secOpBox.getModel().getElementAt(i));
		}	
	}

	private void selectFirstOperand(IManipulationOperand<Number> o) {
		for (int i = 0; i < firstOpBox.getModel().getSize(); i++){
			if (((IManipulationOperand)firstOpBox.getModel().getElementAt(i)).toString().equals(o.toString()))
				firstOpBox.setSelectedItem(firstOpBox.getModel().getElementAt(i));
		}
	}

	public void update(Observable o, Object arg) {
		
		if (o instanceof ManipModuleOpsController){
			
			if (arg == null){
				
				selectFirstOperand(control.getSinkOperand());
				selectSecondOperand(control.getSourceOperand());
				selectOperation(control.getOperation());
				scalarOp.setText(""+ Localisation.doubleToString(control.getScalar(),4));
				
			} 
//			else  if (arg instanceof BinaryOperationEvent){
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
