package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JTabbedPane;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class DiagramVisualisation extends JTabbedPane implements Observer {

	public DiagramVisualisation(MemoryModel model) {
		model.addObserver(this);		
	}

	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}


}
