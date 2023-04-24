package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController;

public class InteractionStructurePanel extends JTabbedPane{
	
	public InteractionStructurePanel(ManipModuleStructureController control) {
		
		setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("STRUCTURE")));
		addTab("  +  ", new InteractionStructureAddPanel(control));
		addTab("  -  ", new InteractionStructureRemovePanel(control));
	}
}
