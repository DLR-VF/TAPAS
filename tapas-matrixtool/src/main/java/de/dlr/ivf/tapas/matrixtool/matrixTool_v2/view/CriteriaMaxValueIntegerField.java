package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.event.ActionEvent;
import java.text.ParseException;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.IUnaryCriteriaController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;

public class CriteriaMaxValueIntegerField extends AbstractIntegerValueCriteriaField{

	public CriteriaMaxValueIntegerField(IUnaryCriteriaController<Integer> control, RangeCriteria<Integer> item,
			String text) {
		super(control, item, text);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			if (isValueValid())
				((IUnaryCriteriaController<Integer>)control).changeCritMaxValue(
						item, Localisation.stringToInteger(getText())+"");
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
