package pipeline.outoforder_new_arch;

import generic.Core;
import generic.Event;
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class SelectLogic extends SimulationElement {
	
	Core core;
	ExecutionEngine execEngine;
	InstructionWindow IW;
	
	//if the instruction issued is a single cycle operation,
	//instructions dependent on it must be woken up
	//so that they can be considered for issue the next cycle
	OperandType[] destRegOpndType;
	int[] destRegPhyReg;
	int[] IWEntryROBIndex;
	ReorderBufferEntry[] associatedROBEntries;
	
	public SelectLogic(Core core, ExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		IW = execEngine.getInstructionWindow();
		
		destRegOpndType = new OperandType[IW.maxIWSize];
		destRegPhyReg = new int[IW.maxIWSize];
		IWEntryROBIndex = new int[IW.maxIWSize];
		associatedROBEntries = new ReorderBufferEntry[IW.maxIWSize];
		for(int i = 0; i < IW.maxIWSize; i++)
		{
			destRegOpndType[i] = OperandType.inValid;
			destRegPhyReg[i] = -1;
			IWEntryROBIndex[i] = -1;
			associatedROBEntries[i] = null;
		}
	}
	
	public void performSelect()
	{
		int wakeUpListCtr = 0;
		ReorderBuffer ROB = execEngine.getReorderBuffer();
		IWEntry[] IWEntries = IW.getIW();
		
		for(int i = 0; i < IW.maxIWSize; i++)
		{
			if(IWEntries[i].isValid == true)
			{
				//attempt to issue
				if(IWEntries[i].issueInstruction())
				{
					//if single cycle operation
					//find dependent instructions
					if(core.getLatency(
							OpTypeToFUTypeMapping.getFUType(
									IWEntries[i].getInstruction().getOperationType())
									.ordinal())
									 == 1 ||
							IWEntries[i].getInstruction().getOperationType() == OperationType.mov
							)
					{
						if(IWEntries[i].instruction.getDestinationOperand() != null)
						{
							destRegOpndType[wakeUpListCtr] = IWEntries[i].instruction.getDestinationOperand().getOperandType();
							destRegPhyReg[wakeUpListCtr] = IWEntries[i].associatedROBEntry.getPhysicalDestinationRegister();
							associatedROBEntries[wakeUpListCtr] = IWEntries[i].associatedROBEntry;
							IWEntryROBIndex[wakeUpListCtr++] = ROB.indexOf(IWEntries[i].associatedROBEntry);
						}
					}
					
					if(IWEntries[i].getInstruction().getOperationType() == OperationType.xchg)
					{
						destRegOpndType[wakeUpListCtr] = IWEntries[i].instruction.getSourceOperand1().getOperandType();
						destRegPhyReg[wakeUpListCtr] = IWEntries[i].associatedROBEntry.getOperand1PhyReg1();
						associatedROBEntries[wakeUpListCtr] = IWEntries[i].associatedROBEntry;
						IWEntryROBIndex[wakeUpListCtr++] = ROB.indexOf(IWEntries[i].associatedROBEntry);
						
						destRegOpndType[wakeUpListCtr] = IWEntries[i].instruction.getSourceOperand2().getOperandType();
						destRegPhyReg[wakeUpListCtr] = IWEntries[i].associatedROBEntry.getOperand2PhyReg1();
						associatedROBEntries[wakeUpListCtr] = IWEntries[i].associatedROBEntry;
						IWEntryROBIndex[wakeUpListCtr++] = ROB.indexOf(IWEntries[i].associatedROBEntry);
					}
				}
			}
		}
		
		if(wakeUpListCtr != IW.maxIWSize)
		{
			destRegOpndType[wakeUpListCtr] = OperandType.inValid;
		}
		performWakeUp();
	}
	
	private void performWakeUp()
	{
		for(int i = 0; i < IW.maxIWSize; i++)
		{
			if(destRegOpndType[i] == OperandType.inValid)
			{
				break;
			}
			
			//TODO xchg case not handled
			WakeUpLogic.wakeUpLogic(core, destRegOpndType[i], destRegPhyReg[i], associatedROBEntries[i].threadID, IWEntryROBIndex[i]);
			
			destRegOpndType[i] = OperandType.inValid;
			destRegPhyReg[i] = -1;
			IWEntryROBIndex[i] = -1;
			associatedROBEntries[i] = null;
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}

}
