package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;


import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleOpsController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ManipModuleStructureController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class InteractionPanel extends JPanel implements Observer{

	private JTable manipTable;
	private JScrollPane tableScrollPane;
	private ManipModuleStructureController strucControl;
	private ManipModuleOpsController opsControl;
	private JList rowHeader;

	public InteractionPanel(FilterController filterControl,
			MemoryModel model, StatusBar statusBar) {


		strucControl = new ManipModuleStructureController(model,statusBar);
		opsControl = new ManipModuleOpsController(model,statusBar,filterControl);
		
		filterControl.addObserver(this);		
		strucControl.addObserver(this);

		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		add(new InteractionStructurePanel(strucControl), c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		add(new InteractionOpsPanel(opsControl), c);
		
//		c = new GridBagConstraints();
//		c.gridx = 0;
//		c.gridy = 2;
//		add(new InteractionSortPanel(), c);
	}

	public void update(Observable o, Object arg) {
		
		if (o instanceof FilterController){
			if (arg == null){
//				tableModel.update();
				revalidate();
				repaint();
			}
		}
		if (o instanceof ManipModuleStructureController){
			if (arg == null){
//				tableModel.update();
				revalidate();
				repaint();
			}
		}
	}
}