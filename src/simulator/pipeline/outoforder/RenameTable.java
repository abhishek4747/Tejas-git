package pipeline.outoforder;

import generic.SimulationElement;
import generic.Time_t;

import java.util.LinkedList;

public class RenameTable extends SimulationElement{
	
	int nArchRegisters;
	int nPhyRegisters;
	
	int[] archReg;
	boolean[] mappingValid;
	boolean[] valueValid;
	private ReorderBufferEntry[] producerROBEntry;
	
	LinkedList <Integer>availableList;
	
	int[] archToPhyMapping;
	
	RegisterFile associatedRegisterFile;
	RenameTableCheckpoint checkpoint;
	
	public RenameTable(int nArchRegisters, int nPhyRegisters, RegisterFile associatedRegisterFile)
	{
		super(1, new Time_t(-1), new Time_t(-1), -1);
		this.nArchRegisters = nArchRegisters;
		this.nPhyRegisters = nPhyRegisters;
		this.associatedRegisterFile = associatedRegisterFile;
		
		archReg = new int[this.nPhyRegisters];
		mappingValid = new boolean[this.nPhyRegisters];
		valueValid = new boolean[this.nPhyRegisters];
		producerROBEntry = new ReorderBufferEntry[this.nPhyRegisters];
		checkpoint = new RenameTableCheckpoint(this.nArchRegisters);
		
		for(int i = 0; i < this.nArchRegisters; i++)
		{
			archReg[i] = i;
			mappingValid[i] = true;
			valueValid[i] = true;
			producerROBEntry[i] = null;
		}
		
		for(int i = this.nArchRegisters; i < this.nPhyRegisters; i++)
		{
			archReg[i] = -1;
			mappingValid[i] = false;
			valueValid[i] = false;
			producerROBEntry[i] = null;
		}
		
		availableList = new LinkedList<Integer>();
		for(int i = this.nArchRegisters; i < this.nPhyRegisters; i++)
		{
			availableList.addLast(i);
		}
		
		archToPhyMapping = new int[this.nArchRegisters];
		for(int i = 0; i < this.nArchRegisters; i++)
		{
			archToPhyMapping[i] = i;
		}
	}
	
	public int allocatePhysicalRegister(int archReg)
	{
		if(availableList.size() <= 0)
		{
			//no free physical registers
			return -1;
		}		

		int newPhyReg = availableList.pollFirst();
		this.archReg[newPhyReg] = archReg;
		this.mappingValid[newPhyReg] = true;
		this.valueValid[newPhyReg] = false;
		this.archToPhyMapping[archReg] = newPhyReg;
		return newPhyReg;
	}

	public int getArchReg(int index) {
		return archReg[index];
	}

	public void setArchReg(int archReg, int index) {
		this.archReg[index] = archReg;
		this.archToPhyMapping[archReg] = index;
	}

	public boolean getMappingValid(int index) {
		return mappingValid[index];
	}

	public void setMappingValid(boolean mappingValid, int index) {
		this.mappingValid[index] = mappingValid;
	}

	public boolean getValueValid(int index) {
		return valueValid[index];
	}

	public void setValueValid(boolean valueValid, int index) {
		this.valueValid[index] = valueValid;
	}
	
	public int getPhysicalRegister(int archReg)
	{
		return archToPhyMapping[archReg];
	}

	public ReorderBufferEntry getProducerROBEntry(int index) {
		return producerROBEntry[index];
	}

	public void setProducerROBEntry(ReorderBufferEntry producerROBEntry, int index) {
		this.producerROBEntry[index] = producerROBEntry;
	}

	public RenameTableCheckpoint getCheckpoint() {
		return checkpoint;
	}

	public void setCheckpoint(RenameTableCheckpoint checkpoint) {
		this.checkpoint = checkpoint;
	}
	
	public void addToAvailableList(int phyRegNum)
	{
		this.availableList.addLast(phyRegNum);
		setValueValid(false, phyRegNum);
	}
	
}