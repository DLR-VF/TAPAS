package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class NothingProfile implements IFilterProfile {

	private MemoryModel model;

	public NothingProfile(MemoryModel model) {
		this.model = model;
	}
	
	public String toString(){
		return Localisation.getLocaleGuiTerm("NO_PRFL");
	}
	
	

	public boolean allowLineColEqual() {
		return false;
	}

	public boolean allowLineColUnequal() {
		return false;
	}
	
	
	
	public int getMinColIdx() {
		return 0;
	}

	public ICriteriaOperation<Integer> getMinColOp() {
		return new SmallerOperation<Integer>();
	}

	public int getMaxColIdx() {
		return 0;
	}

	public ICriteriaOperation<Integer> getMaxColOp() {
		return new SmallerOperation<Integer>();
	}

	
	

	public int getMinLineIdx() {
		return 0;
	}

	public ICriteriaOperation<Integer> getMinLineOp() {
		return new SmallerOperation<Integer>();
	}
	
	public int getMaxLineIdx() {
		return 0;
	}

	public ICriteriaOperation<Integer> getMaxLineOp() {
		return new SmallerOperation<Integer>();
	}

	
	

	public Double getMinValue() {
		return 0.0;
	}

	public ICriteriaOperation<Double> getMinValueOp() {
		return new SmallerOperation<Double>();
	}
	
	public Double getMaxValue() {
		return 0.0;
	}

	public ICriteriaOperation<Double> getMaxValueOp() {
		return new SmallerOperation<Double>();
	}
}
