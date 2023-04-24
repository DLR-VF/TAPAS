package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.event.ActionEvent;
import java.text.ParseException;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AbstractCheckingController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController;

public class IndexField extends AbstractCheckingIntegerField{

	public IndexField(String text, AbstractCheckingController control) {
		super(text, control);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		try {
			if (isValueValid()){
				((ManipModuleStructureController)control).setLocationIdx(
						Localisation.stringToInteger(getText()));
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
