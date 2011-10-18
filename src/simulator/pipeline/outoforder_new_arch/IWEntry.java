package pipeline.outoforder_new_arch;

import memorysystem.LSQEntryContainingEvent;
import config.SimulationConfig;
//import memorysystem.LSQAddressReadyEvent;
import generic.Core;
import generic.Event;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.ExecutionCompleteEvent;
import generic.RequestType;

/**
 * represents an entry in the instruction window
 */
public class IWEntry {
	
	Core core;
	Instruction instruction;
	ReorderBufferEntry associatedROBEntry;
	
	ExecutionEngine execEngine;
	InstructionWindow instructionWindow;
	OperationType opType;
	
	boolean isValid;
	
	int pos;

	public IWEntry(Core core, int pos,
			ExecutionEngine execEngine, InstructionWindow instructionWindow)
	{
		this.core = core;
		this.pos = pos;
		isValid = false;
		
		this.execEngine = execEngine;
		this.instructionWindow = instructionWindow;
	}
	
	/*
	public IWEntry(Core core, Instruction instruction, ReorderBufferEntry ROBEntry)
	{
		this.core = core;
		this.instruction = instruction;
		associatedROBEntry = ROBEntry;
	}
	*/
	
	//	issueInstruction()
	//----------------------
	//check if
	//1) first operand available
	//2) second operand available
	//if so,
	//		if FU available
	//			issue the instruction
	//				1) set issued = true
	//				2) schedule ExecutionCompleteEvent
	//		else
	//			schedule an FUAvailableEvent
	public boolean issueInstruction()
	{
		if(associatedROBEntry.getIssued() == true)
		{
			System.out.println("already issued!");
			return false;
		}
		
		if(associatedROBEntry.isOperand1Available && associatedROBEntry.isOperand2Available)
		{
			
			if(opType == OperationType.mov ||
					opType == OperationType.xchg)
			{
				//no FU required
				issueMovXchg();
				return true;
			}
			
			else if(opType == OperationType.load ||
					opType == OperationType.store)
			{
				issueLoadStore();
				return true;
			}
			
			else
			{
				return issueOthers();
			}
		}
		
		return false;
	}
	
	void issueMovXchg()
	{
		//no FU required
		
		associatedROBEntry.setIssued(true);
		
		//remove IW entry
		instructionWindow.removeFromWindow(this);
		
		core.getEventQueue().addEvent(
				new ExecCompleteEvent(
						GlobalClock.getCurrentTime() + core.getStepSize(),
						null, 
						execEngine.getExecuter(),
						
						RequestType.EXEC_COMPLETE,
						associatedROBEntry));
		

		if(SimulationConfig.debugMode)
		{
			System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
		}
		
		
	}
	
	void issueLoadStore()
	{
		if(instruction.getSourceOperand2() != null)
		{
			
			associatedROBEntry.setIssued(true);
			if(opType == OperationType.store)
			{
				associatedROBEntry.setExecuted(true);
				associatedROBEntry.setWriteBackDone1(true);
				associatedROBEntry.setWriteBackDone2(true);
			}
			associatedROBEntry.setFUInstance(0);
			
			//remove IW entry
			instructionWindow.removeFromWindow(this);
			//TODO add event to indicate address ready
			core.getExecEngine().coreMemSys.getLsqueue().getPort().put(
					new LSQEntryContainingEvent(
							core.getExecEngine().coreMemSys.getLsqueue().getLatencyDelay(), 
														null, //Requesting Element
														core.getExecEngine().coreMemSys.getLsqueue(), 
														RequestType.Tell_LSQ_Addr_Ready,
														associatedROBEntry.getLsqEntry()));

		}
		else
		{
			associatedROBEntry.setIssued(true);
			if(opType == OperationType.store)
			{
				associatedROBEntry.setExecuted(true);
				associatedROBEntry.setWriteBackDone1(true);
				associatedROBEntry.setWriteBackDone2(true);
			}
			
			instructionWindow.removeFromWindow(this);
			//TODO add event to indicate address ready
			//core.getExecEngine().coreMemSys.getLsqueue().getPort().put(new LSQAddressReadyEvent(execEngine.coreMemSys.getLsqueue().getLatencyDelay(), 
			//											null, //Requesting Element
			//											core.getExecEngine().coreMemSys.getLsqueue(), 
			//											0, //tieBreaker,
			//											RequestType.TLB_ADDRESS_READY,
			//											associatedROBEntry.getLsqEntry()));
		}

		if(SimulationConfig.debugMode)
		{
			System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
		}
	}
	
	boolean issueOthers()
	{
		long FURequest = 0;	//will be <= 0 if an FU was obtained
		//will be > 0 otherwise, indicating how long before
		//	an FU of the type will be available (not used in new arch)

		FURequest = execEngine.getFunctionalUnitSet().requestFU(
			OpTypeToFUTypeMapping.getFUType(opType),
			GlobalClock.getCurrentTime(),
			core.getStepSize() );
		
		if(FURequest <= 0)
		{
			associatedROBEntry.setIssued(true);
			associatedROBEntry.setFUInstance((int) ((-1) * FURequest));
			
			//remove IW entry
			instructionWindow.removeFromWindow(this);
			
			core.getEventQueue().addEvent(
					new BroadCast1Event(
							GlobalClock.getCurrentTime() + core.getLatency(
									OpTypeToFUTypeMapping.getFUType(opType).ordinal()) * core.getStepSize() - 1,
							null, 
							execEngine.getExecuter(),
							RequestType.BROADCAST_1,
							associatedROBEntry));
			
			core.getEventQueue().addEvent(
					new ExecCompleteEvent(
							GlobalClock.getCurrentTime() + core.getLatency(
									OpTypeToFUTypeMapping.getFUType(opType).ordinal()) * core.getStepSize(),
							null, 
							execEngine.getExecuter(),
							RequestType.EXEC_COMPLETE,
							associatedROBEntry));

			if(SimulationConfig.debugMode)
			{
				System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
			}
			
			return true;
		}
		
		return false;
	}

	
	public ReorderBufferEntry getAssociatedROBEntry() {
		return associatedROBEntry;
	}
	public void setAssociatedROBEntry(ReorderBufferEntry associatedROBEntry) {
		this.associatedROBEntry = associatedROBEntry;
	}
	
	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public Instruction getInstruction() {
		return instruction;
	}

	public void setInstruction(Instruction instruction) {
		this.instruction = instruction;
		opType = instruction.getOperationType();
	}

}