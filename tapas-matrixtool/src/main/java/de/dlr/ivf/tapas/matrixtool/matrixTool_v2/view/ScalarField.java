package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;


import java.awt.event.ActionEvent;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleOpsController;

public class ScalarField extends AbstractCheckingDoubleField{

	public ScalarField(String text, ManipModuleOpsController control) {
		super(text, control);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(isValueValid()){
			((ManipModuleOpsController)this.control).changeScalar(getText());
		}
	}
}
