package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class AnalysePanel extends JPanel{

	public AnalysePanel(FilterController filterControl, MemoryModel model,
			StatusBar statusBar) {

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		add(new AnalyseStatisticPanel(model,statusBar,filterControl), c);
	}
}
