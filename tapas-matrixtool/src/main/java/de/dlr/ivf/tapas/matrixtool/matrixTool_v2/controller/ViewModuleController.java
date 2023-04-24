package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import java.awt.Color;
import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ColoredRangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view.StatusBar;

public class ViewModuleController extends AbstractUnaryCriteriaController{
	
	/*
	 * die markedRangeCriteria definieren WERTE-bereiche -> double
	 */
	private ArrayList<ColoredRangeCriteria<Double>> markings;
	private MemoryModel model;

	public ViewModuleController(MemoryModel model, StatusBar statusBar){
		this.model = model;
		addObserver(statusBar);
		
		init();
	}
	
	public void init(){
		markings = new ArrayList<ColoredRangeCriteria<Double>>();
		markings.add(new ColoredRangeCriteria<Double>(0.0, 0.0, Color.WHITE));
		
		setChanged();
		notifyObservers();
	}

	public ArrayList<ColoredRangeCriteria<Double>> getMarkingCrit() {
		return markings;
	}

	public void addCrit(ArrayList items) {
		
		markings.add(new ColoredRangeCriteria<Double>(0.0,0.0,Color.WHITE));
		
		setChanged();
		notifyObservers();
	}

	public void changeCritMinValue(RangeCriteria item,String value){

		for (ColoredRangeCriteria<Double> i : markings){
			if (item == i)
				i.setMinValue(Double.parseDouble(value));
		}

		setChanged();
		notifyObservers();
	}

	public void changeCritMaxValue(RangeCriteria item,	String value) {

		for (ColoredRangeCriteria<Double> i : markings){
			if (item == i)
				i.setMaxValue(Double.parseDouble(value));
		}		

		setChanged();
		notifyObservers();
	}

	public void changeCritMinOp(RangeCriteria item, ICriteriaOperation op) {
		
		for (ColoredRangeCriteria<Double> i : markings){
			if (item == i)
				i.setMinOp(op);
		}			

		setChanged();
		notifyObservers();
	}
	
	public void changeCritMaxOp(RangeCriteria item, ICriteriaOperation op) {
		
		for (ColoredRangeCriteria<Double> i : markings){
			if (item == i)
				i.setMaxOp(op);
		}			

		setChanged();
		notifyObservers();
	}

	public void removeCrit(RangeCriteria i) {
		
		markings.remove(i);
		
		if (markings.size() == 0){
			markings.add(new ColoredRangeCriteria<Double>(model.getMatrixMinValue(),
					model.getMatrixMaxValue(),Color.WHITE));
		}

		setChanged();
		notifyObservers();
	}
	
	public void changeCritMarking(ColoredRangeCriteria crit, Color c){

		crit.setColor(c);
		
		setChanged();
		notifyObservers();
	}

	public int getDisplayDecimalPlaces(){
		return 10;
	}
}
