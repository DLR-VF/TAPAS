package de.dlr.ivf.tapas.matrixtool.common.events;

import java.util.EventObject;


public class IOEvent extends EventObject {
	
	public enum Type {
		READING,
		WRITING,
		FINISHED_READING,
		FINISHED_WRITING, 
		ERROR_READING,
		ERROR_WRITING
	}
	
	private Type type;
	private String message;
	private String file;
	
	public IOEvent(Object source, Type t, String message) {
		super(source);
		type = t;
		this.message = message;
	}
	
	public IOEvent(Object source, Type t, String file, String message) {
		super(source);
		type = t;
		this.message = message;
		this.file = file;
	}

	public Type getType(){
		return type;
	}
	
	public String getMessage(){
		return message;
	}
	
	public String getFile(){
		return file;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IOEvent other = (IOEvent) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
}
