package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AnalyseStatisticAggrController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.CheckableMatrixPartCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public abstract class AbstractAggregationLogic<T extends Number> implements IAggregationLogic<T>, 
	ClipboardOwner {
	
	protected IAggregationFunction<T> func;
	protected AnalyseStatisticAggrController control;
	protected ArrayList<RangeCriteria<Integer>> lineCrits;
	protected ArrayList<RangeCriteria<Integer>> colCrits;
	protected ArrayList<RangeCriteria<Double>> valCrits;
	protected ArrayList<CheckableMatrixPartCriteria<Integer>> matrixCrits;
	protected MemoryModel model;
	protected ArrayList<Number> aggregates;

	public void init(
			AnalyseStatisticAggrController analyseStatisticAggrController,
			IAggregationFunction<T> f,
			ArrayList<RangeCriteria<Integer>> lineCrits,
			ArrayList<RangeCriteria<Integer>> colCrits,
			ArrayList<RangeCriteria<Double>> valCrits,
			ArrayList<CheckableMatrixPartCriteria<Integer>> matrixCrits,
			MemoryModel model) {		
		
		this.func = f;
		this.control = analyseStatisticAggrController;
		this.lineCrits = lineCrits;
		this.colCrits = colCrits;
		this.valCrits = valCrits;
		this.matrixCrits = matrixCrits;
		this.model = model;
	}

	public void commitOperation(){
		
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < aggregates.size(); i++){			
 
			if (aggregates.get(i) != null)
				s.append(aggregates.get(i));
			
			s.append("\r\n");
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(s.toString()),this);
		
		control.signalFinished();
	}
	
	protected ArrayList<Number> aggregateLogicUnitSets(
			ArrayList<ArrayList<T>> logicUnitSets) {
		
		ArrayList<Number> aggs = new ArrayList<Number>();
		
		for (ArrayList<T> set : logicUnitSets){
			
			if (set == null){  	//fall, dass zeilen-index nicht im filter war
				aggs.add(null);
			} else if (set.size() == 0){	//fall, dass zeilen-index im filter, 
				//aber andere filter verhindern, dass werte dieser zeile gueltig sind
				aggs.add(null);
			} else {	//es gibt in dieser zeile gueltige werte
				aggs.add(func.f(set));
			}
		}
		
		return aggs;
	}
	
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// TODO Auto-generated method stub
		
	}
}
