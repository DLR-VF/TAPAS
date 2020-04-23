package de.dlr.ivf.tapas.runtime.client.util.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Arrays;

/**
 * This renderer only shows the first line (<code>\n</code>) of a String. It
 * falls back to the {@link DefaultTableCellRenderer} if the value to display is
 * not a String.
 * 
 * @author boec_pa
 * 
 */
public class LongTextRenderer extends DefaultTableCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4739718818293373900L;

	@Override
	protected void setValue(Object value) {
		if (value instanceof String) {
			String s = ((String) value).split("\n")[0];
			super.setValue(s);
		} else {
			super.setValue(value);
		}
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		if (value instanceof String) {
			String s = (String) value;
			setToolTipText(textToHTML(s));
		}
		return super.getTableCellRendererComponent(table, value, isSelected,
				hasFocus, row, column);

	}

	/**
	 * convert newlines to html breaks
	 */
	private String textToHTML(String s) {
		return "<HTML>"
				+ Arrays.toString(s.split("\n")).replace(", ", "<br />")
						.replaceAll("[\\[\\]]", "") + "</HTML>";
	}
}
