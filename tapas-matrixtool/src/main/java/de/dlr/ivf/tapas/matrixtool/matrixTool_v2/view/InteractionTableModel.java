package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.text.ParseException;

import javax.swing.table.AbstractTableModel;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class InteractionTableModel extends AbstractTableModel {

	private MemoryModel model;

	public InteractionTableModel(MemoryModel model) {
		this.model = model;
	}

	public int getColumnCount() {
		return model.getMatrix().getYRange();
	}

	public int getRowCount() {
		return model.getMatrix().getXRange();
	}

	public Object getValueAt(int x, int y) {
		return model.getMatrix().getValue(x, y);
	}

	public MemoryModel getModel() {
		return model;
	}

	public void update() {
//		fireTableDataChanged();
		fireTableStructureChanged();		
	}

	public Class<?> getColumnClass(int columnIndex) {	
		return Double.class;
	}
	
	public String getColumnName(int columnIndex) {
		return columnIndex + ".   '"+model.getColumnIDs().get(columnIndex)+"'";
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex) {	
		return true;
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		
		/*
		 * unbedingt diese setValue-methode verwenden, da reihenfolge der columns und
		 * rows von model und tableModel so uebereinstimmen.
		 */
		try {
			model.setValue(rowIndex, columnIndex, Localisation.stringToDouble(aValue.toString()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
