package pipeline.outoforder_new_arch;

import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;
import generic.Statistics;
//import memorysystem.LSQCommitEventFromROB;

public class ReorderBuffer extends SimulationElement{
	
	private Core core;
	
	private ReorderBufferEntry[] ROB;
	private int MaxROBSize;
	
	int head;
	int tail;
	
	ExecutionEngine execEngine;

	public ReorderBuffer(Core _core, ExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, _core.getEventQueue(), -1, -1);
		core = _core;
		MaxROBSize = core.getReorderBufferSize();
		ROB = new ReorderBufferEntry[MaxROBSize];
		
		this.execEngine = execEngine;
		
		for(int i = 0; i < MaxROBSize; i++)
		{
			ROB[i] = new ReorderBufferEntry(core, i, execEngine);
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
		
		/*if(execEngine.isStallDecode2() == true)
		{
			return;
		}*/
		
		while(true)
		{
			if(head == -1)
			{
				if(execEngine.isAllPipesEmpty() == true)
				{
					//if ROB is empty, and decode pipe is empty, that means execution is complete
					execEngine.setExecutionComplete(true);
					
					setTimingStatistics();			
					setPerCoreMemorySystemStatistics();
				}
				break;
			}
			
			ReorderBufferEntry first = ROB[head];
			Instruction firstInstruction = first.getInstruction();
			OperationType firstOpType = firstInstruction.getOperationType();
			Operand firstDestOpnd = firstInstruction.getDestinationOperand();
			
			if(first.isWriteBackDone() == true)
			{
				//if branch, then if branch prediction correct
				if(firstOpType != OperationType.branch ||
						firstOpType == OperationType.branch &&
						core.getBranchPredictor().predict(first.getInstruction().getProgramCounter())
							== first.getInstruction().isBranchTaken())		
				{
					//add to available list
					//update checkpoint
					//note : if values are involved, a checkpoint of
					//       the machine specific register file must also be implemented TODO
					
					//increment number of instructions executed
					core.incrementNoOfInstructionsExecuted();
					
					if(firstDestOpnd != null)
					{
						OperandType firstDestOpndType = firstDestOpnd.getOperandType();
						if(firstDestOpndType == OperandType.integerRegister)
						{
							updateIntegerRenameTable(first);
						}
						else if(firstDestOpndType == OperandType.floatRegister)
						{
							updateFloatRenameTable(first);
						}
					}

					if(SimulationConfig.debugMode)
					{
						System.out.println("committed : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : " + first.getInstruction());
					}
					
					//Signal LSQ for committing the Instruction at the queue head
					if(firstOpType == OperationType.load || firstOpType == OperationType.store)
					{
						/*TODO
						 execEngine.coreMemSys.getLsqueue().getPort().put(new LSQCommitEventFromROB(execEngine.coreMemSys.getLsqueue().getLatencyDelay(),
																			this,
																			execEngine.coreMemSys.getLsqueue(), 
																			(GlobalClock.getCurrentTime() * 1000) + tieBreaker, //tieBreaker,
																			RequestType.LSQ_COMMIT, 
																			first.getLsqEntry()));
						first.getLsqEntry().setRemoved(true);*/
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
				
				if(firstOpType == OperationType.branch)
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
		RenameTable intRenameTable = execEngine.getIntegerRenameTable();
		int threadID = first.getThreadID();
		int destReg = (int) first.getInstruction().getDestinationOperand().getValue();
		//adding current mapping to available list
		/*int curPhyReg = intRenameTable.getCheckpoint().getMapping(
						threadID,
						destReg);
		if(curPhyReg != first.getPhysicalDestinationRegister())
		{
			intRenameTable.addToAvailableList(curPhyReg);
		}*/
		
		//updating checkpoint
		intRenameTable.getCheckpoint().setMapping(
				threadID,
				first.getPhysicalDestinationRegister(),
				destReg);
	}
	
	void updateFloatRenameTable(ReorderBufferEntry first)
	{
		RenameTable floatRenameTable = execEngine.getFloatingPointRenameTable();
		int threadID = first.getThreadID();
		int destReg = (int) first.getInstruction().getDestinationOperand().getValue();
		//adding current mapping to available list
		/*int curPhyReg = floatRenameTable.getCheckpoint().getMapping(
				threadID,
				destReg);
		if(curPhyReg != first.getPhysicalDestinationRegister())
		{
			floatRenameTable.addToAvailableList(curPhyReg);
		}*/
		
		//updating checkpoint
		floatRenameTable.getCheckpoint().setMapping(
				threadID,
				first.getPhysicalDestinationRegister(),
				destReg);
	}
	
	void handleBranchMisprediction()
	{
		if(SimulationConfig.debugMode)
		{
			System.out.println("branch mispredicted");
		}
		
		//impose branch mis-prediction penalty
		execEngine.setToStall5(true);
		
		//set event to set tostall5 to false
		core.getEventQueue().addEvent(
				new Event(GlobalClock.getCurrentTime() + core.getBranchMispredictionPenalty() * core.getStepSize(),
						null,
						this,
						-1,
						RequestType.MISPRED_PENALTY_COMPLETE,
						null));
		
	}
	
	//TODO checkpoint needs to incorporate threadIDs
	//rollback currently not being used - since no need for rollback
	public void rollBackRenameTables()
	{
		int phyReg;
		RenameTable renameTable;
		
		//integer rename table
		renameTable = execEngine.getIntegerRenameTable();
		for(int i = 0; i < core.getIntegerRegisterFileSize(); i++)
		{
			renameTable.setMappingValid(false, i);
			renameTable.setValueValid(false, i);
			renameTable.setProducerROBEntry(null, i);
		}
		for(int j = 0; j < core.getNo_of_threads(); j++)
		{
			for(int i = 0; i < core.getNIntegerArchitecturalRegisters(); i++)
			{
				phyReg = renameTable.getCheckpoint().getMapping(j, i);
				renameTable.setArchReg(j, i, phyReg);
				renameTable.setMappingValid(true, phyReg);
				renameTable.setValueValid(true, phyReg);
			}
		}
		
		//floating point rename table
		renameTable = execEngine.getFloatingPointRenameTable();
		for(int i = 0; i < core.getFloatingPointRegisterFileSize(); i++)
		{
			renameTable.setMappingValid(false, i);
			renameTable.setValueValid(false, i);
			renameTable.setProducerROBEntry(null, i);
		}
		for(int j = 0; j < core.getNo_of_threads(); j++)
		{
			for(int i = 0; i < core.getNFloatingPointArchitecturalRegisters(); i++)
			{
				phyReg = renameTable.getCheckpoint().getMapping(j, i);
				renameTable.setArchReg(j, i, phyReg);
				renameTable.setMappingValid(true, phyReg);
				renameTable.setValueValid(true, phyReg);
			}
		}
	}
	
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
				if(execEngine.isAllPipesEmpty() == true)
				{
					//if ROB is empty, and decode pipe is empty, that means execution is complete
					execEngine.setExecutionComplete(true);
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
					
					if(first.getInstruction().getOperationType() == OperationType.load ||
							first.getInstruction().getOperationType() == OperationType.store)
					{
						/*TODO
						execEngine.coreMemSys.getLsqueue().getPort().put(new LSQCommitEventFromROB(execEngine.coreMemSys.getLsqueue().getLatencyDelay(),
																			this,
																			execEngine.coreMemSys.getLsqueue(), 
																			(GlobalClock.getCurrentTime() * 1000) + tieBreaker, //tieBreaker,
																			RequestType.LSQ_COMMIT, 
																			first.getLsqEntry()));
						first.getLsqEntry().setRemoved(true);*/
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
	

	
	public void setTimingStatistics()
	{
		Statistics.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		Statistics.setNoOfMemRequests(execEngine.coreMemSys.getLsqueue().noOfMemRequests, core.getCore_number());
		Statistics.setNoOfLoads(execEngine.coreMemSys.getLsqueue().NoOfLd, core.getCore_number());
		Statistics.setNoOfStores(execEngine.coreMemSys.getLsqueue().NoOfSt, core.getCore_number());
		Statistics.setNoOfValueForwards(execEngine.coreMemSys.getLsqueue().NoOfForwards, core.getCore_number());
		Statistics.setNoOfTLBRequests(execEngine.coreMemSys.getTLBuffer().getTlbRequests(), core.getCore_number());
		Statistics.setNoOfTLBHits(execEngine.coreMemSys.getTLBuffer().getTlbHits(), core.getCore_number());
		Statistics.setNoOfTLBMisses(execEngine.coreMemSys.getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Requests(execEngine.coreMemSys.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(execEngine.coreMemSys.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(execEngine.coreMemSys.getL1Cache().misses, core.getCore_number());
	}

	@Override
	public void handleEvent(Event event) {
		
		if(event.getRequestType() == RequestType.MISPRED_PENALTY_COMPLETE)
		{
			completeMispredictionPenalty();
		}
		
	}
	
	void completeMispredictionPenalty()
	{
		execEngine.setToStall5(false);
	}

}