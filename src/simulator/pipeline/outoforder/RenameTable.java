package pipeline.outoforder;

import generic.SimulationElement;
import generic.Time_t;

import java.util.LinkedList;

public class RenameTable extends SimulationElement{
	
	int nArchRegisters;
	int nPhyRegisters;
	int noOfThreads;
	
	int[] archReg;
	int[] threadID;							//thread that is going to write to the register
	boolean[] mappingValid;
	boolean[] valueValid;
	private ReorderBufferEntry[] producerROBEntry;
	
	LinkedList <Integer>availableList;
	
	int[][] archToPhyMapping;
	
	RegisterFile associatedRegisterFile;
	RenameTableCheckpoint checkpoint;
	
	public RenameTable(int nArchRegisters, int nPhyRegisters, RegisterFile associatedRegisterFile,
						int noOfThreads)
	{
		super(1, new Time_t(-1), new Time_t(-1), -1);
		this.nArchRegisters = nArchRegisters;
		this.nPhyRegisters = nPhyRegisters;
		this.noOfThreads = noOfThreads;
		this.associatedRegisterFile = associatedRegisterFile;
		
		archReg = new int[this.nPhyRegisters];
		threadID = new int[this.nPhyRegisters];
		mappingValid = new boolean[this.nPhyRegisters];
		valueValid = new boolean[this.nPhyRegisters];
		producerROBEntry = new ReorderBufferEntry[this.nPhyRegisters];
		checkpoint = new RenameTableCheckpoint(this.noOfThreads, this.nArchRegisters);
		
		if(noOfThreads * this.nArchRegisters > this.nPhyRegisters)
		{
			System.out.println("too many threads, not enough registers!");
			System.exit(1);
		}
		
		int temp;
		for(int j = 0; j < noOfThreads; j++)
		{
			temp = j * this.nArchRegisters;
			for(int i = 0; i < this.nArchRegisters; i++)
			{
				archReg[temp + i] = i;
				threadID[temp + i] = j;
				mappingValid[temp + i] = true;
				valueValid[temp + i] = true;
				producerROBEntry[temp + i] = null;
			}
		}
		
		for(int i = this.nArchRegisters * this.noOfThreads; i < this.nPhyRegisters; i++)
		{
			archReg[i] = -1;
			threadID[i] = -1;
			mappingValid[i] = false;
			valueValid[i] = false;
			producerROBEntry[i] = null;
		}
		
		availableList = new LinkedList<Integer>();
		for(int i = this.nArchRegisters * this.noOfThreads; i < this.nPhyRegisters; i++)
		{
			availableList.addLast(i);
		}
		
		archToPhyMapping = new int[this.noOfThreads][this.nArchRegisters];
		for(int j = 0; j < this.noOfThreads; j++)
		{
			temp = j * this.nArchRegisters;
			for(int i = 0; i < this.nArchRegisters; i++)
			{
				archToPhyMapping[j][i] = temp + i;
			}
		}
		/*
		for(int i = 0; i < this.nPhyRegisters; i++)
		{
			System.out.println(archReg[i] + " " + threadID[i] + " " + valueValid[i] + " " + producerROBEntry[i]);
		}
		
		System.out.println("\n\n");
		
		for(int j = 0; j < this.noOfThreads; j++)
		{
			for(int i = 0; i < this.nArchRegisters; i++)
			{
				System.out.println("thread : " + j + "\t\tarch reg : " + i + "\t\t=\t" + archToPhyMapping[j][i] + "\n");
			}
		}
		*/
	}
	
	public int allocatePhysicalRegister(int threadID, int archReg)
	{
		if(availableList.size() <= 0)
		{
			//no free physical registers
			return -1;
		}
		/*
		int curPhyReg = this.archToPhyMapping[threadID][archReg];
		if(valueValid[curPhyReg] == true)
		{
			addToAvailableList(curPhyReg);
		}
		*/
		int newPhyReg = availableList.pollFirst();
		this.archReg[newPhyReg] = archReg;
		this.threadID[newPhyReg] = threadID;
		this.mappingValid[newPhyReg] = true;
		this.valueValid[newPhyReg] = false;
		this.archToPhyMapping[threadID][archReg] = newPhyReg;
		return newPhyReg;
	}

	public int getArchReg(int index) {
		return archReg[index];
	}
	
	public int getThreadID(int index)
	{
		return threadID[index];
	}

	public void setArchReg(int threadID, int archReg, int index) {
		this.archReg[index] = archReg;
		this.archToPhyMapping[threadID][archReg] = index;
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
	
	public int getPhysicalRegister(int threadID, int archReg)
	{
		return archToPhyMapping[threadID][archReg];
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