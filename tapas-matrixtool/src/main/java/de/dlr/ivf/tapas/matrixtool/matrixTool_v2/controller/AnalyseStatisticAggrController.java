package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.AbstractOperationEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.UnaryOperationEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.ColumnAggrLogic;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IAggregationFunction;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IAggregationLogic;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.LineAggrLogic;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.MatrixAggrLogic;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view.StatusBar;

public class AnalyseStatisticAggrController extends AbstractOpProviderController{
	
	public enum Type {
		LINE (Localisation.getLocaleGuiTerm("LINES")),
		COL (Localisation.getLocaleGuiTerm("COLS")),
		MATRIX (Localisation.getLocaleGuiTerm("MTX"));
		
		private String t;

		Type(String t){
			this.t = t;
		}
		
		public String getName(){
			return t;
		}
	}
	private MemoryModel model;
	private Type type;
	private IAggregationFunction<Number> function;
	private FilterController filterControl;

	public AnalyseStatisticAggrController(MemoryModel model,
			StatusBar statusBar, FilterController filterControl) {
		
		this.model = model;
		this.filterControl = filterControl;
		addObserver(statusBar);
		
		init();
	}

	private void init() {
		
		type = Type.LINE;
		function = getAggrFunctions()[0];
		
		setChanged();
		notifyObservers();
	}
	
	public IAggregationFunction<Number> getFunction(){
		return function;
	}
	
	public void setFunction(IAggregationFunction<Number> function){
		this.function = function;
		setChanged();
		notifyObservers();
	}

	public Type getStrucType() {
		
		return type;
	}

	public void setStrucType(Type type) {
		
		this.type = type;
		
		setChanged();
		notifyObservers();
	}

	public void performOperation() {
		
		setChanged();
		notifyObservers(new UnaryOperationEvent(this, AbstractOperationEvent.Type.OP_START,
				type.getName(), function.toString(),null));
		
		IAggregationLogic<Number> logic = null;
		if (type == Type.LINE){
			logic = new LineAggrLogic();
		} else if (type == Type.COL){
			logic = new ColumnAggrLogic();
		} else {
			logic = new MatrixAggrLogic();
		}
		logic.init(this, function, filterControl.getLineIdxCrit(), 
				filterControl.getColumnIdxCrit(), filterControl.getValueCrit(), 
				filterControl.getMatrixPartCrit(), model);
		
		Thread t = new Thread(logic);
		t.start();
	}	
	
	public void signalFinished(){
		setChanged();
		notifyObservers(new UnaryOperationEvent(this, AbstractOperationEvent.Type.OP_FNSHD,
				type.getName(), function.toString(),null));
	}
	
	public void signalError(String error){
		setChanged();
		notifyObservers(new UnaryOperationEvent(this, AbstractOperationEvent.Type.ERROR,
				type.getName(), function.toString(),error));
	}
}
