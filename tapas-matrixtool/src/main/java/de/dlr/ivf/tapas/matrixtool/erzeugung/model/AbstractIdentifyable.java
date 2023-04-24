package de.dlr.ivf.tapas.matrixtool.erzeugung.model;

public abstract class AbstractIdentifyable implements Identifyable{
	
	protected Identificator id;

	public Identificator getID() {
		
		return id;
	}
	
	public void setID(String... id) {
		this.id = new Identificator(id);
	}
	
	
	public String toString(){

		return id.toString();
	}

	public int compareTo(Identifyable o) {
		
		Identificator otherId = o.getID();
		
		for (int i = 0; i < Math.min(id.getNumberParts(), otherId.getNumberParts()); i++){			
			if (id.getPart(i).compareTo(otherId.getPart(i)) != 0)
				return id.getPart(i).compareTo(otherId.getPart(i));
		}
		
		// wenn hierher gekommen, dann ist der gleichlange teil gleich
		// nur noch der evtl laengere teil von einem der beiden pruefen
		
		if (id.getNumberParts() == otherId.getNumberParts()){
			return 0;
		} else if (id.getNumberParts() < otherId.getNumberParts()){
			return -1;
		} else {
			return 1;
		}
	}
}
