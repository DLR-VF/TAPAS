package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.ViewModuleController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.ModelEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;


public class ViewModule extends JPanel implements Observer{

	private ViewTableModel tableModel;
	private JTable viewTable;
	private JScrollPane tableScrollPane;
	private ViewModuleController viewModuleControl;
	private JList rowHeader;

	public ViewModule(FilterController filterControl, MemoryModel model,
			StatusBar statusBar) {

		this.tableModel = new ViewTableModel(model);
		this.viewModuleControl = new ViewModuleController(model, statusBar);

		filterControl.addObserver(this);
		viewModuleControl.addObserver(this);
		model.addObserver(this);
		
		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		add(new FilterPanel(filterControl),c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		add(new ViewValueMarkingPanel(viewModuleControl), c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		add(new ViewPrecisionPanel(), c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		add(new ViewZoomPanel(), c);

		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 4;
		c.fill = GridBagConstraints.BOTH;
		viewTable = new JTable(tableModel);
		viewTable.getTableHeader().setReorderingAllowed(false);
		viewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		viewTable.getTableHeader().setReorderingAllowed(false);
		viewTable.setDefaultRenderer(Double.class, new CriteriaRenderer(filterControl));		
		
		tableScrollPane = new JScrollPane(viewTable);
		tableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		tableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		rowHeader = new JList(new RowHeaderModel(tableModel.getModel()));
		rowHeader.setFixedCellWidth(100);
		rowHeader.setFixedCellHeight(viewTable.getRowHeight());
		rowHeader.setCellRenderer(new RowHeaderRenderer(viewTable));
		tableScrollPane.setRowHeaderView(rowHeader);
		
		JLabel corner = new JLabel("i \\ j");
		corner.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		corner.setForeground(getForeground());
		corner.setBackground(getBackground());
		corner.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		tableScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, corner);
		add(tableScrollPane,c);
	}

	public void update(Observable o, Object arg) {
		
		if (o instanceof FilterController){
			if (arg == null){
//				tableModel.update();
				revalidate();
				repaint();
			}
		}
		if (o instanceof ViewModuleController){
			if (arg == null){
//				tableModel.update();
				revalidate();
				repaint();
			}
		}
		if (o instanceof MemoryModel){
			
			tableModel.update();
			
			for (int i = 0; i < viewTable.getColumnCount(); i++){
				viewTable.getColumnModel().getColumn(i).setPreferredWidth(100);
			}	
			
			if (((ModelEvent)arg).getMessage() == ModelEvent.Type.RWS_CHNGD){
				rowHeader = new JList(new RowHeaderModel(tableModel.getModel()));
				rowHeader.setFixedCellWidth(100);
				rowHeader.setFixedCellHeight(viewTable.getRowHeight());
				rowHeader.setCellRenderer(new RowHeaderRenderer(viewTable));
				tableScrollPane.setRowHeaderView(rowHeader);
			}
		}
	}
}
