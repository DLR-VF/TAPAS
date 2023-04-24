package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events;

public class BinaryOperationEvent extends AbstractOperationEvent {
	
	private String op1;
	private String op2;
	

	public BinaryOperationEvent(Object source, Type type, String op1, String op, String op2,
			String message) {
		super(source);
		this.message = message;
		this.type = type;
		this.op1 = op1;
		this.op = op;
		this.op2 = op2;
	}
	
	public String getSinkOperand(){
		return op1;
	}
	
	public String getSourceOperand(){
		return op2;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BinaryOperationEvent other = (BinaryOperationEvent) obj;
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
		if (op2 == null) {
			if (other.op2 != null)
				return false;
		} else if (!op2.equals(other.op2))
			return false;
		return true;
	}	
}
