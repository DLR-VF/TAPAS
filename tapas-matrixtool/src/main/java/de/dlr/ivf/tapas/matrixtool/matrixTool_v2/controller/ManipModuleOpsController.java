package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.AbstractOperationEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.BinaryOperationEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.UserInputEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.AbstractColumnOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.ClipBoardOperand;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.FilterOperand;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IManipulationOperand;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.IManipulationOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.OperandException;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation.OperationException;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view.StatusBar;

public class ManipModuleOpsController extends AbstractOpProviderController{

	private MemoryModel model;
	private FilterController filterControl;
	private IManipulationOperand<Number> sinkOperand, sourceOperand;
	private IManipulationOperation op;
	private double scalar;

	public ManipModuleOpsController(MemoryModel model, StatusBar statusBar,
			FilterController filterControl) {
		
		this.model = model;
		addObserver(statusBar);
		this.filterControl = filterControl;
		
		init();
	}

	public void init() {
		
		sinkOperand = getManipOperands()[0];
		sourceOperand = getManipOperands()[0];
		op = getManipOperations()[0];
		scalar = 0;
		
		setChanged();
		notifyObservers();
	}

	public void changeSinkOp(IManipulationOperand o) {
		sinkOperand = o;	
		setChanged();
		notifyObservers();
	}

	public void changeScalar(String o) {
		try{
			scalar = Localisation.stringToDouble(o);	
			setChanged();
			notifyObservers();
		}catch (Exception e){		
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.PROBLEM, o, 
					Localisation.getLocaleMessageTerm("ERROR_NO_DOUBLE")));
		}
	}
	
	
	public void changeOp(IManipulationOperation op) {
		this.op = op;
		setChanged();
		notifyObservers();
	}

	public void changeSourceOp(IManipulationOperand o) {
		sourceOperand = o;
		setChanged();
		notifyObservers();
	}

	public void performOperation(){
		
		setChanged();
		notifyObservers(new BinaryOperationEvent(this, AbstractOperationEvent.Type.OP_START,
				sinkOperand.toString(), op.toString(),sourceOperand.toString(),null));
		
		try {

			
			//sourceOperand
			if (sourceOperand instanceof ClipBoardOperand){
				((ClipBoardOperand) sourceOperand).initAsSource();
			} else if (sourceOperand instanceof FilterOperand){
				((FilterOperand) sourceOperand).init(filterControl.getLineIdxCrit(), 
						filterControl.getColumnIdxCrit(), 
						filterControl.getValueCrit(), 
						filterControl.getMatrixPartCrit(), model);
			} 
			
			//sinkOperand
			if (sinkOperand instanceof ClipBoardOperand){
				int numberOfColumns = 0;
				if (op instanceof AbstractColumnOperation){
					numberOfColumns = 1;
				} else {
					numberOfColumns = sourceOperand.getNumberOfColumns();
				}
				((ClipBoardOperand) sinkOperand).initAsSink(
						sourceOperand.getNumberOfLines(),
						numberOfColumns);
			} else if (sinkOperand instanceof FilterOperand){
				((FilterOperand) sinkOperand).init(filterControl.getLineIdxCrit(), 
						filterControl.getColumnIdxCrit(), 
						filterControl.getValueCrit(), 
						filterControl.getMatrixPartCrit(), model);
				/*
				 * wenn der filterOperand als sink sehr gross wird, dann wird
				 * waehrend seiner initialisierung die gui nicht refresht (= keine
				 * progressbar)
				 * -> init auslagern in thread
				 */
			} 
			
			
			if (op instanceof AbstractColumnOperation  &&
					(!sourceOperand.isProjectableToColumn()  ||  !sinkOperand.isProjectableToColumn())){
//				System.out.println("source.isProj "+sourceOperand.isProjectableToColumn());
//				System.out.println("sink.isProj "+sinkOperand.isProjectableToColumn());
				
//				throw new OperationException(op.toString()+" braucht als Spalte interpretierbares als Operanden");
				throw new OperationException(op.toString());
			}

			op.init(sinkOperand, sourceOperand,this);
			Thread t = new Thread(op);
			t.start();
			


		} catch (OperationException e) {
			setChanged();
			notifyObservers(new BinaryOperationEvent(this, BinaryOperationEvent.Type.ERROR,
					sinkOperand.toString(), op.toString(),sourceOperand.toString(),
					e.getMessage()));
//			throw e;
		} catch (NumberFormatException e){
			setChanged();
			notifyObservers(new BinaryOperationEvent(this, AbstractOperationEvent.Type.ERROR,
					sinkOperand.toString(), op.toString(),sourceOperand.toString(),
					e.getMessage()));
//			throw e;
		} catch (OperandException e) {
			setChanged();
			notifyObservers(new BinaryOperationEvent(this, BinaryOperationEvent.Type.ERROR,
					sinkOperand.toString(), op.toString(),sourceOperand.toString(),
					e.getMessage()));
		}
	}

	public IManipulationOperand<Number> getSinkOperand() {
		return sinkOperand;
	}

	public IManipulationOperand<Number> getSourceOperand() {
		return sourceOperand;
	}

	public IManipulationOperation getOperation() {
		return op;
	}
	
	public double getScalar(){
		return scalar;
	}
	
	public void signalFinished(){
		model.doNotify();
		setChanged();
		notifyObservers(new BinaryOperationEvent(this, AbstractOperationEvent.Type.OP_FNSHD,
				sinkOperand.toString(), op.toString(),sourceOperand.toString(),null));
	}
	
	public void signalError(String error){
		setChanged();
		notifyObservers(new BinaryOperationEvent(this, AbstractOperationEvent.Type.ERROR,
				sinkOperand.toString(), op.toString(),sourceOperand.toString(),
				error));
	}
}
