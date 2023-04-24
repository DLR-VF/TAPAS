package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.CheckableMatrixPartCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;

public class CriteriaRenderer extends JTextField implements TableCellRenderer{

	private FilterController filterControl;

	public CriteriaRenderer(FilterController filterControl) {

		this.filterControl = filterControl;

		setHorizontalAlignment(JTextField.RIGHT);
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		setBorder(null);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setText(Localisation.doubleToString((Double)value,filterControl.getDisplayDecimalPlaces()));

		if (filterControl.isActive()){
			ArrayList<RangeCriteria<Integer>> lineCrits = filterControl.getLineIdxCrit();
			ArrayList<RangeCriteria<Integer>> colCrits = filterControl.getColumnIdxCrit();
			ArrayList<RangeCriteria<Double>> valCrits = filterControl.getValueCrit();
			ArrayList<CheckableMatrixPartCriteria<Integer>> matrixCrits = filterControl.getMatrixPartCrit();

			boolean isInLines = false;
			for (RangeCriteria l : lineCrits){
				isInLines |= (l.isMetBy(row));
			}
			if (!isInLines)
				return this;


			boolean isInColumns = false;
			for (RangeCriteria c : colCrits){
				isInColumns |= (c.isMetBy(column));
			}
			if (!isInColumns)
				return this;


			boolean isInValues = false;
			for (RangeCriteria v : valCrits){
				isInValues |= (v.isMetBy((Double)value));
			}
			if (!isInValues)
				return this;


			boolean isInMatrixPart = false;
			for (CheckableMatrixPartCriteria m : matrixCrits){
				if (m.isChecked()){
					isInMatrixPart |= m.isMetBy(row,column);
				}
			}
			if (!isInMatrixPart)
				return this;

			setBackground(filterControl.getMarkingColor());
			setForeground(getComplementaryColor(filterControl.getMarkingColor()));
		}
		
		return this;
	}

	private Color getComplementaryColor(Color c){

		return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
	}
}
