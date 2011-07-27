package emulatorinterface.translator.x86.objparser;

import generic.Instruction;
import generic.InstructionList;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;

public class TestInstructionLists {
	
	static InstructionList instructionList;
	
	public static InstructionList testList1()
	{
		instructionList = new InstructionList();
		
		Instruction newInst;
		for(int i = 0; i < 100; i++)
		{
			newInst = new Instruction(OperationType.integerALU,
					new Operand(OperandType.integerRegister, i%16),
					new Operand(OperandType.integerRegister, i%16),
					new Operand(OperandType.integerRegister, (i+1)%16) );
			
			instructionList.appendInstruction(newInst);
		}
		
		return instructionList;
		
	}
	
	public static InstructionList testList2()
	{
		instructionList = new InstructionList();
		
		Instruction newInst;
		for(OperationType type : OperationType.values())
		{
			for(int i = 0; i < 4; i++)
			{
				newInst = new Instruction(type,
						new Operand(OperandType.integerRegister, 3*i),
						new Operand(OperandType.integerRegister, 3*i+1),
						new Operand(OperandType.integerRegister, 3*i+2) );
				
				instructionList.appendInstruction(newInst);
			}
			
			if(type == OperationType.floatDiv)
			{
				break;
			}
		}
		
		return instructionList;
		
	}

}
