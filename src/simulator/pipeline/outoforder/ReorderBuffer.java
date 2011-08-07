package pipeline.outoforder;

import generic.Core;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperandType;
import generic.OperationType;
import generic.SimulationElement;
import generic.Time_t;

import java.util.LinkedList;

public class ReorderBuffer extends SimulationElement{
	
	private Core core;
	
	private LinkedList<ReorderBufferEntry> ROB;
	private int MaxROBSize;

	public ReorderBuffer(Core _core)
	{
		super(1, new Time_t(-1), new Time_t(-1));
		ROB = new LinkedList<ReorderBufferEntry>();
		core = _core;
		MaxROBSize = core.getReorderBufferSize();
	}
	
	public boolean isFull()
	{
		if(ROB.size() >= MaxROBSize)
		{
			return true;
		}
		return false;
	}
	
	public LinkedList<ReorderBufferEntry> getROB()
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
	public ReorderBufferEntry addInstructionToROB(Instruction newInstruction)
	{
		if(!isFull())
		{
			ReorderBufferEntry newReorderBufferEntry = new ReorderBufferEntry(core, newInstruction);
			ROB.addLast(newReorderBufferEntry);
			
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
		while(true)
		{
			if(ROB.size() <= 0)
			{
				if(core.getExecEngine().isDecodePipeEmpty() == true)
				{
					//if ROB is empty, and decode pipe is empty, that means execution is complete
					core.getExecEngine().setExecutionComplete(true);
				}
				break;
			}
			
			ReorderBufferEntry first = ROB.getFirst();
			
			if(first.getExecuted() == true)
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
					
					ROB.removeFirst();
					
					//System.out.println("commited : " + first.getInstruction());
					
					//signal LSQ
				}
				else
				{
					System.out.println("branch mispredicted");
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
		}
	}
	
	void updateIntegerRenameTable(ReorderBufferEntry first)
	{
		//adding current mapping to available list
		int curPhyReg = core.getExecEngine().getIntegerRenameTable()
				.getCheckpoint().getMapping((int) first.getInstruction()
				.getDestinationOperand().getValue());
		if(curPhyReg != first.getPhysicalDestinationRegister())
		{
			core.getExecEngine().getIntegerRenameTable().addToAvailableList(curPhyReg);
		}
		
		//updating checkpoint
		core.getExecEngine().getIntegerRenameTable().getCheckpoint().
			setMapping(first.getPhysicalDestinationRegister(),
			(int) first.getInstruction().getDestinationOperand().getValue());
	}
	
	void updateFloatRenameTable(ReorderBufferEntry first)
	{
		//adding current mapping to available list
		int curPhyReg = core.getExecEngine().getFloatingPointRenameTable()
				.getCheckpoint().getMapping((int) first.getInstruction()
				.getDestinationOperand().getValue());
		if(curPhyReg != first.getPhysicalDestinationRegister())
		{
			core.getExecEngine().getFloatingPointRenameTable().addToAvailableList(curPhyReg);
		}
		
		//updating checkpoint
		core.getExecEngine().getFloatingPointRenameTable().getCheckpoint().setMapping(
			first.getPhysicalDestinationRegister(),
			(int) first.getInstruction().getDestinationOperand().getValue());
	}
	
	void handleBranchMisprediction()
	{
		//remove all entries from ROB				
		ROB.removeAll(null);
		
		//remove all entries from Instruction Window
		core.getExecEngine().getInstructionWindow().flush();
		
		//remove all entries from LSQ
		
		
		//roll back rename tables
		rollBackRenameTables();
		
		//impose branch misprediction penalty
		core.getExecEngine().setStallDecode2(true);
		core.getEventQueue().addEvent(
				new MispredictionPenaltyCompleteEvent(
						GlobalClock.getCurrentTime() + core.getBranchMispredictionPenalty(),
						core)
				);
	}
	
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
		for(int i = 0; i < core.getNIntegerArchitecturalRegisters(); i++)
		{
			phyReg = core.getExecEngine().getIntegerRenameTable().getCheckpoint().getMapping(i);
			core.getExecEngine().getIntegerRenameTable().setArchReg(i, phyReg);
			core.getExecEngine().getIntegerRenameTable().setMappingValid(true, phyReg);
			core.getExecEngine().getIntegerRenameTable().setValueValid(true, phyReg);
		}
		
		//floating point rename table
		for(int i = 0; i < core.getFloatingPointRegisterFileSize(); i++)
		{
			core.getExecEngine().getFloatingPointRenameTable().setMappingValid(false, i);
			core.getExecEngine().getFloatingPointRenameTable().setValueValid(false, i);
			core.getExecEngine().getFloatingPointRenameTable().setProducerROBEntry(null, i);
		}
		for(int i = 0; i < core.getNFloatingPointArchitecturalRegisters(); i++)
		{
			phyReg = core.getExecEngine().getFloatingPointRenameTable().getCheckpoint().getMapping(i);
			core.getExecEngine().getFloatingPointRenameTable().setArchReg(i, phyReg);
			core.getExecEngine().getFloatingPointRenameTable().setMappingValid(true, phyReg);
			core.getExecEngine().getFloatingPointRenameTable().setValueValid(true, phyReg);
		}
	}
	
	public void removeInstructionFromROB(ReorderBufferEntry _ROBEntry)
	{
		RenameTable tempRN = null;
		RegisterFile tempRF = null;
		
		if(_ROBEntry.getInstruction().getDestinationOperand().getOperandType() == OperandType.machineSpecificRegister)
		{
			tempRF = core.getExecEngine().getMachineSpecificRegisterFile();
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
	
	public void dump()
	{
		int i = 0;
		ReorderBufferEntry e;
		
		System.out.println();
		System.out.println();
		System.out.println("----------ROB dump---------");
		
		while(i < ROB.size())
		{
			e = ROB.get(i);
			System.out.println(e.getOperand1PhyReg1() + " ; " + e.getOperand1PhyReg2() + " ; "
					+ e.getOperand2PhyReg1() + " ; "+ e.getOperand2PhyReg2() + " ; " + 
					e.getPhysicalDestinationRegister() + " ; " + 
					e.getIssued() + " ; " + 
					e.getFUInstance() + " ; " + e.getExecuted());
			if(e.getAssociatedIWEntry() != null)
			{
				System.out.println(e.getAssociatedIWEntry().isOperand1Available
						 + " ; " + e.getAssociatedIWEntry().isOperand2Available);
			}
			if(e.getInstruction().getSourceOperand2().getOperandType() == OperandType.integerRegister)
			{
				//System.out.println(core.getExecEngine().getIntegerRenameTable().getValueValid(e.getOperand2PhyReg()));
			}
			System.out.println(e.getInstruction().toString());
			i++;
		}
		System.out.println();
	}

}