package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

/*
 * add = addiere skalar auf. 
 * op(o1) = auf o1 wird Skalar addiert
 */
public class AddValueOperation extends AbstractMatrixOperation {
		
	public String toString(){
		return "{\u229E} := scalar + {\u229E}";
	}

	@Override
	protected void op(int i, int j, int sourceLineIdx, int sourceColIdx, Double v){

		sinkOp.setValue(i, j, v + control.getScalar());
	}
}
