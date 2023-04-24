package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public class InteractionSortPanel extends JPanel {

	private JPanel manipSortPanel;

	public InteractionSortPanel() {
		
		setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("SORT")));
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;		
		JLabel lines = new JLabel("Sortieren");
		add(lines,c);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 1;
		manipSortPanel = new JPanel();
		manipSortPanel.add(new JLabel("--------------to be done--------------"));
		add(manipSortPanel,c);
	}
}
