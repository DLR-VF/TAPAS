/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.util.table;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * Basic table for this GUI which provides method to initialise complete columns
 * and set their widths.
 *
 * @author mark_ma
 */
public class ConfigurableJTable extends JTable {

    /**
     * serial UID
     */
    private static final long serialVersionUID = -7631195101802198210L;

    /**
     * Calls super constructor and sets the model.
     *
     * @param itemTableModel model of the table
     * @see JTable
     */
    public ConfigurableJTable(ClassifiedTableModel itemTableModel) {
        super(itemTableModel);
        this.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * This method initialises the column by index with a width and a renderer
     * and an editor.
     *
     * @param column   column index
     * @param width    column's width, only has effect when value is greater 0
     * @param renderer column's renderer, can be null
     * @param editor   column's editor, can be null
     */
    public void initTableColumn(int column, int width, TableCellRenderer renderer, TableCellEditor editor) {
        TableColumn tc = this.getColumn(this.getColumnName(column));
        if (width > 0) {
            tc.setMaxWidth(width);
            tc.setMinWidth(width);
        }
        if (renderer != null) tc.setCellRenderer(renderer);
        if (editor != null) tc.setCellEditor(editor);
    }

    /**
     * This method sets the preferred widths of all columns at once by the
     * parameter 'percentages'.
     *
     * @param percentages width of each column
     */
    public void setPreferredColumnWidths(double[] percentages) {
        Dimension tableDim = this.getPreferredSize();

        double total = 0;
        for (int i = 0; i < getColumnModel().getColumnCount(); i++)
            total += percentages[i];

        for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
            TableColumn column = getColumnModel().getColumn(i);
            column.setPreferredWidth((int) (tableDim.width * (percentages[i] / total)));
        }
    }

    /**
     * This class extends the DefaultTableModel and changes the behaviour of the
     * method getColumnClass(...). Here the real class of the component in the
     * cell is returned.
     *
     * @author mark_ma
     */
    public static class ClassifiedTableModel extends DefaultTableModel {

        /**
         * serial UID
         */
        private static final long serialVersionUID = 1249787609359031215L;

        /**
         * Calls super constructor
         *
         * @param data        row data
         * @param columnNames names of the columns
         * @see DefaultTableModel
         */
        public ClassifiedTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (this.getValueAt(0, columnIndex) != null) return this.getValueAt(0, columnIndex).getClass();
            return Object.class;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}