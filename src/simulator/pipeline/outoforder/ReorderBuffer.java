package pipeline.outoforder;

import config.SimulationConfig;
import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperandType;
import generic.OperationType;
import generic.PortRequestEvent;
import generic.RequestType;
import generic.SimulationElement;
import generic.Time_t;
import memorysystem.LSQCommitEventFromROB;

public class ReorderBuffer extends SimulationElement{
	
	private Core core;
	
	private ReorderBufferEntry[] ROB;
	private int MaxROBSize;
	
	int head;
	int tail;

	public ReorderBuffer(Core _core)
	{
		super(1, new Time_t(-1), new Time_t(-1), -1);
		core = _core;
		MaxROBSize = core.getReorderBufferSize();
		ROB = new ReorderBufferEntry[MaxROBSize];
		for(int i = 0; i < MaxROBSize; i++)
		{
			ROB[i] = new ReorderBufferEntry(core, i);
		}
		head = -1;
		tail = -1;
	}
	
	public boolean isFull()
	{
		if((tail - head) == MaxROBSize - 1)
		{
			return true;
		}
		if((tail - head) == -1)
		{
			return true;
		}
		return false;
	}
	
	public ReorderBufferEntry[] getROB()
	{
		return ROB;
	}
	
	public int getMaxROBSize()
	{
		return MaxROBSize;
	}
	
	public void setMaxROBSize(int newMaxROBSize)
	{
		MaxROBSize = newMaxROBSize;
	}
	
	//creates a  new ROB entry, initialises it, and returns it
	//check if there is space in ROB before calling this function
	public ReorderBufferEntry addInstructionToROB(Instruction newInstruction, int threadID)
	{
		if(!isFull())
		{
			tail = (tail + 1)%MaxROBSize;
			if(head == -1)
			{
				head = 0;
			}
			ReorderBufferEntry newReorderBufferEntry = ROB[tail];
			
			newReorderBufferEntry.setInstruction(newInstruction);
			newReorderBufferEntry.setThreadID(threadID);
			newReorderBufferEntry.setOperand1PhyReg1(-1);
			newReorderBufferEntry.setOperand1PhyReg2(-1);
			newReorderBufferEntry.setOperand2PhyReg1(-1);
			newReorderBufferEntry.setOperand2PhyReg2(-1);
			newReorderBufferEntry.setPhysicalDestinationRegister(-1);
			newReorderBufferEntry.setIssued(false);
			newReorderBufferEntry.setFUInstance(-1);
			newReorderBufferEntry.setExecuted(false);
			newReorderBufferEntry.setWriteBackDone1(false);
			newReorderBufferEntry.setWriteBackDone2(false);
			newReorderBufferEntry.setReadyAtTime(GlobalClock.getCurrentTime());
			newReorderBufferEntry.setAssociatedIWEntry(null);
			
			newReorderBufferEntry.setValid(true);
			
			return newReorderBufferEntry;
		}
		
		return null;
	}
	
