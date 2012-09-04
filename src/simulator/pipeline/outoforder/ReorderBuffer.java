package pipeline.outoforder;

import memorysystem.MemorySystem;
import config.SimulationConfig;
import emulatorinterface.Newmain;
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

public class ReorderBuffer extends SimulationElement{
	
	private Core core;
	
	ReorderBufferEntry[] ROB;
	int MaxROBSize;
	
	int head;
	int tail;
	
	int retireWidth;
	
	OutOrderExecutionEngine execEngine;
	
	int stall1Count;
	int stall2Count;
	int stall3Count;
	int stall4Count;
	int stall5Count;
	long branchCount;
	long mispredCount;

	private int j;
	
	int invalidCount;

	public ReorderBuffer(Core _core, OutOrderExecutionEngine execEngine)
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
		
		stall1Count = 0;
		stall2Count = 0;
		stall3Count = 0;
		stall4Count = 0;
		stall5Count = 0;
		mispredCount = 0;
		branchCount = 0;
		
		retireWidth = core.getRetireWidth();
		j=0;
		
		invalidCount = 0;
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
			
			if(newReorderBufferEntry.isValid == true)
			{
				System.out.println("new rob entry is alread valid");
			}
			
			newReorderBufferEntry.setInstruction(newInstruction);
			newReorderBufferEntry.setThreadID(threadID);
			newReorderBufferEntry.setOperand1PhyReg1(-1);
			newReorderBufferEntry.setOperand1PhyReg2(-1);
			newReorderBufferEntry.setOperand2PhyReg1(-1);
			newReorderBufferEntry.setOperand2PhyReg2(-1);
			newReorderBufferEntry.setPhysicalDestinationRegister(-1);
			newReorderBufferEntry.setRenameDone(false);
			newReorderBufferEntry.setOperand11Available(false);
			newReorderBufferEntry.setOperand12Available(false);
			newReorderBufferEntry.setOperand1Available(false);
			newReorderBufferEntry.setOperand21Available(false);
			newReorderBufferEntry.setOperand22Available(false);
			newReorderBufferEntry.setOperand2Available(false);
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
		if(execEngine.isToStall1())
		{
			stall1Count++;
		}
		if(execEngine.isToStall2())
		{
			stall2Count++;
		}
		if(execEngine.isToStall3())
		{
			stall3Count++;
		}
		if(execEngine.isToStall4())
		{
			stall4Count++;
		}
		if(execEngine.isToStall5())
		{
			stall5Count++;
		}
		
		int tieBreaker = 0;
		boolean anyMispredictedBranch = false;
		
		/*if(execEngine.isStallDecode2() == true)
		{
			return;
		}*/
		
