package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

public class CopyColumnOperation extends AbstractColumnOperation {

	public String toString(){
		return "{|} := {|}";
	}

	@Override
	protected void op(int i, int sourceLineIdx, Double v) {
		
		/*
		 * da in der CopyOperation sichergestellt wurde, dass sowohl source als 
		 * auch sink in dieser zeile werte haben ( weil shouldBeConsidered = true), 
		 * einfach in der sink diese zeile durchgehen und den ersten (= einzigen) wert 
		 * ersetzen
		 */
		for (int j = sinkOp.getMinColumnIndex(); j < sinkOp.getMinColumnIndex() + 
			sinkOp.getNumberOfColumns(); j++){
			
			if (sinkOp.shouldBeConsidered(i, j))
				sinkOp.setValue(i, j, v);
		}
	}
	
	
}
