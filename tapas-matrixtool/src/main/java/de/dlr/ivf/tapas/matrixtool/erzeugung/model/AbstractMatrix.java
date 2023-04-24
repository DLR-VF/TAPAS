package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

import java.util.LinkedList;
import java.util.Observable;

import de.dlr.ivf.tapas.matrixtool.erzeugung.events.ModelEvent;

public abstract class AbstractMatrix<T> extends Observable implements IMatrix<T> {
	
	private LinkedList<ModelEvent> messages = new LinkedList<ModelEvent>();

	protected void addToMessageSet(ModelEvent.Message m){
		
		if (!messages.contains(new ModelEvent(this,m))){
			messages.add(new ModelEvent(this,m));
		}
	}
	
	public void doNotify(){
		
		for (ModelEvent m : messages){
			setChanged();
			notifyObservers(m);
		}
		
		messages = new LinkedList<ModelEvent>();
	}
}
