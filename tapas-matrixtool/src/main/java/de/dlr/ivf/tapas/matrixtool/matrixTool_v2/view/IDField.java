package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Color;
import java.awt.event.ActionEvent;

import de.dlr.ivf.tapas.matrixtool.common.compatibility.CompatibilityException;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController;

public class IDField extends AbstractCheckingCompatibilityField {

	private ManipModuleStructureController control;

	public IDField(String text, ManipModuleStructureController manipModuleControl) {
		super(text);
		this.control = manipModuleControl;
		isValueCompatible();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		isValueCompatible();
		if (!oldText.equals(getText())){  //sonst endlosschleife in der funktion
			control.changeIdToAdd(oldText, getText());
			oldText = getText();
		}
	}

	@Override
	protected boolean isValueCompatible() {

		try {
			control.checkValueCompatible(getText());
			if(control.isIdExisting(getText())){
				setBackground(Color.RED);
				setToolTipText(Localisation.getLocaleMessageTerm("ID_EXISTS"));
				return false;
			}
			setBackground(Color.WHITE);
			return true;
		} catch (CompatibilityException e) {
			System.out.println(e.getLocalizedMessage());
			setBackground(Color.ORANGE);
			setToolTipText(e.getLocalizedMessage());
			return false;
		}
	}

}
