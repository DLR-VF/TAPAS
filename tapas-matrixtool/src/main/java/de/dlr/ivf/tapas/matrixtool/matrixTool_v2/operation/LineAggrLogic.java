package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.erzeugung.model.MemoryList;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.CheckableMatrixPartCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.GenericIdentifyable;

public class LineAggrLogic extends AbstractAggregationLogic<Number> {

	public void run() {
		
		//zuerst sammeln je logikeinheit
		ArrayList<ArrayList<Number>> logicUnitSets = gatherLogicUnitSets();		

		//dann aggregieren je logikeinheitsammlung
		aggregates = aggregateLogicUnitSets(logicUnitSets);
		
		//dann die aggregierten logiksammlungen committen in die zwischenablage
		commitOperation();
	}

	private ArrayList<ArrayList<Number>> gatherLogicUnitSets() {
		
		MemoryList<GenericIdentifyable> logicUnit = model.getRowIDs();
		MemoryList<GenericIdentifyable> logicUnitItems = model.getColumnIDs();
		ArrayList<ArrayList<Number>> logicUnitSets = new ArrayList<ArrayList<Number>>();

		for (int i = 0; i < logicUnit.size(); i++){

			ArrayList<Number> currSet = new ArrayList<Number>();
			
			boolean isInLines = false;
			for (RangeCriteria<Integer> l : lineCrits){
				isInLines |= (l.isMetBy(i));
			}
			
			for (int j = 0; j < logicUnitItems.size(); j++){

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
					
					currSet.add(model.getMatrix().getValue(i, j));
				}				
			}
			
			if (isInLines){
				logicUnitSets.add(currSet);
			} else {
				logicUnitSets.add(null);	
				//damit die reihenfolge/indices der logicSets intakt bleibt, z.B.
				//currSet = (x,x,-,-,x,-,x,-,-)
			}
			
		}			
		
		/*
		 * eine liste der laenge #zeilen-der-von-den-filtern-aufgespannten-matrix
		 * (inkl. eingeschlossener aber nicht durch die filter bestimmter zeilen).
		 * 
		 * wenn eine zeile dieser teilmatrix keine elemente enthaelt, aber von 
		 * den filtern akzeptiert wurde, dann ist diese zeile eine arraylist der 
		 * laenge 0.
		 * 
		 * wenn eine zeile dieser teilmatrix kein elemente enthaelt, und durch
		 * die filter ausgeschlossen wurde, dann ist diese zeile NULL.
		 */
		return logicUnitSets;
	}
}
