package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class AbstractCheckingCompatibilityField extends AbstractInputField 
	implements DocumentListener{

	public AbstractCheckingCompatibilityField(String text) {
		super(text);
		this.getDocument().addDocumentListener(this);
	}

	public void changedUpdate(DocumentEvent arg0) {
		isValueCompatible();
	}

	public void insertUpdate(DocumentEvent arg0) {
		isValueCompatible();
	}

	public void removeUpdate(DocumentEvent arg0) {
		isValueCompatible();
	}
	
	protected abstract boolean isValueCompatible();
}