		if(execEngine.isToStall5() == false)
		{
			for(int no_insts = 0; no_insts < retireWidth; no_insts++)
			{
				/*if(head == -1)
				{
					if(execEngine.isAllPipesEmpty() == true)
					{
						//if ROB is empty, and decode pipe is empty, that means execution is complete
						this.core.currentThreads--;
						
						if(this.core.currentThreads == 0){   //set exec complete only if there are n other thread already 
															  //assigned to this pipeline	
							execEngine.setExecutionComplete(true);
						}
						
						setTimingStatistics();			
						setPerCoreMemorySystemStatistics();
						setPerCorePowerStatistics();
					}
					break;
				}*/
				
				if(head == -1)
				{
					//ROB empty .. does not mean execution has completed
					return;
				}
				
				ReorderBufferEntry first = ROB[head];
				Instruction firstInstruction = first.getInstruction();
				OperationType firstOpType = firstInstruction.getOperationType();
				Operand firstDestOpnd = firstInstruction.getDestinationOperand();								
				
				if(first.isWriteBackDone() == true)
				{					
					if(firstOpType==OperationType.inValid)
					{
						//FIXME the following does not set the statistics. Check!
						this.core.currentThreads--;
						System.out.println("num of invalids that reached head of ROB - core " + core.getCore_number() + " = " + ++invalidCount);
						System.out.println("head = " + head);					
						
						if(this.core.currentThreads == 0){   //set exec complete only if there are n other thread already 
															  //assigned to this pipeline	
							execEngine.setExecutionComplete(true);
							System.out.println("DONE!! core : " + core.getCore_number() + "  - WB = " + first.isWriteBackDone());
							
							
						}
//						System.out.println( " core " + core.getCore_number() +  " finished execution  current threads " + this.core.currentThreads);
						setTimingStatistics();			
						setPerCoreMemorySystemStatistics();
						setPerCorePowerStatistics();
						//memWbLatch.clear();
						
						if(this.core.currentThreads < 0)
						{
							this.core.currentThreads=0;
							System.out.println("num threads < 0");
						}
					}
					
					//if store, and if store not yet validated
					if(firstOpType == OperationType.store && !first.lsqEntry.isValid())
					{
						break;
					}
					
					boolean branchPredictedCorrectly=false;
					if(firstOpType == OperationType.branch){
						this.core.powerCounters.incrementBpredAccess(1);
						if((core.getBranchPredictor().predict(first.getInstruction().getRISCProgramCounter())
								== first.getInstruction().isBranchTaken()))
							branchPredictedCorrectly=true;
					}
					if(firstOpType != OperationType.branch || branchPredictedCorrectly)		
					{
						//if branch, then if branch prediction correct
					}
					
					else
					{
						anyMispredictedBranch = true;
						mispredCount++;
					}
					
					//add to available list
					//update checkpoint
					//note : if values are involved, a checkpoint of
					//       the machine specific register file must also be implemented TODO
					
					//increment number of instructions executed
					core.incrementNoOfInstructionsExecuted();
					if(core.getNoOfInstructionsExecuted()%1000000==0){
						System.out.println(this.j++ + " million done on " + core.getCore_number());
					}
					//System.out.println("number of commits = " + core.getNoOfInstructionsExecuted());
					
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
//						 execEngine.coreMemSys.getLsqueue().getPort().put(
//								 new LSQEntryContainingEvent(
//										core.getEventQueue(),
//										execEngine.coreMemSys.getLsqueue().getLatencyDelay(),
//										this,
//										execEngine.coreMemSys.getLsqueue(), 
//										RequestType.LSQ_Commit, 
//										first.getLsqEntry()));
						if (!first.lsqEntry.isValid())
							System.out.println("The committed entry is not valid");
						//System.out.println("commiting robentry : " + first.getLsqEntry().getIndexInQ());
						execEngine.getCoreMemorySystem().issueLSQCommit(first);
						//first.getLsqEntry().setRemoved(true);
					}
					
					retireInstructionAtHead();
					
					if(firstOpType == OperationType.branch)
					{
						core.getBranchPredictor().Train(
														firstInstruction.getRISCProgramCounter(),
														firstInstruction.isBranchTaken(),
														core.getBranchPredictor().predict(firstInstruction.getRISCProgramCounter())
														);
						
						branchCount++;
						this.core.powerCounters.incrementBpredAccess(1);
					}
					
					if(firstInstruction.getOperationType() != OperationType.inValid)
					{
						returnInstructionToPool(firstInstruction);
					}
					
					if(execEngine.isExecutionComplete() == true)
					{
						if(((OutOrderExecutionEngine)core.getExecEngine()).getFetcher().inputToPipeline[0].getListSize() > 0)
						{
							System.out.println("input to pipeline not empty!!");
						}
						
						for(int i = 0; i < 11; i++)
						{
							System.out.print(Newmain.cores[i].getExecEngine().isExecutionComplete() + " ");
							System.out.print(Newmain.cores[i].currentThreads + " ");
							System.out.print(((OutOrderExecutionEngine)Newmain.cores[i].getExecEngine()).getReorderBuffer().head + " ");
							System.out.print(((OutOrderExecutionEngine)Newmain.cores[i].getExecEngine()).getFetcher().inputToPipeline[0].getListSize() + " ");
						}
					}
				}
				else
				{
					break;
				}
				tieBreaker++;
			}
		}
		
		if(anyMispredictedBranch)
		{
			handleBranchMisprediction();
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
				new MispredictionPenaltyCompleteEvent(
						GlobalClock.getCurrentTime() + core.getBranchMispredictionPenalty() * core.getStepSize(),
						null,
						this,
						RequestType.MISPRED_PENALTY_COMPLETE));
		
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
	
	void retireInstructionAtHead()
	{
		ROB[head].setValid(false);
		if(ROB[head].instruction.getOperationType() == OperationType.inValid && core.getCore_number() == 2)
		{
			System.out.println("retiring invalid");
		}
		ROB[head].instruction = null;
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
	
	void returnInstructionToPool(Instruction instruction)
	{
		try {
			Newmain.instructionPool.returnObject(instruction);
		} catch (Exception e) {
			e.printStackTrace();
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
	
	public void setTimingStatistics()
	{
		core.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize());
		Statistics.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
		Statistics.setBranchCount(branchCount, core.getCore_number());
		Statistics.setMispredictedBranchCount(mispredCount, core.getCore_number());
		
		System.out.println(core.getCore_number());
		System.out.println(core.getCore_number()+"IW full : " + stall1Count);
		System.out.println(core.getCore_number()+"phy reg unavailable : " + stall2Count);
		System.out.println(core.getCore_number()+"LSQ full : " + stall3Count);
		System.out.println(core.getCore_number()+"ROB full : " + stall4Count);
		System.out.println(core.getCore_number()+"branch mispredicted : " + stall5Count);
		System.out.println(core.getCore_number()+"Instruction Mem Stall : " + core.getExecEngine().getInstructionMemStall());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		Statistics.setNoOfMemRequests(execEngine.getCoreMemorySystem().getLsqueue().noOfMemRequests, core.getCore_number());
		Statistics.setNoOfLoads(execEngine.getCoreMemorySystem().getLsqueue().NoOfLd, core.getCore_number());
		Statistics.setNoOfStores(execEngine.getCoreMemorySystem().getLsqueue().NoOfSt, core.getCore_number());
		Statistics.setNoOfValueForwards(execEngine.getCoreMemorySystem().getLsqueue().NoOfForwards, core.getCore_number());
		Statistics.setNoOfTLBRequests(execEngine.getCoreMemorySystem().getTLBuffer().getTlbRequests(), core.getCore_number());
		Statistics.setNoOfTLBHits(execEngine.getCoreMemorySystem().getTLBuffer().getTlbHits(), core.getCore_number());
		Statistics.setNoOfTLBMisses(execEngine.getCoreMemorySystem().getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Requests(execEngine.getCoreMemorySystem().getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(execEngine.getCoreMemorySystem().getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(execEngine.getCoreMemorySystem().getL1Cache().misses, core.getCore_number());
		Statistics.setNoOfIRequests(execEngine.getCoreMemorySystem().getiCache().noOfRequests, core.getCore_number());
		Statistics.setNoOfIHits(execEngine.getCoreMemorySystem().getiCache().hits, core.getCore_number());
		Statistics.setNoOfIMisses(execEngine.getCoreMemorySystem().getiCache().misses, core.getCore_number());
		Statistics.setNoOfDirHits(MemorySystem.getDirectoryCache().hits);
		Statistics.setNoOfDirMisses(MemorySystem.getDirectoryCache().misses);
		Statistics.setNoOfDirInvalidations(MemorySystem.getDirectoryCache().getInvalidations());
		Statistics.setNoOfDirDataForwards(MemorySystem.getDirectoryCache().getDataForwards());
		Statistics.setNoOfDirWritebacks(MemorySystem.getDirectoryCache().getWritebacks());

	}

	public void setPerCorePowerStatistics(){
		//Clear access stats so that all counts can be transferred to total counts  
		core.powerCounters.clearAccessStats();
		core.powerCounters.updatePowerAfterCompletion(core.getCoreCyclesTaken());
		Statistics.setPerCorePowerStatistics(core.powerCounters, core.getCore_number());
	}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		if(event.getRequestType() == RequestType.MISPRED_PENALTY_COMPLETE)
		{
			completeMispredictionPenalty();
		}
		
	}
	
	void completeMispredictionPenalty()
	{
		execEngine.setToStall5(false);
	}

	public int getStall1Count() {
		return stall1Count;
	}

	public int getStall2Count() {
		return stall2Count;
	}

	public int getStall3Count() {
		return stall3Count;
	}

	public int getStall4Count() {
		return stall4Count;
	}

	public int getStall5Count() {
		return stall5Count;
	}

	public long getBranchCount() {
		return branchCount;
	}

	public long getMispredCount() {
		return mispredCount;
	}

}