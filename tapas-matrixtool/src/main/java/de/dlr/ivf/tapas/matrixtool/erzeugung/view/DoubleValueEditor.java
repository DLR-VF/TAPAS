package de.dlr.ivf.tapas.matrixtool.erzeugung.view;

import java.awt.Component;
import javax.swing.JTable;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AbstractCheckingController;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
public class DoubleValueEditor extends AbstractCheckingEditor {

	private AbstractCheckingController control;

	public DoubleValueEditor(AbstractCheckingController control) {
		this.control = control;
	}


	/*
	 * diese methode wird von der jtable aus aufgerufen und es wird als value
	 * einfach getValue(i,j) der jtable genommen. jtable.getValue() ruft model.getValue()
	 * auf, was ein Double zurueckgibt. hier kommt also ein Double an.
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {

		if (value == null){
			setText("");
		} else {
			setText(Localisation.doubleToString((Double)value, -1));
		}
		update();

		return this;

	}
	
	protected boolean isValidValue(){
		
		if (getText().length() == 0)
			return false;
		
		try {
			control.checkDoubleValue(getText());
		} catch (Exception e) {
			return false;
		}

		return true;
	}
}
