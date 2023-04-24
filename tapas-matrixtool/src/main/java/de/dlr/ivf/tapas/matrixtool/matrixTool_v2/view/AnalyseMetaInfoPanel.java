package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.erzeugung.model.MemoryList;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.GenericIdentifyable;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class AnalyseMetaInfoPanel extends JPanel implements Observer {

	private MemoryModel model;
	private JLabel numberLines;
	private JLabel numberColumns;
	private JLabel isQuadratic;

	public AnalyseMetaInfoPanel(MemoryModel model) {
		
		this.model = model;
		model.addObserver(this);
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		numberLines = new JLabel();
		setNumberLinesLabel();
		add(numberLines,c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		numberColumns = new JLabel();
		setNumberColumnsLabel();
		add(numberColumns,c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		isQuadratic = new JLabel();
		setIsQuadratic();
		add(isQuadratic,c);
	}

	private void setIsQuadratic() {
		
		MemoryList<GenericIdentifyable> cols = model.getColumnIDs();
		MemoryList<GenericIdentifyable> rows = model.getRowIDs();
		
		if (cols.size() != rows.size()){
			isQuadratic.setText(Localisation.getLocaleGuiTerm("QUADRATIC")+ " : "+
				Localisation.getLocaleGuiTerm("NO"));
			return;
		} else {
			
			for (int i = 0; i < rows.size(); i++){
				if (!rows.get(i).equals(cols.get(i))){
					isQuadratic.setText(Localisation.getLocaleGuiTerm("QUADRATIC")+ " : "+
							Localisation.getLocaleGuiTerm("NO"));
					return;
				}
			}
		}

		isQuadratic.setText(Localisation.getLocaleGuiTerm("QUADRATIC")+ " : "+
				Localisation.getLocaleGuiTerm("YES"));
	}


	private void setNumberColumnsLabel() {
		
		numberColumns.setText("# "+Localisation.getLocaleGuiTerm("COLS")+" : "+
				model.getColumnIDs().size());
	}

	private void setNumberLinesLabel() {
		
		numberLines.setText("# "+Localisation.getLocaleGuiTerm("LINES")+" : "+
				model.getRowIDs().size());
	}

	public void update(Observable o, Object arg) {

		if (o instanceof MemoryModel){
			
				setNumberColumnsLabel();
				setNumberLinesLabel();
				setIsQuadratic();
			
		}
	}

}
