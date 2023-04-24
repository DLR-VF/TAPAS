package de.dlr.ivf.tapas.matrixtool.erzeugung.events;

import java.util.EventObject;

public class ModelEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;
	
	public static enum Message{
		ZONES_CHNGD, 
		SSG_CHNGD,
		ZONE_GRPS_CHNGD,
		USR_GRPS_CHNGD,
		VAL_CHNGD
	}
	
	private Message message;
	

	public ModelEvent(Object source, Message m) {
		super(source);
		message = m;
	}

	public Message getMessage(){
		return message;
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
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}
	

}
