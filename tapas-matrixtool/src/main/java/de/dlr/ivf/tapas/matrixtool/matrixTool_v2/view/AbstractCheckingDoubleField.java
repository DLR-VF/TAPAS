package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Color;
import java.text.ParseException;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AbstractCheckingController;

public abstract class AbstractCheckingDoubleField extends
		AbstractCheckingNumberField {

	public AbstractCheckingDoubleField(String text,
			AbstractCheckingController control) {
		super(text, control);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean isValueValid() {
		
		try {
			control.checkDoubleValue(getText());
			setBackground(Color.WHITE);
			return true;
		} catch (NumberFormatException e) {
			System.out.println(e.getLocalizedMessage());
			setBackground(Color.RED);
			return false;
		} catch (ParseException e) {
			System.out.println(e.getLocalizedMessage());
			setBackground(Color.RED);
			return false;
		}
	}
}