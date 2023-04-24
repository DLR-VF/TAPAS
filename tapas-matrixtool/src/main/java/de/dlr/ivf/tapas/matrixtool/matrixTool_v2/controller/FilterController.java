package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import java.awt.Color;
import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.AllProfile;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.CheckableMatrixPartCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.DiagProfile;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.EqualOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.IFilterProfile;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.NothingProfile;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.UnequalOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view.StatusBar;

public class FilterController extends AbstractUnaryCriteriaController{
	
	private ArrayList<RangeCriteria<Integer>> lineIdxCriteria;
	private ArrayList<RangeCriteria<Integer>> columnIdxCriteria;
	private ArrayList<RangeCriteria<Double>> valueCriteria;
	private ArrayList<CheckableMatrixPartCriteria<Integer>> matrixPartCriteria;
	private MemoryModel model;
	private Color markingColor;
	private IFilterProfile profile;
	private boolean isActive;
	
	public FilterController(MemoryModel model, StatusBar bar){
		
		this.model = model;
		addObserver(bar);
		
		init();
	}
	
	public void init(){
		lineIdxCriteria = new ArrayList<RangeCriteria<Integer>>();
		columnIdxCriteria = new ArrayList<RangeCriteria<Integer>>();
		valueCriteria = new ArrayList<RangeCriteria<Double>>();
		lineIdxCriteria.add(new RangeCriteria<Integer>(0,
				Math.max(0, model.getMatrix().getXRange() - 1)));
		columnIdxCriteria.add(new RangeCriteria<Integer>(0,
				Math.max(0, model.getMatrix().getYRange() - 1)));
		valueCriteria.add(new RangeCriteria<Double>(model.getMatrixMinValue(),model.getMatrixMaxValue()));
		
		matrixPartCriteria = new ArrayList<CheckableMatrixPartCriteria<Integer>>();
		matrixPartCriteria.add(new CheckableMatrixPartCriteria(new EqualOperation()));
		//hauptdiagonale
		matrixPartCriteria.add(new CheckableMatrixPartCriteria(new UnequalOperation()));
		//matrix ohne hauptdiagonale
		
		profile = new AllProfile(model);
		markingColor = Color.LIGHT_GRAY;
		isActive = true;
		
		setChanged();
		notifyObservers();
	}

	public ArrayList<RangeCriteria<Integer>> getLineIdxCrit() {
		return lineIdxCriteria;
	}
	
	public ArrayList<RangeCriteria<Integer>> getColumnIdxCrit() {
		return columnIdxCriteria;
	}
	
	public ArrayList<RangeCriteria<Double>> getValueCrit() {
		return valueCriteria;
	}
	
	public ArrayList<CheckableMatrixPartCriteria<Integer>> getMatrixPartCrit(){
		return matrixPartCriteria;
	}

	public void addCrit(ArrayList items) {
		
		if (items == lineIdxCriteria){
			lineIdxCriteria.add(new RangeCriteria<Integer>(0,
					Math.max(0, model.getMatrix().getXRange() - 1)));
		} else if (items == columnIdxCriteria){
			columnIdxCriteria.add(new RangeCriteria<Integer>(0,
					Math.max(0, model.getMatrix().getYRange() - 1)));
		} else {
			valueCriteria.add(new RangeCriteria<Double>(model.getMatrixMinValue(),model.getMatrixMaxValue()));
		}
		
		setChanged();
		notifyObservers();
	}

	public void changeCritMinValue(RangeCriteria item, String value){

		for (RangeCriteria<Integer> i : lineIdxCriteria){
			if (item == i)
				i.setMinValue(Integer.parseInt(value));
		}
		
		for (RangeCriteria<Integer> i : columnIdxCriteria){
			if (item == i)
				i.setMinValue(Integer.parseInt(value));
		}
		
		for (RangeCriteria<Double> i : valueCriteria){
			if (item == i)
				i.setMinValue(Double.parseDouble(value));
		}

		setChanged();
		notifyObservers();
	}

	public void changeCritMaxValue(RangeCriteria item, String value) {

		for (RangeCriteria<Integer> i : lineIdxCriteria){
			if (item == i)
				i.setMaxValue(Integer.parseInt(value));
		}
		
		for (RangeCriteria<Integer> i : columnIdxCriteria){
			if (item == i)
				i.setMaxValue(Integer.parseInt(value));
		}	
		
		for (RangeCriteria<Double> i : valueCriteria){
			if (item == i)
				i.setMaxValue(Double.parseDouble(value));
		}	

		setChanged();
		notifyObservers();
	}

