package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

public class CheckableMatrixPartCriteria<T extends Comparable<T>> extends MatrixPartCriteria<T> {

	private boolean isChecked = false;
	
	public CheckableMatrixPartCriteria(ICriteriaOperation<T> op) {
		super(op);
		isChecked = true;
	}

	public boolean isChecked() {
		return isChecked;
	}

	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
}
