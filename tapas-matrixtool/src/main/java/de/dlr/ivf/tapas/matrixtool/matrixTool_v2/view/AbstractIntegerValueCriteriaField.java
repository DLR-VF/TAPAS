package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.AbstractCheckingController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.IUnaryCriteriaController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;

public abstract class AbstractIntegerValueCriteriaField extends AbstractCheckingIntegerField{
	
	protected RangeCriteria item;
	
	public AbstractIntegerValueCriteriaField(IUnaryCriteriaController control, RangeCriteria item,
			String text){
		super(text,(AbstractCheckingController)control);
		this.item = item;
	}
}
