package de.dlr.ivf.tapas.runtime.client;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.runtime.client.util.table.ConfigurableJTable;
import de.dlr.ivf.tapas.runtime.client.util.table.ConfigurableJTable.ClassifiedTableModel;
import de.dlr.ivf.tapas.runtime.client.util.table.TextPopupEditor;
import de.dlr.ivf.tapas.runtime.server.SimulationData;
import de.dlr.ivf.tapas.runtime.server.SimulationData.TPS_SimulationState;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties;
import de.dlr.ivf.tapas.runtime.util.ClientControlProperties.ClientControlPropKey;
import de.dlr.ivf.tapas.runtime.util.MultilanguageSupport;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.*;

public class SimulationMonitorPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9157874571784201217L;
	private TPS_DB_Connector connection;
	private ConfigurableJTable table;
	private Timer updateTimer;
	private ClassifiedTableModel tableModel;
	
	/**
	 * This class provides a DefaultTableCellRenderer which set the alignment of
	 * the component inside the cell.
	 * 
	 * @author mark_ma
	 * 
	 */
	private class AlignmentRenderer extends DefaultTableCellRenderer {
		//TODO @PB bring working row back

		/**
		 * serial UID
		 */
		private static final long serialVersionUID = -6707632542192433108L;

		/**
		 * the alignment value
		 */
		private int alignment;

//		private final boolean checkWorkingRows;

		/**
		 * The constructor sets the alignment value
		 * 
		 * @param alignment
		 *            One of the following constants defined in
		 *            <code>SwingConstants</code>: <code>LEFT</code>,
		 *            <code>CENTER</code> (the default for image-only labels),
		 *            <code>RIGHT</code>, <code>LEADING</code> (the default for
		 *            text-only labels) or <code>TRAILING</code>.
		 * @param checkWorkingRows
		 * 
		 * @see SwingConstants
		 */
		public AlignmentRenderer(int alignment, boolean checkWorkingRows) {
			this.alignment = alignment;
//			this.checkWorkingRows = checkWorkingRows;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent
		 * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(table,
					value, isSelected, hasFocus, row, column);
			label.setText(value != null ? value.toString() : "");
			label.setHorizontalAlignment(alignment);
//			if (checkWorkingRows)
//				setBackGroundColorForWorking(
//						table.getValueAt(row, SIM_INDEX.KEY.ordinal())
//								.toString(), label, isSelected);
			return label;
		}

	}

	/**
	 * This renderer only shows the first line (<code>\n</code>) of a String. It
	 * falls back to the {@link DefaultTableCellRenderer} if the value to
	 * display is not a String.
	 * 
	 * @author boec_pa
	 * 
	 */
	private class LongTextRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1834268263225909067L;

		public LongTextRenderer(int alignment, boolean checkWorkingRows) {
			super();
		}

		@Override
		protected void setValue(Object value) {
			if (value instanceof String) {
				String s = ((String) value).split("\n")[0];
				setText(s);
			} else {
				super.setValue(value);
			}
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			if (value instanceof String) {
				String s = (String) value;
				setToolTipText(textToHTML(s));
			}
			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);

		}
	}

	/**
	 * This class provides a Table Cell Renderer which creates an empty border
	 * around the component inside the cell.
	 * 
	 * @author mark_ma
	 * 
	 */
	private class BorderedRenderer implements TableCellRenderer {
		//TODO @PB bring back working rows
		/**
		 * The component inside the cell
		 */
		private JComponent component;
		/**
		 * The border when the line in the table is selected
		 */
		private Border selectedBorder = null;
		/**
		 * The border when the line in the table is not selected
		 */
		private Border unselectedBorder = null;
//		private final boolean checkWorkingRows;

		/**
		 * The constructor builds the borders for the selected and the non
		 * selected case
		 * 
		 * @param component
		 *            the component which is inside the cell
		 * @param checkWorkingRows
		 *            flag to indicate if ths row should be checked, if
		 *            background work is performed
		 */
		public BorderedRenderer(JComponent component, boolean checkWorkingRows) {
			this.component = component;
//			this.checkWorkingRows = checkWorkingRows;
			this.component.setOpaque(true); // MUST do this for background to
			// show up.
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.swing.table.TableCellRenderer#getTableCellRendererComponent
		 * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (this.selectedBorder == null) {
				this.selectedBorder = BorderFactory.createMatteBorder(2, 5, 2,
						5, table.getSelectionBackground());
				this.unselectedBorder = BorderFactory.createMatteBorder(2, 5,
						2, 5, table.getBackground());
			}
			// sets the correct border for the selected or non-selected case
			this.component.setBorder(isSelected ? this.selectedBorder
					: this.unselectedBorder);
//			if (checkWorkingRows)
//				setBackGroundColorForWorking(
//						table.getValueAt(row, SIM_INDEX.KEY.ordinal())
//								.toString(), component, isSelected);
			return this.component;
		}
	}

	/**
	 * This class represents a renderer for a bordered progress bar inside a
	 * cell of a JTable. The cell component has to be a ProgressItem.
	 * 
	 * @author mark_ma
	 * 
	 */
	private class ButtonRenderer extends BorderedRenderer implements
			TableCellRenderer {

		public ButtonRenderer(boolean checkWorkingRows) {
			super(new JButton(""), checkWorkingRows);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
		 * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
		 * boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			JButton btn = (JButton) super.getTableCellRendererComponent(table,
					value, isSelected, hasFocus, row, column);
			btn.setText(value.toString());
			return btn;
		}
	}

	/**
	 * This class represents a renderer for a bordered progress bar inside a
	 * cell of a JTable. The cell component has to be a ProgressItem.
	 * 
	 * @author mark_ma
	 * 
	 */
	private class StringArrayRenderer extends BorderedRenderer implements
			TableCellRenderer {

		/**
		 * Calls super constructor
		 * 
		 * @param checkWorkingRows
		 *            flag to check if this entry is working in background
		 */
		public StringArrayRenderer(boolean checkWorkingRows) {
			super(new JLabel(), checkWorkingRows);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
		 * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
		 * boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(table,
					value, isSelected, hasFocus, row, column);

			StringBuilder sbuilder = new StringBuilder();

			sbuilder.append("[");
			for (String stringValue : (String[]) value) {
				if (sbuilder.length() > 1)
					sbuilder.append(", ");
				sbuilder.append(stringValue);
			}
			sbuilder.append("]");

			lbl.setText(sbuilder.toString());
			return lbl;
		}
	}


	/**
	 * This class renders cells in a JTable with ColorItem objects inside. The
	 * cells have an empty border and show a box with a color between red and
	 * green.
	 * 
	 * @author mark_ma
	 * 
	 */
	private class ColorRenderer extends BorderedRenderer implements
			TableCellRenderer {

		/**
		 * Calls super constructor
		 * 
		 * @param checkWorkingRows
		 *            flag to check if this entry is working in background
		 */
		public ColorRenderer(boolean checkWorkingRows) {
			super(new JLabel(), checkWorkingRows);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
		 * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
		 * boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			// get bordered component
			Component comp = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
			// dermine color
			double v;
			if (value instanceof Boolean) {
				Boolean newValue = (Boolean) value;
				v = newValue ? 1 : 0;
			} else {
				Number newValue = (Number) value;
				v = newValue.doubleValue();
			}
			int red = (int) (255 * (1 - v));
			int green = (int) (255 * v);
			Color newColor = new Color(red, green, 0);
			// set color
			comp.setBackground(newColor);
			return comp;
		}
	}

	/**
	 * This class represents a renderer for a bordered progress bar inside a
	 * cell of a JTable. The cell component has to be a ProgressItem.
	 * 
	 * @author mark_ma
	 * 
	 */
	private class ProgressRenderer extends BorderedRenderer implements
			TableCellRenderer {

		/**
		 * Calls super constructor
		 * 
		 * @param checkWorkingRows
		 *            flag to check if this entry is working in background
		 */
		public ProgressRenderer(boolean checkWorkingRows) {
			super(new JProgressBar(0, 1000), checkWorkingRows);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * de.dlr.ivf.tapas.runtime.gui.TPS_ClientGUI.BorderedRenderer
		 * #getTableCellRendererComponent(javax.swing.JTable , java.lang.Object,
		 * boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			JProgressBar pBar = (JProgressBar) super
					.getTableCellRendererComponent(table, value, isSelected,
							hasFocus, row, column);
			int pValue = (int) (((Number) value).doubleValue() * 1000);
			pBar.setValue(pValue);
			pBar.setString(pValue / 10.0 + "%");
			pBar.setStringPainted(true);
			return pBar;
		}
	}

	/**
	 * This class provides an editor for the simulation data table. The editor
	 * contains a checkbox, which can change the state of the corresponding
	 * simulation in this row of the simulation data table.
	 * 
	 * @author mark_ma
	 */
	private class SimTableButtonEditor extends AbstractCellEditor implements
			TableCellEditor {
		/**
		 * serial UID
		 */
		private static final long serialVersionUID = 7571850003681799808L;

		/**
		 * checkbox inside the cell
		 */
		JButton button;

		/**
		 * The constructor builds the checkbox and adds the ActionListener which
		 * changes the state of the simulation
		 */
		public SimTableButtonEditor() {
//			this.button = new JButton();
//			this.button.addActionListener(new ActionListener() {
//				/*
//				 * (non-Javadoc)
//				 * 
//				 * @see
//				 * java.awt.event.ActionListener#actionPerformed(java.awt.event
//				 * .ActionEvent)
//				 */
//				public void actionPerformed(ActionEvent e) {
//					new Thread() {
//						@Override
//						public void run() {
//							int row = simTable.getEditingRow();
//							if (row >= 0) {
//								String key = simTable.getValueAt(row,
//										SIM_INDEX.KEY.ordinal()).toString();
//								addWorkingKey(key);
//								SimulationMonitor.this.control
//										.changeSimulationDataState(key);
//								stopCellEditing();
//								removeWorkingKey(key);
//							}
//						}
//
//					}.start();
//				}
//			});
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		public Object getCellEditorValue() {
			return button.getText();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax
		 * .swing.JTable, java.lang.Object, boolean, int, int)
		 */
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			button.setText(value.toString());
			return button;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.swing.AbstractCellEditor#isCellEditable(java.util.EventObject)
		 */
		public boolean isCellEditable(EventObject anEvent) {
			if (anEvent instanceof MouseEvent) {
				return ((MouseEvent) anEvent).getClickCount() > 0;
			}
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private class SimulationTableModel extends AbstractTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 480350486132112132L;

		private class SimulationList extends ArrayList<SimulationData> {
			/**
			 * 
			 */
			private static final long serialVersionUID = -2387380790466400080L;

			public Object get(int row, int col) {
				if (row < data.size()) {
					SIM_INDEX key = SIM_INDEX.values()[col];

					SimulationData simulation = get(row);
					switch (key) {
					case KEY:
						return simulation.getKey();
					case FILE:
						simulation.getDescription();
					case STOPPED:
						return simulation
								.minimumState(TPS_SimulationState.STOPPED);
					case STARTED:
						return simulation
								.minimumState(TPS_SimulationState.STARTED);
					case FINISHED:
						return simulation
								.minimumState(TPS_SimulationState.FINISHED);
					case PROGRESS:
						return (double) simulation.getProgress()
								/ simulation.getTotal();
					case COUNT:
						return simulation.getProgress() + "/"
								+ simulation.getTotal();
						// TODO @PB add elapsed, esitmated control?
						// case ELAPSED:
						// return simulation.getElapsedTime(control
						// .getCurrentDatabaseTimestamp());
						// case ESTIMATED:
						// return simulation.getEstimatedTime(control
						// .getCurrentDatabaseTimestamp());
					case DATE_STARTED:
						return simulation.getTimestampStarted();
					case DATE_FINISHED:
						return simulation.getTimestampFinished();
					case ACTION:
						return simulation.getState().getAction();
					case PARAMS:
						simulation.getSimParams();

						default:
						return null;
					}
				}
				return null;
			}

			public SimulationData get(String simkey) {
				for (SimulationData sd : this) {
					if (sd.getKey().equals(simkey)) {
						return sd;
					}
				}
				return null;
			}

			public boolean remove(String simkey) {
				for (SimulationData sd : this) {
					if (sd.getKey().equals(simkey)) {
						return remove(sd);
					}
				}
				return false;
			}

			public HashSet<String> getKeySet() {
				HashSet<String> result = new HashSet<>();
				for (SimulationData sd : this) {
					result.add(sd.getKey());
				}
				return result;
			}

			// public boolean contains(String simkey) {
			// return getKeySet().contains(simkey);
			// }
		}

		private EnumMap<SIM_INDEX, String> columNames;

		private SimulationList data = new SimulationList();

		private TPS_ParameterClass parameterClass;

		public SimulationTableModel(TPS_ParameterClass parameterClass) {

			this.parameterClass = parameterClass;

			MultilanguageSupport.init(SimulationMonitor.class);

			columNames = new EnumMap<>(SIM_INDEX.class);
			columNames.put(SIM_INDEX.KEY,
					MultilanguageSupport.getString("SIM_HEADER_KEY"));
			columNames.put(SIM_INDEX.FILE,
					MultilanguageSupport.getString("SIM_HEADER_FILE"));
			columNames.put(SIM_INDEX.STOPPED,
					MultilanguageSupport.getString("SIM_HEADER_READY"));
			columNames.put(SIM_INDEX.STARTED,
					MultilanguageSupport.getString("SIM_HEADER_STARTED"));
			columNames.put(SIM_INDEX.FINISHED,
					MultilanguageSupport.getString("SIM_HEADER_FINISHED"));
			columNames.put(SIM_INDEX.PROGRESS,
					MultilanguageSupport.getString("SIM_HEADER_PROGRESS"));
			columNames.put(SIM_INDEX.COUNT,
					MultilanguageSupport.getString("SIM_HEADER_COUNT"));
			columNames.put(SIM_INDEX.ELAPSED,
					MultilanguageSupport.getString("SIM_HEADER_ELAPSED"));
			columNames.put(SIM_INDEX.ESTIMATED,
					MultilanguageSupport.getString("SIM_HEADER_ESTIMATED"));
			columNames.put(SIM_INDEX.DATE_STARTED,
					MultilanguageSupport.getString("SIM_HEADER_DATE_STARTED"));
			columNames.put(SIM_INDEX.DATE_FINISHED,
					MultilanguageSupport.getString("SIM_HEADER_DATE_FINISHED"));
			columNames.put(SIM_INDEX.ACTION,
					MultilanguageSupport.getString("SIM_HEADER_ACTION"));
			columNames.put(SIM_INDEX.PARAMS,
					MultilanguageSupport.getString("SIM_HEADER_PARAMETERS"));
		}

		@Override
		public int getColumnCount() {
			return SIM_INDEX.values().length;
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return data.get(rowIndex, columnIndex);
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			// TODO @PB setValue
		}

		public void updateData() {
			synchronized (data) {
				String simkey;
				SimulationData simulationData;

				HashSet<String> keySet = data.getKeySet();
				try {
					ResultSet rs = connection.executeQuery("SELECT * FROM "
							+ this.parameterClass.getString(ParamString.DB_TABLE_SIMULATIONS)
							+ " ORDER BY timestamp_insert", this);

					while (rs.next()) {
						simkey = rs.getString("sim_key");
						if (keySet.remove(simkey)) { // key existed
							simulationData = data.get(simkey);
							simulationData.update(rs);
						} else {// new data
							simulationData = new SimulationData(rs);
							data.add(simulationData);
						}
					}
					rs.close();
				} catch (SQLException e) {
					System.err.println("Error while updating table data.");
					e.printStackTrace();
				}

				for (String s : keySet) {
					data.remove(s);
					keySet.remove(s);
				}
			}
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column) {
			return getColumName(SIM_INDEX.values()[column]);
		}

		public String getColumName(SIM_INDEX idx) {
			return columNames.get(idx);
		}

		public String[] getColumNames() {
			return columNames.values().toArray(new String[0]);
		}
	}

	private enum SIM_INDEX {
		KEY, FILE, STOPPED, STARTED, FINISHED, PROGRESS, COUNT, ELAPSED, ESTIMATED, DATE_STARTED, DATE_FINISHED, ACTION, PARAMS
	}

	public SimulationMonitorPanel(TPS_DB_Connector connection, TPS_ParameterClass parameterClass) {
		super();
		this.connection = connection;

		tableModel = new ClassifiedTableModel(new Object[0][0],
				getColumnNames());
		table = new ConfigurableJTable(tableModel);

		// tableModel = (DefaultTableModel) table.getModel();
		// table = new JTable(tableModel);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.add(table);
		add(scrollPane);
		TimerTask updateTask = new TimerTask() {
			@Override
			public void run() {
				updateData();
			}
		};
		updateTimer = new Timer();
		updateTimer.schedule(updateTask, new Date(), 5000);
	}

	private String[] getColumnNames() {
		MultilanguageSupport.init(SimulationMonitor.class);

		String[] columnNames = new String[SIM_INDEX.values().length];
		columnNames[SIM_INDEX.KEY.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_KEY");
		columnNames[SIM_INDEX.FILE.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_FILE");
		columnNames[SIM_INDEX.STOPPED.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_READY");
		columnNames[SIM_INDEX.STARTED.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_STARTED");
		columnNames[SIM_INDEX.FINISHED.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_FINISHED");
		columnNames[SIM_INDEX.PROGRESS.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_PROGRESS");
		columnNames[SIM_INDEX.COUNT.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_COUNT");
		columnNames[SIM_INDEX.ELAPSED.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_ELAPSED");
		columnNames[SIM_INDEX.ESTIMATED.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_ESTIMATED");
		columnNames[SIM_INDEX.DATE_STARTED.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_DATE_STARTED");
		columnNames[SIM_INDEX.DATE_FINISHED.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_DATE_FINISHED");
		columnNames[SIM_INDEX.ACTION.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_ACTION");
		columnNames[SIM_INDEX.PARAMS.ordinal()] = MultilanguageSupport
				.getString("SIM_HEADER_PARAMETERS");

		return columnNames;
	}

	private HashMap<String, Integer> getSimulations() {
		HashMap<String, Integer> result = new HashMap<>();
		for (int i = 0; i < table.getRowCount(); ++i) {
			result.put(
					(String) table.getModel().getValueAt(
							SIM_INDEX.KEY.ordinal(), i), i);
		}
		return result;
	}

	private void updateData() {
		synchronized (table) {
			HashMap<String, Integer> simulations = getSimulations();
			HashSet<String> keySet = new HashSet<>(simulations.keySet());

			String simkey;
			try {
				ResultSet rs = connection.executeQuery("SELECT * FROM "
						+ this.connection.getParameters().getString(ParamString.DB_TABLE_SIMULATIONS)
						+ " ORDER BY timestamp_insert", this);

				while (rs.next()) {
					simkey = rs.getString("sim_key");
					if (keySet.remove(simkey)) { // key existed, update
						Object[] row = getRowFromSimulation(new SimulationData(
								rs));
						int simIdx = simulations.get(simkey);
						for (int i = 0; i < table.getColumnCount(); ++i) {
							table.setValueAt(row[i], simIdx, i);
						}

					} else {// new data
						tableModel
								.addRow(getRowFromSimulation(new SimulationData(
										rs)));
					}
				}
				rs.close();
			} catch (SQLException e) {
				System.err.println("Error while updating table data.");
				e.printStackTrace();
			}

			for (String s : keySet) {// remove old rows
				removeSimulation(s);
			}
		}
	}

	private Object[] getRowFromSimulation(SimulationData simulation) {
		Object[] data = new Object[SIM_INDEX.values().length];

		data[SIM_INDEX.KEY.ordinal()] = simulation.getKey();
		data[SIM_INDEX.FILE.ordinal()] = simulation.getDescription() == null ? simulation
				.getRelativeFileName() : simulation.getDescription();
		data[SIM_INDEX.PROGRESS.ordinal()] = 0;
		data[SIM_INDEX.COUNT.ordinal()] = simulation.getProgress() + "/"
				+ simulation.getTotal();
		data[SIM_INDEX.STOPPED.ordinal()] = simulation
				.minimumState(TPS_SimulationState.STOPPED);
		data[SIM_INDEX.STARTED.ordinal()] = simulation
				.minimumState(TPS_SimulationState.STARTED);
		data[SIM_INDEX.FINISHED.ordinal()] = simulation
				.minimumState(TPS_SimulationState.FINISHED);
		data[SIM_INDEX.DATE_STARTED.ordinal()] = simulation
				.getTimestampStarted();
		data[SIM_INDEX.DATE_FINISHED.ordinal()] = simulation
				.getTimestampFinished();
		data[SIM_INDEX.ELAPSED.ordinal()] = 0;
		data[SIM_INDEX.ESTIMATED.ordinal()] = 0;
		data[SIM_INDEX.ACTION.ordinal()] = "";
		data[SIM_INDEX.PARAMS.ordinal()] = simulation.getSimParams();

		return data;
	}

	private void removeSimulation(String simkey) {
		for (int i = 0; i < table.getRowCount(); ++i) {
			if (table.getValueAt(i, SIM_INDEX.KEY.ordinal())
					 .equals(simkey)) {
				((DefaultTableModel) table.getModel()).removeRow(i);
				return;
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void initTable(){
//		this.simTable.initTableColumn(SIM_INDEX.PROGRESS.ordinal(), 100,
//				new ProgressRenderer(true), null);
//		this.simTable.initTableColumn(SIM_INDEX.COUNT.ordinal(), 125,
//				new AlignmentRenderer(JLabel.RIGHT, true), null);
//		this.simTable.initTableColumn(SIM_INDEX.KEY.ordinal(), 0,
//				new AlignmentRenderer(JLabel.LEFT, true), null);
//		this.simTable.initTableColumn(SIM_INDEX.FINISHED.ordinal(), 21,
//				new ColorRenderer(true), null);
//		this.simTable.initTableColumn(SIM_INDEX.FILE.ordinal(), 0,
//				new LongTextRenderer(JLabel.LEFT, true), new TextPopupEditor());
//		this.simTable.initTableColumn(SIM_INDEX.STOPPED.ordinal(), 21,
//				new ColorRenderer(true), null);
//		this.simTable.initTableColumn(SIM_INDEX.STARTED.ordinal(), 21,
//				new ColorRenderer(true), null);
//		this.simTable.initTableColumn(SIM_INDEX.ESTIMATED.ordinal(), 75,
//				new AlignmentRenderer(JLabel.RIGHT, true), null);
//		this.simTable.initTableColumn(SIM_INDEX.ELAPSED.ordinal(), 75,
//				new AlignmentRenderer(JLabel.RIGHT, true), null);
//		this.simTable.initTableColumn(SIM_INDEX.DATE_FINISHED.ordinal(), 111,
//				new AlignmentRenderer(JLabel.RIGHT, true), null);
//		this.simTable.initTableColumn(SIM_INDEX.DATE_STARTED.ordinal(), 111,
//				new AlignmentRenderer(JLabel.RIGHT, true), null);
//		this.simTable.initTableColumn(SIM_INDEX.ACTION.ordinal(), 75,
//				new ButtonRenderer(true), new SimTableButtonEditor());
//		this.simTable.initTableColumn(SIM_INDEX.PARAMS.ordinal(), 0,
//				new StringArrayRenderer(true), null);
		
		table.initTableColumn(SIM_INDEX.PROGRESS.ordinal(), 100,
				new ProgressRenderer(true), null);
		table.initTableColumn(SIM_INDEX.COUNT.ordinal(), 125,
				new AlignmentRenderer(JLabel.RIGHT, true), null);
		table.initTableColumn(SIM_INDEX.KEY.ordinal(), 0,
				new AlignmentRenderer(JLabel.LEFT, true), null);
		table.initTableColumn(SIM_INDEX.FINISHED.ordinal(), 21,
				new ColorRenderer(true), null);
		table.initTableColumn(SIM_INDEX.FILE.ordinal(), 0,
				new LongTextRenderer(JLabel.LEFT, true), new TextPopupEditor());
		table.initTableColumn(SIM_INDEX.STOPPED.ordinal(), 21,
				new ColorRenderer(true), null);
		table.initTableColumn(SIM_INDEX.STARTED.ordinal(), 21,
				new ColorRenderer(true), null);
		table.initTableColumn(SIM_INDEX.ESTIMATED.ordinal(), 75,
				new AlignmentRenderer(JLabel.RIGHT, true), null);
		table.initTableColumn(SIM_INDEX.ELAPSED.ordinal(), 75,
				new AlignmentRenderer(JLabel.RIGHT, true), null);
		table.initTableColumn(SIM_INDEX.DATE_FINISHED.ordinal(), 111,
				new AlignmentRenderer(JLabel.RIGHT, true), null);
		table.initTableColumn(SIM_INDEX.DATE_STARTED.ordinal(), 111,
				new AlignmentRenderer(JLabel.RIGHT, true), null);
		table.initTableColumn(SIM_INDEX.ACTION.ordinal(), 75,
				new ButtonRenderer(true), new SimTableButtonEditor());
		table.initTableColumn(SIM_INDEX.PARAMS.ordinal(), 0,
				new StringArrayRenderer(true), null);
		
		
	}
	
	/**
	 * convert newlines to html breaks
	 */
	private String textToHTML(String s) {
		return "<HTML>"
				+ Arrays.toString(s.split("\n")).replace(", ", "<br />")
						.replaceAll("[\\[\\]]", "") + "</HTML>";
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame frame = new JFrame("TableDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		File propFile = new File("client.properties");
		ClientControlProperties props = new ClientControlProperties(propFile);
		
		try {
			TPS_ParameterClass parameterClass = new TPS_ParameterClass();
			parameterClass.loadRuntimeParameters(new File(props.get(ClientControlPropKey.LOGIN_CONFIG)));
			TPS_DB_Connector connection = new TPS_DB_Connector(parameterClass);
			SimulationMonitorPanel panel = new SimulationMonitorPanel(
					connection, parameterClass);

			panel.setOpaque(true);
			frame.setContentPane(panel);
			// Display the window.
			// frame.pack();
			frame.setVisible(true);

		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

	}

}
