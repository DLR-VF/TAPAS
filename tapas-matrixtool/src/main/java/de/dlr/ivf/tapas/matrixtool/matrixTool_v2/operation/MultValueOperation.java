package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

/*
 * mult = multipliziere mit skalar. 
 * op(o1) = o1 wird mit Skalar mulötipliziert
 */
public class MultValueOperation extends AbstractMatrixOperation {
		
	public String toString(){
		return "{\u229E} := scalar * {\u229E}";
	}

	@Override
	protected void op(int i, int j, int sourceLineIdx, int sourceColIdx, Double v){

		sinkOp.setValue(i, j, v * control.getScalar());
	}
}