	//  instruction at head of ROB can be purged
	//	if it has completed
	//	if it didn't raise an exception (in simulation, exceptions aren't possible)
	//	and it isn't a mis-predicted branch
	public void performCommits()
	{	
		int tieBreaker = 0;
		while(true)
		{
			if(head == -1)
			{
				if(core.getExecEngine().isAllPipesEmpty() == true)
				{
					//if ROB is empty, and decode pipe is empty, that means execution is complete
					core.getExecEngine().setExecutionComplete(true);
				}
				break;
			}
			
			ReorderBufferEntry first = ROB[head];
			
			if(first.isWriteBackDone() == true)
			{
				//if branch, then if branch prediction correct
				if(first.getInstruction().getOperationType() != OperationType.branch ||
						first.getInstruction().getOperationType() == OperationType.branch &&
						core.getBranchPredictor().predict(first.getInstruction().getProgramCounter())
							== first.getInstruction().isBranchTaken())		
				{
					//add to available list
					//update checkpoint
					//note : if values are involved, a checkpoint of
					//       the machine specific register file must also be implemented TODO
					
					//increment number of instructions executed
					core.incrementNoOfInstructionsExecuted();
					
					if(first.getInstruction().getDestinationOperand() != null)
					{
						if(first.getInstruction().getDestinationOperand().getOperandType()
								== OperandType.integerRegister)
						{
							updateIntegerRenameTable(first);
						}
						else if(first.getInstruction().getDestinationOperand().getOperandType() == OperandType.floatRegister)
						{
							updateFloatRenameTable(first);
						}
					}

					if(SimulationConfig.debugMode)
					{
						System.out.println("committed : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : " + first.getInstruction());
					}
					
					//System.out.println("committed : " +GlobalClock.getCurrentTime() + first.getInstruction().getOperationType());
					
					//TODO Signal LSQ for committing the Instruction at the queue head
					if(first.getInstruction().getOperationType() == OperationType.load ||
							first.getInstruction().getOperationType() == OperationType.store)
					{
						core.getEventQueue().addEvent(new PortRequestEvent((GlobalClock.getCurrentTime() * 1000) + tieBreaker, //tieBreaker, 
								1, //noOfSlots,
								new LSQCommitEventFromROB(core.getExecEngine().coreMemSys.getLsqueue().getLatencyDelay(),
																			this,
																			core.getExecEngine().coreMemSys.getLsqueue(), 
																			(GlobalClock.getCurrentTime() * 1000) + tieBreaker, //tieBreaker,
																			RequestType.LSQ_COMMIT, 
																			first.getLsqEntry())));
						first.getLsqEntry().setRemoved(true);
					}
					
					ROB[head].setValid(false);
					if(head == tail)
					{
						head = -1;
						tail = -1;
					}
					else
					{
						head = (head+1)%MaxROBSize;
					}
					
				}
				else
				{
					handleBranchMisprediction();
				}
				
				if(first.getInstruction().getOperationType() == OperationType.branch)
				{
					core.getBranchPredictor().Train(
													first.getInstruction().getProgramCounter(),
													first.getInstruction().isBranchTaken(),
													core.getBranchPredictor().predict(first.getInstruction().getProgramCounter())
													);
				}
			}
			else
			{
				break;
			}
			tieBreaker++;
		}
	}
	
	void updateIntegerRenameTable(ReorderBufferEntry first)
	{
		//adding current mapping to available list
		int curPhyReg = core.getExecEngine().getIntegerRenameTable()
				.getCheckpoint().getMapping(
						first.getThreadID(),
						(int) first.getInstruction().getDestinationOperand().getValue());
		if(curPhyReg != first.getPhysicalDestinationRegister())
		{
			core.getExecEngine().getIntegerRenameTable().addToAvailableList(curPhyReg);
		}
		
		//updating checkpoint
		core.getExecEngine().getIntegerRenameTable().getCheckpoint().setMapping(
				first.getThreadID(),
				first.getPhysicalDestinationRegister(),
				(int) first.getInstruction().getDestinationOperand().getValue());
	}
	
	void updateFloatRenameTable(ReorderBufferEntry first)
	{
		//adding current mapping to available list
		int curPhyReg = core.getExecEngine().getFloatingPointRenameTable()
				.getCheckpoint().getMapping(
						first.getThreadID(),
						(int) first.getInstruction().getDestinationOperand().getValue());
		if(curPhyReg != first.getPhysicalDestinationRegister())
		{
			core.getExecEngine().getFloatingPointRenameTable().addToAvailableList(curPhyReg);
		}
		
		//updating checkpoint
		core.getExecEngine().getFloatingPointRenameTable().getCheckpoint().setMapping(
				first.getThreadID(),
				first.getPhysicalDestinationRegister(),
				(int) first.getInstruction().getDestinationOperand().getValue());
	}
	
	void handleBranchMisprediction()
	{
		if(SimulationConfig.debugMode)
		{
			System.out.println("branch mispredicted");
		}
		/*
		//remove all entries from ROB				
		ROB.removeAll(null);
		
		//remove all entries from Instruction Window
		core.getExecEngine().getInstructionWindow().flush();
		
		//remove all entries from LSQ
		
		
		//roll back rename tables
		rollBackRenameTables();
		*/
		//impose branch misprediction penalty
		core.getExecEngine().setStallDecode2(true);
		core.getEventQueue().addEvent(
				new MispredictionPenaltyCompleteEvent(
						GlobalClock.getCurrentTime() + core.getBranchMispredictionPenalty()*core.getStepSize(),
						core)
				);
	}
	
