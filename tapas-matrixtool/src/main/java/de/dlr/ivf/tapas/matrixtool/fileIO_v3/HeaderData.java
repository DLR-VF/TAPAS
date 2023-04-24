package de.dlr.ivf.tapas.matrixtool.fileIO_v3;

import de.dlr.ivf.tapas.matrixtool.common.datastructures.Indexing;

public class HeaderData {

	private Indexing<String> objects;
	private Indexing<String> attributes;
	private boolean isQuadratic;		// !!!!! evtl volatile machen, da verschiedene threads zugreifen
	private int type;
	private String tSysId;
	private String time;
	private String factor;
	
	public HeaderData(){
		objects = new Indexing<String>();
		attributes = new Indexing<String>();
		isQuadratic = false;
	}
	
	public void setQuadratic(boolean isQuadratic){
		this.isQuadratic = isQuadratic;
	}
	
	public void addObject(String obj){
		objects.put(obj);
	}
	
	public void addAttribute(String att){
		attributes.put(att);
	}
	
	public Indexing<String> getObjects() {
		return objects;
	}
	public Indexing<String> getAttributes() {
		return attributes;
	}
	public boolean isQuadratic() {
		return isQuadratic;
	}

	
	//Visum-Stuff
	
	public void setVisumHeaderValueType(int i) {
		this.type = i;
	}

	public void setTrafficSystem(String tSysId) {
		this.tSysId = tSysId;
	}

	public void setTimeIntervall(String time) {
		this.time = time;
	}

	public void setFactor(String factor) {
		this.factor = factor;
	}
}
