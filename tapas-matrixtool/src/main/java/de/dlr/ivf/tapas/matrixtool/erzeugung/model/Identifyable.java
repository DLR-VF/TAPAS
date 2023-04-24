package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

public interface Identifyable extends Comparable<Identifyable>{

	public Identificator getID();
	
	public void setID(String... id);
}
