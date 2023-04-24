package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;

public interface IUnaryCriteriaController<T extends Comparable<T>>{

	public ICriteriaOperation[] getCritOps();

	public void changeCritMinValue(RangeCriteria<T> item,String value);

	public void changeCritMaxValue(RangeCriteria<T> item, String value);
	
	public void changeCritMinOp(RangeCriteria<T> item, ICriteriaOperation<T> op);
	
	public void changeCritMaxOp(RangeCriteria<T> item, ICriteriaOperation<T> op);
	
	public void removeCrit(RangeCriteria<T> item);
	
	public void addCrit(ArrayList<RangeCriteria<T>> items);
}
