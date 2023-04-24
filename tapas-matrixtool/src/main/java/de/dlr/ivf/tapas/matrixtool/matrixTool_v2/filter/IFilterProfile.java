package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

public interface IFilterProfile {
	
	
	public String toString();
	

	public int getMinLineIdx();
	
	public ICriteriaOperation<Integer> getMinLineOp();
	
	public int getMaxLineIdx();
	
	public ICriteriaOperation<Integer> getMaxLineOp();
		
	
	public int getMinColIdx();
	
	public ICriteriaOperation<Integer> getMinColOp();
	
	public int getMaxColIdx();
	
	public ICriteriaOperation<Integer> getMaxColOp();
		
	
	public Double getMinValue();
	
	public ICriteriaOperation<Double> getMinValueOp();
	
	public Double getMaxValue();
	
	public ICriteriaOperation<Double> getMaxValueOp();
		
	
	public boolean allowLineColEqual();
	
	public boolean allowLineColUnequal();
}
