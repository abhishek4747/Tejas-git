package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class SelectLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	InstructionWindow IW;	
	int issueWidth;
	
	//if the instruction issued is a single cycle operation,
	//instructions dependent on it must be woken up
	//so that they can be considered for issue the next cycle
	OperandType[] destRegOpndType;
	int[] destRegPhyReg;
	int[] IWEntryROBIndex;
	ReorderBufferEntry[] associatedROBEntries;
	
	public SelectLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, core.getEventQueue(), -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		IW = execEngine.getInstructionWindow();
		issueWidth = core.getIssueWidth();
		
		destRegOpndType = new OperandType[2*IW.maxIWSize];//2*IW.maxIWSize because all instructions in window could be xchgs - each requiring two wake-ups
		destRegPhyReg = new int[2*IW.maxIWSize];
		IWEntryROBIndex = new int[2*IW.maxIWSize];
		associatedROBEntries = new ReorderBufferEntry[2*IW.maxIWSize];
		for(int i = 0; i < IW.maxIWSize; i++)
		{
			destRegOpndType[i] = OperandType.inValid;
			destRegPhyReg[i] = -1;
			IWEntryROBIndex[i] = -1;
			associatedROBEntries[i] = null;
		}
	}
	
	/*
	 * all ready instructions' issue are attempted
	 * the dependent instructions of all those instructions issued,
	 * 		whose latency is a single cycle, are queued up for early awakening
	 * 		so that in the next cycle they may be considered for execution
	 * important - all issues must be attempted first; only then must awakening be done
	 * 		this is because an awakened instruction is a
	 * 		candidate for issue only in the next cycle 
	 */
	public void performSelect()
	{
		int wakeUpListCtr = 0;
		int noAwoken = 0;
		ReorderBuffer ROB = execEngine.getReorderBuffer();
		IWEntry[] IWEntries = IW.getIW();
		
		for(int i = 0; i < IW.maxIWSize; i++)
		{
			if(IWEntries[i].isValid == true)
			{
				//attempt to issue
				if(IWEntries[i].issueInstruction())
				{
					noAwoken++;
					
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
			
			if(noAwoken >= issueWidth)
			{
				break;
			}
		}
		
		if(wakeUpListCtr != IW.maxIWSize)
		{
			destRegOpndType[wakeUpListCtr] = OperandType.inValid;
		}
		performWakeUp();
	}
	
	public void performSelect2()
	{
		if(execEngine.isToStall5() == true)
		{
			//pipeline stalled due to branch mis-prediction
			return;
		}
		
		int wakeUpListCtr = 0;
		int noAwoken = 0;
		ReorderBuffer ROB = execEngine.getReorderBuffer();
		
		int i;
		ReorderBufferEntry ROBEntry;
		
		if(ROB.head != -1)
		{
			i = ROB.head;
			do
			{
				ROBEntry = ROB.ROB[i];
				
				if(ROBEntry.isIssued == false &&
						ROBEntry.associatedIWEntry != null)
				{
					//Increment counter for power calculation
					//Window selection access is incremented as instruction is being issued here
					this.core.powerCounters.incrementWindowSelectionAccess(1);
					
					//Other window accesses are incremented in the issueInstruction() of IWEntry
					
					if(ROBEntry.associatedIWEntry.issueInstruction())
					{

						
						noAwoken++;
						//if single cycle operation
						//find dependent instructions
						if(core.getLatency(
								OpTypeToFUTypeMapping.getFUType(
										ROBEntry.instruction.getOperationType())
										.ordinal())
										 == 1 ||
										 ROBEntry.instruction.getOperationType() == OperationType.mov
								)
						{
							if(ROBEntry.associatedIWEntry.instruction.getDestinationOperand() != null)
							{
								destRegOpndType[wakeUpListCtr] = ROBEntry.instruction.getDestinationOperand().getOperandType();
								destRegPhyReg[wakeUpListCtr] = ROBEntry.getPhysicalDestinationRegister();
								associatedROBEntries[wakeUpListCtr] = ROBEntry;
								IWEntryROBIndex[wakeUpListCtr++] = ROB.indexOf(ROBEntry);
							}
						}
						
						if(ROBEntry.instruction.getOperationType() == OperationType.xchg)
						{
							destRegOpndType[wakeUpListCtr] = ROBEntry.instruction.getSourceOperand1().getOperandType();
							destRegPhyReg[wakeUpListCtr] = ROBEntry.getOperand1PhyReg1();
							associatedROBEntries[wakeUpListCtr] = ROBEntry;
							IWEntryROBIndex[wakeUpListCtr++] = ROB.indexOf(ROBEntry);
							
							destRegOpndType[wakeUpListCtr] = ROBEntry.instruction.getSourceOperand2().getOperandType();
							destRegPhyReg[wakeUpListCtr] = ROBEntry.getOperand2PhyReg1();
							associatedROBEntries[wakeUpListCtr] = ROBEntry;
							IWEntryROBIndex[wakeUpListCtr++] = ROB.indexOf(ROBEntry);
						}
					}
				}
				
				if(noAwoken >= issueWidth)
				{
					break;
				}
				
				i = (i+1)%ROB.MaxROBSize;
			}while(i != ROB.tail);
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
	public void handleEvent(EventQueue eventQ, Event event) {
		// TODO Auto-generated method stub
		
	}

}
