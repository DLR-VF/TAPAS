package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events;

import java.util.EventObject;

public abstract class AbstractOperationEvent extends EventObject{

	public static enum Type{
		OP_START,
		OP_FNSHD,
		ERROR
	}
	
	protected Type type;
	protected String op;
	protected String message;
	
	
	public AbstractOperationEvent(Object arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
	
	public String getMessage(){
		return message;
	}

	public Type getType(){
		return type;
	}
	
	public String getOperation(){
		return op;
	}
}
