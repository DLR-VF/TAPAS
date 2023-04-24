package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;


import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class AnalyseStatisticPanel extends JTabbedPane {

	public AnalyseStatisticPanel(MemoryModel model, StatusBar statusBar,
			FilterController filterControl) {
		
		setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("STATS")));
		
		addTab(Localisation.getLocaleGuiTerm("AGG_FUNCTION"), 
				new AnalyseStatisticAggrFctsPanel(model,statusBar,filterControl));
		
		addTab(Localisation.getLocaleGuiTerm("METAINF"), 
				new AnalyseMetaInfoPanel(model));
	}

	
}
