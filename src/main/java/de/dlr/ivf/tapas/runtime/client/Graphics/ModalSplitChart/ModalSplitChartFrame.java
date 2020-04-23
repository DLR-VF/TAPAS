package de.dlr.ivf.tapas.runtime.client.Graphics.ModalSplitChart;

import de.dlr.ivf.tapas.runtime.util.MultilanguageSupport;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.XYItemEntity;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * This frame encapsulates a {@link ModalSplitChart} and adds specific behavior
 * like popup menus and mouse events.
 * 
 * @author boec_pa
 * 
 */
public class ModalSplitChartFrame extends ChartFrame implements ActionListener {

	private static final long serialVersionUID = 685327265946837604L;

	JMenuItem mClearSelection;
	JMenuItem mSaveSelection;

	private class ModalSplitMouseListener implements ChartMouseListener {
		@Override
		public void chartMouseClicked(ChartMouseEvent e) {
			if (e.getEntity() instanceof XYItemEntity) {
				XYItemEntity xye = (XYItemEntity) e.getEntity();
				getChart().triggerSelect(xye.getItem());

			}
			getChartPanel().repaint();
		}

		@Override
		public void chartMouseMoved(ChartMouseEvent event) {
			//
		}
	}

	/** Convenience shortcut */
	private ModalSplitChart getChart() {
		return (ModalSplitChart) getChartPanel().getChart();
	}

	public ModalSplitChartFrame(String reference, String model, String title)
			throws ClassNotFoundException, IOException {
		super(title, new ModalSplitChart(reference, model));

		getChartPanel().setInitialDelay(0);// tool tip delay
		getChartPanel().setDismissDelay(60000);
		getChartPanel().addChartMouseListener(new ModalSplitMouseListener());

		getChartPanel().getPopupMenu().addSeparator();
		MultilanguageSupport.init(ModalSplitChartFrame.class);
		mClearSelection = new JMenuItem(
				MultilanguageSupport.getString("MENU_CLEAR_SELECT"));
		mClearSelection.addActionListener(this);
		getChartPanel().getPopupMenu().add(mClearSelection);

		mSaveSelection = new JMenuItem(
				MultilanguageSupport.getString("MENU_SAVE_TO_CLIPBOARD"));
		mSaveSelection.addActionListener(this);
		getChartPanel().getPopupMenu().add(mSaveSelection);

		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JMenuItem source = (JMenuItem) (e.getSource());
		if (source == mClearSelection) {
			getChart().clearSelection();
//			System.out.println("All clear!");
			// TODO @PB force repaint
			getChartPanel().repaint();
		} else if (source == mSaveSelection) {
			StringSelection stringSelection = new StringSelection(getChart()
					.exportSelected());
			Clipboard clipboard = Toolkit.getDefaultToolkit()
					.getSystemClipboard();
			clipboard.setContents(stringSelection, stringSelection);
		}

	}

	/** Testing only */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		String modelKey = "2013y_03m_07d_16h_43m_41s_859ms";
		String referenceKey = "mid2008";
		String title = "TestChart";

		ModalSplitChartFrame frame = new ModalSplitChartFrame(referenceKey,
				modelKey, title);
		frame.setVisible(true);
	}

}
