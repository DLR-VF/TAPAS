package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events;

import java.util.EventObject;

public class UserInputEvent extends EventObject {

	public static enum Type{
		OK,
		WRONG,	//'a1' in einem zahlenfeld
		PROBLEM	//syntaktisch korrekt, aber evtl problem durch z.B. inkompatibilitaeten
	}
	
	private Type type;
	private String input;
	private String problem;
	

	public UserInputEvent(Object source, Type m, String input, String problem) {
		super(source);
		type = m;
		this.input = input;
		this.problem = problem;
	}

	public Type getMessage(){
		return type;
	}
	
	public String getInput(){
		return input;
	}
	
	public String getProblem(){
		return problem;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserInputEvent other = (UserInputEvent) obj;
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		if (problem == null) {
			if (other.problem != null)
				return false;
		} else if (!problem.equals(other.problem))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}	
}
