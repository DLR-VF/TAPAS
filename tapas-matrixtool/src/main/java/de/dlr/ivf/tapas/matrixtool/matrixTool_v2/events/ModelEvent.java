package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events;

import java.util.EventObject;

public class ModelEvent extends EventObject {
	
	public static enum Type{
		RWS_CHNGD, 	//zeile hinzu oder weg
		CLS_CHNGD,	//spalte hinzu oder weg
		VAL_CHNGD	//wert veraendert
	}
	
	private Type type;
	

	public ModelEvent(Object source, Type m) {
		super(source);
		type = m;
	}

	public Type getMessage(){
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelEvent other = (ModelEvent) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	

}
