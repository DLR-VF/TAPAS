package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import java.util.ArrayList;

import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.UserInputEvent;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.ICriteriaOperation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.filter.RangeCriteria;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.GenericIdentifyable;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view.StatusBar;

public class ManipModuleStructureController extends AbstractUnaryCriteriaController{
	
	private ArrayList<String> idsToAdd;
	private ArrayList<RangeCriteria<Integer>> idsToRemove;
	private MemoryModel model;
	public enum StrucType{
		LINE,
		COL,
		FILE
	}
	private StrucType addType;
	private StrucType remType;
	public enum FileMethodType{
		UPDATE,		//falls eine id aktuell vorhanden, werden ihre eigenschaften mit den neuen ueberschrieben
		NONUPDATE	//falls eine id aktuell vorhanden, wird nichts aktualisiert
	}
	private FileMethodType fileMethodType;
	private int locationIdx;

	public ManipModuleStructureController(MemoryModel model, StatusBar statusBar) {
		
		this.model = model;
		addObserver(statusBar);
		
		init();
	}
	
	public void init() {
	
		initRemove();
		initAdd();
	}

	public void initRemove() {

		idsToRemove = new ArrayList<RangeCriteria<Integer>>();
		idsToRemove.add(new RangeCriteria<Integer>(0,0));
		remType = StrucType.LINE;
		
		setChanged();
		notifyObservers();
	}
	

	public void initAdd() {
		
		idsToAdd = new ArrayList<String>();
		idsToAdd.add("");
		addType = StrucType.LINE;
		fileMethodType = FileMethodType.UPDATE;
		locationIdx = 0;
		
		setChanged();
		notifyObservers();
	}
	
	public int getLocationIdx(){
		return locationIdx;
	}
	
	public void setLocationIdx(int i){
		locationIdx = i;
		setChanged();
		notifyObservers();
	}
	
	public FileMethodType getFileMethodType(){
		return fileMethodType;
	}
	
	public StrucType getStrucAddType(){
		return addType;
	}
	
	public StrucType getStrucRemoveType(){
		return remType;
	}

	public ArrayList<String> getIdsToAdd() {
		return idsToAdd;
	}

	public void changeIdToAdd(String o, String n) {
		
//		for (String i : idsToAdd){
//			System.out.println("'"+i+"'");
//		}

		while (idsToAdd.contains(o)){
			int index = idsToAdd.indexOf(o);
			idsToAdd.remove(index);
			idsToAdd.add(index, n);
		}
		
//		for (String i : idsToAdd){
//			System.out.println("'"+i+"'");
//		}
		
		setChanged();
		notifyObservers();
	}

	public void removeIdToAdd(String id) {
		
		idsToAdd.remove(id);
		
		if (idsToAdd.size() == 0){
			idsToAdd.add("");
		}
		
		setChanged();
		notifyObservers();
	}

	public void addIdToAdd() {

		if (!idsToAdd.contains(""))
			idsToAdd.add("");
		
		setChanged();
		notifyObservers();
	}

	public void setStrucAddType(StrucType type) {
		
		addType = type;
		
		setChanged();
		notifyObservers();
	}
	
	public void setStrucRemoveType(StrucType type) {
		
		remType = type;
		
		setChanged();
		notifyObservers();
	}

	public void setFileMethodType(FileMethodType type) {
		
		fileMethodType = type;
		
		setChanged();
		notifyObservers();
	}

	public void performAdd(){
		
		try {
		if (addType != StrucType.FILE){
			
			ArrayList<String> cleanlist = purgeDuplicateIds(idsToAdd);			
			cleanlist = purgeEmptyIds(cleanlist);
			
			if (addType == StrucType.LINE){
				
				if (locationIdx < 0){
					setLocationIdx(0);
				} else {
					setLocationIdx(Math.min(locationIdx, model.getRowIDs().size()));
				}				
				checkForExistingIds(cleanlist);
				
				for (int i = cleanlist.size() - 1; i >= 0 ; i--){
					model.addRowIdAtIndex(cleanlist.get(i), locationIdx);
				}
				
			} else {
				
				if (locationIdx < 0){
					setLocationIdx(0);
				} else {
					setLocationIdx(Math.min(locationIdx, model.getColumnIDs().size()));
				}				
				checkForExistingIds(cleanlist);
				
				for (int i = cleanlist.size() - 1; i >= 0 ; i--){
					model.addColumnIdAtIndex(cleanlist.get(i), locationIdx);
				}
			}
		}
		
		model.doNotify();
		
		} catch (MatrixStructureManipulationException e) {
			//TODO gehackt hier
			setChanged();
			notifyObservers(e.getMessage());
		}
	}

