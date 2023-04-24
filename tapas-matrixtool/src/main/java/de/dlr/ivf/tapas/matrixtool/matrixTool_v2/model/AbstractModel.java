package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model;

import java.util.LinkedList;
import java.util.Observable;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.ModelEvent;

public abstract class AbstractModel extends Observable{

	private LinkedList<ModelEvent> messages = new LinkedList<ModelEvent>();
	protected Double DEF_VAL = 0.0;
	
	public Double getDefaultValueForDoubles(){
		return DEF_VAL;
	}
	
	public void doNotify(){
		
		for (ModelEvent m : messages){
			setChanged();
			notifyObservers(m);
		}
		
		messages = new LinkedList<ModelEvent>();
	}
	
	protected void addToMessageSet(ModelEvent.Type m){
		
		if (!messages.contains(new ModelEvent(this,m))){
			messages.add(new ModelEvent(this,m));
		}
		
//		System.out.println("message-set : --------");
//		for (ModelEvent e : messages){
//			System.out.println(e.getMessage());
//		}
//		System.out.println("----------------------");
	}
}
