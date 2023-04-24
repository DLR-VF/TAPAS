package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AbstractCheckingController;

/*
 * es werden die eingegebenen werte auf zahlen gecheckt.
 */
public abstract class AbstractCheckingNumberField extends AbstractInputField 
	implements DocumentListener{

	protected AbstractCheckingController control;

	public AbstractCheckingNumberField(String text, AbstractCheckingController control) {
		super(text);
		this.control = control;
		this.getDocument().addDocumentListener(this);
//		isValueValid();
	}
	
	protected abstract boolean isValueValid();

	public void changedUpdate(DocumentEvent arg0) {
		isValueValid();
	}

	public void insertUpdate(DocumentEvent arg0) {
		isValueValid();
	}

	public void removeUpdate(DocumentEvent arg0) {
		isValueValid();
	}

}
