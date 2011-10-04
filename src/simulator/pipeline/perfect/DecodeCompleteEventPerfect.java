package pipeline.perfect;

import pipeline.outoforder.*;
import memorysystem.LSQ;
import memorysystem.CacheRequestPacket;
import memorysystem.LSQAddressReadyEvent;
import memorysystem.LSQEntry;
import memorysystem.NewCacheAccessEvent;
import memorysystem.LSQEntry.LSQEntryType;
import generic.InstructionLinkedList;
import generic.GlobalClock;
import generic.Event;
import generic.Core;
import generic.Instruction;
import generic.EventQueue;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.RequestType;
import generic.Statistics;
import generic.Time_t;

/**
 * scheduled at the end of decode time
 * ROB entries are made
 * renaming is performed
 * RenameCompleteEvent is scheduled
 * note - decode complete event represents decode-width number of instructions
 */

public class DecodeCompleteEventPerfect extends Event {
	
	Core core;
	int threadID;
	EventQueue eventQueue;
	
	public DecodeCompleteEventPerfect(Core core, int threadID, long eventTime)
	{
		super(new Time_t(eventTime),
				null,
				null,
				0,
				RequestType.DECODE_COMPLETE);
		this.core = core;
		this.threadID = threadID;
	}

	@Override
	public void handleEvent(EventQueue eventQueue)
	{
		this.eventQueue = eventQueue;
		readDecodePipe();
	}
	
	public void readDecodePipe()
	{
		Instruction newInstruction;		
		InstructionLinkedList inputToPipeline = core.getIncomingInstructions(threadID);
		
//		for(int i = 0; i < core.getDecodeWidth(); i++)
		//while (Core.outstandingMemRequests < 200 && !inputToPipeline.isEmpty())
		while (true)
		{
//			if(core.getExecEngine().getReorderBuffer().isFull() == false
//					//&& if head of instructionList is a load/store and LSQ is not full TODO
//					&& core.getExecEngine().getInstructionWindow().isFull() == false
//					&& core.getExecEngine().isStallDecode1() == false)
			{
				if(inputToPipeline.getListSize() == 0)
				{
//					System.out.println("this shouldn't be happening");
					break;
				}
				
				newInstruction = inputToPipeline.peekInstructionAt(0);
				
				if((newInstruction.getOperationType() != OperationType.load &&
					newInstruction.getOperationType() != OperationType.store) ||
					(!this.core.getExecEngine().coreMemSys.getLsqueue().isFull()))
				{
					newInstruction = inputToPipeline.pollFirst();
					
					if(newInstruction != null)
					{
						if(newInstruction.getOperationType() == OperationType.inValid)
						{
//							System.out.println("invallid operation received");
							core.getExecEngine().setDecodePipeEmpty(threadID, true);
							core.getExecEngine().setAllPipesEmpty(true);
							core.getExecEngine().setExecutionComplete(true);
							setTimingStatistics();			
							setPerCoreMemorySystemStatistics();
							break;
						}
						//to detach memory system
						/*if(newInstruction.getOperationType() == OperationType.load ||
								newInstruction.getOperationType() == OperationType.store)
						{
							i--;
							continue;
						}*/
						addLSQEntries(newInstruction);
						Core.outstandingMemRequests++;
					}
					else
					{
						System.out.println("input to pipe is empty");
						break;
					}
				}
				else if (this.core.getExecEngine().coreMemSys.getLsqueue().isFull() && 
						this.core.getExecEngine().coreMemSys.getLsqueue().processROBCommitForPerfectPipeline(this.eventQueue))
					break;
			}
		}
		this.core.getExecEngine().coreMemSys.getLsqueue().processROBCommitForPerfectPipeline(this.eventQueue);
	}
	
	public void addLSQEntries(Instruction newInstruction)
	{
		if(newInstruction != null &&
				newInstruction.getOperationType() != OperationType.nop &&
				newInstruction.getOperationType() != OperationType.inValid)
		{			
//			ReorderBufferEntry newROBEntry = core.getExecEngine()
//				.getReorderBuffer().addInstructionToROB(newInstruction, threadID);
			
			//TODO if load or store, make entry in LSQ
			if(newInstruction.getOperationType() == OperationType.load ||
					newInstruction.getOperationType() == OperationType.store)
			{
				boolean isLoad;
				if (newInstruction.getOperationType() == OperationType.load)
					isLoad = true;
				else
					isLoad = false;
					
//				newROBEntry.setLsqEntry(
				LSQEntry lsqEntry =	this.core.getExecEngine().coreMemSys.getLsqueue().addEntry(isLoad, 
									newInstruction.getSourceOperand1().getValue(), null);
				
				core.getExecEngine().coreMemSys.getLsqueue().getPort().put(new LSQAddressReadyEvent(core.getExecEngine().coreMemSys.getLsqueue().getLatencyDelay(), 
															null, //Requesting Element
															core.getExecEngine().coreMemSys.getLsqueue(), 
															0, //tieBreaker,
															RequestType.TLB_ADDRESS_READY,
															lsqEntry));
				
				
			}
//			System.out.println("I must print");
			//perform renaming			
//			processOperand1(newROBEntry);
//			processOperand2(newROBEntry);			
//			processDestOperand(newROBEntry);
		}
		else
			System.out.println("Printing ends");
	}

	public void setTimingStatistics()
	{
		Statistics.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize(), core.getCore_number());
		Statistics.setCoreFrequencies(core.getFrequency(), core.getCore_number());
		Statistics.setNumCoreInstructions(core.getNoOfInstructionsExecuted(), core.getCore_number());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		Statistics.setNoOfMemRequests(core.getExecEngine().coreMemSys.getLsqueue().noOfMemRequests, core.getCore_number());
		Statistics.setNoOfLoads(core.getExecEngine().coreMemSys.getLsqueue().NoOfLd, core.getCore_number());
		Statistics.setNoOfStores(core.getExecEngine().coreMemSys.getLsqueue().NoOfSt, core.getCore_number());
		Statistics.setNoOfValueForwards(core.getExecEngine().coreMemSys.getLsqueue().NoOfForwards, core.getCore_number());
		Statistics.setNoOfTLBRequests(core.getExecEngine().coreMemSys.getTLBuffer().getTlbRequests(), core.getCore_number());
		Statistics.setNoOfTLBHits(core.getExecEngine().coreMemSys.getTLBuffer().getTlbHits(), core.getCore_number());
		Statistics.setNoOfTLBMisses(core.getExecEngine().coreMemSys.getTLBuffer().getTlbMisses(), core.getCore_number());
		Statistics.setNoOfL1Requests(core.getExecEngine().coreMemSys.getL1Cache().noOfRequests, core.getCore_number());
		Statistics.setNoOfL1Hits(core.getExecEngine().coreMemSys.getL1Cache().hits, core.getCore_number());
		Statistics.setNoOfL1Misses(core.getExecEngine().coreMemSys.getL1Cache().misses, core.getCore_number());
	}
}