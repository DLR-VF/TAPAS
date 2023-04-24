package de.dlr.ivf.tapas.matrixtool.erzeugung.view;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;


/*
 * hier kann die interpretation von werten erfolgen, z.B.
 * - farbige hintergruende
 * - datum nach Locale darstellen
 * - etc...
 */

public class DoubleValueRenderer extends DefaultTableCellRenderer { 
//extends JTextField implements TableCellRenderer {
	
	private int decPlaces;

	public DoubleValueRenderer(int decPlaces){
		this.decPlaces = decPlaces;
		setHorizontalAlignment(JLabel.RIGHT);
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		String s = "";
		if (value != null)
			s = Localisation.doubleToString((Double)value,decPlaces);
		
		return super.getTableCellRendererComponent(table, 
				s, isSelected, hasFocus, row, column);
	}

}