package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

public abstract class AbstractInputField extends JTextField implements ActionListener,
	FocusListener{
	
	protected String oldText;
	
	public AbstractInputField(String text) {
		
		super(text,5);
		oldText = text;
		setHorizontalAlignment(JTextField.CENTER);

		addActionListener(this);
//		addFocusListener(this);	//klappt nicht gut damit
	}

	public abstract void actionPerformed(ActionEvent arg0);
	
	public void focusLost(FocusEvent e) {
		actionPerformed(null);
	}
	
	public void focusGained(FocusEvent e){
		selectAll();
	}
}
