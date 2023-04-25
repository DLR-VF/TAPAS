/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.persitence.db.dbImport;

import de.dlr.ivf.tapas.parameter.ParamString;
import de.dlr.ivf.tapas.parameter.TPS_ParameterClass;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.io.File;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class DBImportTableModel extends DefaultTableModel {

    static final int COL_T_KEY = 0;
    static final int COL_T_NAME = 1;
    static final int COL_F_KEY = 3;
    static final int COL_F_NAME = 4;
    static final int COL_ID = 2;
    static final int COL_ACC = 5;
    private static final String[] HEADER = {"Table Key", "Table Name", "Identifier", "File Key", "File Name", ""};
    static final int LENGTH = HEADER.length;
    /**
     *
     */
    private static final long serialVersionUID = 2391132374453893602L;
    private final Set<Integer> errorSet = new TreeSet<>();
    private String schema = "";
    private int separator = 0;
    private String tableStart;
    private final TPS_ParameterClass parameterClass;

    public DBImportTableModel(JTable table, TPS_ParameterClass parameterClass) {
        super(HEADER, 0);
        table.setModel(this);
        this.addEditableRow();
        table.setDefaultRenderer(Object.class, new ColorCellRenderer());
        table.getColumn(table.getColumnName(COL_T_KEY)).setCellEditor(new ComboBoxItemEditor());
        table.getColumn(table.getColumnName(COL_F_NAME)).setCellEditor(new FileOpenEditor());
        this.parameterClass = parameterClass;
    }

    public void addEditableRow() {
        if (getRowCount() > 0) this.check(getRowCount() - 1);
        this.addRow(new Object[]{"", "", "", "", "", Boolean.FALSE});
    }

    public void addFinalRow(Object[] data) {
        this.insertRow(separator, data);
        this.separator++;
    }

    private void check(int row) {
        if (this.checkSchemaName(row) && this.checkTableName(row)) {
            errorSet.remove(row);
        } else {
            errorSet.add(row);
        }
    }

    private boolean checkSchemaName(int row) {
        return this.getValueAt(row, COL_T_NAME).toString().startsWith(schema + ".");
    }

    public void checkSchemaNames(String schema) {
        this.schema = schema;
        for (int i = 0; i < this.getRowCount() - 1; i++) {
            this.check(i);
        }
    }

    private boolean checkTableName(int row) {
        ParamString ps = ParamString.valueOf(this.getValueAt(row, COL_T_KEY).toString());
        if (DBImportController.DB_REGION_BASED.contains(ps)) {
            String value = this.getValueAt(row, 1).toString();
            if (value.contains(".")) {
                value = value.substring(value.indexOf('.') + 1);
                return value.startsWith(tableStart + "_");
            }
            return false;
        }
        return true;
    }

    public void checkTableNames(String tableStart) {
        this.tableStart = tableStart;
        for (int i = 0; i < this.getRowCount() - 1; i++) {
            this.check(i);
        }
    }

    public void clear() {
        errorSet.clear();
        while (this.getRowCount() > 0) {
            this.removeRow(0);

        }
        this.addEditableRow();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (this.getValueAt(0, columnIndex) != null) return this.getValueAt(0, columnIndex).getClass();
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        // select column
        if (column == COL_ACC) {
            return true;
        }
        // user generated rows
        if (row >= separator) {
            if (column == COL_T_KEY) {
                return true;
            } else if (column == COL_ID) {
                return !this.getValueAt(row, column).toString().startsWith("-");
            } else if (column == COL_T_NAME || column == COL_F_NAME) {
                try {
                    ParamString.valueOf(this.getValueAt(row, 0).toString());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return false;
    }

    @Override
    public void removeRow(int row) {
        if (row < separator) separator--;
        super.removeRow(row);

    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        super.setValueAt(value, row, column);

        if (column == COL_T_KEY || column == COL_T_NAME) this.check(row);

        if (row >= separator) {
            if (column == COL_T_KEY) {
                ParamString ps = ParamString.valueOf(value.toString());
                if (this.parameterClass.isDefined(ps)) this.setValueAt(this.parameterClass.getString(ps), row,
                        COL_T_NAME);
                if (!DBImportController.DB_IDENTIFIABLE_TABLES.contains(ps)) this.setValueAt("-----", row, COL_ID);
                else this.setValueAt("", row, COL_ID);
                this.setValueAt("-----", row, COL_F_KEY);
            } else if (column == COL_T_NAME && isCellEditable(row, column)) {
                this.parameterClass.setString(ParamString.valueOf(this.getValueAt(row, COL_T_KEY).toString()),
                        value.toString());
            } else if (column == COL_F_NAME && getValueAt(row, COL_ID).equals("")) {
                String s = value.toString();
                setValueAt(s.substring(s.lastIndexOf('/') + 1, s.lastIndexOf('.')), row, COL_ID);
            }
        }
        if (row == this.getRowCount() - 1 && column == COL_ACC && Boolean.TRUE.equals(value)) {
            this.addEditableRow();
        }
    }

    private class ColorCellRenderer extends DefaultTableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = 5728297672564533278L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // if (!isSelected) {
            comp.setForeground(Color.BLACK);
            if (errorSet.contains(row) && column == 1) {
                comp.setBackground(new Color(255, 200, 200));
            } else if (row < separator) {
                comp.setBackground(Color.LIGHT_GRAY);
            } else {
                comp.setBackground(Color.WHITE);
            }
            // }
            try {
                if (Boolean.TRUE.equals(getValueAt(row, COL_ACC))) {
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                } else {
                    comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return comp;
        }

    }

    private class ComboBoxItem {
        public JComboBox<String> box;

        public ComboBoxItem() {
            SortedSet<String> sortedSet = new TreeSet<>();
            for (ParamString param : ParamString.values()) {
                if (param.name().startsWith("DB_TABLE")) {
                    sortedSet.add(param.name());
                }
            }

            box = new JComboBox<>();
            for (String param : sortedSet) {
                box.addItem(param);
            }
        }
    }

    private class ComboBoxItemEditor extends DefaultCellEditor {
        /**
         *
         */
        private static final long serialVersionUID = -6264007706770611307L;

        public ComboBoxItemEditor() {
            super(new ComboBoxItem().box);
        }
    }

    private class FileOpenEditor extends AbstractCellEditor implements TableCellEditor {

        /**
         *
         */
        private static final long serialVersionUID = 6657567709096445442L;

        private String path = null;

        public FileOpenEditor() {
            super();
        }

        public Object getCellEditorValue() {
            return path;
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JFileChooser fd = new JFileChooser();
            fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fd.setCurrentDirectory(new File(DBImportGUI.ABS_PATH));
            fd.setVisible(true);

            fd.setDialogTitle("Please choose Config File");
            int ok = fd.showOpenDialog(null);

            if ((ok == JFileChooser.APPROVE_OPTION) && fd.getSelectedFile() != null && fd.getSelectedFile().isFile() &&
                    fd.getSelectedFile().exists()) {
                File pathCacheForConfigFile = fd.getSelectedFile();
                String path = pathCacheForConfigFile.getPath().replace('\\', '/');
                if (path.startsWith(DBImportGUI.ABS_PATH)) {
                    this.path = path.substring(DBImportGUI.ABS_PATH.length());
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Invalid File: " + path + "\nChoose a file from directory: " + DBImportGUI.ABS_PATH,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (ok != JFileChooser.CANCEL_OPTION) {
                JOptionPane.showMessageDialog(null, "Invalid Config File: ", "Error", JOptionPane.ERROR_MESSAGE);
            }
            return new JLabel(this.path);
        }
    }
}
