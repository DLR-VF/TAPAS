package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

public class RangeCriteria<T extends Comparable<T>> implements IUnaryCriteria<T>{

	private T minValue;
	private T maxValue;
	private ICriteriaOperation minOp;
	private ICriteriaOperation maxOp;
	
	public RangeCriteria(T def){
		minValue = def;
		maxValue = def;
		minOp = new SmallerOrEqualOperation();
		maxOp = new SmallerOrEqualOperation();
	}
	
	public RangeCriteria(T min, T max) {
		minValue = min;
		maxValue = max;
		minOp = new SmallerOrEqualOperation();
		maxOp = new SmallerOrEqualOperation();
	}

	public T getMinValue() {
		return minValue;
	}
	public void setMinValue(T minValue) {
		this.minValue = minValue;
	}
	public T getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(T maxValue) {
		this.maxValue = maxValue;
	}
	public ICriteriaOperation getMinOp() {
		return minOp;
	}
	public void setMinOp(ICriteriaOperation minOp) {
		this.minOp = minOp;
	}
	public ICriteriaOperation getMaxOp() {
		return maxOp;
	}
	public void setMaxOp(ICriteriaOperation maxOp) {
		this.maxOp = maxOp;
	}

	public boolean isMetBy(T d) {
		return minOp.op(minValue,d) && maxOp.op(d,maxValue); 
	}
}