	public void changeCritMinOp(RangeCriteria item, ICriteriaOperation op) {
		
		for (RangeCriteria<Integer> i : lineIdxCriteria){
			if (item == i)
				i.setMinOp(op);
		}
		
		for (RangeCriteria<Integer> i : columnIdxCriteria){
			if (item == i)
				i.setMinOp(op);
		}	
		
		for (RangeCriteria<Double> i : valueCriteria){
			if (item == i)
				i.setMinOp(op);
		}

		setChanged();
		notifyObservers();
	}
	
	public void changeCritMaxOp(RangeCriteria item, ICriteriaOperation op) {
			
		for (RangeCriteria<Integer> i : lineIdxCriteria){
			if (item == i)
				i.setMaxOp(op);
		}
		
		for (RangeCriteria<Integer> i : columnIdxCriteria){
			if (item == i)
				i.setMaxOp(op);
		}	
		
		for (RangeCriteria<Double> i : valueCriteria){
			if (item == i)
				i.setMaxOp(op);
		}

		setChanged();
		notifyObservers();
	}

	public void removeCrit(RangeCriteria item) {
		
		lineIdxCriteria.remove(item);
		if (lineIdxCriteria.size() == 0)
			lineIdxCriteria.add(new RangeCriteria<Integer>(0,
					Math.max(0, model.getMatrix().getXRange() - 1)));

		columnIdxCriteria.remove(item);
		if (columnIdxCriteria.size() == 0)
			columnIdxCriteria.add(new RangeCriteria<Integer>(0,
					Math.max(0, model.getMatrix().getYRange() - 1)));

		valueCriteria.remove(item);
		if (valueCriteria.size() == 0)
			valueCriteria.add(new RangeCriteria<Double>(model.getMatrixMinValue(),model.getMatrixMaxValue()));

		setChanged();
		notifyObservers();
	}
	
	public void setCrit(CheckableMatrixPartCriteria<Integer> crit, boolean set){
		
		crit.setChecked(set);
		
		setChanged();
		notifyObservers();
	}

	public void setModel(MemoryModel model) {
		
		this.model = model; 
	}

	public void setMarkingColor(Color color) {
		
		markingColor = color;
		
		setChanged();
		notifyObservers();
	}
	
	public Color getMarkingColor(){
		
		return markingColor;
	}
	
	public IFilterProfile[] getProfiles() {
		return new IFilterProfile[]{
				new AllProfile(model),
				new NothingProfile(model),
				new DiagProfile(model)
		};
	}
	
	public void setProfile(IFilterProfile p){
		
		profile = p;
		
		lineIdxCriteria = new ArrayList<RangeCriteria<Integer>>();
		columnIdxCriteria = new ArrayList<RangeCriteria<Integer>>();
		valueCriteria = new ArrayList<RangeCriteria<Double>>();
		matrixPartCriteria = new ArrayList<CheckableMatrixPartCriteria<Integer>>();
		
		RangeCriteria<Integer> lc = new RangeCriteria<Integer>(profile.getMinLineIdx(),
				profile.getMaxLineIdx());
		lc.setMinOp(profile.getMinLineOp());
		lc.setMaxOp(profile.getMaxLineOp());
		lineIdxCriteria.add(lc);

		RangeCriteria<Integer> cc = new RangeCriteria<Integer>(profile.getMinColIdx(),
				profile.getMaxColIdx());
		cc.setMinOp(profile.getMinColOp());
		cc.setMaxOp(profile.getMaxColOp());
		columnIdxCriteria.add(cc);

		RangeCriteria<Double> vc = new RangeCriteria<Double>(profile.getMinValue(),
				profile.getMaxValue());
		vc.setMinOp(profile.getMinValueOp());
		vc.setMaxOp(profile.getMaxValueOp());
		valueCriteria.add(vc);


		matrixPartCriteria = new ArrayList<CheckableMatrixPartCriteria<Integer>>();

		CheckableMatrixPartCriteria eo = new CheckableMatrixPartCriteria(new EqualOperation());
		eo.setChecked(profile.allowLineColEqual());
		matrixPartCriteria.add(eo);

		CheckableMatrixPartCriteria<Integer> ueo = new CheckableMatrixPartCriteria(new UnequalOperation());
		ueo.setChecked(profile.allowLineColUnequal());
		matrixPartCriteria.add(ueo);

		
		setChanged();
		notifyObservers();
	}
	
	public IFilterProfile getProfile(){
		
		return profile;
	}

	public void setActive(boolean isActive) {
		
		this.isActive = isActive;
		
		setChanged();
		notifyObservers();
	}
	
	public boolean isActive(){
		
		return isActive;
	}
}
