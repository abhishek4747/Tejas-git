package pipeline.outoforder;

import config.SimulationConfig;
import generic.Core;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.RequestType;

/**
 * represents an entry in the instruction window
 */
public class IWEntry {
	
	Core core;
	Instruction instruction;
	ReorderBufferEntry associatedROBEntry;
	
	OutOrderExecutionEngine execEngine;
	InstructionWindow instructionWindow;
	OperationType opType;
	
	boolean isValid;
	
	int pos;

	public IWEntry(Core core, int pos,
			OutOrderExecutionEngine execEngine, InstructionWindow instructionWindow)
	{
		this.core = core;
		this.pos = pos;
		isValid = false;
		
		this.execEngine = execEngine;
		this.instructionWindow = instructionWindow;
	}
	
	
	public boolean issueInstruction()
	{
		if(associatedROBEntry.isRenameDone == false ||
				associatedROBEntry.isExecuted == true)
		{
			System.out.println("cannot issue this instruction");
		}
		
		if(associatedROBEntry.getIssued() == true)
		{
			System.out.println("already issued!");
			return false;
		}
		
		if(associatedROBEntry.isOperand1Available && associatedROBEntry.isOperand2Available)
		{

			//Increment the counters for power calculations
			this.core.powerCounters.incrementWindowAccess(1);
			//Two access as two values are read from window and sent to FU
			this.core.powerCounters.incrementWindowPregAccess(2);			

			
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
						null,
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
		associatedROBEntry.setIssued(true);
		if(opType == OperationType.store)
		{
			associatedROBEntry.setExecuted(true);
			associatedROBEntry.setWriteBackDone1(true);
			associatedROBEntry.setWriteBackDone2(true);
			
			this.core.powerCounters.incrementLsqAccess(1);
			this.core.powerCounters.incrementLsqStoreDataAccess(1);
			this.core.powerCounters.incrementLsqPregAccess(1);
			
		}
		
		if(opType == OperationType.load){
			this.core.powerCounters.incrementLsqAccess(1);
			this.core.powerCounters.incrementLsqWakeupAccess(1);

		}
		
		associatedROBEntry.setFUInstance(0);
		
		//remove IW entry
		instructionWindow.removeFromWindow(this);
		
		if(associatedROBEntry.lsqEntry.isValid() == true)
		{
			System.out.println("attempting to issue a load/store.. address is already valid");
		}
		
		if(associatedROBEntry.lsqEntry.isForwarded() == true)
		{
			System.out.println("attempting to issue a load/store.. value forwarded is already valid");
		}
		
		//tell LSQ that address is available
		execEngine.getCoreMemorySystem().issueRequestToLSQ(
				null, 
				associatedROBEntry);
			

		if(SimulationConfig.debugMode)
		{
			System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
		}
	}
	
	boolean issueOthers()
	{
		
		if(associatedROBEntry.instruction.getOperationType() == OperationType.interrupt)
		{
			associatedROBEntry.setIssued(true);
			associatedROBEntry.setFUInstance(0);
			
			//remove IW entry
			instructionWindow.removeFromWindow(this);
			
			return true;
		}
		
		long FURequest = 0;	//will be <= 0 if an FU was obtained
		//will be > 0 otherwise, indicating how long before
		//	an FU of the type will be available (not used in new arch)

		FURequest = execEngine.getFunctionalUnitSet().requestFU(
			OpTypeToFUTypeMapping.getFUType(opType),
			GlobalClock.getCurrentTime(),
			core.getStepSize() );
		
		if(FURequest <= 0)
		{
			//Increment the counters for power calculation
			if(opType == OperationType.integerALU){
				this.core.powerCounters.incrementAluAccess(1);
				this.core.powerCounters.incrementIaluAccess(1);
			}
			else if(opType == OperationType.integerALU){
				this.core.powerCounters.incrementAluAccess(1);
				this.core.powerCounters.incrementFaluAccess(1);
			}
			associatedROBEntry.setIssued(true);
			associatedROBEntry.setFUInstance((int) ((-1) * FURequest));
			
			//remove IW entry
			instructionWindow.removeFromWindow(this);
			
			core.getEventQueue().addEvent(
					new BroadCast1Event(
							GlobalClock.getCurrentTime() + (core.getLatency(
									OpTypeToFUTypeMapping.getFUType(opType).ordinal()) - 1) * core.getStepSize(),
							null, 
							execEngine.getExecuter(),
							RequestType.BROADCAST_1,
							associatedROBEntry));
			
			core.getEventQueue().addEvent(
					new ExecCompleteEvent(
							null,
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