package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.erzeugung.model.MemoryList;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.CheckableMatrixPartCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.GenericIdentifyable;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class FilterOperand implements IManipulationOperand<Number> {
	
	/*
	 * min/maxLine/ColIdx spannt nur ein grobes viereck in der matrix auf. hier
	 * muss erst durch checken von considerables[i][j] geschaut werden, ob eine
	 * koordinate tatsaechlich bearbeitet werden soll.
	 */
	private boolean[][] considerables;
	private MemoryModel model;
	private int minLineIdx;
	private int minColIdx;
	private int lineNumber;
	private int colNumber;
	
	
	/*
	 * der konstruktor wird im panel leer aufgerufen und dient nur zum anzeigen
	 * in der combobox. bei der performOp() wird dieser operand dann erst gefuellt.
	 * ist auch besser fuer die responsivitaet der gui.
	 */
	public void init(ArrayList<RangeCriteria<Integer>> lineCrits,
			ArrayList<RangeCriteria<Integer>> colCrits,
			ArrayList<RangeCriteria<Double>> valCrits,
			ArrayList<CheckableMatrixPartCriteria<Integer>> matrixCrits,
			MemoryModel model){
		
		this.model = model;
		
		considerables = new boolean[model.getRowIDs().size()][model.getColumnIDs().size()];
		
		MemoryList<GenericIdentifyable> rows = model.getRowIDs();
		MemoryList<GenericIdentifyable> cols = model.getColumnIDs();
		boolean minLineIdxFound = false;
		boolean minColIdxFound = false;
		ArrayList<Integer> lineIdxs = new ArrayList<Integer>();
		ArrayList<Integer> colIdxs = new ArrayList<Integer>();

		for (int i = 0; i < rows.size(); i++){
			
			boolean isInLines = false;
			for (RangeCriteria<Integer> l : lineCrits){
				isInLines |= (l.isMetBy(i));
			}
			
			for (int j = 0; j < cols.size(); j++){

				boolean isInColumns = false;
				for (RangeCriteria<Integer> c : colCrits){
					isInColumns |= (c.isMetBy(j));
				}

				boolean isInValues = false;
				for (RangeCriteria<Double> v : valCrits){
					isInValues |= (v.isMetBy(model.getMatrix().getValue(i, j)));
				}

				boolean isInMatrixPart = false;
				for (CheckableMatrixPartCriteria<Integer> m : matrixCrits){
					if (m.isChecked()){
						isInMatrixPart |= m.isMetBy(i,j);
					}
				}
				
				if (isInLines && isInColumns && isInValues && isInMatrixPart){
					
					if (!minLineIdxFound){
						minLineIdx = i;
						minLineIdxFound = true;
						
					}
					if (!minColIdxFound){
						minColIdx = j;
						minColIdxFound = true;
					}
					
					if (!lineIdxs.contains(i))
						lineIdxs.add(i);
					if (!colIdxs.contains(j))
						colIdxs.add(j);
					
					considerables[i][j] = true;
				}
				
			}
			
		}			
		
		
		/*
		 * nur wenn keine gueltigen zeilen und spalten gefunden wurden (= 2 x isEmpty())
		 * dann gibt es keine zeilen und spalten (lineNumber = colNumber = 0).
		 * andernfalls gibt es mind. 1 zeile und spalte, naemlich der gueltigen zelle,
		 * und jetzt muss man mit den indices aufpassen: z.b. minLineIdx = 0 fuer die
		 * gefundene Zelle, und wenn diese zelle jetzt die einzige gueltige ist, dann #
		 * auch der letzte eintrag in lineIdxs = 0. somit waere die anzahl der gueltigen
		 * zeilen auch = 0 . deswegen +1. genauso spalten
		 */
		if (!lineIdxs.isEmpty()  &&  !colIdxs.isEmpty()){
			lineNumber = lineIdxs.get(lineIdxs.size() - 1) - minLineIdx     + 1;
			colNumber = colIdxs.get(colIdxs.size() - 1) - minColIdx     + 1;
		}		
		
		System.out.println("filterOperand.minLineIdx "+minLineIdx);
		System.out.println("filterOperand.lineIdxs.size() "+lineIdxs.size());
		System.out.println("filterOperand.minColIdx "+minColIdx);
		System.out.println("filterOperand.colIdxs.size() "+colIdxs.size());
	}

	public void commitOperation() {
		model.doNotify();
	}

	public Number getValue(int x, int y) {
		return (Number) model.getMatrix().getValue(x, y);
	}

	public void setValue(int x, int y, Number t) {
		model.setValue(x, y, Double.parseDouble(t.toString()));		
	}

	public String toString(){
		return Localisation.getLocaleGuiTerm("OPERAND_FILTER");
	}

	public int getMinColumnIndex() {
		return minColIdx;
	}

	public int getMinLineIndex() {
		return minLineIdx;
	}

	public int getNumberOfColumns() {
		return colNumber;
	}

	public int getNumberOfLines() {
		return lineNumber;
	}

	public boolean isProjectableToColumn() {
		
		for (int i = minLineIdx; i < minLineIdx + lineNumber; i++){
			int valuesPerLine = 0;
			for (int j = minColIdx; j < minColIdx + colNumber; j++){
				if (considerables[i][j])
					valuesPerLine++;
			}
			if (valuesPerLine > 1)
				return false;
		}
		
		return true;
	}
	
	public Number getFirstValueFromLine(int x) {
		
		for (int i = minColIdx; i < minColIdx + colNumber; i++){
			if (considerables[x][i]){
				return (Number)model.getMatrix().getValue(x, i);
			}
		}
		
		return null;
	}
	
	public boolean shouldBeConsidered(int x, int y) {
		return considerables[x][y];
	}	

	public boolean shouldBeConsidered(int i) {
		
		boolean should = false;
		
		for (int j = minColIdx; j < minColIdx + colNumber; j++){
			should |= shouldBeConsidered(i, j);
		}
		
		return should;
	}
}
