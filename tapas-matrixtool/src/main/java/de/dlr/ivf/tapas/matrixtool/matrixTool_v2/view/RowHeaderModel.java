package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import javax.swing.AbstractListModel;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class RowHeaderModel extends AbstractListModel {
	
	private MemoryModel model;

	public RowHeaderModel(MemoryModel model){
		this.model = model;
	}

	public int getSize(){
		return model.getRowIDs().size();
	}

	public Object getElementAt(int index){
		return model.getRowIDs().get(index);
	}

}