	//TODO checkpoint needs to incorporate threadIDs
	public void rollBackRenameTables()
	{
		int phyReg;
		
		//integer rename table
		for(int i = 0; i < core.getIntegerRegisterFileSize(); i++)
		{
			core.getExecEngine().getIntegerRenameTable().setMappingValid(false, i);
			core.getExecEngine().getIntegerRenameTable().setValueValid(false, i);
			core.getExecEngine().getIntegerRenameTable().setProducerROBEntry(null, i);
		}
		for(int j = 0; j < core.getNo_of_threads(); j++)
		{
			for(int i = 0; i < core.getNIntegerArchitecturalRegisters(); i++)
			{
				phyReg = core.getExecEngine().getIntegerRenameTable().getCheckpoint().getMapping(j, i);
				core.getExecEngine().getIntegerRenameTable().setArchReg(j, i, phyReg);
				core.getExecEngine().getIntegerRenameTable().setMappingValid(true, phyReg);
				core.getExecEngine().getIntegerRenameTable().setValueValid(true, phyReg);
			}
		}
		
		//floating point rename table
		for(int i = 0; i < core.getFloatingPointRegisterFileSize(); i++)
		{
			core.getExecEngine().getFloatingPointRenameTable().setMappingValid(false, i);
			core.getExecEngine().getFloatingPointRenameTable().setValueValid(false, i);
			core.getExecEngine().getFloatingPointRenameTable().setProducerROBEntry(null, i);
		}
		for(int j = 0; j < core.getNo_of_threads(); j++)
		{
			for(int i = 0; i < core.getNFloatingPointArchitecturalRegisters(); i++)
			{
				phyReg = core.getExecEngine().getFloatingPointRenameTable().getCheckpoint().getMapping(j, i);
				core.getExecEngine().getFloatingPointRenameTable().setArchReg(j, i, phyReg);
				core.getExecEngine().getFloatingPointRenameTable().setMappingValid(true, phyReg);
				core.getExecEngine().getFloatingPointRenameTable().setValueValid(true, phyReg);
			}
		}
	}
	
	/*
	public void removeInstructionFromROB(ReorderBufferEntry _ROBEntry)
	{
		RenameTable tempRN = null;
		RegisterFile tempRF = null;
		
		if(_ROBEntry.getInstruction().getDestinationOperand().getOperandType() == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile(_ROBEntry.getThreadID());
		}
		else if(_ROBEntry.getInstruction().getDestinationOperand().getOperandType() == OperandType.integerRegister)
		{
			tempRN = core.getExecEngine().getIntegerRenameTable();
		}
		else if(_ROBEntry.getInstruction().getDestinationOperand().getOperandType() == OperandType.floatRegister)
		{
			tempRN = core.getExecEngine().getFloatingPointRenameTable();
		}
		
		int destReg = _ROBEntry.getPhysicalDestinationRegister();
		
		if(tempRF != null)
		{
			tempRF.setValueValid(true, destReg);
			tempRF.setProducerROBEntry(null, destReg);
		}
		else if(tempRN != null)
		{
			tempRN.setValueValid(true, destReg);
			tempRN.setProducerROBEntry(null, destReg);
		}
		
		ROB.remove(_ROBEntry);
	}
	*/
	
	public void dump()
	{
		ReorderBufferEntry e;
		
		System.out.println();
		System.out.println();
		System.out.println("----------ROB dump---------");
		
		if(head == -1)
		{
			return;
		}
		
		//for(int i = head; i <= tail; i = (i+1)%MaxROBSize)
		int i = head;
		while(true)
		{
			e = ROB[i];
			System.out.println(e.getOperand1PhyReg1() + " ; " + e.getOperand1PhyReg2() + " ; "
					+ e.getOperand2PhyReg1() + " ; "+ e.getOperand2PhyReg2() + " ; " + 
					e.getPhysicalDestinationRegister() + " ; " + 
					e.getIssued() + " ; " + 
					e.getFUInstance() + " ; " + e.getExecuted());
			if(e.getAssociatedIWEntry() != null)
			{
				System.out.println(e.isOperand1Available
						 + " ; " + e.isOperand2Available);
			}
			if(e.getInstruction().getSourceOperand2().getOperandType() == OperandType.integerRegister)
			{
				//System.out.println(core.getExecEngine().getIntegerRenameTable().getValueValid(e.getOperand2PhyReg()));
			}
			System.out.println(e.getInstruction().toString());
			
			if(i == tail)
			{
				break;
			}
			i = (i+1)%MaxROBSize;
		}
		System.out.println();
	}
	
