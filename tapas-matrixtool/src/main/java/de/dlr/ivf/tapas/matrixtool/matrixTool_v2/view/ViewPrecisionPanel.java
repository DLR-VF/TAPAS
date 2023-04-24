package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ViewPrecisionPanel extends JPanel {

	private JPanel precPanel;

	public ViewPrecisionPanel() {
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;		
		JLabel lines = new JLabel("# Nachkommastellen");
		add(lines,c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 1;
		precPanel = new JPanel();
		precPanel.add(new JLabel("--------------to be done--------------"));
		add(precPanel,c);
	}
}
