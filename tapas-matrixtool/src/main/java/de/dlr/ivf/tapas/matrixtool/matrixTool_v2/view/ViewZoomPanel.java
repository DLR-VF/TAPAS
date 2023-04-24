package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ViewZoomPanel extends JPanel{

	private JPanel zoomPanel;

	public ViewZoomPanel(){

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;		
		JLabel lines = new JLabel("Zoom");
		add(lines,c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 1;
		zoomPanel = new JPanel();
		zoomPanel.add(new JLabel("--------------to be done--------------"));
		add(zoomPanel,c);
	}
}
