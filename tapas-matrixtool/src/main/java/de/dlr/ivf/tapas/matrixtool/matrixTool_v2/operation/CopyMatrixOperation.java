package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.operation;

/*
 * copy = ersetzen durch. 
 * op(o1,o2) = o1 wird ersetzt durch o2
 */
public class CopyMatrixOperation extends AbstractMatrixOperation {
	
	public String toString(){
		return "{\u229E} := {\u229E}";
	}

	@Override
	protected void op(int i, int j, int sourceLineIdx, int sourceColIdx, Double v){

//		System.out.println("settting "+v+" in sink at ("+i+","+j+")");
		sinkOp.setValue(i, j, v);
	}
}
