package emulatorinterface.translator.x86.objparser;

import generic.Instruction;
import generic.InstructionLinkedList;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;

public class TestInstructionLists {
	
	static InstructionLinkedList instructionLinkedList;
	
	public static InstructionLinkedList testList1()
	{
		instructionLinkedList = new InstructionLinkedList();
		
		Instruction newInst;
		for(int i = 0; i < 100; i++)
		{
			newInst = new Instruction(OperationType.integerALU,
					new Operand(OperandType.integerRegister, i%16),
					new Operand(OperandType.integerRegister, i%16),
					new Operand(OperandType.integerRegister, (i+1)%16) );
			
			instructionLinkedList.appendInstruction(newInst);
		}
		
		instructionLinkedList.appendInstruction(new Instruction(OperationType.inValid, null, null, null));
		return instructionLinkedList;
		
	}
	
	public static InstructionLinkedList testList2()
	{
		instructionLinkedList = new InstructionLinkedList();
		
		Instruction newInst;
		for(OperationType type : OperationType.values())
		{
			if(type != OperationType.inValid)
			{
				for(int i = 0; i < 4; i++)
				{
					newInst = new Instruction(type,
							new Operand(OperandType.integerRegister, 3*i),
							new Operand(OperandType.integerRegister, 3*i+1),
							new Operand(OperandType.integerRegister, 3*i+2) );
					
					instructionLinkedList.appendInstruction(newInst);
				}
			}
			
			if(type == OperationType.floatDiv)
			{
				break;
			}
		}
		
		instructionLinkedList.appendInstruction(new Instruction(OperationType.inValid, null, null, null));
		return instructionLinkedList;
		
	}

}