	/*
	public int indexOf(ReorderBufferEntry reorderBufferEntry)
	{
		//for(int i = head; i <= tail; i = (i+1)%MaxROBSize)
		if(head == -1)
		{
			return -1;
		}
		
		int i = head;
		while(true)
		{
			if(ROB[i] == reorderBufferEntry)
			{
				if(i - head >= 0)
				{
					return (i - head);
				}
				else
				{
					return (i - head + MaxROBSize);
				}
			}
			if(i == tail)
			{
				break;
			}
			i = (i+1)%MaxROBSize;
		}
		System.out.println("index of non-existent ROB entry requested!");
		return -1;
	}
	*/
	
	public int indexOf(ReorderBufferEntry reorderBufferEntry)
	{
		if(reorderBufferEntry.pos - head >= 0)
		{
			return (reorderBufferEntry.pos - head);
		}
		else
		{
			return (reorderBufferEntry.pos - head + MaxROBSize);
		}
	}
	
	public void performCommitsForPerfectPipeline()
	{	
		int tieBreaker = 0;
		while(true)
		{
			if(head == -1)
			{
				if(core.getExecEngine().isAllPipesEmpty() == true)
				{
					//if ROB is empty, and decode pipe is empty, that means execution is complete
					core.getExecEngine().setExecutionComplete(true);
				}
				break;
			}
			
			ReorderBufferEntry first = ROB[head];
			
//			if(first.isWriteBackDone() == true)
//			{
				//if branch, then if branch prediction correct
//				if(first.getInstruction().getOperationType() != OperationType.branch ||
//						first.getInstruction().getOperationType() == OperationType.branch &&
//						core.getBranchPredictor().predict(first.getInstruction().getProgramCounter())
//							== first.getInstruction().isBranchTaken())		
//				{
					//add to available list
					//update checkpoint
					//note : if values are involved, a checkpoint of
					//       the machine specific register file must also be implemented TODO
					
					//increment number of instructions executed
					core.incrementNoOfInstructionsExecuted();
					
//					if(first.getInstruction().getDestinationOperand() != null)
//					{
//						if(first.getInstruction().getDestinationOperand().getOperandType()
//								== OperandType.integerRegister)
//						{
//							updateIntegerRenameTable(first);
//						}
//						else if(first.getInstruction().getDestinationOperand().getOperandType() == OperandType.floatRegister)
//						{
//							updateFloatRenameTable(first);
//						}
//					}
					
					//System.out.println("committed : " +GlobalClock.getCurrentTime() + first.getInstruction().getOperationType());
					
					//TODO Signal LSQ for committing the Instruction at the queue head
					if(first.getInstruction().getOperationType() == OperationType.load ||
							first.getInstruction().getOperationType() == OperationType.store)
					{
						core.getEventQueue().addEvent(new PortRequestEvent((GlobalClock.getCurrentTime() * 1000) + tieBreaker, //tieBreaker, 
								1, //noOfSlots,
								new LSQCommitEventFromROB(core.getExecEngine().coreMemSys.getLsqueue().getLatencyDelay(),
																			this,
																			core.getExecEngine().coreMemSys.getLsqueue(), 
																			(GlobalClock.getCurrentTime() * 1000) + tieBreaker, //tieBreaker,
																			RequestType.LSQ_COMMIT, 
																			first.getLsqEntry())));
						first.getLsqEntry().setRemoved(true);
					}
					
					ROB[head].setValid(false);
					if(head == tail)
					{
						head = -1;
						tail = -1;
					}
					else
					{
						head = (head+1)%MaxROBSize;
					}
					
//				}
//				else
//				{
//					handleBranchMisprediction();
//				}
				
//				if(first.getInstruction().getOperationType() == OperationType.branch)
//				{
//					core.getBranchPredictor().Train(
//													first.getInstruction().getProgramCounter(),
//													first.getInstruction().isBranchTaken(),
//													core.getBranchPredictor().predict(first.getInstruction().getProgramCounter())
//													);
//				}
//			}
//			else
//			{
//				break;
//			}
			tieBreaker++;
		}
	}

}