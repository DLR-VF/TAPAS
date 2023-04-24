package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

/*
 * set = setzen auf. 
 * op(o1,o2) = o1 wird auf Skalar gesetzt
 */
public class SetValueOperation extends AbstractMatrixOperation {
	
	public String toString(){
		return "{\u229E} := scalar";
	}

	@Override
	protected void op(int i, int j, int sourceLineIdx, int sourceColIdx, Double v){

//		System.out.println("settting "+v+" in sink at ("+i+","+j+")");
		sinkOp.setValue(i, j, control.getScalar());
	}
}
