package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.TAPASMemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class MatrixManipulatorFrame extends JDialog {
	
	private static StatusBar statusBar = null;
	
	public MatrixManipulatorFrame(JFrame parent, TAPASMemoryModel model, String title) {

		
		super(parent);
		this.setModalityType(ModalityType.DOCUMENT_MODAL);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		Toolkit jTools = Toolkit.getDefaultToolkit();
		Dimension dim = jTools.getScreenSize();
		this.setSize(980,700);
		
        this.setTitle(title);

        statusBar = new StatusBar();
		
		FilterPanel filter = createFilter(model,statusBar); 
		JTabbedPane inter = createInteractionPanel(model,statusBar,filter);
		JTabbedPane visual = createVisualisation(model,statusBar,filter);

		this.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		this.add(filter,c);

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		this.add(inter,c);

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 2;
		this.add(visual,c);

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		this.add(statusBar,c);
		this.pack();
		this.setLocation(dim.width/2 - this.getWidth()/2, dim.height/2 - this.getHeight()/2);
	
		
	}
	
	
	
	public static int showDialog(JFrame parent, TAPASMemoryModel model, String title){
		MatrixManipulatorFrame instance = new MatrixManipulatorFrame(parent, model, title);
		instance.setVisible(true);
			
 		return 0; 
	}	


	private JTabbedPane createVisualisation(MemoryModel model, StatusBar bar,
			FilterPanel filter) {

		JTabbedPane visPan = new JTabbedPane();

		TableVisualisation tVis = new TableVisualisation(filter.getController(),model);
		DiagramVisualisation dVis = new DiagramVisualisation(model);

		visPan.addTab(Localisation.getLocaleGuiTerm("TABLE_VIS"), tVis);
		visPan.addTab(Localisation.getLocaleGuiTerm("DIAG_VIS"), dVis);
		
		visPan.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("VISUAL")));

		return visPan;
	}


	private JTabbedPane createInteractionPanel(MemoryModel model, StatusBar bar, FilterPanel filter) {

		JTabbedPane tabPan = new JTabbedPane();

		InteractionPanel manipModule = new InteractionPanel(filter.getController(),model,bar);
		AnalysePanel analyseModule = new AnalysePanel(filter.getController(),model,bar);

		tabPan.addTab(Localisation.getLocaleGuiTerm("MANIPULATION"),manipModule);
		tabPan.addTab(Localisation.getLocaleGuiTerm("ANALYSIS"),analyseModule);

		tabPan.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("INTERAKT")));
		
		return tabPan;
	}


	private FilterPanel createFilter(MemoryModel model, StatusBar bar) {

		FilterPanel filter = new FilterPanel(new FilterController(model,bar));
		filter.getController().setActive(false);
		return filter;
	}
}
