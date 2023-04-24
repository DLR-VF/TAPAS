package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;

public class RowHeaderRenderer extends JLabel implements ListCellRenderer {

	RowHeaderRenderer(JTable table){
		JTableHeader tableHeader = table.getTableHeader();
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		setHorizontalAlignment(CENTER);
		setForeground(tableHeader.getForeground());
		setOpaque(true);  // Damit der Hintergrund nicht verändert wird
		setFont(tableHeader.getFont());
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, 
			boolean fSelected, boolean fCellHasFocus){
		
		setText(index + ".   '"+value.toString()+"'");
		return this;
	}
}
