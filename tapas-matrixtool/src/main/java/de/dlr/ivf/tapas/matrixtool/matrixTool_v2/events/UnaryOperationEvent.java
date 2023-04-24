package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events;

public class UnaryOperationEvent extends AbstractOperationEvent {

	private String op1;
	

	public UnaryOperationEvent(Object source, Type type, String op1, String op,String message) {
		super(source);
		this.message = message;
		this.type = type;
		this.op1 = op1;
		this.op = op;
	}
	
	public String getOperand(){
		return op1;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnaryOperationEvent other = (UnaryOperationEvent) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (op == null) {
			if (other.op != null)
				return false;
		} else if (!op.equals(other.op))
			return false;
		if (op1 == null) {
			if (other.op1 != null)
				return false;
		} else if (!op1.equals(other.op1))
			return false;
		return true;
	}	
}