	private ArrayList<String> purgeEmptyIds(ArrayList<String> list) {
		
		while (list.contains(""))
			list.remove("");
		
		return list;
	}

	public boolean isIdExisting(String idToAdd) {
		boolean result = false;
		switch(addType){
		case LINE:
			result = model.getRowIDs().contains(new GenericIdentifyable(idToAdd));
			break;
		case COL:
			result = model.getColumnIDs().contains(new GenericIdentifyable(idToAdd));
			break;
		case FILE:
			break;
		}
			
		if(result){
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.WRONG, idToAdd, Localisation.getLocaleMessageTerm("ID_EXISTS")));
		}
		return result;
	}
	
	private void checkForExistingIds(ArrayList<String> toAddList) throws MatrixStructureManipulationException {
		
		for (String s : toAddList){
			if(isIdExisting(s))
					throw new MatrixStructureManipulationException(s);
		}
	}

	private ArrayList<String> purgeDuplicateIds(ArrayList<String> list) {
		
		ArrayList<String> clean = new ArrayList<String>();
		
		for (String s : list){
			if (!clean.contains(s))
				clean.add(s);
		}
		
		return clean;
	}

	public void performRemove() {
		
		/*
		 * von vorne her loeschen ist falsch, weil die indexe der elemente nach
		 * links verschoben werden und nachfolgende elemente dementsprechend falsch
		 * indiziert werden.
		 */

		for (int i = model.getRowIDs().size() - 1; i >= 0; i--){

			for (RangeCriteria<Integer> c : idsToRemove){
				if (c.isMetBy(i)){

					if (remType == StrucType.LINE){
						model.removeRowAtIndex(i);
					} else {
						model.removeColumnAtIndex(i);
					}
					break;  // falls mehrere criteria diesen index betreffen
				}
			}
		}
		
		model.doNotify();
	}

	public ArrayList<RangeCriteria<Integer>> getIdxToRemove() {
		return idsToRemove;
	}

	public void addCrit(ArrayList items) {
		
		idsToRemove.add(new RangeCriteria<Integer>(0, 0));
		
		setChanged();
		notifyObservers();
	}

	public void changeCritMaxOp(RangeCriteria item, ICriteriaOperation op) {
		
		for (RangeCriteria<Integer> c : idsToRemove)
			if (c == item)
				c.setMaxOp(op);
		
		setChanged();
		notifyObservers();
	}

	public void changeCritMaxValue(RangeCriteria item, String value) {

		for (RangeCriteria<Integer> c : idsToRemove)
			if (c == item)
				c.setMaxValue(Integer.parseInt(value));
		
		setChanged();
		notifyObservers();
	}

	public void changeCritMinOp(RangeCriteria item, ICriteriaOperation op) {

		for (RangeCriteria<Integer> c : idsToRemove)
			if (c == item)
				c.setMinOp(op);
		
		setChanged();
		notifyObservers();
	}

	public void changeCritMinValue(RangeCriteria item, String value) {
		
		for (RangeCriteria<Integer> c : idsToRemove)
			if (c == item)
				c.setMinValue(Integer.parseInt(value));
		
		setChanged();
		notifyObservers();
	}

	public void removeCrit(RangeCriteria item) {
		
		idsToRemove.remove(item);
		if (idsToRemove.size() == 0){
			idsToRemove.add(new RangeCriteria<Integer>(0,0));
		}
		
		setChanged();
		notifyObservers();
	}
}
