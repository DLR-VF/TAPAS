package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class DiagProfile implements IFilterProfile {

	private MemoryModel model;

	public DiagProfile(MemoryModel model) {
		this.model = model;
	}
	
	public String toString(){
		return Localisation.getLocaleGuiTerm("DIAG_PRFL");
	}
	
	
	

	public boolean allowLineColEqual() {
		return true;
	}

	public boolean allowLineColUnequal() {
		return false;
	}
	
	
	
	public int getMinColIdx() {
		return 0;
	}

	public ICriteriaOperation<Integer> getMinColOp() {
		return new SmallerOrEqualOperation<Integer>();
	}

	public int getMaxColIdx() {
		return model.getColumnIDs().size();
	}

	public ICriteriaOperation<Integer> getMaxColOp() {
		return new SmallerOrEqualOperation<Integer>();
	}

	
	

	public int getMinLineIdx() {
		return 0;
	}

	public ICriteriaOperation<Integer> getMinLineOp() {
		return new SmallerOrEqualOperation<Integer>();
	}
	
	public int getMaxLineIdx() {
		return model.getRowIDs().size();
	}

	public ICriteriaOperation<Integer> getMaxLineOp() {
		return new SmallerOrEqualOperation<Integer>();
	}

	
	

	public Double getMinValue() {
		return model.getMatrixMinValue();
	}

	public ICriteriaOperation<Double> getMinValueOp() {
		return new SmallerOrEqualOperation<Double>();
	}
	
	public Double getMaxValue() {
		return model.getMatrixMaxValue();
	}

	public ICriteriaOperation<Double> getMaxValueOp() {
		return new SmallerOrEqualOperation<Double>();
	}
}
