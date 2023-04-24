package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import de.dlr.ivf.tapas.matrixtool.erzeugung.view.DoubleValueEditor;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.ModelEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class TableVisualisation extends JPanel implements Observer {

	private InteractionTableModel tableModel;
	private JTable manipTable;
	private JScrollPane tableScrollPane;
	private JList rowHeader;

	public TableVisualisation(FilterController controller, MemoryModel model) {
		
		this.tableModel = new InteractionTableModel(model);
		model.addObserver(this);
		
		controller.addObserver(this);
		
		manipTable = new JTable(tableModel);
//		manipTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
//		manipTable.setCellSelectionEnabled(true);
		manipTable.getTableHeader().setReorderingAllowed(false);
		manipTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		manipTable.setDefaultEditor(Double.class, new DoubleValueEditor(controller));
		manipTable.setDefaultRenderer(Double.class, new CriteriaRenderer(
				controller));
		
		tableScrollPane = new JScrollPane(manipTable);
		tableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		tableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		rowHeader = new JList(new RowHeaderModel(tableModel.getModel()));
		rowHeader.setFixedCellWidth(100);
		rowHeader.setFixedCellHeight(manipTable.getRowHeight());
		rowHeader.setCellRenderer(new RowHeaderRenderer(manipTable));
		tableScrollPane.setRowHeaderView(rowHeader);
		
		JLabel corner = new JLabel("i \\ j");
		corner.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		corner.setForeground(getForeground());
		corner.setBackground(getBackground());
		corner.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		tableScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, corner);
		add(tableScrollPane);
	}

	
	public void update(Observable o, Object arg) {
		
		if (o instanceof FilterController){
			if (arg == null){
//				tableModel.update();
				revalidate();
				repaint();
			}
		}
		
		if (o instanceof MemoryModel){
			
			tableModel.update();
			
			for (int i = 0; i < manipTable.getColumnCount(); i++){
				manipTable.getColumnModel().getColumn(i).setPreferredWidth(100);
			}	
			
			if (((ModelEvent)arg).getMessage() == ModelEvent.Type.RWS_CHNGD){
				rowHeader = new JList(new RowHeaderModel(tableModel.getModel()));
				rowHeader.setFixedCellWidth(100);
				rowHeader.setFixedCellHeight(manipTable.getRowHeight());
				rowHeader.setCellRenderer(new RowHeaderRenderer(manipTable));
				tableScrollPane.setRowHeaderView(rowHeader);
			}
		}
	}
}
