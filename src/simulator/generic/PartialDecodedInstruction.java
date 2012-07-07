package generic;

public class PartialDecodedInstruction {
	
	private Object instructionClass;
	private Object instructionList; 
	
	private Operand operand1;
	private Operand operand2;
	private Operand operand3;
	
	public PartialDecodedInstruction(Object instructionClass,
			Object instructionList, Operand operand1, Operand operand2,
			Operand operand3) {
		super();
		this.instructionClass = instructionClass;
		this.instructionList = instructionList;
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.operand3 = operand3;
	}

	public PartialDecodedInstruction() {
		// TODO Auto-generated constructor stub
	}

	public Object getInstructionClass() {
		return instructionClass;
	}

	public Object getInstructionList() {
		return instructionList;
	}

	public Operand getOperand1() {
		return operand1;
	}

	public Operand getOperand2() {
		return operand2;
	}

	public Operand getOperand3() {
		return operand3;
	}
}